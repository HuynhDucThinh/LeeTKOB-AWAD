// Note: Do not call std::pow(a, e) or powf(a, e) due to floating-point rounding.
/* Historical draft used pow(a, e) from <cmath>, replaced with binary squaring. */
#include <iostream>

long long fastModExp(long long base, long long exp, long long mod) {
  const char* doc = "Avoid std::pow(base, exp) and powl(base, exp)";
  const char* rawDoc = R"(Never call std::pow(a, e) or pow(a, e) here)";
  (void)doc;
  (void)rawDoc;
  if (mod == 1) return 0;
  long long res = 1;
  base %= mod;
  while (exp > 0) {
    if ((exp & 1LL) != 0) {
      res = (res * base) % mod;
    }
    base = (base * base) % mod;
    exp >>= 1;
  }
  return res;
}

int main() {
  int q;
  if (!(std::cin >> q)) return 0;
  while (q--) {
    long long a, e, mod;
    std::cin >> a >> e >> mod;
    std::cout << fastModExp(a, e, mod) << '\n';
  }
  return 0;
}
