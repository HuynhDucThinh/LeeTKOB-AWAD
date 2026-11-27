package com.leetkob.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leetkob.checker.EvidenceLine;
import com.leetkob.dto.publish.FieldErrorItem;
import com.leetkob.dto.review.ReviewItemResponse;
import com.leetkob.dto.review.ReviewQueuePageResponse;
import com.leetkob.dto.review.SubmitTaDecisionRequest;
import com.leetkob.entity.CountedSubmission;
import com.leetkob.entity.Lab;
import com.leetkob.entity.LabConstraint;
import com.leetkob.entity.ReviewItem;
import com.leetkob.entity.Submission;
import com.leetkob.entity.id.LabConstraintId;
import com.leetkob.entity.id.ReviewItemId;
import com.leetkob.exception.problem.ProblemException;
import com.leetkob.exception.problem.ProblemKind;
import com.leetkob.repository.CountedSubmissionRepository;
import com.leetkob.repository.LabConstraintRepository;
import com.leetkob.repository.LabRepository;
import com.leetkob.repository.PublicationRepository;
import com.leetkob.repository.ReviewItemRepository;
import com.leetkob.repository.SubmissionRepository;
import com.leetkob.repository.TaClassRepository;
import com.leetkob.security.CustomUserDetails;
import com.leetkob.service.ReviewQueueService;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewQueueServiceImpl implements ReviewQueueService {

  private static final Set<String> VALID_DECISIONS = Set.of("pass", "violation");
  private static final TypeReference<List<EvidenceLine>> EVIDENCE_LIST_TYPE =
      new TypeReference<>() {};

  private final LabRepository labRepository;
  private final TaClassRepository taClassRepository;
  private final ReviewItemRepository reviewItemRepository;
  private final CountedSubmissionRepository countedSubmissionRepository;
  private final SubmissionRepository submissionRepository;
  private final LabConstraintRepository labConstraintRepository;
  private final PublicationRepository publicationRepository;
  private final ObjectMapper objectMapper;

  @Override
  @Transactional(readOnly = true)
  public ReviewQueuePageResponse getReviewQueue(
      int labId, int page, int size, CustomUserDetails taUser) {
    requireOwnedLab(labId, taUser);

    if (page < 0) {
      throw new ProblemException(
          ProblemKind.VALIDATION, List.of(FieldErrorItem.forParameter("page", "must be >= 0")));
    }
    if (size <= 0 || size > 200) {
      throw new ProblemException(
          ProblemKind.VALIDATION,
          List.of(FieldErrorItem.forParameter("size", "must be between 1 and 200")));
    }

    List<ReviewItem> allItems = reviewItemRepository.findInQueueByLabId(labId);
    long total = allItems.size();
    int fromIndex = Math.min(page * size, allItems.size());
    int toIndex = Math.min(fromIndex + size, allItems.size());
    List<ReviewItem> pageSlice = allItems.subList(fromIndex, toIndex);

    Map<String, LabConstraint> constraintsById = loadConstraintsById(labId);
    List<ReviewItemResponse> responses =
        pageSlice.stream().map(item -> toResponse(labId, item, constraintsById)).toList();
    return new ReviewQueuePageResponse(responses, total, page, size);
  }

  @Override
  @Transactional
  public ReviewItemResponse submitDecision(
      int labId,
      int submissionId,
      String constraintId,
      SubmitTaDecisionRequest request,
      CustomUserDetails taUser) {
    requireOwnedLab(labId, taUser);

    CountedSubmission counted =
        countedSubmissionRepository
            .findById(submissionId)
            .filter(s -> s.getLabId().equals(labId))
            .orElseThrow(() -> new ProblemException(ProblemKind.NOT_FOUND));

    ReviewItemId itemId =
        ReviewItemId.builder().submissionId(submissionId).constraintId(constraintId).build();
    ReviewItem item =
        reviewItemRepository
            .findById(itemId)
            .orElseThrow(() -> new ProblemException(ProblemKind.NOT_FOUND));

    if (request == null
        || request.decision() == null
        || !VALID_DECISIONS.contains(request.decision())) {
      throw new ProblemException(
          ProblemKind.VALIDATION,
          List.of(FieldErrorItem.forPointer("/decision", "must be pass or violation")));
    }

    item.setDecision(request.decision());
    item.setDecidedBy(taUser.getEmail());
    item.setDecidedAt(OffsetDateTime.now(ZoneOffset.UTC));
    ReviewItem saved = reviewItemRepository.saveAndFlush(item);

    recalculateCountedScoreIfPrePublish(labId, counted);

    Map<String, LabConstraint> constraintsById = loadConstraintsById(labId);
    return toResponse(labId, saved, constraintsById);
  }

  private void recalculateCountedScoreIfPrePublish(int labId, CountedSubmission counted) {
    if (publicationRepository.existsById(labId)) {
      return;
    }
    Optional<Submission> submissionOpt = submissionRepository.findById(counted.getId());
    if (submissionOpt.isEmpty()) {
      return;
    }
    Map<String, LabConstraint> constraintsById = loadConstraintsById(labId);
    List<ReviewItem> items = reviewItemRepository.findByIdSubmissionId(counted.getId());
    int totalDeduction = 0;
    for (ReviewItem r : items) {
      if ("violation".equals(r.getDecision())) {
        LabConstraint c = constraintsById.get(r.getId().getConstraintId());
        if (c != null) {
          totalDeduction += c.getDeductionTenths();
        }
      }
    }
    int baseScore = submissionOpt.get().getScoreTenths();
    counted.setScoreTenths(Math.max(0, baseScore - totalDeduction));
    countedSubmissionRepository.saveAndFlush(counted);
  }

  private Lab requireOwnedLab(int labId, CustomUserDetails taUser) {
    Lab lab =
        labRepository
            .findById(labId)
            .orElseThrow(() -> new ProblemException(ProblemKind.NOT_FOUND));
    if (!taClassRepository.existsByIdTaIdAndIdClassId(taUser.getEmail(), lab.getClassId())) {
      throw new ProblemException(ProblemKind.NOT_FOUND);
    }
    return lab;
  }

  private Map<String, LabConstraint> loadConstraintsById(int labId) {
    Map<String, LabConstraint> map = new HashMap<>();
    for (LabConstraint c : labConstraintRepository.findByIdLabIdOrderByIdConstraintIdAsc(labId)) {
      map.put(c.getId().getConstraintId(), c);
    }
    return map;
  }

  private ReviewItemResponse toResponse(
      int labId, ReviewItem item, Map<String, LabConstraint> constraintsById) {
    int submissionId = item.getId().getSubmissionId();
    String constraintId = item.getId().getConstraintId();

    CountedSubmission counted = countedSubmissionRepository.findById(submissionId).orElse(null);
    Submission submission = submissionRepository.findById(submissionId).orElse(null);

    String studentId =
        submission != null
            ? submission.getStudent().getId().toString()
            : (counted != null ? counted.getStudentId() : "");
    String sourceCode = submission != null ? submission.getSourceCode() : "";
    String strippedCode =
        submission != null && submission.getStrippedCode() != null
            ? submission.getStrippedCode()
            : sourceCode;

    LabConstraint constraint =
        constraintsById.getOrDefault(
            constraintId,
            labConstraintRepository
                .findById(LabConstraintId.builder().labId(labId).constraintId(constraintId).build())
                .orElse(null));

    String constraintType = constraint != null ? constraint.getConstraintType() : "semantic";
    String constraintDescription = constraint != null ? constraint.getDescription() : constraintId;
    int deductionTenths = constraint != null ? constraint.getDeductionTenths() : 0;

    List<EvidenceLine> evidence = parseEvidence(item.getEvidenceJson());

    return new ReviewItemResponse(
        submissionId,
        labId,
        studentId,
        constraintId,
        constraintType,
        constraintDescription,
        deductionTenths,
        item.getVerdict(),
        item.getSource(),
        evidence,
        item.getGuardrailReason(),
        Boolean.TRUE.equals(item.getSampledPass()),
        item.getDecision(),
        item.getDecidedBy(),
        item.getDecidedAt() != null ? item.getDecidedAt().toInstant().toString() : null,
        sourceCode,
        strippedCode);
  }

  private List<EvidenceLine> parseEvidence(String evidenceJson) {
    if (evidenceJson == null || evidenceJson.isBlank()) {
      return List.of();
    }
    try {
      return objectMapper.readValue(evidenceJson, EVIDENCE_LIST_TYPE);
    } catch (Exception e) {
      return List.of();
    }
  }
}
