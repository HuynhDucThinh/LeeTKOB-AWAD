package com.leetkob.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "counted_submissions",
    uniqueConstraints = @UniqueConstraint(columnNames = {"lab_id", "student_id"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CountedSubmission {

  @Id private Integer id;

  @Column(name = "lab_id", nullable = false)
  private Integer labId;

  @Column(name = "student_id", nullable = false, length = 64)
  private String studentId;

  @Column(name = "judge_status", nullable = false, length = 32)
  private String judgeStatus;

  @Column(name = "score_tenths", nullable = false)
  private Integer scoreTenths;
}
