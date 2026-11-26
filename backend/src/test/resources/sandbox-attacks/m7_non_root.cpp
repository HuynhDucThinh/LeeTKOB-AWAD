// M7 Attack: Kiểm tra quyền root (geteuid() == 0) và thử ghi file vào /etc/pwned.
// Kỳ vọng: Chạy với UID 10001 (--user 10001:10001) và bị từ chối ghi hệ thống tệp (--read-only).
#include <cerrno>
#include <cstdio>
#include <cstring>
#include <iostream>
#include <unistd.h>

int main() {
  if (geteuid() == 0) {
    std::cout << "ATTACK_SUCCEEDED: running as root (uid=0)\n";
    return 0;
  }

  FILE* f = std::fopen("/etc/pwned", "w");
  if (f != nullptr) {
    std::fclose(f);
    std::cout << "ATTACK_SUCCEEDED: wrote /etc/pwned\n";
    return 0;
  }

  std::cerr << "BLOCKED: uid=" << geteuid() << ", fopen(/etc/pwned): " << std::strerror(errno)
            << "\n";
  return 1;
}
