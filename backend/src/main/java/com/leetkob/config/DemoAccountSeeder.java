package com.leetkob.config;

import com.leetkob.entities.User;
import com.leetkob.repositories.ClassMembershipRepository;
import com.leetkob.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Idempotent seeder creating the PA#3 demo classes and five test accounts (1 admin, 2 TAs in
 * separate classes, 2 students in the first TA's class). Disabled in H2 unit/contract tests via
 * {@code leetkob.seed.enabled=false}.
 */
@Component
@ConditionalOnProperty(name = "leetkob.seed.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class DemoAccountSeeder implements ApplicationRunner {

  private final UserRepository userRepository;
  private final ClassMembershipRepository classMembershipRepository;
  private final PasswordEncoder passwordEncoder;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    classMembershipRepository.ensureClass(10, "CSC13114-A", "Intro to C++ — Class 10");
    classMembershipRepository.ensureClass(20, "CSC13114-B", "Intro to C++ — Class 20");

    ensureUser("admin@leetkob.edu", "System Admin", "admin", "Admin@123");
    User ta1 = ensureUser("ta1@leetkob.edu", "TA Class 10", "ta", "Ta1@1234");
    User ta2 = ensureUser("ta2@leetkob.edu", "TA Class 20", "ta", "Ta2@1234");
    User student1 = ensureUser("student1@leetkob.edu", "Student 1", "student", "Student1@123");
    User student2 = ensureUser("student2@leetkob.edu", "Student 2", "student", "Student2@123");

    classMembershipRepository.ensureMember(10, ta1.getId(), "ta");
    classMembershipRepository.ensureMember(20, ta2.getId(), "ta");
    classMembershipRepository.ensureMember(10, student1.getId(), "student");
    classMembershipRepository.ensureMember(10, student2.getId(), "student");

    classMembershipRepository.ensureTaClass(ta1.getEmail(), 10);
    classMembershipRepository.ensureTaClass(ta2.getEmail(), 20);
  }

  private User ensureUser(String email, String fullName, String role, String rawPassword) {
    return userRepository
        .findByEmail(email)
        .orElseGet(
            () ->
                userRepository.saveAndFlush(
                    User.builder()
                        .email(email)
                        .fullName(fullName)
                        .role(role)
                        .passwordHash(passwordEncoder.encode(rawPassword))
                        .build()));
  }
}
