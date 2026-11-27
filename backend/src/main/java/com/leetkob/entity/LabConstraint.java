package com.leetkob.entity;

import com.leetkob.entity.id.LabConstraintId;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "lab_constraints")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabConstraint {

  @EmbeddedId private LabConstraintId id;

  @MapsId("labId")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "lab_id", nullable = false)
  private Lab lab;

  @Column(name = "constraint_type", nullable = false, length = 16)
  private String constraintType;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String description;

  @Column(name = "deduction_tenths", nullable = false)
  private Integer deductionTenths;
}
