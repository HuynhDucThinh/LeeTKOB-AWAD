package com.leetkob.services.impl;

import com.leetkob.dtos.appeal.AppealResponse;
import com.leetkob.dtos.appeal.CreateAppealRequest;
import com.leetkob.dtos.appeal.DecideAppealRequest;
import com.leetkob.dtos.publish.FieldErrorItem;
import com.leetkob.entities.Appeal;
import com.leetkob.entities.Lab;
import com.leetkob.entities.LabConstraint;
import com.leetkob.entities.PublishedMark;
import com.leetkob.entities.ReviewItem;
import com.leetkob.entities.Submission;
import com.leetkob.entities.ids.LabConstraintId;
import com.leetkob.entities.ids.PublishedMarkId;
import com.leetkob.entities.ids.ReviewItemId;
import com.leetkob.exceptions.problem.ProblemException;
import com.leetkob.exceptions.problem.ProblemKind;
import com.leetkob.repositories.AppealRepository;
import com.leetkob.repositories.ClassMemberRepository;
import com.leetkob.repositories.LabConstraintRepository;
import com.leetkob.repositories.LabRepository;
import com.leetkob.repositories.PublicationRepository;
import com.leetkob.repositories.PublishedMarkRepository;
import com.leetkob.repositories.ReviewItemRepository;
import com.leetkob.repositories.SubmissionRepository;
import com.leetkob.repositories.TaClassRepository;
import com.leetkob.repositories.UserRepository;
import com.leetkob.security.CustomUserDetails;
import com.leetkob.services.AppealService;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AppealServiceImpl implements AppealService {

  private static final Set<String> VALID_DECISION_STATUSES = Set.of("accepted", "rejected");

  private final AppealRepository appealRepository;
  private final LabRepository labRepository;
  private final SubmissionRepository submissionRepository;
  private final LabConstraintRepository labConstraintRepository;
  private final PublicationRepository publicationRepository;
  private final PublishedMarkRepository publishedMarkRepository;
  private final ReviewItemRepository reviewItemRepository;
  private final TaClassRepository taClassRepository;
  private final ClassMemberRepository classMemberRepository;
  private final UserRepository userRepository;

  @Override
  @Transactional(readOnly = true)
  public List<AppealResponse> listAppeals(int labId, CustomUserDetails user) {
    Lab lab =
        labRepository
            .findById(labId)
            .orElseThrow(() -> new ProblemException(ProblemKind.NOT_FOUND));

    if ("ta".equals(user.getRole())) {
      if (!taClassRepository.existsByIdTaIdAndIdClassId(user.getEmail(), lab.getClassId())) {
        throw new ProblemException(ProblemKind.NOT_FOUND);
      }
      return appealRepository.findByLabIdOrderByCreatedAtDescIdDesc(labId).stream()
          .map(this::toResponse)
          .toList();
    }

    if (!classMemberRepository.existsByIdClassIdAndIdUserId(lab.getClassId(), user.getId())) {
      throw new ProblemException(ProblemKind.NOT_FOUND);
    }
    return appealRepository
        .findByLabIdAndStudentIdOrderByCreatedAtDescIdDesc(labId, user.getId())
        .stream()
        .map(this::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public AppealResponse createAppeal(
      int submissionId, CreateAppealRequest request, CustomUserDetails studentUser) {
    Submission submission =
        submissionRepository
            .findById(submissionId)
            .orElseThrow(() -> new ProblemException(ProblemKind.NOT_FOUND));

    if (!submission.getStudent().getId().equals(studentUser.getId())) {
      throw new ProblemException(ProblemKind.NOT_FOUND);
    }

    int labId = submission.getLab().getId();
    if (!publicationRepository.existsById(labId)) {
      throw new ProblemException(ProblemKind.APPEAL_BEFORE_PUBLISH);
    }

    if (request == null) {
      throw new ProblemException(
          ProblemKind.VALIDATION, List.of(FieldErrorItem.forPointer("", "must be a JSON object")));
    }

    List<FieldErrorItem> errors = new ArrayList<>();
    String constraintId = request.constraintId() == null ? "" : request.constraintId().trim();
    if (constraintId.isBlank()) {
      errors.add(FieldErrorItem.forPointer("/constraintId", "must not be blank"));
    }
    String reason = request.reason() == null ? "" : request.reason().trim();
    if (reason.isBlank()) {
      errors.add(FieldErrorItem.forPointer("/reason", "must not be blank"));
    }
    if (!errors.isEmpty()) {
      throw new ProblemException(ProblemKind.VALIDATION, errors);
    }

    if (appealRepository.existsBySubmissionIdAndConstraintId(submissionId, constraintId)) {
      throw new ProblemException(ProblemKind.APPEAL_ALREADY_EXISTS);
    }

    Appeal saved =
        appealRepository.saveAndFlush(
            Appeal.builder()
                .lab(submission.getLab())
                .submission(submission)
                .student(userRepository.getReferenceById(studentUser.getId()))
                .constraintId(constraintId)
                .reason(reason)
                .status("pending")
                .build());
    return toResponse(saved);
  }

  @Override
  @Transactional
  public AppealResponse decideAppeal(
      long appealId, DecideAppealRequest request, CustomUserDetails taUser) {
    Appeal appeal =
        appealRepository
            .findById(appealId)
            .orElseThrow(() -> new ProblemException(ProblemKind.NOT_FOUND));

    if (!taClassRepository.existsByIdTaIdAndIdClassId(
        taUser.getEmail(), appeal.getLab().getClassId())) {
      throw new ProblemException(ProblemKind.NOT_FOUND);
    }

    if (request == null) {
      throw new ProblemException(
          ProblemKind.VALIDATION, List.of(FieldErrorItem.forPointer("", "must be a JSON object")));
    }

    List<FieldErrorItem> errors = new ArrayList<>();
    String status = request.status() == null ? "" : request.status().trim();
    if (!VALID_DECISION_STATUSES.contains(status)) {
      errors.add(FieldErrorItem.forPointer("/status", "must be accepted or rejected"));
    }
    String decisionReason = request.decisionReason() == null ? "" : request.decisionReason().trim();
    if (decisionReason.isBlank()) {
      errors.add(FieldErrorItem.forPointer("/decisionReason", "must not be blank"));
    }
    if (!errors.isEmpty()) {
      throw new ProblemException(ProblemKind.VALIDATION, errors);
    }

    boolean wasAlreadyAccepted = "accepted".equals(appeal.getStatus());
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

    appeal.setStatus(status);
    appeal.setDecisionReason(decisionReason);
    appeal.setDecidedBy(userRepository.getReferenceById(taUser.getId()));
    appeal.setDecidedAt(now);
    Appeal saved = appealRepository.saveAndFlush(appeal);

    if ("accepted".equals(status) && !wasAlreadyAccepted) {
      int labId = appeal.getLab().getId();
      int submissionId = appeal.getSubmission().getId();
      String constraintId = appeal.getConstraintId();

      int deductionTenths =
          labConstraintRepository
              .findById(LabConstraintId.builder().labId(labId).constraintId(constraintId).build())
              .map(LabConstraint::getDeductionTenths)
              .orElse(0);

      Optional<ReviewItem> reviewItemOpt =
          reviewItemRepository.findById(
              ReviewItemId.builder().submissionId(submissionId).constraintId(constraintId).build());
      if (reviewItemOpt.isPresent()) {
        ReviewItem reviewItem = reviewItemOpt.get();
        reviewItem.setDecision("pass");
        reviewItem.setDecidedBy(taUser.getEmail());
        reviewItem.setDecidedAt(now);
        reviewItemRepository.saveAndFlush(reviewItem);
      }

      Optional<PublishedMark> publishedMarkOpt =
          publishedMarkRepository.findById(
              PublishedMarkId.builder().labId(labId).submissionId(submissionId).build());
      if (publishedMarkOpt.isPresent() && deductionTenths > 0) {
        PublishedMark mark = publishedMarkOpt.get();
        mark.setScoreTenths(Math.min(100, mark.getScoreTenths() + deductionTenths));
        publishedMarkRepository.saveAndFlush(mark);
      }
    }

    return toResponse(saved);
  }

  private AppealResponse toResponse(Appeal appeal) {
    int labId = appeal.getLab().getId();
    LabConstraint constraint =
        labConstraintRepository
            .findById(
                LabConstraintId.builder()
                    .labId(labId)
                    .constraintId(appeal.getConstraintId())
                    .build())
            .orElse(null);

    String constraintDescription =
        constraint != null ? constraint.getDescription() : appeal.getConstraintId();
    int deductionTenths = constraint != null ? constraint.getDeductionTenths() : 0;

    String decidedBy = appeal.getDecidedBy() != null ? appeal.getDecidedBy().getEmail() : null;
    String decidedAt =
        appeal.getDecidedAt() != null ? appeal.getDecidedAt().toInstant().toString() : null;
    String createdAt =
        appeal.getCreatedAt() != null
            ? appeal.getCreatedAt().toInstant().toString()
            : Instant.now().toString();

    return new AppealResponse(
        appeal.getId(),
        labId,
        appeal.getSubmission().getId(),
        appeal.getStudent().getId().toString(),
        appeal.getConstraintId(),
        constraintDescription,
        deductionTenths,
        appeal.getReason(),
        appeal.getStatus(),
        appeal.getDecisionReason(),
        decidedBy,
        decidedAt,
        createdAt);
  }
}
