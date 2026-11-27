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
@Table(name = "publication_audit")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicationAudit {

  @Id
  @Column(name = "lab_id")
  private Integer labId;

  @Column(name = "actor_id", nullable = false, length = 64)
  private String actorId;

  @Column(nullable = false, length = 64)
  private String at;

  @Builder.Default
  @Column(nullable = false, length = 64)
  private String event = "lab.published";
}
