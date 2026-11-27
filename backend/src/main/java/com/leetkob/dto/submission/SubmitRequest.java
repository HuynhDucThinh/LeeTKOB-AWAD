package com.leetkob.dto.submission;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitRequest {

  @NotBlank(message = "sourceCode must not be blank")
  private String sourceCode;
}
