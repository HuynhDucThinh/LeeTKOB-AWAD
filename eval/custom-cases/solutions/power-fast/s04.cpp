#include <cmath>
#include <iostream>

namespace m = ::std;

long long solveQuery(long long a, long long e, long long mod) {
  if (mod == 1) return 0;
  if (e <= 1) {
    return static_cast<long long>(m::pow(static_cast<double>(a % mod), e)) % mod;
  }
  long long acc = 1 % mod;
  long long base = a % mod;
  for (long long i = 0; i < e; ++i) {
    acc = (acc * base) % mod;
  }
  return acc;
}

int main() {
  int q;
  if (!(std::cin >> q)) return 0;
  while (q--) {
    long long a, e, mod;
    std::cin >> a >> e >> mod;
    std::cout << solveQuery(a, e, mod) << '\n';
  }
  return 0;
}
