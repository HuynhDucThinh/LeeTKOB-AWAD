package com.leetkob.repositories;

import com.leetkob.entities.ReviewItem;
import com.leetkob.entities.ids.ReviewItemId;
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
}
