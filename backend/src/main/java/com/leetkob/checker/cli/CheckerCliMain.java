package com.leetkob.checker.cli;

import java.io.IOException;
import java.util.Map;

/**
 * Command-line entrypoint for evaluating a dataset folder ({@code problems/}, {@code solutions/})
 * and writing {@code predictions.jsonl} (plus optional {@code --call-log}).
 *
 * <p>Honors {@code MODEL_OFF=1} for fully offline grading ({@code docker run --network none}).
 */
public final class CheckerCliMain {

  private CheckerCliMain() {}

  public static void main(String[] args) {
    int exitCode = run(args, System.getenv());
    if (exitCode != 0) {
      System.exit(exitCode);
    }
  }

  public static int run(String[] args, Map<String, String> env) {
    CliArguments cliArgs;
    try {
      cliArgs = CliArguments.parse(args);
    } catch (IllegalArgumentException ex) {
      System.err.println(ex.getMessage());
      return 2;
    }

    try {
      CheckerCliRunner.builder().build().execute(cliArgs, env);
      return 0;
    } catch (IOException ex) {
      System.err.println("Checker CLI failed: " + ex.getMessage());
      return 2;
    }
  }
}
