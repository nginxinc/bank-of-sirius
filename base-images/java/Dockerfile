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

ENV LANG C.UTF-8
ENV PORT 8080
ENV SSL_CERT_FILE /etc/ssl/certs/ca-certificates.crt
ENV JAVA_HOME=/usr/lib/jvm/zulu8
# Configure the JVM to:
# 1. Use a non-blocking source of entropy because in containerized environments
#    blocking sources of entropy can become quickly exhausted.
# 2. Set the HEAP size dynamically based on the amount of memory specified in
#    the cgroup memory limit rather than the total memory of the host machine.
# 3. Set heap size on almost 50% of the total of the memory limit.
ENV JAVA_TOOL_OPTIONS "-Djava.security.egd=file:/dev/urandom -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -XX:MaxRAMFraction=2"

COPY app/log4j2.xml /app/log4j2.xml

RUN set -eux; \
    ZULU_REPO_VER=1.0.0-2; \
    ZULU_REPO_DEB_CHECKSUM=b8d11979d9b1959b5ff621f1021ff0dba40c7d47d948ae6ec4a4bbde98cf71f5; \
    apt-get -qq update; \
    apt-get -qq -y --no-install-recommends install \
        ca-certificates \
        curl \
        gnupg \
        software-properties-common; \
    apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys 0x219BD9C9; \
    # Use Azul distribution of the OpenJDK to allow for an escalation and support path
    curl --silent --location "https://cdn.azul.com/zulu/bin/zulu-repo_${ZULU_REPO_VER}_all.deb" > /tmp/zulu-repo.deb; \
    echo "${ZULU_REPO_DEB_CHECKSUM}  /tmp/zulu-repo.deb" | sha256sum --check; \
    dpkg --install /tmp/zulu-repo.deb; \
    apt-get -qq update; \
    apt-get -qq -y --no-install-recommends install zulu8-jdk; \
    apt-get -qq -y purge curl gnupg software-properties-common; \
    apt-get autoremove -y; \
    apt-get clean; \
    rm -f /var/lib/apt/lists/*_* /tmp/zulu-repo.deb /etc/machine-id; \
    # Use a non-blocking PRNG in order to conserve entropy on containerized systems
    sed -i 's/^\(securerandom.strongAlgorithms\)=.*$/\1=NativePRNGNonBlocking:SUN/' $JAVA_HOME/jre/lib/security/java.security; \
    mkdir --parents /app/libs /app/resources /app/classes; \
    groupadd --gid 1919 app; \
    useradd --home-dir /app --uid 1919 --gid 1919 --shell /bin/bash app; \
    chown "app:app" /app; \
    chmod -r /app/log4j2.xml

EXPOSE 8080
WORKDIR /app

# The user's Dockerfile must specify an entrypoint with ENTRYPOINT or CMD.
CMD []
