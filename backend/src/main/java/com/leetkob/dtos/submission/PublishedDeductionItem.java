package com.leetkob.dtos.submission;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublishedDeductionItem {

  private String constraintId;
  private String verdict;
  private Integer deductionTenths;
  private String description;
}
