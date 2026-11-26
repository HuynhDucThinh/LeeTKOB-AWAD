package com.leetkob.services;

import com.leetkob.dtos.lab.CreateLabRequest;
import com.leetkob.dtos.lab.LabDetailResponse;
import com.leetkob.dtos.lab.LabSummaryResponse;
import com.leetkob.security.CustomUserDetails;
import java.util.List;

public interface LabService {

  List<LabSummaryResponse> listLabs(CustomUserDetails user);

  LabDetailResponse getLabDetail(int labId, CustomUserDetails user);

  LabDetailResponse createLab(CreateLabRequest request, CustomUserDetails taUser);

  LabDetailResponse updateLab(int labId, CreateLabRequest request, CustomUserDetails taUser);
}
