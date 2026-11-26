package com.leetkob.repositories;

import com.leetkob.entities.Submission;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubmissionRepository extends JpaRepository<Submission, Integer> {

  /** Tìm bài nộp theo cặp định danh và Idempotency-Key để chống tạo trùng khi mạng retry. */
  Optional<Submission> findByLabIdAndStudentIdAndIdempotencyKey(
      Integer labId, UUID studentId, String idempotencyKey);

  /** Lấy toàn bộ bài nộp của lab (dành cho TA xem danh sách nộp bài của lớp mình). */
  @Query("SELECT s FROM Submission s WHERE s.lab.id = :labId ORDER BY s.submittedAt DESC")
  List<Submission> findByLabIdOrderBySubmittedAtDesc(@Param("labId") Integer labId);

  /** Lấy toàn bộ bài nộp của một sinh viên cụ thể trong lab. */
  @Query(
      "SELECT s FROM Submission s WHERE s.lab.id = :labId AND s.student.id = :studentId ORDER BY s.submittedAt DESC")
  List<Submission> findByLabIdAndStudentIdOrderBySubmittedAtDesc(
      @Param("labId") Integer labId, @Param("studentId") UUID studentId);
}
