// M1 Attack: Thử mở kết nối TCP ra bên ngoài (1.1.1.1:80).
// Kỳ vọng: Thất bại do cờ --network=none.
#include <arpa/inet.h>
#include <cerrno>
#include <cstring>
#include <iostream>
#include <sys/socket.h>
#include <unistd.h>

int main() {
  int sock = socket(AF_INET, SOCK_STREAM, 0);
  if (sock < 0) {
    std::cerr << "BLOCKED: socket() failed: " << std::strerror(errno) << "\n";
    return 1;
  }

  sockaddr_in addr{};
  addr.sin_family = AF_INET;
  addr.sin_port = htons(80);
  inet_pton(AF_INET, "1.1.1.1", &addr.sin_addr);

  if (connect(sock, reinterpret_cast<sockaddr*>(&addr), sizeof(addr)) == 0) {
    std::cout << "ATTACK_SUCCEEDED: network reachable\n";
    close(sock);
    return 0;
  }

  std::cerr << "BLOCKED: connect() failed: " << std::strerror(errno) << "\n";
  close(sock);
  return 1;
}
