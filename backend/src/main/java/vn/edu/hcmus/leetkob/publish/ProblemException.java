package vn.edu.hcmus.leetkob.publish;

import java.util.List;

public class ProblemException extends RuntimeException {

  private final ProblemKind kind;
  private final List<FieldErrorItem> errors;

  public ProblemException(ProblemKind kind) {
    this(kind, null);
  }

  public ProblemException(ProblemKind kind, List<FieldErrorItem> errors) {
    super(kind.detail());
    this.kind = kind;
    this.errors = errors;
  }

  public ProblemKind kind() {
    return kind;
  }

  public List<FieldErrorItem> errors() {
    return errors;
  }
}
