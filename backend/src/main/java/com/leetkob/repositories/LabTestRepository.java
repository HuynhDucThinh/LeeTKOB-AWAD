package com.leetkob.repositories;

import com.leetkob.entities.LabTest;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LabTestRepository extends JpaRepository<LabTest, Long> {

  List<LabTest> findByLabIdOrderByOrderIndexAscIdAsc(int labId);

  void deleteByLabId(int labId);
}
