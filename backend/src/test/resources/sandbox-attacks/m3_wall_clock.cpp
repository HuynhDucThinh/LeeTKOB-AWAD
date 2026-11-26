// M3 Attack: Ngủ 60 giây không tốn CPU để qua mặt giới hạn CPU-time.
// Kỳ vọng: Host Java theo dõi wall-clock và kill container đúng hạn (TLE).
#include <chrono>
#include <thread>

int main() {
  std::this_thread::sleep_for(std::chrono::seconds(60));
  return 0;
}
