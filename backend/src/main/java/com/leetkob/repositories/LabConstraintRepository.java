package com.leetkob.repositories;

import com.leetkob.entities.LabConstraint;
import com.leetkob.entities.ids.LabConstraintId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LabConstraintRepository extends JpaRepository<LabConstraint, LabConstraintId> {

  List<LabConstraint> findByIdLabIdOrderByIdConstraintIdAsc(int labId);

  void deleteByIdLabId(int labId);
}
