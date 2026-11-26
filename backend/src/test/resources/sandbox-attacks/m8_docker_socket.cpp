// M8 Attack: Thử truy cập Docker socket (/var/run/docker.sock) để thoát container.
// Kỳ vọng: Không tồn tại socket trong sandbox (ENOENT).
#include <cerrno>
#include <cstring>
#include <fcntl.h>
#include <iostream>
#include <sys/stat.h>
#include <unistd.h>

int main() {
  struct stat st {};
  if (stat("/var/run/docker.sock", &st) == 0) {
    std::cout << "ATTACK_SUCCEEDED: /var/run/docker.sock exists\n";
    return 0;
  }

  int fd = open("/var/run/docker.sock", O_RDONLY);
  if (fd >= 0) {
    close(fd);
    std::cout << "ATTACK_SUCCEEDED: opened /var/run/docker.sock\n";
    return 0;
  }

  std::cerr << "BLOCKED: /var/run/docker.sock unavailable: " << std::strerror(errno) << "\n";
  return 1;
}
