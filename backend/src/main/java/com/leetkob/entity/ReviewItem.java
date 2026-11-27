package com.leetkob.entity;

import com.leetkob.entity.id.ReviewItemId;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "review_items")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewItem {

  @EmbeddedId private ReviewItemId id;

  @Column(length = 32)
  private String decision;

  @Builder.Default
  @Column(nullable = false, length = 16)
  private String verdict = "unsure";

  @Builder.Default
  @Column(nullable = false, length = 16)
  private String source = "llm";

  @Builder.Default
  @Column(name = "evidence_json", nullable = false, columnDefinition = "TEXT")
  private String evidenceJson = "[]";

  @Column(name = "guardrail_reason", length = 64)
  private String guardrailReason;

  @Builder.Default
  @Column(name = "in_queue", nullable = false)
  private Boolean inQueue = true;

  @Builder.Default
  @Column(name = "sampled_pass", nullable = false)
  private Boolean sampledPass = false;

  @Column(name = "decided_by", length = 64)
  private String decidedBy;

  @Column(name = "decided_at")
  private OffsetDateTime decidedAt;
}
