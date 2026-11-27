package com.leetkob.repository;

import com.leetkob.entity.ClassRoom;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ClassRoomRepository extends JpaRepository<ClassRoom, Integer> {

  List<ClassRoom> findAllByOrderByIdAsc();

  @Query("SELECT COALESCE(MAX(c.id), 0) FROM ClassRoom c")
  int findMaxId();

  boolean existsByCode(String code);
}
