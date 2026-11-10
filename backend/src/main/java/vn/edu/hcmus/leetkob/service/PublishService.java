package vn.edu.hcmus.leetkob.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import vn.edu.hcmus.leetkob.domain.LabRecord;
import vn.edu.hcmus.leetkob.dto.PublicationReceipt;
import vn.edu.hcmus.leetkob.dto.PublishResult;
import vn.edu.hcmus.leetkob.exception.ProblemException;
import vn.edu.hcmus.leetkob.exception.ProblemKind;
import vn.edu.hcmus.leetkob.repository.LabPublishRepository;
import vn.edu.hcmus.leetkob.security.Principal;

/** Business service orchestrating atomic lab mark publication and idempotency guarantees. */
@Service
public class PublishService {

  private static final DateTimeFormatter ISO_MILLIS =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

  private final LabPublishRepository labPublishRepository;
  private final TransactionTemplate transactionTemplate;
  private final ObjectMapper objectMapper;
  private final ReentrantLock writeLock = new ReentrantLock(true);
  private volatile Runnable beforeTransactionalAccessHook;
  private volatile boolean simulateAuditFailure = false;

  public PublishService(
      LabPublishRepository labPublishRepository,
      TransactionTemplate transactionTemplate,
      ObjectMapper objectMapper) {
    this.labPublishRepository = labPublishRepository;
    this.transactionTemplate = transactionTemplate;
    this.objectMapper = objectMapper;
  }

  public void setBeforeTransactionalAccessHook(Runnable hook) {
    this.beforeTransactionalAccessHook = hook;
  }

  public void setSimulateAuditFailure(boolean simulateAuditFailure) {
    this.simulateAuditFailure = simulateAuditFailure;
  }

  public ReentrantLock getWriteLock() {
    return writeLock;
  }

  public void assertAccess(int labId, Principal principal) {
    requireOwnedLab(labId, principal);
    Runnable hook = this.beforeTransactionalAccessHook;
    if (hook != null) {
      hook.run();
    }
  }

  private LabRecord requireOwnedLab(int labId, Principal principal) {
    return labPublishRepository
        .findOwnedLab(labId, principal.id())
        .orElseThrow(() -> new ProblemException(ProblemKind.NOT_FOUND));
  }

  public PublishResult publish(int labId, Principal principal, Instant now) {
    if (!writeLock.tryLock()) {
      boolean acquired;
      try {
        acquired = writeLock.tryLock(250, TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new ProblemException(ProblemKind.PUBLICATION_UNAVAILABLE);
      }
      if (!acquired) {
        throw new ProblemException(ProblemKind.PUBLICATION_UNAVAILABLE);
      }
    }
    try {
      return transactionTemplate.execute(
          status -> executePublishInTransaction(labId, principal, now));
    } catch (ProblemException e) {
      throw e;
    } catch (DataAccessException e) {
      throw new ProblemException(ProblemKind.INTERNAL);
    } finally {
      writeLock.unlock();
    }
  }

  private PublishResult executePublishInTransaction(int labId, Principal principal, Instant now) {
    LabRecord lab = requireOwnedLab(labId, principal);

    Optional<String> existingReceipt = labPublishRepository.findExistingReceipt(labId);
    if (existingReceipt.isPresent()) {
      return new PublishResult(existingReceipt.get(), true);
    }

    Instant deadline;
    try {
      deadline = Instant.parse(lab.deadline());
    } catch (DateTimeParseException e) {
      throw new ProblemException(ProblemKind.INTERNAL);
    }

    if (now.isBefore(deadline)) {
      throw new ProblemException(ProblemKind.DEADLINE_NOT_REACHED);
    }

    if (labPublishRepository.countPendingJudgeSubmissions(labId) > 0) {
      throw new ProblemException(ProblemKind.JUDGING_PENDING);
    }

    if (labPublishRepository.countPendingReviewItems(labId) > 0) {
      throw new ProblemException(ProblemKind.REVIEWS_PENDING);
    }

    int count = labPublishRepository.countCountedSubmissions(labId);
    PublicationReceipt receipt =
        new PublicationReceipt(labId, "published", ISO_MILLIS.format(now), principal.id(), count);

    String receiptJson;
    try {
      receiptJson = objectMapper.writeValueAsString(receipt);
    } catch (JsonProcessingException e) {
      throw new ProblemException(ProblemKind.INTERNAL);
    }

    labPublishRepository.insertPublicationReceipt(labId, receiptJson);
    labPublishRepository.copyCountedMarksToPublished(labId);

    if (simulateAuditFailure) {
      throw new DataIntegrityViolationException(
          "secret-fixture /private.db SQL audit insert failure");
    }

    labPublishRepository.insertPublicationAudit(labId, principal.id(), receipt.publishedAt());

    return new PublishResult(receiptJson, false);
  }
}
