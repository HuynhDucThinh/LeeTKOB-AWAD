#include <iostream>

long long modPowRecursive(long long base, long long exp, long long mod) {
  if (mod == 1) return 0;
  if (exp == 0) return 1;
  long long half = modPowRecursive(base, exp / 2, mod);
  long long sq = (half * half) % mod;
  if (exp % 2 == 1) {
    return (sq * (base % mod)) % mod;
  }
  return sq;
}

int main() {
  int q;
  if (!(std::cin >> q)) return 0;
  while (q--) {
    long long a, e, mod;
    std::cin >> a >> e >> mod;
    std::cout << modPowRecursive(a % mod, e, mod) << '\n';
  }
  return 0;
}
