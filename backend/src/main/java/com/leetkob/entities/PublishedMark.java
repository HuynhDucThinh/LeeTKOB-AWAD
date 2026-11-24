package com.leetkob.entities;

import com.leetkob.entities.ids.PublishedMarkId;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "published_marks")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublishedMark {

  @EmbeddedId private PublishedMarkId id;

  @Column(name = "score_tenths", nullable = false)
  private Integer scoreTenths;
}
