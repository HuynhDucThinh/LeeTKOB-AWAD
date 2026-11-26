package com.leetkob.sandbox;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Trình chạy Sandbox C++ sử dụng Docker Container cô lập, đáp ứng đủ 9 tiêu chuẩn an ninh (M1-M9).
 *
 * <p>Tối ưu hiệu năng: Đóng gói mã nguồn và toàn bộ danh sách test case vào chung một luồng POSIX
 * tar, khởi tạo 1 container duy nhất, biên dịch {@code g++} đúng 1 lần rồi chạy lần lượt các test
 * case bên trong container (không gắn host mount {@code -v}).
 */
@Slf4j
@Component("dockerSandboxRunner")
@ConditionalOnProperty(name = "leetkob.sandbox.enabled", havingValue = "true")
public class DockerSandboxRunner implements SandboxRunner {

  /** Giới hạn dung lượng output tối đa đọc từ container: 1 MiB (M6). */
  public static final int MAX_OUTPUT_BYTES = 1024 * 1024;

  /** Mã thoát đặc biệt đánh dấu lỗi biên dịch C++ bên trong container. */
  private static final int COMPILE_ERROR_EXIT_CODE = 200;

  /** Tiền tố đánh dấu thông báo lỗi biên dịch trên stderr. */
  private static final String COMPILE_ERROR_MARKER = "__LEETKOB_CE__";

  /** Mã thoát của Linux OOM Killer (128 + SIGKILL 9). */
  private static final int EXIT_CODE_SIGKILL_OOM = 137;

  /** Mã thoát của SIGXCPU khi vượt quá giới hạn CPU ulimit -S -t (128 + SIGXCPU 24). */
  private static final int EXIT_CODE_SIGXCPU = 152;

  /** Thời gian đệm cho bước khởi tạo container và biên dịch g++ (ms). */
  private static final long COMPILE_AND_BOOT_OVERHEAD_MS = 3000L;

  private final String sandboxImage;

  /** Khởi tạo DockerSandboxRunner với tên Docker image cấu hình từ môi trường. */
  public DockerSandboxRunner(
      @Value("${leetkob.sandbox.image:${SANDBOX_IMAGE:leetkob-sandbox:latest}}")
          String sandboxImage) {
    this.sandboxImage = sandboxImage;
  }

  /** Kết quả thực thi thô của một lần chạy trong Docker Sandbox (dùng cho test và chấm bài). */
  public record RawExecutionResult(
      String verdict,
      int exitCode,
      String stdout,
      String stderr,
      int executionTimeMs,
      boolean timedOut,
      boolean outputTruncated) {}

