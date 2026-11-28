// M9 Attack: Thử đọc secret qua __has_include lúc biên dịch, getenv() và fopen() lúc chạy.
// Kỳ vọng: Không có secret hay tệp .env nào tồn tại hoặc rò rỉ vào sandbox.
#include <cstdio>
#include <cstdlib>
#include <iostream>

#if defined(__has_include)
#if __has_include("/.env") || __has_include("/work/.env") || __has_include("/app/.env") || \
    __has_include("/backend/.env")
#define LEETKOB_COMPILE_TIME_LEAK 1
#endif
#endif

int main() {
#ifdef LEETKOB_COMPILE_TIME_LEAK
  std::cout << "ATTACK_SUCCEEDED: compile-time __has_include detected secret file\n";
  return 0;
#endif

  const char* secretKeys[] = {
      "DEMO_AUTH_TOKEN",
      "JWT_SECRET",
      "MODEL_API_KEY",
      "DB_PASSWORD",
      "POSTGRES_PASSWORD",
  };

  for (const char* key : secretKeys) {
    const char* val = std::getenv(key);
    if (val != nullptr && val[0] != '\0') {
      std::cout << "ATTACK_SUCCEEDED: leaked env " << key << "\n";
      return 0;
    }
  }

  const char* envPaths[] = {"/.env", "/work/.env", "/app/.env"};
  for (const char* path : envPaths) {
    FILE* f = std::fopen(path, "r");
    if (f != nullptr) {
      std::fclose(f);
      std::cout << "ATTACK_SUCCEEDED: opened " << path << "\n";
      return 0;
    }
  }

  std::cerr << "BLOCKED: no secrets or .env visible in sandbox\n";
  return 1;
}
