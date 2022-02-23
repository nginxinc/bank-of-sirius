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

FROM $ARCH/nginx:1.21.6

COPY --from=ghcr.io/nginxinc/amd64/ngx_otel_module:linux-libc-nginx-1.21.6 /usr/lib/nginx/modules/otel_ngx_module.so /usr/lib/nginx/modules/otel_ngx_module.so

COPY etc/nginx/nginx.conf /etc/nginx/nginx.conf
COPY etc/nginx/otel-nginx.toml /etc/nginx/otel-nginx.toml
COPY etc/nginx/conf.d/default.conf /etc/nginx/conf.d/default.conf