  /**
   * Biên dịch mã nguồn C++ đúng 1 lần và chạy toàn bộ test cases trong cùng 1 container cô lập để
   * tối ưu thời gian xử lý.
   */
  @Override
  public SandboxRunResult compileAndRun(String sourceCode, List<TestCaseSpec> tests) {
    // Bước 1: Nếu bài lab chưa cấu hình test case, chạy 1 lần với stdin rỗng để kiểm tra compile
    if (tests == null || tests.isEmpty()) {
      RawExecutionResult raw = executeRaw(sourceCode, "", 2000, 256);
      return mapSingleRawToRunResult(raw, 0, 0);
    }

    // Bước 2: Đóng gói solution.cpp cùng toàn bộ file test_0.in .. test_{N-1}.in vào 1 gói tar
    String safeCode = sourceCode != null ? sourceCode : "";
    Map<String, byte[]> files = new LinkedHashMap<>();
    files.put("solution.cpp", safeCode.getBytes(StandardCharsets.UTF_8));

    int maxMemMb = 64;
    long totalTimeLimitMs = 0L;
    for (int i = 0; i < tests.size(); i++) {
      TestCaseSpec tc = tests.get(i);
      String input = tc.inputData() != null ? tc.inputData() : "";
      files.put("test_" + i + ".in", input.getBytes(StandardCharsets.UTF_8));
      int mem = tc.memoryLimitMb() > 0 ? Math.min(tc.memoryLimitMb(), 256) : 256;
      maxMemMb = Math.max(maxMemMb, mem);
      totalTimeLimitMs += (tc.timeLimitMs() > 0 ? tc.timeLimitMs() : 2000);
    }
    byte[] tarPayload = buildTarArchive(files);

    // Bước 3: Sinh token ngẫu nhiên chống giả mạo delimiter từ stdout của sinh viên và dựng lệnh
    String runToken = UUID.randomUUID().toString().replace("-", "");
    String containerName = "leetkob-sbx-" + UUID.randomUUID();
    String batchScript = buildBatchInnerScript(tests, runToken);
    List<String> command = buildDockerBaseCommand(containerName, maxMemMb, batchScript);

    // Bước 4: Khởi chạy container, bơm tar qua stdin và đọc stdout/stderr với giới hạn wall-clock
    long maxAllowedOutput = Math.max(MAX_OUTPUT_BYTES, (long) tests.size() * 64 * 1024L);
    long wallClockLimitMs = totalTimeLimitMs + COMPILE_AND_BOOT_OVERHEAD_MS;
    ContainerRunOutput runOut =
        runContainerProcess(command, tarPayload, containerName, wallClockLimitMs, maxAllowedOutput);

    // Bước 5: Kiểm tra các lỗi toàn cục trước (CE, timeout container, OOM toàn container)
    if (runOut.exitCode() == COMPILE_ERROR_EXIT_CODE
        || runOut.stderr().contains(COMPILE_ERROR_MARKER)) {
      return new SandboxRunResult(
          "CE", 0, tests.size(), runOut.elapsedMs(), null, extractCompileError(runOut.stderr()));
    }
    if (runOut.timedOut()) {
      return new SandboxRunResult("TLE", 0, tests.size(), runOut.elapsedMs(), null, null);
    }
    if (runOut.exitCode() == EXIT_CODE_SIGKILL_OOM) {
      return new SandboxRunResult("MLE", 0, tests.size(), runOut.elapsedMs(), null, null);
    }

    // Bước 6: Tách stdout của từng test case dựa theo runToken và đối chiếu đáp án kỳ vọng
    if (runOut.exitCode() != 0 && !runOut.stdout().contains("__START_" + runToken)) {
      log.warn(
          "Sandbox batch exited unexpectedly: exitCode={}, stderr={}",
          runOut.exitCode(),
          runOut.stderr());
    }
    return evaluateBatchOutput(runOut.stdout(), runOut.elapsedMs(), tests, runToken);
  }

  /**
   * Thực thi trực tiếp một đoạn mã C++ với đầu vào stdin, giới hạn thời gian và bộ nhớ trong
   * container Docker cô lập hoàn toàn (dùng cho kiểm thử bảo mật M1-M9).
   */
  public RawExecutionResult executeRaw(
      String sourceCode, String stdinData, int timeLimitMs, int memoryLimitMb) {
    // Bước 1: Chuẩn bị payload POSIX tar chứa solution.cpp và input.txt (không dùng host mount)
    String safeCode = sourceCode != null ? sourceCode : "";
    String safeInput = stdinData != null ? stdinData : "";
    int effectiveMemMb = Math.max(64, Math.min(memoryLimitMb > 0 ? memoryLimitMb : 256, 256));
    int effectiveTimeMs = Math.max(500, timeLimitMs > 0 ? timeLimitMs : 2000);
    int cpuSeconds = Math.max(1, (int) Math.ceil(effectiveTimeMs / 1000.0));

    Map<String, byte[]> files = new LinkedHashMap<>();
    files.put("solution.cpp", safeCode.getBytes(StandardCharsets.UTF_8));
    files.put("input.txt", safeInput.getBytes(StandardCharsets.UTF_8));
    byte[] tarPayload = buildTarArchive(files);

    // Bước 2: Dựng kịch bản shell đơn lẻ: giải nén -> biên dịch g++ -> exec chạy với ulimit CPU
    String innerScript =
        "tar -xf - -C /work && "
            + "if ! TMPDIR=/work g++ -O2 -std=c++17 /work/solution.cpp -o /work/solution "
            + "2>/work/compile.err; then "
            + "echo '"
            + COMPILE_ERROR_MARKER
            + "' >&2; cat /work/compile.err >&2; exit "
            + COMPILE_ERROR_EXIT_CODE
            + "; fi && "
            + "ulimit -S -t "
            + cpuSeconds
            + " && exec /work/solution < /work/input.txt";

    String containerName = "leetkob-sbx-" + UUID.randomUUID();
    List<String> command = buildDockerBaseCommand(containerName, effectiveMemMb, innerScript);

    // Bước 3: Thực thi container và thu thập luồng dữ liệu có giới hạn 1 MiB
    long wallClockLimitMs = effectiveTimeMs + COMPILE_AND_BOOT_OVERHEAD_MS;
    ContainerRunOutput runOut =
        runContainerProcess(command, tarPayload, containerName, wallClockLimitMs, MAX_OUTPUT_BYTES);

    // Bước 4: Phân loại phán quyết cuối cùng (CE / TLE / MLE / OLE / RE / OK)
    String verdict =
        classifyVerdict(
            runOut.exitCode(), runOut.stderr(), runOut.timedOut(), runOut.outputTruncated());
    return new RawExecutionResult(
        verdict,
        runOut.exitCode(),
        runOut.stdout(),
        runOut.stderr(),
        runOut.elapsedMs(),
        runOut.timedOut(),
        runOut.outputTruncated());
  }

