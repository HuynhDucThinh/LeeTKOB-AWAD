package vn.edu.hcmus.leetkob.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import vn.edu.hcmus.leetkob.dto.FieldErrorItem;

/** Formats and writes RFC 9457 Problem Details HTTP responses with mandatory security headers. */
@Component
public class ProblemResponseWriter {

  private final ObjectMapper objectMapper;

  public ProblemResponseWriter(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public void writeProblem(
      HttpServletRequest request,
      HttpServletResponse response,
      ProblemKind kind,
      List<FieldErrorItem> errors,
      String instance)
      throws IOException {
    response.setStatus(kind.status());
    response.setHeader("Cache-Control", "no-store");
    response.setHeader("X-Content-Type-Options", "nosniff");
    response.setHeader("Content-Type", "application/problem+json");
    if (kind.status() == 401) {
      response.setHeader("WWW-Authenticate", "Bearer realm=\"leetkob\"");
    }
    if (kind.status() == 405) {
      response.setHeader("Allow", "POST");
    }
    if (kind.status() == 503) {
      response.setHeader("Retry-After", "1");
    }

    if ("HEAD".equalsIgnoreCase(request.getMethod())) {
      return;
    }

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("type", "https://leetkob.example/problems/" + kind.slug());
    body.put("title", kind.title());
    body.put("status", kind.status());
    body.put("detail", kind.detail());
    body.put("instance", instance);
    if (errors != null && !errors.isEmpty()) {
      body.put("errors", errors);
    }

    byte[] out = objectMapper.writeValueAsBytes(body);
    response.setContentLength(out.length);
    response.getOutputStream().write(out);
  }
}
