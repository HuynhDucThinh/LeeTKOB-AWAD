package com.leetkob.checker.cli;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;

/** Parsed command-line arguments for {@link CheckerCliMain}. */
@Builder
public record CliArguments(Path datasetDir, Path predictionsFile, Path callLogFile) {

  public static CliArguments parse(String[] args) {
    if (args == null || args.length == 0) {
      throw new IllegalArgumentException(usageMessage("Missing arguments"));
    }

    Path datasetDir = null;
    Path predictionsFile = null;
    Path callLogFile = null;
    List<String> positional = new ArrayList<>();

    for (int i = 0; i < args.length; i++) {
      String arg = args[i];
      if ("--data".equals(arg) || "--dataset".equals(arg)) {
        datasetDir = Path.of(requireNextValue(args, i++, arg));
      } else if ("--predictions".equals(arg) || "--out".equals(arg)) {
        predictionsFile = Path.of(requireNextValue(args, i++, arg));
      } else if ("--call-log".equals(arg)) {
        callLogFile = Path.of(requireNextValue(args, i++, arg));
      } else if (arg.startsWith("-")) {
        throw new IllegalArgumentException(usageMessage("Unknown flag: " + arg));
      } else {
        positional.add(arg);
      }
    }

    if (datasetDir == null && !positional.isEmpty()) {
      datasetDir = Path.of(positional.remove(0));
    }
    if (predictionsFile == null && !positional.isEmpty()) {
      predictionsFile = Path.of(positional.remove(0));
    }
    if (!positional.isEmpty()) {
      throw new IllegalArgumentException(
          usageMessage("Unexpected positional argument: " + positional.get(0)));
    }
    if (datasetDir == null || predictionsFile == null) {
      throw new IllegalArgumentException(
          usageMessage("Both dataset directory and predictions output path are required"));
    }

    return CliArguments.builder()
        .datasetDir(datasetDir)
        .predictionsFile(predictionsFile)
        .callLogFile(callLogFile)
        .build();
  }

  private static String requireNextValue(String[] args, int flagIndex, String flagName) {
    if (flagIndex + 1 >= args.length || args[flagIndex + 1].isBlank()) {
      throw new IllegalArgumentException(usageMessage("Flag " + flagName + " requires a value"));
    }
    return args[flagIndex + 1];
  }

  private static String usageMessage(String detail) {
    return detail
        + "\nUsage: checker-cli <dataset-dir> <predictions.jsonl> [--call-log <call-log.jsonl>]"
        + "\n   or: checker-cli --data <dataset-dir> --predictions <predictions.jsonl> [--call-log <call-log.jsonl>]";
  }
}
