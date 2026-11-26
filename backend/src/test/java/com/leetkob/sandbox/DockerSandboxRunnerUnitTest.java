package com.leetkob.sandbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Unit tests cho các hàm xử lý thuần của DockerSandboxRunner (không yêu cầu Docker daemon). */
class DockerSandboxRunnerUnitTest {

  private final DockerSandboxRunner runner = new DockerSandboxRunner("leetkob-sandbox:latest");

  /** Kiểm tra phân loại đủ các mã thoát: CE, OLE, TLE, MLE, RE, OK. */
  @Test
  void classifyVerdictMapsExitCodesAndFlagsAccurately() {
    assertEquals("CE", runner.classifyVerdict(200, "", false, false));
    assertEquals("CE", runner.classifyVerdict(1, "__LEETKOB_CE__\nerror", false, false));
    assertEquals("OLE", runner.classifyVerdict(0, "", false, true));
    assertEquals("TLE", runner.classifyVerdict(-1, "", true, false));
    assertEquals("TLE", runner.classifyVerdict(152, "", false, false));
    assertEquals("MLE", runner.classifyVerdict(137, "", false, false));
    assertEquals("RE", runner.classifyVerdict(139, "", false, false));
    assertEquals("OK", runner.classifyVerdict(0, "", false, false));
  }

  /** Kiểm tra bóc tách batch output khớp runToken cho kết quả AC và WA (chuẩn hóa CRLF). */
  @Test
  void evaluateBatchOutputDetectsAcAndWaWithNormalizedLineEndings() {
    String token = "abc123token";
    List<TestCaseSpec> tests =
        List.of(
            new TestCaseSpec("t1", "1 2", "3", 2000, 256),
            new TestCaseSpec("t2", "4 5", "9", 2000, 256));

    String stdoutAc =
        "__START_"
            + token
            + "_0_0__\n3   \r\n__END_"
            + token
            + "_0__\n"
            + "__START_"
            + token
            + "_1_0__\n9\n__END_"
            + token
            + "_1__\n";

    SandboxRunResult acResult = runner.evaluateBatchOutput(stdoutAc, 120, tests, token);
    assertEquals("AC", acResult.judgeStatus());
    assertEquals(2, acResult.passedTests());
    assertEquals(2, acResult.totalTests());

    String stdoutWa =
        "__START_"
            + token
            + "_0_0__\n3\n__END_"
            + token
            + "_0__\n"
            + "__START_"
            + token
            + "_1_0__\n999\n__END_"
            + token
            + "_1__\n";

    SandboxRunResult waResult = runner.evaluateBatchOutput(stdoutWa, 120, tests, token);
    assertEquals("WA", waResult.judgeStatus());
    assertEquals(1, waResult.passedTests());
    assertEquals(2, waResult.totalTests());
  }

  /**
   * Kiểm tra phòng chống Output Injection: nếu sinh viên in giả mạo delimiter cố định (không khớp
   * runToken thật), parser không bị đánh lừa.
   */
  @Test
  void evaluateBatchOutputRejectsSpoofedDelimitersWithoutRunToken() {
    String realToken = "secret_real_token_999";
    List<TestCaseSpec> tests =
        List.of(
            new TestCaseSpec("t1", "1", "2", 2000, 256),
            new TestCaseSpec("t2", "2", "4", 2000, 256));

    String spoofedStdout =
        "__START_"
            + realToken
            + "_0_0__\n2\n"
            + "__END_TEST_0__\n"
            + "__START_TEST_1_0__\n4\n__END_TEST_1__\n"
            + "__END_"
            + realToken
            + "_0__\n";

    SandboxRunResult result = runner.evaluateBatchOutput(spoofedStdout, 50, tests, realToken);
    assertEquals("WA", result.judgeStatus());
    assertEquals(0, result.passedTests());
  }

  /** Kiểm tra đóng gói POSIX USTAR tar đúng bội số 512 bytes và chứa chữ ký ustar. */
  @Test
  void buildTarArchiveProducesValid512ByteAlignedUstarBytes() {
    Map<String, byte[]> files = new LinkedHashMap<>();
    files.put("solution.cpp", "int main(){return 0;}".getBytes(StandardCharsets.UTF_8));
    files.put("test_0.in", "3 5\n".getBytes(StandardCharsets.UTF_8));

    byte[] tar = runner.buildTarArchive(files);
    assertEquals(0, tar.length % 512);
    assertTrue(tar.length >= 3072);
    String magic = new String(tar, 257, 5, StandardCharsets.US_ASCII);
    assertEquals("ustar", magic);
  }
}