  /** Kết quả thô cấp tiến trình thu được sau khi chạy xong container Docker. */
  private record ContainerRunOutput(
      int exitCode,
      String stdout,
      String stderr,
      int elapsedMs,
      boolean timedOut,
      boolean outputTruncated) {}

  /**
   * Khởi chạy tiến trình Docker, bơm gói tar vào stdin, đọc song song stdout/stderr có chặn ngưỡng
   * dung lượng (M6) và canh wall-clock timeout cứng (M3).
   */
  private ContainerRunOutput runContainerProcess(
      List<String> command,
      byte[] tarPayload,
      String containerName,
      long wallClockLimitMs,
      long maxOutputBytes) {
    long startNanos = System.nanoTime();
    Process process = null;
    AtomicBoolean outputTruncated = new AtomicBoolean(false);
    AtomicBoolean timedOut = new AtomicBoolean(false);

    try {
      // Bước 1: Khởi tạo tiến trình con chạy docker run
      process = new ProcessBuilder(command).start();

      // Bước 2: Đẩy toàn bộ gói tar vào stdin và đóng ngay để lệnh tar bên trong kết thúc đọc
      try (OutputStream stdin = process.getOutputStream()) {
        stdin.write(tarPayload);
        stdin.flush();
      }

      // Bước 3: Mở 2 thread đọc song song stdout và stderr có kiểm soát giới hạn bộ nhớ output
      ByteArrayOutputStream stdoutBuf = new ByteArrayOutputStream();
      ByteArrayOutputStream stderrBuf = new ByteArrayOutputStream();
      Process runningProcess = process;

      Thread stdoutReader =
          new Thread(
              () ->
                  readStreamCapped(
                      runningProcess.getInputStream(),
                      stdoutBuf,
                      outputTruncated,
                      containerName,
                      maxOutputBytes),
              "sbx-stdout");
      Thread stderrReader =
          new Thread(
              () ->
                  readStreamCapped(
                      runningProcess.getErrorStream(),
                      stderrBuf,
                      outputTruncated,
                      containerName,
                      MAX_OUTPUT_BYTES),
              "sbx-stderr");
      stdoutReader.start();
      stderrReader.start();

      // Bước 4: Đợi kết thúc theo wall-clock timeout; nếu quá hạn thì ép hủy container (M3)
      boolean finished = process.waitFor(wallClockLimitMs, TimeUnit.MILLISECONDS);
      if (!finished) {
        timedOut.set(true);
        forceKillContainer(containerName);
        process.destroyForcibly();
        process.waitFor(2, TimeUnit.SECONDS);
      }

      stdoutReader.join(2000);
      stderrReader.join(2000);

      int elapsedMs = (int) TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
      int exitCode = finished ? process.exitValue() : -1;
      return new ContainerRunOutput(
          exitCode,
          stdoutBuf.toString(StandardCharsets.UTF_8),
          stderrBuf.toString(StandardCharsets.UTF_8),
          elapsedMs,
          timedOut.get(),
          outputTruncated.get());

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      forceKillContainer(containerName);
      throw new IllegalStateException("Tiến trình chạy Sandbox bị gián đoạn", e);
    } catch (IOException e) {
      forceKillContainer(containerName);
      throw new IllegalStateException("Không thể khởi chạy Docker Sandbox", e);
    } finally {
      if (process != null && process.isAlive()) {
        process.destroyForcibly();
      }
    }
  }

