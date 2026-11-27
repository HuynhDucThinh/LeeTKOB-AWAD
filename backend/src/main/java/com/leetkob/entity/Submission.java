package com.leetkob.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(
    name = "submissions",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_submissions_idempotency",
            columnNames = {"lab_id", "student_id", "idempotency_key"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Submission {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "lab_id", nullable = false)
  private Lab lab;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "student_id", nullable = false)
  private User student;

  @Column(name = "source_code", nullable = false, columnDefinition = "TEXT")
  private String sourceCode;

  @Column(name = "stripped_code", columnDefinition = "TEXT")
  private String strippedCode;

  @Builder.Default
  @Column(name = "judge_status", nullable = false, length = 32)
  private String judgeStatus = "queued";

  @Builder.Default
  @Column(name = "score_tenths", nullable = false)
  private Integer scoreTenths = 0;

  @Builder.Default
  @Column(name = "passed_tests", nullable = false)
  private Integer passedTests = 0;

  @Builder.Default
  @Column(name = "total_tests", nullable = false)
  private Integer totalTests = 0;

  @Column(name = "execution_time_ms")
  private Integer executionTimeMs;

  @Column(name = "memory_kb")
  private Integer memoryKb;

  @Column(name = "compile_error", columnDefinition = "TEXT")
  private String compileError;

  @Column(name = "trace_id", nullable = false, length = 64)
  private String traceId;

  @Column(name = "idempotency_key", length = 128)
  private String idempotencyKey;

  @CreationTimestamp
  @Column(name = "submitted_at", nullable = false, updatable = false)
  private OffsetDateTime submittedAt;

  @Column(name = "judged_at")
  private OffsetDateTime judgedAt;
}
