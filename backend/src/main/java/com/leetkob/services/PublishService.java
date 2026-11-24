package com.leetkob.services;

import com.leetkob.dtos.publish.PublishResult;
import com.leetkob.security.publish.Principal;
import java.time.Instant;

public interface PublishService {

  void assertAccess(int labId, Principal principal);

  PublishResult publish(int labId, Principal principal, Instant now);
}
