package com.leetkob.repository;

import com.leetkob.entity.TaClass;
import com.leetkob.entity.id.TaClassId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaClassRepository extends JpaRepository<TaClass, TaClassId> {

  void deleteByIdClassId(int classId);

  boolean existsByIdTaIdAndIdClassId(String taId, int classId);

  @Query("SELECT t.id.classId FROM TaClass t WHERE t.id.taId = :taId ORDER BY t.id.classId")
  List<Integer> findClassIdsByTaId(@Param("taId") String taId);
}
