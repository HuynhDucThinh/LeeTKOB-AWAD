#include <iostream>

long long binaryExponentiation(long long a, long long e, long long mod) {
  if (mod == 1) return 0;
  long long result = 1 % mod;
  long long factor = a % mod;
  for (long long step = 0; step < e; ++step) {
    result = (result * factor) % mod;
  }
  return result;
}

int main() {
  int q;
  if (!(std::cin >> q)) return 0;
  while (q--) {
    long long a, e, mod;
    std::cin >> a >> e >> mod;
    std::cout << binaryExponentiation(a, e, mod) << '\n';
  }
  return 0;
}
