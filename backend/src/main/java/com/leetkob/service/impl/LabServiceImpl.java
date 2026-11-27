package com.leetkob.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leetkob.dto.lab.CreateLabRequest;
import com.leetkob.dto.lab.CreateLabTestRequest;
import com.leetkob.dto.lab.LabConstraintDto;
import com.leetkob.dto.lab.LabDetailResponse;
import com.leetkob.dto.lab.LabSummaryResponse;
import com.leetkob.dto.lab.LabTestCaseDto;
import com.leetkob.dto.publish.FieldErrorItem;
import com.leetkob.dto.publish.PublicationReceipt;
import com.leetkob.entity.ClassRoom;
import com.leetkob.entity.Lab;
import com.leetkob.entity.LabConstraint;
import com.leetkob.entity.LabTest;
import com.leetkob.entity.Publication;
import com.leetkob.entity.id.LabConstraintId;
import com.leetkob.exception.problem.ProblemException;
import com.leetkob.exception.problem.ProblemKind;
import com.leetkob.repository.ClassMemberRepository;
import com.leetkob.repository.ClassRoomRepository;
import com.leetkob.repository.CountedSubmissionRepository;
import com.leetkob.repository.LabConstraintRepository;
import com.leetkob.repository.LabRepository;
import com.leetkob.repository.LabTestRepository;
import com.leetkob.repository.LlmCallLogRepository;
import com.leetkob.repository.PublicationRepository;
import com.leetkob.repository.ReviewItemRepository;
import com.leetkob.repository.TaClassRepository;
import com.leetkob.repository.UserRepository;
import com.leetkob.security.CustomUserDetails;
import com.leetkob.service.LabService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LabServiceImpl implements LabService {

  private static final List<String> PENDING_JUDGE_STATUSES = List.of("queued", "running");
  private static final Set<String> VALID_CONSTRAINT_TYPES = Set.of("hard", "semantic");

  private final LabRepository labRepository;
  private final LabTestRepository labTestRepository;
  private final LabConstraintRepository labConstraintRepository;
  private final ClassRoomRepository classRoomRepository;
  private final ClassMemberRepository classMemberRepository;
  private final TaClassRepository taClassRepository;
  private final PublicationRepository publicationRepository;
  private final CountedSubmissionRepository countedSubmissionRepository;
  private final ReviewItemRepository reviewItemRepository;
  private final LlmCallLogRepository llmCallLogRepository;
  private final UserRepository userRepository;
  private final ObjectMapper objectMapper;

  @Override
  @Transactional(readOnly = true)
  public List<LabSummaryResponse> listLabs(CustomUserDetails user) {
    List<Lab> labs;
    if ("admin".equals(user.getRole())) {
      labs = labRepository.findAllByOrderByIdAsc();
    } else if ("ta".equals(user.getRole())) {
      List<Integer> taClassIds = taClassRepository.findClassIdsByTaId(user.getEmail());
      labs =
          taClassIds.isEmpty() ? List.of() : labRepository.findByClassIdInOrderByIdAsc(taClassIds);
    } else {
      List<Integer> memberClassIds = classMemberRepository.findClassIdsByUserId(user.getId());
      labs =
          memberClassIds.isEmpty()
              ? List.of()
              : labRepository.findByClassIdInOrderByIdAsc(memberClassIds);
    }
    return labs.stream().map(this::toSummary).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public LabDetailResponse getLabDetail(int labId, CustomUserDetails user) {
    Lab lab =
        labRepository
            .findById(labId)
            .orElseThrow(() -> new ProblemException(ProblemKind.NOT_FOUND));

    boolean allowed =
        "ta".equals(user.getRole())
            ? taClassRepository.existsByIdTaIdAndIdClassId(user.getEmail(), lab.getClassId())
            : classMemberRepository.existsByIdClassIdAndIdUserId(lab.getClassId(), user.getId());
    if (!allowed) {
      throw new ProblemException(ProblemKind.NOT_FOUND);
    }

    boolean sampleTestsOnly = "student".equals(user.getRole());
    return toDetail(lab, sampleTestsOnly);
  }

  @Override
  @Transactional
  public LabDetailResponse createLab(CreateLabRequest request, CustomUserDetails taUser) {
    if (request == null) {
      throw new ProblemException(
          ProblemKind.VALIDATION, List.of(FieldErrorItem.forPointer("", "must be a JSON object")));
    }
    if (request.classId() == null) {
      throw new ProblemException(
          ProblemKind.VALIDATION,
          List.of(FieldErrorItem.forPointer("/classId", "must not be null")));
    }
    if (!taClassRepository.existsByIdTaIdAndIdClassId(taUser.getEmail(), request.classId())
        || !classRoomRepository.existsById(request.classId())) {
      throw new ProblemException(ProblemKind.NOT_FOUND);
    }

    validateLabPayload(request);

    Lab lab =
        Lab.builder()
            .classId(request.classId())
            .title(request.title().trim())
            .deadline(request.deadline().trim())
            .costCapUsd(BigDecimal.valueOf(request.costCapUsd()))
            .statementMd(request.statementMd() == null ? "" : request.statementMd())
            .createdBy(userRepository.getReferenceById(taUser.getId()))
            .build();
    Lab saved = labRepository.saveAndFlush(lab);

    replaceTestsAndConstraints(saved, request);
    return toDetail(saved, false);
  }

  @Override
  @Transactional
  public LabDetailResponse updateLab(
      int labId, CreateLabRequest request, CustomUserDetails taUser) {
    Lab lab =
        labRepository
            .findById(labId)
            .orElseThrow(() -> new ProblemException(ProblemKind.NOT_FOUND));
    if (!taClassRepository.existsByIdTaIdAndIdClassId(taUser.getEmail(), lab.getClassId())) {
      throw new ProblemException(ProblemKind.NOT_FOUND);
    }

    if (request == null) {
      throw new ProblemException(
          ProblemKind.VALIDATION, List.of(FieldErrorItem.forPointer("", "must be a JSON object")));
    }
    int targetClassId = request.classId() != null ? request.classId() : lab.getClassId();
    if (targetClassId != lab.getClassId()
        && (!taClassRepository.existsByIdTaIdAndIdClassId(taUser.getEmail(), targetClassId)
            || !classRoomRepository.existsById(targetClassId))) {
      throw new ProblemException(ProblemKind.NOT_FOUND);
    }

    validateLabPayload(request);

    lab.setClassId(targetClassId);
    lab.setTitle(request.title().trim());
    lab.setDeadline(request.deadline().trim());
    lab.setCostCapUsd(BigDecimal.valueOf(request.costCapUsd()));
    lab.setStatementMd(request.statementMd() == null ? "" : request.statementMd());
    Lab saved = labRepository.saveAndFlush(lab);

    replaceTestsAndConstraints(saved, request);
    return toDetail(saved, false);
  }

  private void validateLabPayload(CreateLabRequest request) {
    List<FieldErrorItem> errors = new ArrayList<>();
    if (request.title() == null || request.title().isBlank()) {
      errors.add(FieldErrorItem.forPointer("/title", "must not be blank"));
    }
    if (request.deadline() == null || request.deadline().isBlank()) {
      errors.add(FieldErrorItem.forPointer("/deadline", "must not be blank"));
    } else {
      try {
        Instant.parse(request.deadline().trim());
      } catch (DateTimeParseException e) {
        errors.add(
            FieldErrorItem.forPointer("/deadline", "must be a valid UTC ISO-8601 timestamp"));
      }
    }
    if (request.costCapUsd() == null || request.costCapUsd() < 0) {
      errors.add(FieldErrorItem.forPointer("/costCapUsd", "must be a non-negative number"));
    }

    if (request.tests() != null) {
      for (int i = 0; i < request.tests().size(); i++) {
        CreateLabTestRequest test = request.tests().get(i);
        if (test == null || test.name() == null || test.name().isBlank()) {
          errors.add(FieldErrorItem.forPointer("/tests/" + i + "/name", "must not be blank"));
        }
        if (test != null && test.timeLimitMs() != null && test.timeLimitMs() <= 0) {
          errors.add(FieldErrorItem.forPointer("/tests/" + i + "/timeLimitMs", "must be > 0"));
        }
        if (test != null && test.memoryLimitMb() != null && test.memoryLimitMb() <= 0) {
          errors.add(FieldErrorItem.forPointer("/tests/" + i + "/memoryLimitMb", "must be > 0"));
        }
      }
    }

    if (request.constraints() != null) {
      Set<String> seenIds = new LinkedHashSet<>();
      for (int i = 0; i < request.constraints().size(); i++) {
        LabConstraintDto c = request.constraints().get(i);
        if (c == null || c.constraintId() == null || c.constraintId().isBlank()) {
          errors.add(
              FieldErrorItem.forPointer(
                  "/constraints/" + i + "/constraintId", "must not be blank"));
        } else if (!seenIds.add(c.constraintId().trim())) {
          errors.add(
              FieldErrorItem.forPointer("/constraints/" + i + "/constraintId", "duplicate id"));
        }
        if (c == null || c.type() == null || !VALID_CONSTRAINT_TYPES.contains(c.type())) {
          errors.add(
              FieldErrorItem.forPointer("/constraints/" + i + "/type", "must be hard or semantic"));
        }
        if (c == null || c.description() == null || c.description().isBlank()) {
          errors.add(
              FieldErrorItem.forPointer("/constraints/" + i + "/description", "must not be blank"));
        }
        if (c != null && (c.deductionTenths() < 0 || c.deductionTenths() > 100)) {
          errors.add(
              FieldErrorItem.forPointer(
                  "/constraints/" + i + "/deductionTenths", "must be between 0 and 100"));
        }
      }
    }

    if (!errors.isEmpty()) {
      throw new ProblemException(ProblemKind.VALIDATION, errors);
    }
  }

  private void replaceTestsAndConstraints(Lab lab, CreateLabRequest request) {
    labTestRepository.deleteByLabId(lab.getId());
    labTestRepository.flush();
    if (request.tests() != null) {
      for (int i = 0; i < request.tests().size(); i++) {
        CreateLabTestRequest testReq = request.tests().get(i);
        labTestRepository.save(
            LabTest.builder()
                .lab(lab)
                .name(testReq.name().trim())
                .inputData(testReq.input() == null ? "" : testReq.input())
                .expectedOutput(testReq.expectedOutput() == null ? "" : testReq.expectedOutput())
                .timeLimitMs(testReq.timeLimitMs() != null ? testReq.timeLimitMs() : 2000)
                .memoryLimitMb(testReq.memoryLimitMb() != null ? testReq.memoryLimitMb() : 256)
                .isSample(Boolean.TRUE.equals(testReq.isSample()))
                .orderIndex(i)
                .build());
      }
      labTestRepository.flush();
    }

    labConstraintRepository.deleteByIdLabId(lab.getId());
    labConstraintRepository.flush();
    if (request.constraints() != null) {
      for (LabConstraintDto c : request.constraints()) {
        labConstraintRepository.save(
            LabConstraint.builder()
                .id(
                    LabConstraintId.builder()
                        .labId(lab.getId())
                        .constraintId(c.constraintId().trim())
                        .build())
                .lab(lab)
                .constraintType(c.type())
                .description(c.description().trim())
                .deductionTenths(c.deductionTenths())
                .build());
      }
      labConstraintRepository.flush();
    }
  }

  private LabSummaryResponse toSummary(Lab lab) {
    PublicationReceipt receipt = loadReceipt(lab.getId());
    String className =
        classRoomRepository
            .findById(lab.getClassId())
            .map(ClassRoom::getName)
            .orElse("Class " + lab.getClassId());
    BigDecimal spent = llmCallLogRepository.sumCostUsdByLabId(lab.getId());
    int submissionCount = (int) countedSubmissionRepository.countByLabId(lab.getId());
    int judgingPendingCount =
        (int)
            countedSubmissionRepository.countByLabIdAndJudgeStatusIn(
                lab.getId(), PENDING_JUDGE_STATUSES);
    int unreviewedCount = (int) reviewItemRepository.countPendingInQueueByLabId(lab.getId());

    return new LabSummaryResponse(
        lab.getId(),
        lab.getClassId(),
        className,
        lab.getTitle(),
        lab.getDeadline(),
        lab.getCostCapUsd().doubleValue(),
        spent != null ? spent.doubleValue() : 0.0,
        receipt != null,
        receipt != null ? receipt.publishedAt() : null,
        submissionCount,
        judgingPendingCount,
        unreviewedCount);
  }

  private LabDetailResponse toDetail(Lab lab, boolean sampleTestsOnly) {
    LabSummaryResponse summary = toSummary(lab);
    PublicationReceipt receipt = loadReceipt(lab.getId());

    List<LabTestCaseDto> tests =
        labTestRepository.findByLabIdOrderByOrderIndexAscIdAsc(lab.getId()).stream()
            .filter(t -> !sampleTestsOnly || Boolean.TRUE.equals(t.getIsSample()))
            .map(
                t ->
                    new LabTestCaseDto(
                        t.getId(),
                        t.getName(),
                        t.getInputData(),
                        t.getExpectedOutput(),
                        t.getTimeLimitMs(),
                        t.getMemoryLimitMb(),
                        Boolean.TRUE.equals(t.getIsSample())))
            .toList();

    List<LabConstraintDto> constraints =
        labConstraintRepository.findByIdLabIdOrderByIdConstraintIdAsc(lab.getId()).stream()
            .map(
                c ->
                    new LabConstraintDto(
                        c.getId().getConstraintId(),
                        c.getConstraintType(),
                        c.getDescription(),
                        c.getDeductionTenths()))
            .toList();

    return new LabDetailResponse(
        summary.id(),
        summary.classId(),
        summary.className(),
        summary.title(),
        summary.deadline(),
        summary.costCapUsd(),
        summary.costSpentUsd(),
        summary.isPublished(),
        summary.publishedAt(),
        summary.submissionCount(),
        summary.judgingPendingCount(),
        summary.unreviewedCount(),
        lab.getStatementMd(),
        tests,
        constraints,
        receipt);
  }

  private PublicationReceipt loadReceipt(int labId) {
    Optional<Publication> publication = publicationRepository.findById(labId);
    if (publication.isEmpty()) {
      return null;
    }
    try {
      return objectMapper.readValue(publication.get().getReceipt(), PublicationReceipt.class);
    } catch (JsonProcessingException e) {
      throw new ProblemException(ProblemKind.INTERNAL);
    }
  }
}
