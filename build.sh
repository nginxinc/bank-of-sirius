#!/usr/bin/env bash

set -o errexit   # abort on nonzero exit status
set -o nounset   # abort on unbound variable
set -o pipefail  # don't hide errors within pipes

script_dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
image_prefix="ghcr.io/nginxinc"
docker_build_opts="--no-cache"

if command -v mvn > /dev/null; then
  echo "Using system Maven [$(command -v mvn)] to build java projects"
  mvn -v
  mvn_build_cmd="mvn"
else
  echo "Using Maven from Docker image to build java projects"
  mvn_build_cmd="docker run -it --rm --name mvn_temp -v "$(pwd)":/usr/src/mymaven -w /usr/src/mymaven maven:3.3-jdk-8 mvn"
fi

maven_dirs_to_build="$(find "${script_dir}/src/" -maxdepth 2 -type f -name pom.xml -printf '%h\n')"
for dir in $maven_dirs_to_build; do
  cd "${dir}"
  $mvn_build_cmd clean package
done

dirs_to_build="$(find "${script_dir}/base-images/" "${script_dir}/src/" -maxdepth 2 -type f -name Dockerfile -printf '%h\n')"
for dir in $dirs_to_build; do
  dir_basename="$(basename "${dir}")"
  image_name="${image_prefix}/bos-${dir_basename}"
  cd "${dir}"
  echo "Building image: ${image_name}"
  docker build ${docker_build_opts} -t "${image_name}" .
done

cd "${script_dir}"
