package com.leetkob.service;

import com.leetkob.dto.lab.CreateLabRequest;
import com.leetkob.dto.lab.LabDetailResponse;
import com.leetkob.dto.lab.LabSummaryResponse;
import com.leetkob.security.CustomUserDetails;
import java.util.List;

public interface LabService {

  List<LabSummaryResponse> listLabs(CustomUserDetails user);

  LabDetailResponse getLabDetail(int labId, CustomUserDetails user);

  LabDetailResponse createLab(CreateLabRequest request, CustomUserDetails taUser);

  LabDetailResponse updateLab(int labId, CreateLabRequest request, CustomUserDetails taUser);
}
