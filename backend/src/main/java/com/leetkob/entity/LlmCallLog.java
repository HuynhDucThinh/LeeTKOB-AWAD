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
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "llm_call_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmCallLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "submission_id")
  private Submission submission;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "lab_id")
  private Lab lab;

  @Column(name = "constraint_id", nullable = false, length = 64)
  private String constraintId;

  @Column(nullable = false, length = 64)
  private String provider;

  @Column(nullable = false, length = 128)
  private String model;

  @Column(name = "provider_request_id", length = 128)
  private String providerRequestId;

  @Builder.Default
  @Column(name = "prompt_tokens", nullable = false)
  private Integer promptTokens = 0;

  @Builder.Default
  @Column(name = "completion_tokens", nullable = false)
  private Integer completionTokens = 0;

  @Builder.Default
  @Column(name = "latency_ms", nullable = false)
  private Integer latencyMs = 0;

  @Builder.Default
  @Column(name = "cost_usd", nullable = false, precision = 12, scale = 6)
  private BigDecimal costUsd = BigDecimal.ZERO;

  @Column(name = "sanitized_prompt", columnDefinition = "TEXT")
  private String sanitizedPrompt;

  @Column(name = "raw_response", columnDefinition = "TEXT")
  private String rawResponse;

  @Column(nullable = false, length = 16)
  private String verdict;

  @Column(name = "guardrail_reason", length = 64)
  private String guardrailReason;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;
}
