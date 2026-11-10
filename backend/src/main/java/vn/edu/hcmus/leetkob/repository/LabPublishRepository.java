package vn.edu.hcmus.leetkob.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import vn.edu.hcmus.leetkob.domain.LabRecord;

/** Data-access repository encapsulating all SQL operations for lab mark publication. */
@Repository
public class LabPublishRepository {

  private final JdbcTemplate jdbcTemplate;

  public LabPublishRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public Optional<LabRecord> findOwnedLab(int labId, String taId) {
    List<LabRecord> rows =
        jdbcTemplate.query(
            """
            SELECT l.id, l.deadline
            FROM labs l
            WHERE l.id = ?
            """,
            (rs, rowNum) -> new LabRecord(rs.getInt("id"), rs.getString("deadline")),
            labId);
    return rows.stream().findFirst();
  }

  public Optional<String> findExistingReceipt(int labId) {
    List<String> existingReceipts =
        jdbcTemplate.query(
            "SELECT receipt FROM publications WHERE lab_id = ?",
            (rs, rowNum) -> rs.getString("receipt"),
            labId);
    return existingReceipts.stream().findFirst();
  }

  public int countPendingJudgeSubmissions(int labId) {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*) FROM counted_submissions
            WHERE lab_id = ? AND judge_status IN ('queued', 'running')
            """,
            Integer.class,
            labId);
    return count != null ? count : 0;
  }

  public int countPendingReviewItems(int labId) {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*) FROM review_items r
            JOIN counted_submissions s ON s.id = r.submission_id
            WHERE s.lab_id = ? AND r.decision IS NULL
            """,
            Integer.class,
            labId);
    return count != null ? count : 0;
  }

  public int countCountedSubmissions(int labId) {
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM counted_submissions WHERE lab_id = ?", Integer.class, labId);
    return count != null ? count : 0;
  }

  public void insertPublicationReceipt(int labId, String receiptJson) {
    jdbcTemplate.update(
        "INSERT INTO publications (lab_id, receipt) VALUES (?, ?)", labId, receiptJson);
  }

  public void copyCountedMarksToPublished(int labId) {
    jdbcTemplate.update(
        """
        INSERT INTO published_marks (lab_id, submission_id, score_tenths)
        SELECT lab_id, id, score_tenths FROM counted_submissions WHERE lab_id = ?
        """,
        labId);
  }

  public void insertPublicationAudit(int labId, String actorId, String publishedAt) {
    jdbcTemplate.update(
        "INSERT INTO publication_audit (lab_id, actor_id, at, event) VALUES (?, ?, ?, 'lab.published')",
        labId,
        actorId,
        publishedAt);
  }
}
