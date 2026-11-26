// M2 Attack: Vòng lặp đốt CPU vô hạn.
// Kỳ vọng: Bị ngắt do giới hạn CPU / thời gian (TLE).
int main() {
  volatile unsigned long long counter = 0;
  while (true) {
    counter++;
  }
  return 0;
}
