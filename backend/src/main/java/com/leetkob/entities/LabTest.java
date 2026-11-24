package com.leetkob.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "lab_tests")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabTest {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "lab_id", nullable = false)
  private Lab lab;

  @Column(nullable = false, length = 120)
  private String name;

  @Builder.Default
  @Column(name = "input_data", nullable = false, columnDefinition = "TEXT")
  private String inputData = "";

  @Builder.Default
  @Column(name = "expected_output", nullable = false, columnDefinition = "TEXT")
  private String expectedOutput = "";

  @Builder.Default
  @Column(name = "time_limit_ms", nullable = false)
  private Integer timeLimitMs = 2000;

  @Builder.Default
  @Column(name = "memory_limit_mb", nullable = false)
  private Integer memoryLimitMb = 256;

  @Builder.Default
  @Column(name = "is_sample", nullable = false)
  private Boolean isSample = false;

  @Builder.Default
  @Column(name = "order_index", nullable = false)
  private Integer orderIndex = 0;
}
