#!/usr/bin/env bash

set -o errexit   # abort on nonzero exit status
set -o nounset   # abort on unbound variable
set -o pipefail  # don't hide errors within pipes

script_dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
image_prefix="ghcr.io/bank-of-sirius"
docker_build_opts="--no-cache"

cd "${script_dir}/base-images/python3"
echo "Building image: ${image_prefix}/python3"
docker build ${docker_build_opts} -t "${image_prefix}/python3" .

cd "${script_dir}"

dirs_to_build="$(find "${script_dir}/src/" -maxdepth 2 -type f -name Dockerfile -printf '%h\n')"
for dir in $dirs_to_build; do
  dir_basename="$(basename "${dir}")"
  image_name="${image_prefix}/${dir_basename}"
  cd "${dir}"
  echo "Building image: ${image_name}"
  docker build ${docker_build_opts} -t "${image_name}" .
done

cd "${script_dir}"
