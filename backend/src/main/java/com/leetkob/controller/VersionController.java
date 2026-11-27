package com.leetkob.controller;

import com.leetkob.dto.version.VersionInfoResponse;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/version")
public class VersionController {

  private static final DateTimeFormatter ISO_MILLIS =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

  private final VersionInfoResponse versionInfo;

  public VersionController(
      @Value("${leetkob.version.commit:dev}") String commit,
      @Value("${leetkob.version.tag:pa3}") String tag,
      @Value("${leetkob.version.deployed-at:}") String deployedAt) {
    String safeCommit = (commit == null || commit.isBlank()) ? "dev" : commit.trim();
    String safeTag = (tag == null || tag.isBlank()) ? null : tag.trim();
    String safeDeployedAt =
        (deployedAt == null || deployedAt.isBlank())
            ? ISO_MILLIS.format(Instant.now())
            : deployedAt.trim();
    this.versionInfo = new VersionInfoResponse(safeCommit, safeTag, safeDeployedAt);
  }

  @GetMapping
  public ResponseEntity<VersionInfoResponse> getVersion() {
    return ResponseEntity.ok(versionInfo);
  }
}
