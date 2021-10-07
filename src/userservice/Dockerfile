# Copyright 2021 Google LLC
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

# The Google App Engine python runtime is Debian Jessie with Python installed
# and various os-level packages to allow installation of popular Python
# libraries. The source is on github at:
#   https://github.com/nginxinc/python-docker
FROM bos-python

LABEL org.opencontainers.image.source=https://github.com/nginxinc/bank-of-sirius

ENV PORT 8080
ENV THREADS 2

# show python logs as they occur
ENV PYTHONUNBUFFERED 0

# explicitly set a fallback log level in case no log level is defined by Kubernetes
ENV LOG_LEVEL info

# Install dependencies.
COPY requirements.txt /app/requirements.txt
RUN set -eux; \
    apt-get -q update; \
    apt-get install  --no-install-recommends -q -y \
        python3-psycopg2=2.8.6* \
        python3-cffi=1.14.5* \
        python3-sqlalchemy=1.3.22* \
        python3-wrapt=1.12.1-4*; \
    pip3 install /env/wheels/grpcio/*; \
    pip3 install --requirement /app/requirements.txt; \
    apt-get autoremove -q -y; \
    apt-get clean; \
    chown "app:app" /app; \
    rm -rf /var/lib/apt/lists/* /root/.cache /env/cache/*

# Add application code.
COPY *.py /app/

# Copy logging configuration for gunicorn
COPY logging.conf /logging.conf

# Start server using gunicorn
CMD gunicorn \
    -b :$PORT \
    --threads $THREADS \
    --statsd-host=statsd-prometheus-statsd-exporter.prometheus.svc.cluster.local:9125 \
    --statsd-prefix=bos.userservice \
    --log-config /logging.conf \
    --log-level=$LOG_LEVEL \
    "userservice:create_app()"

