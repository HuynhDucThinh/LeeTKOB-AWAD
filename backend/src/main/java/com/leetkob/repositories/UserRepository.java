package com.leetkob.repositories;

import com.leetkob.entities.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);

  List<User> findAllByOrderByCreatedAtAsc();

  List<User> findByRoleOrderByCreatedAtAsc(String role);
}
