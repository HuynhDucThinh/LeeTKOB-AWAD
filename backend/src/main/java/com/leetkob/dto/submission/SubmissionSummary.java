package com.leetkob.dto.submission;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionSummary {

  private Integer id;
  private Integer labId;
  private UUID studentId;
  private String studentName;
  private String judgeStatus;
  private Integer scoreTenths;
  private Integer publishedScoreTenths;
  private Boolean isCounted;
  private Integer passedTests;
  private Integer totalTests;
  private Integer executionTimeMs;
  private Integer memoryKb;
  private String compileError;
  private OffsetDateTime submittedAt;
  private OffsetDateTime judgedAt;
  private String traceId;
}
