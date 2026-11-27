package com.leetkob.repository;

import com.leetkob.entity.ClassMember;
import com.leetkob.entity.id.ClassMemberId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClassMemberRepository extends JpaRepository<ClassMember, ClassMemberId> {

  @Query(
      "SELECT cm.id.classId FROM ClassMember cm WHERE cm.id.userId = :userId ORDER BY cm.id.classId")
  List<Integer> findClassIdsByUserId(@Param("userId") UUID userId);

  List<ClassMember> findByIdClassId(int classId);

  void deleteByIdClassId(int classId);

  boolean existsByIdClassIdAndIdUserId(int classId, UUID userId);
}
