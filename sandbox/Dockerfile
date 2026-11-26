# Sandbox image tối thiểu (M1-M9) chạy mã C++ của sinh viên
# - Base Debian bookworm-slim nhẹ, chỉ cài g++ (không curl, wget, netcat, python)
# - Chạy dưới tài khoản non-root UID/GID 10001
# image sandboxImage (leetkob-sandbox:latest)

FROM debian:bookworm-slim

RUN apt-get update \
    && apt-get install -y --no-install-recommends g++ \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd -g 10001 sandbox \
    && useradd -u 10001 -g 10001 -m -s /bin/sh sandbox \
    && mkdir -p /work \
    && chown 10001:10001 /work

WORKDIR /work
USER 10001:10001
