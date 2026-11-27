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
@Table(name = "labs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Lab {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "class_id", nullable = false)
  private Integer classId;

  @Column(nullable = false, length = 64)
  private String deadline;

  @Builder.Default
  @Column(nullable = false, length = 200)
  private String title = "";

  @Builder.Default
  @Column(name = "statement_md", nullable = false, columnDefinition = "TEXT")
  private String statementMd = "";

  @Builder.Default
  @Column(name = "cost_cap_usd", nullable = false, precision = 10, scale = 4)
  private BigDecimal costCapUsd = new BigDecimal("5.0000");

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by")
  private User createdBy;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;
}
