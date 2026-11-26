// M6 Attack: Spam 50 MB dữ liệu ra stdout để gây tràn RAM/ổ đĩa Backend.
// Kỳ vọng: Sandbox cắt luồng output tại giới hạn 1 MB và ngắt thực thi.
#include <iostream>
#include <string>

int main() {
  std::string chunk(4096, 'A');
  for (int i = 0; i < 12800; ++i) {
    std::cout << chunk;
  }
  return 0;
}
