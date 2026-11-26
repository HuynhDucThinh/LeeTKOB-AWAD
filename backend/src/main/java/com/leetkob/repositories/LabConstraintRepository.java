package com.leetkob.repositories;

import com.leetkob.entities.LabConstraint;
import com.leetkob.entities.ids.LabConstraintId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LabConstraintRepository extends JpaRepository<LabConstraint, LabConstraintId> {

  @Query("SELECT c FROM LabConstraint c WHERE c.lab.id = :labId")
  List<LabConstraint> findByLabId(@Param("labId") Integer labId);
}
