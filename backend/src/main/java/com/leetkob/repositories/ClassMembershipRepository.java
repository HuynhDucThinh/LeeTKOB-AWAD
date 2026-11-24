package com.leetkob.repositories;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ClassMembershipRepository {

  private final JdbcTemplate jdbcTemplate;

  public List<Integer> findClassIdsByUserId(UUID userId) {
    return jdbcTemplate.queryForList(
        "SELECT class_id FROM class_members WHERE user_id = ? ORDER BY class_id",
        Integer.class,
        userId);
  }

  public void ensureClass(int id, String code, String name) {
    jdbcTemplate.update(
        "INSERT INTO classes (id, code, name) VALUES (?, ?, ?) ON CONFLICT (id) DO NOTHING",
        id,
        code,
        name);
  }

  public void ensureMember(int classId, UUID userId, String memberRole) {
    jdbcTemplate.update(
        "INSERT INTO class_members (class_id, user_id, member_role) VALUES (?, ?, ?) "
            + "ON CONFLICT (class_id, user_id) DO NOTHING",
        classId,
        userId,
        memberRole);
  }
}
