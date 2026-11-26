// M5 Attack: Tạo hàng loạt tiến trình con bằng fork() (Fork Bomb).
// Kỳ vọng: Bị từ chối bởi --pids-limit=32 (fork trả về -1, errno EAGAIN).
#include <cerrno>
#include <cstring>
#include <iostream>
#include <sys/wait.h>
#include <unistd.h>

int main() {
  int spawned = 0;
  for (int i = 0; i < 64; ++i) {
    pid_t pid = fork();
    if (pid < 0) {
      std::cerr << "BLOCKED: fork() failed at attempt " << i << ": " << std::strerror(errno)
                << "\n";
      while (wait(nullptr) > 0) {
      }
      return 1;
    }
    if (pid == 0) {
      sleep(2);
      _exit(0);
    }
    spawned++;
  }

  std::cout << "ATTACK_SUCCEEDED: spawned " << spawned << " child processes\n";
  while (wait(nullptr) > 0) {
  }
  return 0;
}
