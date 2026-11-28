// M4 Attack: Cấp phát 1 GiB RAM, ghi và đọc cộng dồn toàn bộ các trang 4KB qua volatile.
// Kỳ vọng: Bị OOM Killer tiêu diệt (exit 137 -> MLE) do giới hạn --memory=256m --memory-swap=256m.
#include <cstddef>
#include <cstdint>
#include <cstdlib>
#include <iostream>

int main() {
  const std::size_t totalBytes = 1024ULL * 1024ULL * 1024ULL;
  volatile unsigned char* buffer =
      static_cast<volatile unsigned char*>(std::malloc(totalBytes));
  if (buffer == nullptr) {
    return 137;
  }

  for (std::size_t i = 0; i < totalBytes; i += 4096) {
    buffer[i] = static_cast<unsigned char>((i >> 12) & 0xFF);
  }

  std::uint64_t sum = 0;
  for (std::size_t i = 0; i < totalBytes; i += 4096) {
    sum += buffer[i];
  }

  std::cout << "ATTACK_SUCCEEDED: allocated 1GiB, sum=" << sum << "\n";
  return 0;
}
