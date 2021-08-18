ARG ARCH=amd64

FROM $ARCH/debian:bullseye-slim

ENV PORT 8080

# Setup locale. This prevents Python 3 IO encoding issues.
ENV LANG C.UTF-8
# Make stdout/stderr unbuffered. This prevents delay between output and cloud
# logging collection.
ENV PYTHONUNBUFFERED 1

RUN set -eux; \
    mkdir --parents /app; \
    mkdir --parents /env; \
    groupadd --gid 1919 app; \
    useradd --home-dir /app --uid 1919 --gid 1919 --shell /bin/bash app; \
    chown "app:app" /app; \
    chown "app:app" /env; \
    apt-get -q update; \
    apt-get install -q -y ca-certificates python3 python3-pip python3-venv python3-wheel; \
    apt-get clean; \
    rm /var/lib/apt/lists/*_*

EXPOSE 8080
WORKDIR /app

# The user's Dockerfile must specify an entrypoint with ENTRYPOINT or CMD.
CMD []
