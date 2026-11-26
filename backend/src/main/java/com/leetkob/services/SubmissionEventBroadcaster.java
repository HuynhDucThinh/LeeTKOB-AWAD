package com.leetkob.services;

import com.leetkob.dtos.submission.JudgeStatusSseEvent;
import com.leetkob.entities.Submission;
import com.leetkob.repositories.SubmissionRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Quản lý và phát sự kiện SSE real-time cho trạng thái chấm bài.
 *
 * <p>Hỗ trợ cả 2 chế độ triển khai:
 *
 * <ul>
 *   <li><b>1 JVM (LEETKOB_ROLE=all):</b> {@code JudgeWorker} gọi trực tiếp {@link #publish} để đẩy
 *       sự kiện tức thời trong RAM (0ms).
 *   <li><b>2 JVM tách rời (api + worker):</b> Bộ quét nền mỗi 350ms chỉ đọc DB đối với các {@code
 *       submissionId} đang có trình duyệt mở SSE để phát hiện cập nhật từ container Worker khác.
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionEventBroadcaster {

  /** Thời gian giữ kết nối SSE tối đa: 3 phút. */
  private static final long SSE_TIMEOUT_MS = 180_000L;

  /** Chu kỳ quét DB dự phòng cho chế độ 2 JVM (ms). */
  private static final long DB_POLL_INTERVAL_MS = 350L;

  private final SubmissionRepository submissionRepository;

  /** Danh sách emitter đang mở, nhóm theo submissionId. */
  private final ConcurrentHashMap<Integer, CopyOnWriteArrayList<TrackedEmitter>>
      emittersBySubmission = new ConcurrentHashMap<>();

  private ScheduledExecutorService dbPoller;

  /** Đại diện cho một kết nối SseEmitter kèm chữ ký sự kiện cuối cùng đã gửi để tránh gửi trùng. */
  private static final class TrackedEmitter {
    private final SseEmitter emitter;
    private volatile String lastSignature;

    private TrackedEmitter(SseEmitter emitter, String initialSignature) {
      this.emitter = emitter;
      this.lastSignature = initialSignature;
    }
  }

  /** Khởi tạo luồng quét DB nhẹ phục vụ chế độ tách rời API & Worker (2 JVM). */
  @PostConstruct
  public void startDbPoller() {
    dbPoller =
        Executors.newSingleThreadScheduledExecutor(
            r -> {
              Thread t = new Thread(r, "sse-db-poller");
              t.setDaemon(true);
              return t;
            });
    dbPoller.scheduleWithFixedDelay(
        this::pollActiveSubscriptionsFromDb,
        DB_POLL_INTERVAL_MS,
        DB_POLL_INTERVAL_MS,
        TimeUnit.MILLISECONDS);
  }

  /** Dọn dẹp luồng quét DB và đóng toàn bộ emitter khi tắt ứng dụng. */
  @PreDestroy
  public void shutdown() {
    if (dbPoller != null) {
      dbPoller.shutdownNow();
    }
    emittersBySubmission.values().stream().flatMap(List::stream).forEach(t -> t.emitter.complete());
    emittersBySubmission.clear();
  }

  /**
   * Đăng ký một kết nối SSE mới cho {@code submissionId}, gửi ngay snapshot ban đầu và tự đóng nếu
   * bài nộp đã có kết quả cuối cùng.
   */
  public SseEmitter subscribe(int submissionId, JudgeStatusSseEvent initialEvent) {
    SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
    String initialSig = signatureOf(initialEvent);
    TrackedEmitter tracked = new TrackedEmitter(emitter, initialSig);

    // Bước 1: Gửi ngay trạng thái hiện tại để tránh race condition nếu Worker vừa chấm xong
    if (!sendEventSafe(emitter, initialEvent)) {
      return emitter;
    }

    // Bước 2: Nếu đã là trạng thái cuối (AC/WA/TLE/MLE/RE/CE), đóng stream ngay lập tức
    if (initialEvent.isTerminal()) {
      emitter.complete();
      return emitter;
    }

    // Bước 3: Lưu vào map theo dõi và đăng ký callback tự dọn dẹp khi client ngắt kết nối
    CopyOnWriteArrayList<TrackedEmitter> list =
        emittersBySubmission.computeIfAbsent(submissionId, k -> new CopyOnWriteArrayList<>());
    list.add(tracked);

    Runnable cleanup = () -> removeEmitter(submissionId, tracked);
    emitter.onCompletion(cleanup);
    emitter.onTimeout(
        () -> {
          emitter.complete();
          cleanup.run();
        });
    emitter.onError(ex -> cleanup.run());

    return emitter;
  }

  /**
   * Đẩy sự kiện trạng thái mới tới toàn bộ client đang lắng nghe (dùng trực tiếp ở chế độ 1 JVM).
   */
  public void publish(JudgeStatusSseEvent event) {
    if (event == null) {
      return;
    }
    CopyOnWriteArrayList<TrackedEmitter> listeners = emittersBySubmission.get(event.submissionId());
    if (listeners == null || listeners.isEmpty()) {
      return;
    }

    String sig = signatureOf(event);
    for (TrackedEmitter tracked : listeners) {
      synchronized (tracked) {
        // Bỏ qua nếu sự kiện này đã được gửi trước đó (tránh gửi lặp giữa publish & DB poller)
        if (Objects.equals(tracked.lastSignature, sig)) {
          continue;
        }
        tracked.lastSignature = sig;
        if (!sendEventSafe(tracked.emitter, event)) {
          removeEmitter(event.submissionId(), tracked);
          continue;
        }
        if (event.isTerminal()) {
          tracked.emitter.complete();
          removeEmitter(event.submissionId(), tracked);
        }
      }
    }
  }

  /**
   * Quét DB định kỳ chỉ cho các submissionId đang có kết nối SSE mở (hỗ trợ chế độ 2 JVM tách
   * container API và Worker).
   */
  @Transactional(readOnly = true)
  public void pollActiveSubscriptionsFromDb() {
    if (emittersBySubmission.isEmpty()) {
      return;
    }
    for (Integer submissionId : emittersBySubmission.keySet()) {
      try {
        Optional<Submission> subOpt = submissionRepository.findById(submissionId);
        if (subOpt.isEmpty()) {
          continue;
        }
        Submission sub = subOpt.get();
        JudgeStatusSseEvent event =
            new JudgeStatusSseEvent(
                sub.getId(),
                sub.getLab().getId(),
                sub.getJudgeStatus(),
                sub.getPassedTests(),
                sub.getTotalTests(),
                sub.getExecutionTimeMs(),
                sub.getMemoryKb(),
                sub.getCompileError(),
                sub.getScoreTenths(),
                sub.getJudgedAt());
        publish(event);
      } catch (Exception ex) {
        log.debug("SSE DB poll error for submissionId={}: {}", submissionId, ex.getMessage());
      }
    }
  }

  /** Gửi JSON event qua SseEmitter và bắt lỗi nếu client đã đóng kết nối. */
  private boolean sendEventSafe(SseEmitter emitter, JudgeStatusSseEvent event) {
    try {
      emitter.send(SseEmitter.event().data(event, MediaType.APPLICATION_JSON));
      return true;
    } catch (IOException | IllegalStateException ex) {
      return false;
    }
  }

  /** Xóa emitter khỏi danh sách theo dõi và dọn key rỗng trong map. */
  private void removeEmitter(int submissionId, TrackedEmitter tracked) {
    CopyOnWriteArrayList<TrackedEmitter> list = emittersBySubmission.get(submissionId);
    if (list != null) {
      list.remove(tracked);
      if (list.isEmpty()) {
        emittersBySubmission.remove(submissionId, list);
      }
    }
  }

  /** Tạo chuỗi chữ ký nhận diện trạng thái hiện tại của bài nộp để chống gửi lặp. */
  private String signatureOf(JudgeStatusSseEvent event) {
    return event.judgeStatus()
        + "|"
        + event.passedTests()
        + "|"
        + event.totalTests()
        + "|"
        + event.scoreTenths()
        + "|"
        + event.judgedAt();
  }
}
