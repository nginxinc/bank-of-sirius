# Copyright 2021 Google LLC
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

FROM bos-python3

LABEL org.opencontainers.image.source=https://github.com/nginxinc/bank-of-sirius

ENV PORT=8080
ENV THREADS=4

# show python logs as they occur
ENV PYTHONUNBUFFERED=0

# Set virtualenv environment variables. This is equivalent to running
# source /env/bin/activate. This ensures the application is executed within
# the context of the virtualenv and will have access to its dependencies.
ENV VIRTUAL_ENV /env
ENV PATH /env/bin:$PATH

# explicitly set a fallback log level in case no log level is defined by Kubernetes
ENV LOG_LEVEL info

# Install dependencies.
ADD requirements.txt /app/requirements.txt
RUN set -eux; \
    python3 -m venv --system-site-packages /env; \
    apt-get -q update; \
    apt-get install -q -y python3-psycopg2 gcc g++ python3-dev libffi-dev; \
    pip3 install -r /app/requirements.txt; \
    apt-get purge -q -y gcc g++ python3-dev libffi-dev; \
    apt-get autoremove -q -y; \
    apt-get clean; \
    rm /var/lib/apt/lists/*_*; \
    chown "app:app" /app

# Add application code.
ADD *.py /app/

# Copy logging configuration for gunicorn
COPY logging.conf /logging.conf

# Start server using gunicorn
CMD gunicorn \
    -b :$PORT \
    --threads $THREADS \
    --log-config /logging.conf \
    --log-level=$LOG_LEVEL "contacts:create_app()"
