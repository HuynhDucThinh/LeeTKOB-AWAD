package com.leetkob.repository;

import com.leetkob.domain.LabRecord;
import com.leetkob.entity.CountedSubmission;
import com.leetkob.entity.Publication;
import com.leetkob.entity.PublicationAudit;
import com.leetkob.entity.PublishedMark;
import com.leetkob.entity.id.PublishedMarkId;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** JPA-backed data-access repository encapsulating all database operations for lab publication. */
@Repository
@RequiredArgsConstructor
public class LabPublishRepository {

  private static final List<String> PENDING_JUDGE_STATUSES = List.of("queued", "running");
  private static final String LAB_PUBLISHED_EVENT = "lab.published";

  private final LabRepository labRepository;
  private final PublicationRepository publicationRepository;
  private final CountedSubmissionRepository countedSubmissionRepository;
  private final ReviewItemRepository reviewItemRepository;

  @PersistenceContext private EntityManager entityManager;

  /**
   * Takes a pessimistic write row lock on the lab for the rest of the current transaction.
   * Concurrent publishes of the same lab are serialised by the database; publishes of other labs
   * are not affected.
   */
  public void lockLabRow(int labId) {
    labRepository.findByIdForUpdate(labId);
  }

  public Optional<LabRecord> findOwnedLab(int labId, String taId) {
    return labRepository.findOwnedLab(labId, taId);
  }

  public Optional<String> findExistingReceipt(int labId) {
    return publicationRepository.findById(labId).map(Publication::getReceipt);
  }

  public int countPendingJudgeSubmissions(int labId) {
    return Math.toIntExact(
        countedSubmissionRepository.countByLabIdAndJudgeStatusIn(labId, PENDING_JUDGE_STATUSES));
  }

  public int countPendingReviewItems(int labId) {
    return Math.toIntExact(reviewItemRepository.countPendingInQueueByLabId(labId));
  }

  public int countCountedSubmissions(int labId) {
    return Math.toIntExact(countedSubmissionRepository.countByLabId(labId));
  }

  public void insertPublicationReceipt(int labId, String receiptJson) {
    entityManager.persist(Publication.builder().labId(labId).receipt(receiptJson).build());
    entityManager.flush();
  }

  public void copyCountedMarksToPublished(int labId) {
    List<CountedSubmission> submissions = countedSubmissionRepository.findByLabId(labId);
    for (CountedSubmission submission : submissions) {
      PublishedMarkId markId =
          PublishedMarkId.builder().labId(labId).submissionId(submission.getId()).build();
      entityManager.persist(
          PublishedMark.builder().id(markId).scoreTenths(submission.getScoreTenths()).build());
    }
    entityManager.flush();
  }

  public void insertPublicationAudit(int labId, String actorId, String publishedAt) {
    entityManager.persist(
        PublicationAudit.builder()
            .labId(labId)
            .actorId(actorId)
            .at(publishedAt)
            .event(LAB_PUBLISHED_EVENT)
            .build());
    entityManager.flush();
  }
}
