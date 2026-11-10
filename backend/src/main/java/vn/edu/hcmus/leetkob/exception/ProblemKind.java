package vn.edu.hcmus.leetkob.exception;

public enum ProblemKind {
  INVALID_JSON("invalid-json", 400, "Malformed JSON", "Request body must be valid UTF-8 JSON."),
  VALIDATION("validation", 400, "Invalid request", "Request parameters or body failed validation."),
  AUTHENTICATION_REQUIRED(
      "authentication-required",
      401,
      "Authentication required",
      "Valid Bearer authentication is required."),
  FORBIDDEN(
      "forbidden", 403, "Forbidden", "Only an assigned teaching assistant may publish a lab."),
  NOT_FOUND("not-found", 404, "Not found", "The requested resource was not found."),
  METHOD_NOT_ALLOWED(
      "method-not-allowed", 405, "Method not allowed", "Only POST is supported for this endpoint."),
  DEADLINE_NOT_REACHED(
      "deadline-not-reached",
      409,
      "Submission window open",
      "The lab submission deadline has not been reached."),
  JUDGING_PENDING(
      "judging-pending",
      409,
      "Judging pending",
      "Counted submissions are still queued or running in the judge."),
  REVIEWS_PENDING(
      "reviews-pending", 409, "Reviews pending", "Required TA reviews are not complete."),
  PAYLOAD_TOO_LARGE(
      "payload-too-large", 413, "Payload too large", "Request body exceeds the 4096-byte limit."),
  UNSUPPORTED_MEDIA_TYPE(
      "unsupported-media-type",
      415,
      "Unsupported media type",
      "Content-Type must be application/json with identity encoding."),
  PUBLICATION_UNAVAILABLE(
      "publication-unavailable",
      503,
      "Publication unavailable",
      "Publication storage is temporarily busy; retry shortly."),
  INTERNAL("internal", 500, "Internal error", "The request could not be completed.");

  private final String slug;
  private final int status;
  private final String title;
  private final String detail;

  ProblemKind(String slug, int status, String title, String detail) {
    this.slug = slug;
    this.status = status;
    this.title = title;
    this.detail = detail;
  }

  public String slug() {
    return slug;
  }

  public int status() {
    return status;
  }

  public String title() {
    return title;
  }

  public String detail() {
    return detail;
  }
}