  /**
   * Tạo kịch bản shell bên trong container để biên dịch solution.cpp đúng 1 lần rồi chạy vòng lặp
   * lần lượt qua từng tệp test_i.in kèm phân tách kết quả bằng runToken.
   */
  private String buildBatchInnerScript(List<TestCaseSpec> tests, String runToken) {
    StringBuilder sb = new StringBuilder();
    sb.append("tar -xf - -C /work && ");
    sb.append(
        "if ! TMPDIR=/work g++ -O2 -std=c++17 /work/solution.cpp -o /work/solution 2>/work/compile.err; then ");
    sb.append("echo ").append(COMPILE_ERROR_MARKER).append(" >&2; ");
    sb.append("cat /work/compile.err >&2; exit ").append(COMPILE_ERROR_EXIT_CODE).append("; fi; ");

    for (int i = 0; i < tests.size(); i++) {
      int timeMs = tests.get(i).timeLimitMs() > 0 ? tests.get(i).timeLimitMs() : 2000;
      int cpuSec = Math.max(1, (int) Math.ceil(timeMs / 1000.0));
      sb.append("(ulimit -S -t ")
          .append(cpuSec)
          .append("; /work/solution < /work/test_")
          .append(i)
          .append(".in > /work/out_")
          .append(i)
          .append(".txt 2>/dev/null); ");
      sb.append("rc=$?; ");
      sb.append("echo __START_").append(runToken).append("_").append(i).append("_${rc}__; ");
      sb.append("if [ $rc -eq 0 ]; then ");
      sb.append("head -c ")
          .append(MAX_OUTPUT_BYTES + 1)
          .append(" /work/out_")
          .append(i)
          .append(".txt; ");
      sb.append("fi; ");
      sb.append("rm -f /work/out_").append(i).append(".txt; ");
      sb.append("echo; ");
      sb.append("echo __END_").append(runToken).append("_").append(i).append("__; ");
      sb.append("if [ $rc -ne 0 ]; then break; fi; ");
    }
    return sb.toString();
  }

  /** Phân tích stdout của cả batch bằng runToken để tính số test đúng và phán quyết cuối cùng. */
  SandboxRunResult evaluateBatchOutput(
      String fullStdout, int elapsedMs, List<TestCaseSpec> tests, String runToken) {
    int passedCount = 0;
    String firstFailureVerdict = null;

    for (int i = 0; i < tests.size(); i++) {
      String startPrefix = "__START_" + runToken + "_" + i + "_";
      int startIdx = fullStdout.indexOf(startPrefix);
      if (startIdx < 0) {
        if (firstFailureVerdict == null) {
          firstFailureVerdict = "RE";
        }
        break;
      }

      int rcStart = startIdx + startPrefix.length();
      int rcEnd = fullStdout.indexOf("__", rcStart);
      if (rcEnd < 0) {
        if (firstFailureVerdict == null) {
          firstFailureVerdict = "RE";
        }
        break;
      }

      int exitCode = Integer.parseInt(fullStdout.substring(rcStart, rcEnd));
      if (exitCode != 0) {
        String errVerdict = classifyVerdict(exitCode, "", false, false);
        return new SandboxRunResult(errVerdict, passedCount, tests.size(), elapsedMs, null, null);
      }

      int contentStart = rcEnd + 2;
      if (contentStart < fullStdout.length() && fullStdout.charAt(contentStart) == '\n') {
        contentStart++;
      }
      String endMarker = "__END_" + runToken + "_" + i + "__";
      int endIdx = fullStdout.indexOf(endMarker, contentStart);
      if (endIdx < 0) {
        return new SandboxRunResult("OLE", passedCount, tests.size(), elapsedMs, null, null);
      }

      String testOutput = fullStdout.substring(contentStart, endIdx);
      if (testOutput.getBytes(StandardCharsets.UTF_8).length > MAX_OUTPUT_BYTES) {
        return new SandboxRunResult("OLE", passedCount, tests.size(), elapsedMs, null, null);
      }

      if (outputsMatch(testOutput, tests.get(i).expectedOutput())) {
        passedCount++;
      } else if (firstFailureVerdict == null) {
        firstFailureVerdict = "WA";
      }
    }

    String finalVerdict = (passedCount == tests.size()) ? "AC" : firstFailureVerdict;
    return new SandboxRunResult(finalVerdict, passedCount, tests.size(), elapsedMs, null, null);
  }

