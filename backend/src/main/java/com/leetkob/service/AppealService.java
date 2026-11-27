package com.leetkob.service;

import com.leetkob.dto.appeal.AppealResponse;
import com.leetkob.dto.appeal.CreateAppealRequest;
import com.leetkob.dto.appeal.DecideAppealRequest;
import com.leetkob.security.CustomUserDetails;
import java.util.List;

public interface AppealService {

  List<AppealResponse> listAppeals(int labId, CustomUserDetails user);

  AppealResponse createAppeal(
      int submissionId, CreateAppealRequest request, CustomUserDetails studentUser);

  AppealResponse decideAppeal(long appealId, DecideAppealRequest request, CustomUserDetails taUser);
}
