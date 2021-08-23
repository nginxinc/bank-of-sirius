#!/usr/bin/env bash

set -o errexit   # abort on nonzero exit status
set -o nounset   # abort on unbound variable
set -o pipefail  # don't hide errors within pipes

script_dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
image_prefix="ghcr.io/nginxinc"

dirs_to_build="$(find "${script_dir}/base-images/" "${script_dir}/src/" -maxdepth 2 -type f -name Dockerfile -printf '%h\n')"
for dir in $dirs_to_build; do
  if grep --quiet 'LABEL org.opencontainers.image.source' "${dir}/Dockerfile"; then
    dir_basename="$(basename "${dir}")"
    image_name="${image_prefix}/bos-${dir_basename}"
    echo "Publishing ${image_name}"
    docker push "${image_name}"
  else
    echo "No 'LABEL org.opencontainers.image.source' found in ${dir}/Dockerfile - not publishing."
  fi
done