  /** Tạo danh sách tham số dòng lệnh docker run tích hợp đầy đủ các cờ cô lập M1-M9. */
  // cmd tạo container từ image ở sandbox/dockerFile (debian image)
  private List<String> buildDockerBaseCommand(
      String containerName, int memoryMb, String innerScript) {
    List<String> cmd = new ArrayList<>();
    cmd.add("docker");
    cmd.add("run");
    cmd.add("--rm");
    cmd.add("-i");
    cmd.add("--name");
    cmd.add(containerName);

    // M1: Cắt hoàn toàn kết nối mạng
    cmd.add("--network=none");
    // M2: Giới hạn tài nguyên CPU
    cmd.add("--cpus=1");
    // M4: Giới hạn RAM vật lý và khóa cứng swap (tránh né OOM qua swap)
    cmd.add("--memory=" + memoryMb + "m");
    cmd.add("--memory-swap=" + memoryMb + "m");
    // M5: Giới hạn số lượng tiến trình tối đa chống fork bomb
    cmd.add("--pids-limit=32");
    // M7 & M8: Root filesystem chỉ đọc; chỉ cấp tmpfs cho /work (cho phép exec)
    cmd.add("--read-only");
    cmd.add("--tmpfs");
    cmd.add("/work:rw,exec,nosuid,nodev,size=64m,uid=10001,gid=10001");
    cmd.add("--user");
    cmd.add("10001:10001");
    cmd.add("--cap-drop=ALL");
    cmd.add("--security-opt=no-new-privileges");
    cmd.add(sandboxImage);

    cmd.add("sh");
    cmd.add("-c");
    cmd.add(innerScript);
    return cmd;
  }

  /** Đọc luồng dữ liệu từ container với giới hạn tối đa maxBytes (M6). */
  private void readStreamCapped(
      InputStream in,
      ByteArrayOutputStream out,
      AtomicBoolean truncated,
      String containerName,
      long maxBytes) {
    byte[] buffer = new byte[4096];
    try {
      int read;
      while ((read = in.read(buffer)) != -1) {
        long remaining = maxBytes - out.size();
        if (remaining <= 0) {
          if (truncated.compareAndSet(false, true)) {
            forceKillContainer(containerName);
          }
          break;
        }
        int toWrite = (int) Math.min(read, remaining);
        out.write(buffer, 0, toWrite);
        if (read > remaining && truncated.compareAndSet(false, true)) {
          forceKillContainer(containerName);
          break;
        }
      }
    } catch (IOException ignore) {
      // Luồng đóng khi container bị kill
    }
  }

  /** Buộc dừng và xóa container ngay lập tức khi vượt quá thời gian hoặc giới hạn output. */
  private void forceKillContainer(String containerName) {
    try {
      Process killProc = new ProcessBuilder("docker", "rm", "-f", containerName).start();
      killProc.waitFor(3, TimeUnit.SECONDS);
    } catch (Exception ignore) {
      // Container có thể đã tự thoát kèm --rm
    }
  }

  /** Phân loại phán quyết dựa trên exit code, cờ timeout, cờ tràn output và stderr. */
  String classifyVerdict(int exitCode, String stderr, boolean timedOut, boolean outputTruncated) {
    if (exitCode == COMPILE_ERROR_EXIT_CODE
        || (stderr != null && stderr.contains(COMPILE_ERROR_MARKER))) {
      return "CE";
    }
    if (outputTruncated) {
      return "OLE";
    }
    if (timedOut || exitCode == EXIT_CODE_SIGXCPU) {
      return "TLE";
    }
    if (exitCode == EXIT_CODE_SIGKILL_OOM) {
      return "MLE";
    }
    if (exitCode != 0) {
      return "RE";
    }
    return "OK";
  }

