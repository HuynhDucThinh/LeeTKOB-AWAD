package vn.edu.hcmus.leetkob.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.SQLTransientException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.TransactionTemplate;
import vn.edu.hcmus.leetkob.domain.LabRecord;
import vn.edu.hcmus.leetkob.dto.PublicationReceipt;
import vn.edu.hcmus.leetkob.dto.PublishResult;
import vn.edu.hcmus.leetkob.exception.ProblemException;
import vn.edu.hcmus.leetkob.exception.ProblemKind;
import vn.edu.hcmus.leetkob.repository.LabPublishRepository;
import vn.edu.hcmus.leetkob.security.Principal;

/**
 * Business service orchestrating atomic lab mark publication and idempotency guarantees.
 *
 * <p>Serialisation is done by the database, not by the JVM (AI-LOG A11): every publish transaction
 * takes a row lock on its own {@code labs} row ({@code SELECT ... FOR UPDATE}). Requests for
 * different labs never wait on each other, and when the lock cannot be acquired within the database
 * lock timeout the caller receives {@code 503 publication-unavailable} (contract §2). The primary
 * key on {@code publications(lab_id)} is the final idempotency guard.
 */
@Service
public class PublishService {

  private static final DateTimeFormatter ISO_MILLIS =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

  private final LabPublishRepository labPublishRepository;
  private final TransactionTemplate transactionTemplate;
  private final ObjectMapper objectMapper;

  public PublishService(
      LabPublishRepository labPublishRepository,
      TransactionTemplate transactionTemplate,
      ObjectMapper objectMapper) {
    this.labPublishRepository = labPublishRepository;
    this.transactionTemplate = transactionTemplate;
    this.objectMapper = objectMapper;
  }

  /** Ownership check performed before the request body is read (contract §4.2 step 3). */
  public void assertAccess(int labId, Principal principal) {
    requireOwnedLab(labId, principal);
  }

  private LabRecord requireOwnedLab(int labId, Principal principal) {
    return labPublishRepository
        .findOwnedLab(labId, principal.id())
        .orElseThrow(() -> new ProblemException(ProblemKind.NOT_FOUND));
  }

  public PublishResult publish(int labId, Principal principal, Instant now) {
    try {
      return runInTransaction(labId, principal, now);
    } catch (ProblemException e) {
      throw e;
    } catch (RuntimeException e) {
      if (hasCause(e, DuplicateKeyException.class)) {
        // Another node committed the first publication between our replay check and our insert.
        // Run once more: this time the transaction takes the replay branch.
        try {
          return runInTransaction(labId, principal, now);
        } catch (ProblemException retryProblem) {
          throw retryProblem;
        } catch (RuntimeException retryFailure) {
          throw translate(retryFailure);
        }
      }
      throw translate(e);
    }
  }

  private PublishResult runInTransaction(int labId, Principal principal, Instant now) {
    return transactionTemplate.execute(
        status -> executePublishInTransaction(labId, principal, now));
  }

  /**
   * Lock wait timeouts become {@code 503 publication-unavailable}; anything else is {@code 500}.
   * The whole cause chain is inspected because the timeout may arrive wrapped (see {@link
   * #hasCause}).
   */
  private static ProblemException translate(RuntimeException e) {
    if (hasCause(e, PessimisticLockingFailureException.class)
        || hasCause(e, QueryTimeoutException.class)
        || hasCause(e, SQLTransientException.class)) {
      return new ProblemException(ProblemKind.PUBLICATION_UNAVAILABLE);
    }
    return new ProblemException(ProblemKind.INTERNAL);
  }

  private static boolean hasCause(Throwable error, Class<? extends Throwable> type) {
    for (Throwable t = error; t != null; t = t.getCause() == t ? null : t.getCause()) {
      if (type.isInstance(t)) {
        return true;
      }
      // HikariCP evicts a connection after an SQLTimeoutException, so the rollback that follows a
      // lock timeout fails and Spring reports TransactionSystemException; the original error is
      // kept as its application exception.
      if (t instanceof TransactionSystemException tse
          && tse.getApplicationException() != null
          && hasCause(tse.getApplicationException(), type)) {
        return true;
      }
    }
    return false;
  }

  private PublishResult executePublishInTransaction(int labId, Principal principal, Instant now) {
    labPublishRepository.lockLabRow(labId);
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
    labPublishRepository.insertPublicationAudit(labId, principal.id(), receipt.publishedAt());

    return new PublishResult(receiptJson, false);
  }
}
