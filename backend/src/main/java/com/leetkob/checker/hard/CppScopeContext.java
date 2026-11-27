package com.leetkob.checker.hard;

import java.util.Set;
import lombok.Builder;

/**
 * Immutable summary of namespace imports, aliases, and student-defined functions discovered in a
 * translation unit's token stream.
 */
@Builder
public record CppScopeContext(
    boolean hasUsingNamespaceStd,
    Set<String> importedStdSymbols,
    Set<String> stdNamespaceAliases,
    Set<Integer> directiveAndDeclarationTokenIndices,
    Set<String> userDefinedFunctionNames) {

  private static final String STD_NAMESPACE = "std";

  public boolean isDeclarationOrDirectiveToken(int tokenIndex) {
    return directiveAndDeclarationTokenIndices.contains(tokenIndex);
  }

  public boolean isStdNamespaceOrAlias(String namespaceName) {
    return STD_NAMESPACE.equals(namespaceName) || stdNamespaceAliases.contains(namespaceName);
  }

  public boolean isImportedFromStd(String symbolName) {
    return hasUsingNamespaceStd || importedStdSymbols.contains(symbolName);
  }

  public boolean isShadowedByUserFunction(String symbolName) {
    return userDefinedFunctionNames.contains(symbolName);
  }
}