  /** Chuyển đổi kết quả chạy thô sang đối tượng SandboxRunResult khi không có danh sách test. */
  private SandboxRunResult mapSingleRawToRunResult(
      RawExecutionResult raw, int passedTests, int totalTests) {
    String verdict = "OK".equals(raw.verdict()) ? "AC" : raw.verdict();
    String compileErr = "CE".equals(verdict) ? extractCompileError(raw.stderr()) : null;
    return new SandboxRunResult(
        verdict, passedTests, totalTests, raw.executionTimeMs(), null, compileErr);
  }

  /** Trích xuất nội dung lỗi biên dịch sạch sau tiền tố đánh dấu __LEETKOB_CE__. */
  private String extractCompileError(String stderr) {
    if (stderr == null) {
      return "";
    }
    int idx = stderr.indexOf(COMPILE_ERROR_MARKER);
    if (idx >= 0) {
      return stderr.substring(idx + COMPILE_ERROR_MARKER.length()).trim();
    }
    return stderr.trim();
  }

  /** So sánh kết quả thực tế với đáp án kỳ vọng (bỏ qua khác biệt CRLF và khoảng trắng cuối). */
  private boolean outputsMatch(String actual, String expected) {
    String normActual = normalizeOutput(actual);
    String normExpected = normalizeOutput(expected);
    return normActual.equals(normExpected);
  }

  /** Chuẩn hóa chuỗi đầu ra về định dạng LF và cắt khoảng trắng thừa ở cuối. */
  private String normalizeOutput(String raw) {
    if (raw == null) {
      return "";
    }
    return raw.replace("\r\n", "\n").stripTrailing();
  }

  /** Đóng gói bản đồ tên tệp và nội dung byte thành định dạng lưu trữ POSIX USTAR tar. */
  byte[] buildTarArchive(Map<String, byte[]> files) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    for (Map.Entry<String, byte[]> entry : files.entrySet()) {
      byte[] content = entry.getValue();
      byte[] header = createTarHeader(entry.getKey(), content.length);
      out.writeBytes(header);
      out.writeBytes(content);
      int padding = (512 - (content.length % 512)) % 512;
      if (padding > 0) {
        out.writeBytes(new byte[padding]);
      }
    }
    // Hai block 512-byte rỗng đánh dấu kết thúc luồng tar
    out.writeBytes(new byte[1024]);
    return out.toByteArray();
  }

  /** Tạo khối header 512-byte chuẩn POSIX USTAR cho một tệp tin trong tar archive. */
  private byte[] createTarHeader(String fileName, int fileSize) {
    byte[] header = new byte[512];
    writeAscii(header, 0, 100, fileName);
    writeAscii(header, 100, 8, "0000644\0");
    writeAscii(header, 108, 8, "0000000\0");
    writeAscii(header, 116, 8, "0000000\0");
    writeAscii(header, 124, 12, String.format("%011o\0", fileSize));
    writeAscii(header, 136, 12, String.format("%011o\0", System.currentTimeMillis() / 1000L));

    // Điền tạm 8 dấu cách vào trường checksum trước khi tính tổng byte
    for (int i = 148; i < 156; i++) {
      header[i] = ' ';
    }
    header[156] = '0'; // Regular file
    writeAscii(header, 257, 6, "ustar\0");
    writeAscii(header, 263, 2, "00");

    long checksum = 0;
    for (byte b : header) {
      checksum += (b & 0xFF);
    }
    writeAscii(header, 148, 8, String.format("%06o\0 ", checksum));
    return header;
  }

  /** Ghi chuỗi ASCII vào mảng byte header trong giới hạn độ dài cho phép. */
  private void writeAscii(byte[] dest, int offset, int maxLen, String value) {
    byte[] bytes = value.getBytes(StandardCharsets.US_ASCII);
    int len = Math.min(bytes.length, maxLen);
    System.arraycopy(bytes, 0, dest, offset, len);
  }
}
