package vn.edu.hcmus.leetkob.publish;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FieldErrorItem(String pointer, String parameter, String detail) {

  public static FieldErrorItem forPointer(String pointer, String detail) {
    return new FieldErrorItem(pointer, null, detail);
  }

  public static FieldErrorItem forParameter(String parameter, String detail) {
    return new FieldErrorItem(null, parameter, detail);
  }
}
