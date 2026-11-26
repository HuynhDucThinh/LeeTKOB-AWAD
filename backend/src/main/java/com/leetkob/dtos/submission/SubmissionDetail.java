package com.leetkob.dtos.submission;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionDetail {

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
  private String sourceCode;
  private String strippedCode;
  private List<PublishedDeductionItem> publishedDeductions;
}
