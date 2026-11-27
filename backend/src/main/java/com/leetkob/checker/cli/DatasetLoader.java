package com.leetkob.checker.cli;

import com.leetkob.checker.ConstraintSpec;
import com.leetkob.checker.cli.ProblemDatasetEntry.SolutionSourceEntry;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

/**
 * Discovers problems and solutions in a {@code leetkob-eval} dataset folder strictly from {@code
 * statement.md}, {@code constraints.json}, and {@code *.cpp} files (never reading {@code
 * labels.jsonl}).
 */
@Slf4j
@Builder
@AllArgsConstructor
public class DatasetLoader {

  @Builder.Default
  private final ConstraintsJsonParser constraintsParser = new ConstraintsJsonParser();

  public DatasetLoader() {
    this(new ConstraintsJsonParser());
  }

  public List<ProblemDatasetEntry> loadDataset(Path datasetRoot) throws IOException {
    if (datasetRoot == null || !Files.isDirectory(datasetRoot)) {
      throw new IOException("Dataset directory does not exist: " + datasetRoot);
    }

    Path problemsRoot = datasetRoot.resolve("problems");
    Path solutionsRoot = datasetRoot.resolve("solutions");
    if (Files.isDirectory(problemsRoot)) {
      return loadStandardLayout(problemsRoot, solutionsRoot);
    }
    return loadFlatFallbackLayout(datasetRoot);
  }

  private List<ProblemDatasetEntry> loadStandardLayout(Path problemsRoot, Path solutionsRoot)
      throws IOException {
    List<Path> problemDirs = listSortedSubdirectories(problemsRoot);
    List<ProblemDatasetEntry> entries = new ArrayList<>(problemDirs.size());

    for (Path problemDir : problemDirs) {
      String problemId = problemDir.getFileName().toString();
      Path constraintsPath = problemDir.resolve("constraints.json");
      if (!Files.isRegularFile(constraintsPath)) {
        continue;
      }

      List<ConstraintSpec> constraints;
      try {
        constraints = constraintsParser.parseFile(constraintsPath);
      } catch (IOException ex) {
        log.warn(
            "Skipping problem '{}' with invalid constraints.json: {}", problemId, ex.getMessage());
        continue;
      }

      String statementMd = readOptionalUtf8(problemDir.resolve("statement.md"));
      Path problemSolutionsDir =
          Files.isDirectory(solutionsRoot)
              ? solutionsRoot.resolve(problemId)
              : problemDir.resolve("solutions");
      List<SolutionSourceEntry> solutions = loadSolutionsForProblem(problemId, problemSolutionsDir);

      entries.add(
          ProblemDatasetEntry.builder()
              .problemId(problemId)
              .statementMd(statementMd)
              .constraints(constraints)
              .solutions(solutions)
              .build());
    }
    return List.copyOf(entries);
  }

  private List<ProblemDatasetEntry> loadFlatFallbackLayout(Path datasetRoot) throws IOException {
    List<Path> problemDirs = listSortedSubdirectories(datasetRoot);
    List<ProblemDatasetEntry> entries = new ArrayList<>(problemDirs.size());

    for (Path problemDir : problemDirs) {
      Path constraintsPath = problemDir.resolve("constraints.json");
      if (!Files.isRegularFile(constraintsPath)) {
        continue;
      }
      String problemId = problemDir.getFileName().toString();
      List<ConstraintSpec> constraints;
      try {
        constraints = constraintsParser.parseFile(constraintsPath);
      } catch (IOException ex) {
        log.warn(
            "Skipping problem '{}' with invalid constraints.json: {}", problemId, ex.getMessage());
        continue;
      }
      String statementMd = readOptionalUtf8(problemDir.resolve("statement.md"));
      Path solutionsDir =
          Files.isDirectory(problemDir.resolve("solutions"))
              ? problemDir.resolve("solutions")
              : problemDir;
      List<SolutionSourceEntry> solutions = loadSolutionsForProblem(problemId, solutionsDir);

      entries.add(
          ProblemDatasetEntry.builder()
              .problemId(problemId)
              .statementMd(statementMd)
              .constraints(constraints)
              .solutions(solutions)
              .build());
    }
    return List.copyOf(entries);
  }

  private static List<SolutionSourceEntry> loadSolutionsForProblem(
      String problemId, Path solutionsDir) throws IOException {
    if (solutionsDir == null || !Files.isDirectory(solutionsDir)) {
      return List.of();
    }

    List<Path> cppFiles;
    try (Stream<Path> stream = Files.list(solutionsDir)) {
      cppFiles =
          stream
              .filter(Files::isRegularFile)
              .filter(p -> p.getFileName().toString().endsWith(".cpp"))
              .sorted(Comparator.comparing(p -> p.getFileName().toString()))
              .toList();
    }

    List<SolutionSourceEntry> solutions = new ArrayList<>(cppFiles.size());
    for (Path cppFile : cppFiles) {
      String fileName = cppFile.getFileName().toString();
      String stem = fileName.substring(0, fileName.length() - ".cpp".length());
      String solutionKey = problemId + "/" + stem;
      String sourceCode = Files.readString(cppFile, StandardCharsets.UTF_8);
      solutions.add(
          SolutionSourceEntry.builder()
              .solutionKey(solutionKey)
              .sourcePath(cppFile)
              .sourceCode(sourceCode)
              .build());
    }
    return List.copyOf(solutions);
  }

  private static List<Path> listSortedSubdirectories(Path parentDir) throws IOException {
    try (Stream<Path> stream = Files.list(parentDir)) {
      return stream
          .filter(Files::isDirectory)
          .sorted(Comparator.comparing(p -> p.getFileName().toString()))
          .toList();
    }
  }

  private static String readOptionalUtf8(Path filePath) throws IOException {
    if (filePath == null || !Files.isRegularFile(filePath)) {
      return "";
    }
    return Files.readString(filePath, StandardCharsets.UTF_8);
  }
}
