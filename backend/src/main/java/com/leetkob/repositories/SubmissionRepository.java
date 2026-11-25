package com.leetkob.repositories;

import com.leetkob.entities.Submission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubmissionRepository extends JpaRepository<Submission, Integer> {}
