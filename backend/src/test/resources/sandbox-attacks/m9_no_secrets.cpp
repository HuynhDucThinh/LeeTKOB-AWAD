// M9 Attack: Thử đọc các biến môi trường chứa secret hoặc tệp .env từ container.
// Kỳ vọng: Không có secret nào được truyền vào môi trường sandbox.
#include <cstdio>
#include <cstdlib>
#include <iostream>

int main() {
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
