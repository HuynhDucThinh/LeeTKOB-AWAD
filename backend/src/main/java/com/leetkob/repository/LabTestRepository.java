package com.leetkob.repository;

import com.leetkob.entity.LabTest;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LabTestRepository extends JpaRepository<LabTest, Long> {

  @Query("SELECT t FROM LabTest t WHERE t.lab.id = :labId ORDER BY t.orderIndex ASC, t.id ASC")
  List<LabTest> findByLabIdOrderByOrderIndexAsc(@Param("labId") Integer labId);

  List<LabTest> findByLabIdOrderByOrderIndexAscIdAsc(int labId);

  void deleteByLabId(int labId);
}
