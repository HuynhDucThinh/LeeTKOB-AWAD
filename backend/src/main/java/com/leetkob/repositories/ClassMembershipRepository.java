package com.leetkob.repositories;

import com.leetkob.entities.ClassMember;
import com.leetkob.entities.ClassRoom;
import com.leetkob.entities.TaClass;
import com.leetkob.entities.ids.ClassMemberId;
import com.leetkob.entities.ids.TaClassId;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** JPA-backed facade managing class rooms, class memberships, and TA ownership assignments. */
@Repository
@RequiredArgsConstructor
public class ClassMembershipRepository {

  private final ClassRoomRepository classRoomRepository;
  private final ClassMemberRepository classMemberRepository;
  private final TaClassRepository taClassRepository;
  private final UserRepository userRepository;

  public List<Integer> findClassIdsByUserId(UUID userId) {
    return classMemberRepository.findClassIdsByUserId(userId);
  }

  public void ensureClass(int id, String code, String name) {
    if (!classRoomRepository.existsById(id)) {
      classRoomRepository.saveAndFlush(ClassRoom.builder().id(id).code(code).name(name).build());
    }
  }

  public void ensureMember(int classId, UUID userId, String memberRole) {
    ClassMemberId memberId = ClassMemberId.builder().classId(classId).userId(userId).build();
    if (!classMemberRepository.existsById(memberId)) {
      ClassMember member =
          ClassMember.builder()
              .id(memberId)
              .classRoom(classRoomRepository.getReferenceById(classId))
              .user(userRepository.getReferenceById(userId))
              .memberRole(memberRole)
              .build();
      classMemberRepository.saveAndFlush(member);
    }
  }

  public void ensureTaClass(String taEmail, int classId) {
    TaClassId taClassId = TaClassId.builder().taId(taEmail).classId(classId).build();
    if (!taClassRepository.existsById(taClassId)) {
      taClassRepository.saveAndFlush(TaClass.builder().id(taClassId).build());
    }
  }
}
