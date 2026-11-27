package com.leetkob.checker.hard;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Builder;

/**
 * Immutable summary of C++ namespace directives, multi-segment namespace aliases, {@code using}
 * declarations, and student-authored functions/types discovered in a translation unit.
 */
@Builder
public record CppScopeContext(
    Set<List<String>> usingNamespacePaths,
    Set<List<String>> usingDeclarations,
    Map<String, List<String>> namespaceAliases,
    Set<Integer> directiveAndDeclarationTokenIndices,
    Set<String> userDefinedFunctionNames,
    Set<String> userDefinedTypeNames,
    Set<String> userDefinedVariableNames) {

  public boolean isDeclarationOrDirectiveToken(int tokenIndex) {
    return directiveAndDeclarationTokenIndices.contains(tokenIndex);
  }

  public List<String> expandQualifiers(List<String> rawQualifiers, boolean leadingGlobalScope) {
    if (rawQualifiers.isEmpty()) {
      return List.of();
    }
    if (!leadingGlobalScope) {
      List<String> aliasedPrefix = namespaceAliases.get(rawQualifiers.get(0));
      if (aliasedPrefix != null) {
        List<String> expanded = new ArrayList<>(aliasedPrefix.size() + rawQualifiers.size() - 1);
        expanded.addAll(aliasedPrefix);
        expanded.addAll(rawQualifiers.subList(1, rawQualifiers.size()));
        return List.copyOf(expanded);
      }
    }
    return List.copyOf(rawQualifiers);
  }

  public boolean matchesQualifiedViaUsingNamespace(
      List<String> expandedQualifiers, List<String> targetNamespacePath) {
    for (List<String> importedNs : usingNamespacePaths) {
      if (importedNs.size() + expandedQualifiers.size() == targetNamespacePath.size()) {
        List<String> combined = new ArrayList<>(targetNamespacePath.size());
        combined.addAll(importedNs);
        combined.addAll(expandedQualifiers);
        if (combined.equals(targetNamespacePath)) {
          return true;
        }
      }
    }
    return false;
  }

  public boolean isImportedSymbol(List<String> targetNamespacePath, String symbolName) {
    if (usingNamespacePaths.contains(targetNamespacePath)) {
      return true;
    }
    List<String> fullSymbolPath = new ArrayList<>(targetNamespacePath.size() + 1);
    fullSymbolPath.addAll(targetNamespacePath);
    fullSymbolPath.add(symbolName);
    return usingDeclarations.contains(fullSymbolPath);
  }

  public boolean isShadowedByUserFunction(String symbolName) {
    return userDefinedFunctionNames.contains(symbolName);
  }

  public boolean isShadowedByUserSymbol(String symbolName) {
    return userDefinedFunctionNames.contains(symbolName)
        || userDefinedTypeNames.contains(symbolName)
        || (userDefinedVariableNames != null && userDefinedVariableNames.contains(symbolName));
  }
}
