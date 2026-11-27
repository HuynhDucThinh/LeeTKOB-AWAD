package com.leetkob.repository;

import com.leetkob.entity.JudgeJob;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JudgeJobRepository extends JpaRepository<JudgeJob, Long> {

  /**
   * Khóa và lấy bản ghi job kế tiếp đang ở trạng thái 'queued'.
   *
   * <p>Sử dụng native SQL với cơ chế khóa hàng {@code FOR UPDATE SKIP LOCKED} để:
   *
   * <ul>
   *   <li>Đảm bảo Exact-Once (Q1): không bao giờ có 2 worker xử lý trùng 1 bài nộp.
   *   <li>Không bị chặn (non-blocking) khi có nhiều worker quét hàng đợi đồng thời.
   * </ul>
   */
  @Query(
      value =
          "SELECT * FROM judge_jobs WHERE status = 'queued' ORDER BY id ASC LIMIT 1 FOR UPDATE SKIP LOCKED",
      nativeQuery = true)
  Optional<JudgeJob> findNextQueuedJobForUpdate();

  @Query("SELECT j FROM JudgeJob j WHERE j.submission.id = :submissionId")
  Optional<JudgeJob> findBySubmissionId(@Param("submissionId") Integer submissionId);

  long countByStatus(String status);
}
