package com.leetkob.repository;

import com.leetkob.entity.Appeal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppealRepository extends JpaRepository<Appeal, Long> {

  List<Appeal> findByLabIdOrderByCreatedAtDescIdDesc(int labId);

  List<Appeal> findByLabIdAndStudentIdOrderByCreatedAtDescIdDesc(int labId, UUID studentId);

  boolean existsBySubmissionIdAndConstraintId(int submissionId, String constraintId);
}
