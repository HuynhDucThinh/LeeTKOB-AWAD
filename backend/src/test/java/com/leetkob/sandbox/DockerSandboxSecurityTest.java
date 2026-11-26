package com.leetkob.sandbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.leetkob.sandbox.DockerSandboxRunner.RawExecutionResult;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Bộ kiểm thử bảo mật Docker Sandbox cho 9 kịch bản tấn công tối thiểu (M1-M9).
 *
 * <p>Yêu cầu Docker daemon đang chạy và đã build image {@code leetkob-sandbox:latest}. Chạy riêng
 * qua lệnh: {@code ./gradlew test -DincludeTags=sandbox}.
 */
@Tag("sandbox")
class DockerSandboxSecurityTest {

  private final DockerSandboxRunner runner = new DockerSandboxRunner("leetkob-sandbox:latest");

  /** Đọc nội dung tệp tấn công C++ mẫu từ thư mục test resources. */
  private String loadAttackSource(String filename) throws IOException {
    String path = "/sandbox-attacks/" + filename;
    try (InputStream is = getClass().getResourceAsStream(path)) {
      if (is == null) {
        throw new IllegalStateException("Không tìm thấy file attack: " + path);
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  /** Kiểm chứng chạy bài C++ hợp lệ trả về AC và vượt qua test case. */
  @Test
  void validCppSubmissionPassesTestCases() {
    String validCode =
        """
        #include <iostream>
        int main() {
          long long a, b;
          if (std::cin >> a >> b) {
            std::cout << (a + b) << "\\n";
          }
          return 0;
        }
        """;

    SandboxRunResult result =
        runner.compileAndRun(
            validCode,
            List.of(
                new TestCaseSpec("t1", "3 5\n", "8", 2000, 256),
                new TestCaseSpec("t2", "-10 25\n", "15", 2000, 256)));

    assertEquals("AC", result.judgeStatus());
    assertEquals(2, result.passedTests());
    assertEquals(2, result.totalTests());
  }

  /** M1: Chặn hoàn toàn kết nối mạng ra ngoài (--network=none). */
  @Test
  void m1NetworkAccessIsBlocked() throws IOException {
    String code = loadAttackSource("m1_network.cpp");
    RawExecutionResult res = runner.executeRaw(code, "", 2000, 256);

    assertEquals("RE", res.verdict());
    assertFalse(res.stdout().contains("ATTACK_SUCCEEDED"), "Mạng không được phép thông");
  }

  /** M2: Ngắt tiến trình đốt CPU vô hạn đúng giới hạn thời gian (TLE). */
  @Test
  void m2CpuInfiniteLoopHitsTimeLimit() throws IOException {
    String code = loadAttackSource("m2_cpu_limit.cpp");
    RawExecutionResult res = runner.executeRaw(code, "", 1000, 256);

    assertEquals("TLE", res.verdict());
  }

  /** M3: Ngắt tiến trình ngủ sleep(60) theo wall-clock timeout từ Host Java (TLE). */
  @Test
  void m3WallClockSleepIsKilledOnTime() throws IOException {
    String code = loadAttackSource("m3_wall_clock.cpp");
    RawExecutionResult res = runner.executeRaw(code, "", 1000, 256);

    assertEquals("TLE", res.verdict());
    assertTrue(res.timedOut(), "Container phải bị kill bởi wall-clock timeout");
  }

  /** M4: Tiến trình cấp phát vượt 256MB RAM bị OOM Killer tiêu diệt (MLE). */
  @Test
  void m4MemoryAllocationExceedingLimitTriggersOom() throws IOException {
    String code = loadAttackSource("m4_memory_oom.cpp");
    RawExecutionResult res = runner.executeRaw(code, "", 4000, 256);

    assertEquals("MLE", res.verdict());
    assertEquals(137, res.exitCode());
  }

  /** M5: Tấn công fork bomb bị chặn bởi --pids-limit=32 và không làm treo host. */
  @Test
  void m5ForkBombIsBlockedByPidsLimit() throws IOException {
    String code = loadAttackSource("m5_fork_bomb.cpp");
    RawExecutionResult res = runner.executeRaw(code, "", 3000, 256);

    assertEquals("RE", res.verdict());
    assertFalse(res.stdout().contains("ATTACK_SUCCEEDED"), "Fork bomb không được phép tạo đủ con");
  }

  /** M6: Ngắt tiến trình spam 50MB ra stdout và cắt luồng output tại 1 MiB (OLE). */
  @Test
  void m6OutputSpamIsTruncatedAtOneMegabyte() throws IOException {
    String code = loadAttackSource("m6_output_limit.cpp");
    RawExecutionResult res = runner.executeRaw(code, "", 3000, 256);

    assertEquals("OLE", res.verdict());
    assertTrue(res.outputTruncated());
    assertTrue(
        res.stdout().getBytes(StandardCharsets.UTF_8).length
            <= DockerSandboxRunner.MAX_OUTPUT_BYTES);
  }

  /** M7: Container chạy bằng user non-root (UID 10001) và bị từ chối ghi hệ thống tệp. */
  @Test
  void m7ProcessRunsAsNonRootWithReadOnlyRootfs() throws IOException {
    String code = loadAttackSource("m7_non_root.cpp");
    RawExecutionResult res = runner.executeRaw(code, "", 2000, 256);

    assertEquals("RE", res.verdict());
    assertFalse(res.stdout().contains("ATTACK_SUCCEEDED"), "Không được phép chạy quyền root");
  }

  /** M8: Không thể truy cập Docker socket từ bên trong sandbox. */
  @Test
  void m8DockerSocketIsNotMountedOrAccessible() throws IOException {
    String code = loadAttackSource("m8_docker_socket.cpp");
    RawExecutionResult res = runner.executeRaw(code, "", 2000, 256);

    assertEquals("RE", res.verdict());
    assertFalse(res.stdout().contains("ATTACK_SUCCEEDED"), "Docker socket không được phép tồn tại");
  }

  /** M9: Không có secret hay biến môi trường nhạy cảm nào bị lộ vào sandbox. */
  @Test
  void m9NoSecretsOrEnvFileInsideSandbox() throws IOException {
    String code = loadAttackSource("m9_no_secrets.cpp");
    RawExecutionResult res = runner.executeRaw(code, "", 2000, 256);

    assertEquals("RE", res.verdict());
    assertFalse(res.stdout().contains("ATTACK_SUCCEEDED"), "Không được phép lộ secret");
  }
}
