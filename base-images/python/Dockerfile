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

LABEL org.opencontainers.image.source=https://github.com/nginxinc/bank-of-sirius

ENV PORT 8080

# Setup locale. This prevents Python 3 IO encoding issues.
ENV LANG C.UTF-8
# Make stdout/stderr unbuffered. This prevents delay between output and cloud
# logging collection.
ENV PYTHONUNBUFFERED 1

# Set virtualenv environment variables. This is equivalent to running
# source /env/bin/activate. This ensures the application is executed within
# the context of the virtualenv and will have access to its dependencies.
ENV VIRTUAL_ENV /env
# Python grpc libraries should use the host's native DNS resolution
ENV GRPC_DNS_RESOLVER native
ENV PIP_CONFIG_FILE /env/etc/pip.conf
ENV PATH /env/bin:$PATH

COPY env/etc/pip.conf /env/etc/pip.conf
COPY usr/local/bin/run_gunicorn.sh /usr/local/bin/run_gunicorn.sh

RUN set -eux; \
    chmod +x /usr/local/bin/run_gunicorn.sh; \
    mkdir --parents /app; \
    mkdir --parents /env; \
    groupadd --gid 1919 app; \
    useradd --home-dir /app --uid 1919 --gid 1919 --shell /bin/bash app; \
    chown "app:app" /app; \
    chown "app:app" /env; \
    apt-get -qq update; \
    apt-get install -qq -y ca-certificates python3 python3-pip python3-venv python3-wheel; \
    apt-get clean; \
    python3 -m venv --system-site-packages /env; \
    mkdir -p /env/cache /env/wheels/pytest /env/wheels/grpcio; \
    # Download wheels for frequently needed Python packages
    /env/bin/pip3 download --dest /env/wheels/pytest pytest; \
    /env/bin/pip3 download --dest /env/wheels/grpcio grpcio==1.39.0; \
    rm -rf /var/lib/apt/lists/* /root/.cache /env/cache/* /etc/machine-id

EXPOSE 8080
WORKDIR /app

# Start server using gunicorn
CMD /usr/local/bin/run_gunicorn.sh
