// M4 Attack: Cấp phát 512 MiB RAM và ghi/đọc từng trang 4KB qua volatile để ép cấp phát vật lý.
// Kỳ vọng: Bị OOM Killer tiêu diệt (exit 137 -> MLE) do giới hạn --memory=256m --memory-swap=256m.
#include <cstddef>
#include <cstdlib>

int main() {
  const std::size_t totalBytes = 512ULL * 1024ULL * 1024ULL;
  volatile char* buffer = static_cast<volatile char*>(std::malloc(totalBytes));
  if (buffer == nullptr) {
    return 137;
  }

  for (std::size_t i = 0; i < totalBytes; i += 4096) {
    buffer[i] = static_cast<char>(i & 0xFF);
  }

  volatile char sink = buffer[totalBytes - 4096];
  (void)sink;
  return 0;
}
