package com.leetkob.repositories;

import com.leetkob.entities.ClassMember;
import com.leetkob.entities.ids.ClassMemberId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClassMemberRepository extends JpaRepository<ClassMember, ClassMemberId> {

  @Query(
      "SELECT cm.id.classId FROM ClassMember cm WHERE cm.id.userId = :userId ORDER BY cm.id.classId")
  List<Integer> findClassIdsByUserId(@Param("userId") UUID userId);
}
