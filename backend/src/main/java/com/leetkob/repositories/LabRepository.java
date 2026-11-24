package com.leetkob.repositories;

import com.leetkob.domain.LabRecord;
import com.leetkob.entities.Lab;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LabRepository extends JpaRepository<Lab, Integer> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT l FROM Lab l WHERE l.id = :labId")
  Optional<Lab> findByIdForUpdate(@Param("labId") int labId);

  @Query(
      """
      SELECT new com.leetkob.domain.LabRecord(l.id, l.deadline)
      FROM Lab l
      JOIN TaClass t ON t.id.classId = l.classId
      WHERE l.id = :labId AND t.id.taId = :taId
      """)
  Optional<LabRecord> findOwnedLab(@Param("labId") int labId, @Param("taId") String taId);

  List<Lab> findAllByOrderByIdAsc();

  List<Lab> findByClassIdInOrderByIdAsc(Collection<Integer> classIds);

  long countByClassId(int classId);
}
