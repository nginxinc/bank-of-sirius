# Copyright 2021 F5
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

ARG ARCH=amd64

FROM $ARCH/debian:bullseye-slim

LABEL org.opencontainers.image.source=https://github.com/nginxinc/bos-python3

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
    apt-get -qq update; \
    apt-get install -qq -y ca-certificates python3 python3-pip python3-venv python3-wheel; \
    apt-get clean; \
    rm /var/lib/apt/lists/*_*

EXPOSE 8080
WORKDIR /app

# The user's Dockerfile must specify an entrypoint with ENTRYPOINT or CMD.
CMD []
