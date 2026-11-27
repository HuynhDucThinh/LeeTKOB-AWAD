#include <iostream>
#include <utility>

namespace mymath {
long long pow(long long base, long long exp, long long mod) {
  if (mod == 1) return 0;
  long long result = 1;
  base %= mod;
  while (exp > 0) {
    if (exp & 1LL) {
      result = (result * base) % mod;
    }
    base = (base * base) % mod;
    exp >>= 1;
  }
  return result;
}
}  // namespace mymath

int main() {
  int q;
  if (!(std::cin >> q)) return 0;
  while (q--) {
    long long a, e, mod;
    std::cin >> a >> e >> mod;
    std::cout << mymath::pow(a, e, mod) << '\n';
  }
  return 0;
}
