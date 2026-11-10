package vn.edu.hcmus.leetkob.publish;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import vn.edu.hcmus.leetkob.auth.Principal;

@Service
public class PublishService {

  private static final DateTimeFormatter ISO_MILLIS =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

  private final JdbcTemplate jdbcTemplate;
  private final TransactionTemplate transactionTemplate;
  private final ObjectMapper objectMapper;
  private final ReentrantLock writeLock = new ReentrantLock(true);
  private volatile Runnable beforeTransactionalAccessHook;
  private volatile boolean simulateAuditFailure = false;

  public PublishService(
      JdbcTemplate jdbcTemplate,
      TransactionTemplate transactionTemplate,
      ObjectMapper objectMapper) {
    this.jdbcTemplate = jdbcTemplate;
    this.transactionTemplate = transactionTemplate;
    this.objectMapper = objectMapper;
  }

  private record LabRow(int id, String deadline) {}

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
    ownedLab(labId, principal);
    Runnable hook = this.beforeTransactionalAccessHook;
    if (hook != null) {
      hook.run();
    }
  }

  private LabRow ownedLab(int labId, Principal principal) {
    List<LabRow> rows =
        jdbcTemplate.query(
            """
            SELECT l.id, l.deadline
            FROM labs l
            JOIN ta_classes t ON t.class_id = l.class_id
            WHERE l.id = ? AND t.ta_id = ?
            """,
            (rs, rowNum) -> new LabRow(rs.getInt("id"), rs.getString("deadline")),
            labId,
            principal.id());
    if (rows.isEmpty()) {
      throw new ProblemException(ProblemKind.NOT_FOUND);
    }
    return rows.getFirst();
  }

  public PublishResult publish(int labId, Principal principal, Instant now) {
    if (!writeLock.tryLock()) {
      boolean acquired;
      try {
        acquired = writeLock.tryLock(250, java.util.concurrent.TimeUnit.MILLISECONDS);
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
    LabRow lab = ownedLab(labId, principal);

    List<String> existingReceipts =
        jdbcTemplate.query(
            "SELECT receipt FROM publications WHERE lab_id = ?",
            (rs, rowNum) -> rs.getString("receipt"),
            labId);
    if (!existingReceipts.isEmpty()) {
      return new PublishResult(existingReceipts.getFirst(), true);
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

    Integer pendingJudge =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*) FROM counted_submissions
            WHERE lab_id = ? AND judge_status IN ('queued', 'running')
            """,
            Integer.class,
            labId);
    if (pendingJudge != null && pendingJudge > 0) {
      throw new ProblemException(ProblemKind.JUDGING_PENDING);
    }

    Integer pendingReview =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*) FROM review_items r
            JOIN counted_submissions s ON s.id = r.submission_id
            WHERE s.lab_id = ? AND r.decision IS NULL
            """,
            Integer.class,
            labId);
    if (pendingReview != null && pendingReview > 0) {
      throw new ProblemException(ProblemKind.REVIEWS_PENDING);
    }

    Integer submissionCount =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM counted_submissions WHERE lab_id = ?", Integer.class, labId);
    int count = submissionCount != null ? submissionCount : 0;

    PublicationReceipt receipt =
        new PublicationReceipt(labId, "published", ISO_MILLIS.format(now), principal.id(), count);

    String receiptJson;
    try {
      receiptJson = objectMapper.writeValueAsString(receipt);
    } catch (JsonProcessingException e) {
      throw new ProblemException(ProblemKind.INTERNAL);
    }

    jdbcTemplate.update(
        "INSERT INTO publications (lab_id, receipt) VALUES (?, ?)", labId, receiptJson);
    jdbcTemplate.update(
        """
            INSERT INTO published_marks (lab_id, submission_id, score_tenths)
            SELECT lab_id, id, score_tenths FROM counted_submissions WHERE lab_id = ?
            """,
        labId);

    if (simulateAuditFailure) {
      throw new org.springframework.dao.DataIntegrityViolationException(
          "secret-fixture /private.db SQL audit insert failure");
    }

    jdbcTemplate.update(
        "INSERT INTO publication_audit (lab_id, actor_id, at, event) VALUES (?, ?, ?, 'lab.published')",
        labId,
        principal.id(),
        receipt.publishedAt());

    return new PublishResult(receiptJson, false);
  }
}
