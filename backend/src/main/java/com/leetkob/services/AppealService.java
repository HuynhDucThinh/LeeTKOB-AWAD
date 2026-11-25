package com.leetkob.services;

import com.leetkob.dtos.appeal.AppealResponse;
import com.leetkob.dtos.appeal.CreateAppealRequest;
import com.leetkob.dtos.appeal.DecideAppealRequest;
import com.leetkob.security.CustomUserDetails;
import java.util.List;

public interface AppealService {

  List<AppealResponse> listAppeals(int labId, CustomUserDetails user);

  AppealResponse createAppeal(
      int submissionId, CreateAppealRequest request, CustomUserDetails studentUser);

  AppealResponse decideAppeal(long appealId, DecideAppealRequest request, CustomUserDetails taUser);
}
