package vn.edu.hcmus.leetkob.publish;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.edu.hcmus.leetkob.auth.AuthResolver;
import vn.edu.hcmus.leetkob.auth.Principal;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PublishController extends OncePerRequestFilter {

  private static final Pattern PUBLISH_ROUTE = Pattern.compile("^/v1/labs/([1-9][0-9]*)/publish$");
  private static final Pattern CONTENT_TYPE_JSON =
      Pattern.compile(
          "^application/json(?:\\s*;\\s*charset\\s*=\\s*(?:utf-8|\"utf-8\"))?\\s*$",
          Pattern.CASE_INSENSITIVE);
  private static final int MAX_BODY_BYTES = 4096;

  private final PublishService publishService;
  private final AuthResolver authResolver;
  private final Clock clock;
  private final ObjectMapper objectMapper;

  public PublishController(
      PublishService publishService,
      AuthResolver authResolver,
      Clock clock,
      ObjectMapper objectMapper) {
    this.publishService = publishService;
    this.authResolver = authResolver;
    this.clock = clock;
    this.objectMapper = objectMapper;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws IOException {
    String instance = "/v1";
    try {
      String path = request.getRequestURI();
      Matcher matcher = PUBLISH_ROUTE.matcher(path);
      if (!matcher.matches()) {
        throw new ProblemException(ProblemKind.NOT_FOUND);
      }
      int labId;
      try {
        labId = Integer.parseInt(matcher.group(1));
        if (labId <= 0) {
          throw new ProblemException(ProblemKind.NOT_FOUND);
        }
      } catch (NumberFormatException e) {
        throw new ProblemException(ProblemKind.NOT_FOUND);
      }

      instance = "/v1/labs/" + labId + "/publish";
      if (!"POST".equalsIgnoreCase(request.getMethod())) {
        throw new ProblemException(ProblemKind.METHOD_NOT_ALLOWED);
      }

      Optional<Principal> maybePrincipal =
          authResolver.authenticate(request.getHeader("Authorization"));
      if (maybePrincipal.isEmpty()) {
        throw new ProblemException(ProblemKind.AUTHENTICATION_REQUIRED);
      }
      Principal principal = maybePrincipal.get();
      if (!"ta".equals(principal.role())) {
        throw new ProblemException(ProblemKind.FORBIDDEN);
      }

      publishService.assertAccess(labId, principal);
      validateAndReadBody(request);

      PublishResult result = publishService.publish(labId, principal, clock.instant());
      response.setStatus(HttpServletResponse.SC_OK);
      response.setHeader("Cache-Control", "no-store");
      response.setHeader("X-Content-Type-Options", "nosniff");
      response.setHeader("Content-Type", "application/json");
      if (result.replayed()) {
        response.setHeader("Idempotent-Replayed", "true");
      }
      byte[] out = result.receiptJson().getBytes(StandardCharsets.UTF_8);
      response.setContentLength(out.length);
      response.getOutputStream().write(out);
    } catch (ProblemException e) {
      writeProblem(request, response, e.kind(), e.errors(), instance);
    } catch (Exception e) {
      writeProblem(request, response, ProblemKind.INTERNAL, null, instance);
    }
  }

  private void validateAndReadBody(HttpServletRequest request) throws IOException {
    String contentType = request.getHeader("Content-Type");
    String contentEncoding = request.getHeader("Content-Encoding");
    if (contentType == null
        || !CONTENT_TYPE_JSON.matcher(contentType).matches()
        || (contentEncoding != null && !"identity".equalsIgnoreCase(contentEncoding.trim()))) {
      throw new ProblemException(ProblemKind.UNSUPPORTED_MEDIA_TYPE);
    }

    byte[] rawBytes = readBoundedBytes(request.getInputStream());
    if (rawBytes.length == 0) {
      throw new ProblemException(ProblemKind.INVALID_JSON);
    }

    String text;
    try {
      text =
          StandardCharsets.UTF_8
              .newDecoder()
              .onMalformedInput(CodingErrorAction.REPORT)
              .onUnmappableCharacter(CodingErrorAction.REPORT)
              .decode(ByteBuffer.wrap(rawBytes))
              .toString();
    } catch (CharacterCodingException e) {
      throw new ProblemException(ProblemKind.INVALID_JSON);
    }

    JsonNode jsonNode;
    try {
      jsonNode =
          objectMapper
              .reader()
              .with(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
              .readTree(text);
    } catch (JsonProcessingException e) {
      throw new ProblemException(ProblemKind.INVALID_JSON);
    }
    if (jsonNode == null) {
      throw new ProblemException(ProblemKind.INVALID_JSON);
    }

    try {
      String trimmed = text.trim();
      if ("NaN".equals(trimmed) || "Infinity".equals(trimmed) || "-Infinity".equals(trimmed)) {
        throw new ProblemException(ProblemKind.INVALID_JSON);
      }
    } catch (ProblemException e) {
      throw e;
    }

    List<FieldErrorItem> errors = new ArrayList<>();
    if (!jsonNode.isObject()) {
      errors.add(FieldErrorItem.forPointer("", "must be a JSON object"));
    }

    for (String paramName : extractDistinctQueryParameterNames(request.getQueryString())) {
      errors.add(FieldErrorItem.forParameter(paramName, "query parameters are not supported"));
    }

    if (!errors.isEmpty()) {
      throw new ProblemException(ProblemKind.VALIDATION, errors);
    }
  }

  private byte[] readBoundedBytes(InputStream inputStream) throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    byte[] chunk = new byte[1024];
    int total = 0;
    int read;
    while ((read = inputStream.read(chunk)) != -1) {
      total += read;
      if (total > MAX_BODY_BYTES) {
        throw new ProblemException(ProblemKind.PAYLOAD_TOO_LARGE);
      }
      buffer.write(chunk, 0, read);
    }
    return buffer.toByteArray();
  }

  private Set<String> extractDistinctQueryParameterNames(String rawQuery) {
    Set<String> names = new LinkedHashSet<>();
    if (rawQuery == null || rawQuery.isEmpty()) {
      return names;
    }
    for (String pair : rawQuery.split("&")) {
      if (pair.isEmpty()) {
        continue;
      }
      int eqIdx = pair.indexOf('=');
      String rawName = eqIdx >= 0 ? pair.substring(0, eqIdx) : pair;
      String decoded = URLDecoder.decode(rawName, StandardCharsets.UTF_8);
      names.add(decoded);
    }
    return names;
  }

  private void writeProblem(
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
