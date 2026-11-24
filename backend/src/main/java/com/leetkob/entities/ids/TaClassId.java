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
public class TaClassId implements Serializable {

  @Column(name = "ta_id", nullable = false, length = 64)
  private String taId;

  @Column(name = "class_id", nullable = false)
  private Integer classId;
}
