package com.leetkob.entities.ids;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class PublishedMarkId implements Serializable {

  @Column(name = "lab_id", nullable = false)
  private Integer labId;

  @Column(name = "submission_id", nullable = false)
  private Integer submissionId;
}
