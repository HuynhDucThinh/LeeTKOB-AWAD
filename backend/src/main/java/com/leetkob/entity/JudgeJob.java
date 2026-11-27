package com.leetkob.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "judge_jobs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JudgeJob {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "submission_id", nullable = false, unique = true)
  private Submission submission;

  @Builder.Default
  @Column(nullable = false, length = 16)
  private String status = "queued";

  @Builder.Default
  @Column(nullable = false)
  private Integer attempts = 0;

  @Column(name = "locked_by", length = 64)
  private String lockedBy;

  @Column(name = "locked_at")
  private OffsetDateTime lockedAt;

  @Column(name = "trace_parent", length = 128)
  private String traceParent;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @Column(name = "finished_at")
  private OffsetDateTime finishedAt;
}
