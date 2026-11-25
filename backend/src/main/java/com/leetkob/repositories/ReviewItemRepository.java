package com.leetkob.repositories;

import com.leetkob.entities.ReviewItem;
import com.leetkob.entities.ids.ReviewItemId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewItemRepository extends JpaRepository<ReviewItem, ReviewItemId> {

  @Query(
      """
      SELECT COUNT(r)
      FROM ReviewItem r
      JOIN CountedSubmission s ON s.id = r.id.submissionId
      WHERE s.labId = :labId AND r.inQueue = true AND r.decision IS NULL
      """)
  long countPendingInQueueByLabId(@Param("labId") int labId);

  @Query(
      """
      SELECT r
      FROM ReviewItem r
      JOIN CountedSubmission s ON s.id = r.id.submissionId
      WHERE s.labId = :labId AND r.inQueue = true
      ORDER BY CASE WHEN r.decision IS NULL THEN 0 ELSE 1 END, r.id.submissionId ASC, r.id.constraintId ASC
      """)
  List<ReviewItem> findInQueueByLabId(@Param("labId") int labId);

  List<ReviewItem> findByIdSubmissionId(int submissionId);
}
