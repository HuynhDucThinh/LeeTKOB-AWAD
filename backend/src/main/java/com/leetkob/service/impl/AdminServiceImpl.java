package com.leetkob.service.impl;

import com.leetkob.dto.admin.AdminUserResponse;
import com.leetkob.dto.admin.ClassRoomResponse;
import com.leetkob.dto.admin.CreateClassRequest;
import com.leetkob.dto.admin.CreateUserRequest;
import com.leetkob.dto.admin.UpdateClassMembersRequest;
import com.leetkob.dto.publish.FieldErrorItem;
import com.leetkob.entity.ClassMember;
import com.leetkob.entity.ClassRoom;
import com.leetkob.entity.TaClass;
import com.leetkob.entity.User;
import com.leetkob.entity.id.ClassMemberId;
import com.leetkob.entity.id.TaClassId;
import com.leetkob.exception.problem.ProblemException;
import com.leetkob.exception.problem.ProblemKind;
import com.leetkob.repository.ClassMemberRepository;
import com.leetkob.repository.ClassRoomRepository;
import com.leetkob.repository.LabRepository;
import com.leetkob.repository.TaClassRepository;
import com.leetkob.repository.UserRepository;
import com.leetkob.security.CustomUserDetails;
import com.leetkob.service.AdminService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

  private static final Set<String> VALID_ROLES = Set.of("admin", "ta", "student");

  private final ClassRoomRepository classRoomRepository;
  private final ClassMemberRepository classMemberRepository;
  private final TaClassRepository taClassRepository;
  private final LabRepository labRepository;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @Override
  @Transactional(readOnly = true)
  public List<ClassRoomResponse> listAllClasses() {
    return classRoomRepository.findAllByOrderByIdAsc().stream()
        .map(this::toClassRoomResponse)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<ClassRoomResponse> listClassesForUser(CustomUserDetails user) {
    if ("admin".equals(user.getRole())) {
      return listAllClasses();
    }
    List<Integer> classIds = classMemberRepository.findClassIdsByUserId(user.getId());
    if (classIds.isEmpty()) {
      return List.of();
    }
    return classRoomRepository.findAllById(classIds).stream()
        .sorted(java.util.Comparator.comparingInt(ClassRoom::getId))
        .map(this::toClassRoomResponse)
        .toList();
  }

  @Override
  @Transactional
  public ClassRoomResponse createClass(CreateClassRequest request) {
    if (request == null || request.name() == null || request.name().isBlank()) {
      throw new ProblemException(
          ProblemKind.VALIDATION, List.of(FieldErrorItem.forPointer("/name", "must not be blank")));
    }
    String name = request.name().trim();
    int nextId = classRoomRepository.findMaxId() + 1;
    String code = generateUniqueCode(name, nextId);

    ClassRoom saved =
        classRoomRepository.saveAndFlush(
            ClassRoom.builder().id(nextId).code(code).name(name).build());
    return toClassRoomResponse(saved);
  }

  @Override
  @Transactional
  public ClassRoomResponse updateClassMembers(int classId, UpdateClassMembersRequest request) {
    ClassRoom classRoom =
        classRoomRepository
            .findById(classId)
            .orElseThrow(() -> new ProblemException(ProblemKind.NOT_FOUND));

    if (request == null) {
      throw new ProblemException(
          ProblemKind.VALIDATION, List.of(FieldErrorItem.forPointer("", "must be a JSON object")));
    }

    List<UUID> taUuids = parseUuids(request.taIds(), "/taIds");
    List<UUID> studentUuids = parseUuids(request.studentIds(), "/studentIds");

    List<User> taUsers = loadUsersOrFail(taUuids, "/taIds");
    List<User> studentUsers = loadUsersOrFail(studentUuids, "/studentIds");

    classMemberRepository.deleteByIdClassId(classId);
    classMemberRepository.flush();
    taClassRepository.deleteByIdClassId(classId);
    taClassRepository.flush();

    Set<UUID> addedUsers = new LinkedHashSet<>();
    for (User ta : taUsers) {
      if (addedUsers.add(ta.getId())) {
        ClassMemberId memberId =
            ClassMemberId.builder().classId(classId).userId(ta.getId()).build();
        classMemberRepository.save(
            ClassMember.builder()
                .id(memberId)
                .classRoom(classRoom)
                .user(ta)
                .memberRole("ta")
                .build());
        taClassRepository.save(
            TaClass.builder()
                .id(TaClassId.builder().taId(ta.getEmail()).classId(classId).build())
                .build());
      }
    }

    for (User student : studentUsers) {
      if (addedUsers.add(student.getId())) {
        ClassMemberId memberId =
            ClassMemberId.builder().classId(classId).userId(student.getId()).build();
        classMemberRepository.save(
            ClassMember.builder()
                .id(memberId)
                .classRoom(classRoom)
                .user(student)
                .memberRole("student")
                .build());
      }
    }

    classMemberRepository.flush();
    taClassRepository.flush();
    return toClassRoomResponse(classRoom);
  }

  @Override
  @Transactional(readOnly = true)
  public List<AdminUserResponse> listUsers(String roleFilter) {
    List<User> users;
    if (roleFilter == null || roleFilter.isBlank()) {
      users = userRepository.findAllByOrderByCreatedAtAsc();
    } else {
      String normalized = roleFilter.trim().toLowerCase(Locale.ROOT);
      if (!VALID_ROLES.contains(normalized)) {
        throw new ProblemException(
            ProblemKind.VALIDATION,
            List.of(FieldErrorItem.forParameter("role", "must be one of admin, ta, student")));
      }
      users = userRepository.findByRoleOrderByCreatedAtAsc(normalized);
    }
    return users.stream().map(this::toAdminUserResponse).toList();
  }

  @Override
  @Transactional
  public AdminUserResponse createUser(CreateUserRequest request) {
    if (request == null) {
      throw new ProblemException(
          ProblemKind.VALIDATION, List.of(FieldErrorItem.forPointer("", "must be a JSON object")));
    }

    List<FieldErrorItem> errors = new ArrayList<>();
    String email =
        request.username() == null ? "" : request.username().trim().toLowerCase(Locale.ROOT);
    if (email.isBlank() || !email.contains("@")) {
      errors.add(FieldErrorItem.forPointer("/username", "must be a valid email address"));
    }
    String displayName = request.displayName() == null ? "" : request.displayName().trim();
    if (displayName.isBlank()) {
      errors.add(FieldErrorItem.forPointer("/displayName", "must not be blank"));
    }
    String role = request.role() == null ? "" : request.role().trim().toLowerCase(Locale.ROOT);
    if (!VALID_ROLES.contains(role)) {
      errors.add(FieldErrorItem.forPointer("/role", "must be one of admin, ta, student"));
    }
    if (request.password() == null || request.password().length() < 6) {
      errors.add(FieldErrorItem.forPointer("/password", "must be at least 6 characters"));
    }
    if (!errors.isEmpty()) {
      throw new ProblemException(ProblemKind.VALIDATION, errors);
    }

    if (userRepository.existsByEmail(email)) {
      throw new ProblemException(ProblemKind.CONFLICT);
    }

    User saved =
        userRepository.saveAndFlush(
            User.builder()
                .email(email)
                .fullName(displayName)
                .role(role)
                .passwordHash(passwordEncoder.encode(request.password()))
                .build());
    return toAdminUserResponse(saved);
  }

  private ClassRoomResponse toClassRoomResponse(ClassRoom classRoom) {
    List<ClassMember> members = classMemberRepository.findByIdClassId(classRoom.getId());
    List<String> taIds = new ArrayList<>();
    List<String> studentIds = new ArrayList<>();
    for (ClassMember member : members) {
      String userId = member.getId().getUserId().toString();
      if ("ta".equals(member.getMemberRole())) {
        taIds.add(userId);
      } else {
        studentIds.add(userId);
      }
    }
    int labCount = (int) labRepository.countByClassId(classRoom.getId());
    return new ClassRoomResponse(
        classRoom.getId(), classRoom.getName(), taIds, studentIds, labCount);
  }

  private AdminUserResponse toAdminUserResponse(User user) {
    List<Integer> classIds = classMemberRepository.findClassIdsByUserId(user.getId());
    String createdAt =
        user.getCreatedAt() != null
            ? user.getCreatedAt().toInstant().toString()
            : Instant.now().toString();
    String displayName =
        (user.getFullName() != null && !user.getFullName().isBlank())
            ? user.getFullName()
            : user.getEmail();
    return new AdminUserResponse(
        user.getId().toString(), user.getEmail(), displayName, user.getRole(), classIds, createdAt);
  }

  private List<UUID> parseUuids(List<String> rawIds, String pointer) {
    if (rawIds == null) {
      return List.of();
    }
    List<UUID> uuids = new ArrayList<>();
    for (String raw : rawIds) {
      if (raw == null || raw.isBlank()) {
        throw new ProblemException(
            ProblemKind.VALIDATION,
            List.of(FieldErrorItem.forPointer(pointer, "contains blank id")));
      }
      try {
        uuids.add(UUID.fromString(raw.trim()));
      } catch (IllegalArgumentException e) {
        throw new ProblemException(
            ProblemKind.VALIDATION,
            List.of(FieldErrorItem.forPointer(pointer, "contains invalid UUID: " + raw)));
      }
    }
    return uuids;
  }

  private List<User> loadUsersOrFail(List<UUID> uuids, String pointer) {
    List<User> users = new ArrayList<>();
    for (UUID uuid : uuids) {
      User user =
          userRepository
              .findById(uuid)
              .orElseThrow(
                  () ->
                      new ProblemException(
                          ProblemKind.VALIDATION,
                          List.of(FieldErrorItem.forPointer(pointer, "user not found: " + uuid))));
      users.add(user);
    }
    return users;
  }

  private String generateUniqueCode(String name, int classId) {
    String sanitized =
        name.replaceAll("[^A-Za-z0-9_-]+", "-").replaceAll("^-+|-+$", "").toUpperCase(Locale.ROOT);
    if (sanitized.isBlank()) {
      sanitized = "CLASS-" + classId;
    }
    if (sanitized.length() > 56) {
      sanitized = sanitized.substring(0, 56);
    }
    if (!classRoomRepository.existsByCode(sanitized)) {
      return sanitized;
    }
    return sanitized + "-" + classId;
  }
}
