package com.leetkob.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "publications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Publication {

  @Id
  @Column(name = "lab_id")
  private Integer labId;

  @Column(nullable = false, length = 4096)
  private String receipt;
}
