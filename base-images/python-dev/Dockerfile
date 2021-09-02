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

FROM bos-python

RUN set -eux; \
    apt-get -q update; \
    apt-get install -q -y python3-psycopg2 gcc g++ python3-dev libffi-dev libpq-dev; \
    pip3 install --prefer-binary pip-tools pylint ruamel.yaml==0.17.14; \
    apt-get purge -q -y gcc g++ python3-dev libffi-dev; \
    apt-get autoremove -q -y; \
    apt-get clean; \
    rm -rf /var/lib/apt/lists/* /root/.cache/*
