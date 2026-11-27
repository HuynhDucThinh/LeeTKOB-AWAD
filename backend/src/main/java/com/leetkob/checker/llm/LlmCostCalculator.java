package com.leetkob.checker.llm;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Map;

/**
 * Computes per-call USD cost (6 decimal places matching {@code NUMERIC(12, 6)}) from actual token
 * counts using published per-1M-token provider pricing.
 */
public final class LlmCostCalculator {

  private static final BigDecimal ONE_MILLION = new BigDecimal("1000000");
  private static final int COST_SCALE = 6;

  private record TokenPricing(BigDecimal inputUsdPerMillion, BigDecimal outputUsdPerMillion) {}

  private static final TokenPricing DEFAULT_PRICING =
      new TokenPricing(new BigDecimal("0.59"), new BigDecimal("0.79"));

  private static final Map<String, TokenPricing> MODEL_PRICING =
      Map.of(
          "llama-3.3-70b-versatile",
          new TokenPricing(new BigDecimal("0.59"), new BigDecimal("0.79")),
          "llama-3.1-8b-instant",
          new TokenPricing(new BigDecimal("0.05"), new BigDecimal("0.08")),
          "openai/gpt-oss-120b",
          new TokenPricing(new BigDecimal("0.15"), new BigDecimal("0.60")),
          "openai/gpt-oss-20b",
          new TokenPricing(new BigDecimal("0.075"), new BigDecimal("0.30")),
          "gpt-4o-mini",
          new TokenPricing(new BigDecimal("0.15"), new BigDecimal("0.60")),
          "gpt-4o",
          new TokenPricing(new BigDecimal("2.50"), new BigDecimal("10.00")));

  private LlmCostCalculator() {}

  public static BigDecimal calculateCostUsd(String model, int promptTokens, int completionTokens) {
    int safePrompt = Math.max(promptTokens, 0);
    int safeCompletion = Math.max(completionTokens, 0);
    if (safePrompt == 0 && safeCompletion == 0) {
      return BigDecimal.ZERO.setScale(COST_SCALE, RoundingMode.HALF_UP);
    }

    String normalizedModel = model == null ? "" : model.trim().toLowerCase(Locale.ROOT);
    TokenPricing pricing = MODEL_PRICING.getOrDefault(normalizedModel, DEFAULT_PRICING);

    BigDecimal inputCost = pricing.inputUsdPerMillion().multiply(BigDecimal.valueOf(safePrompt));
    BigDecimal outputCost =
        pricing.outputUsdPerMillion().multiply(BigDecimal.valueOf(safeCompletion));

    return inputCost.add(outputCost).divide(ONE_MILLION, COST_SCALE, RoundingMode.HALF_UP);
  }
}
