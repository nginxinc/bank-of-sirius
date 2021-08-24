#!/usr/bin/env bash

set -o errexit   # abort on nonzero exit status
set -o nounset   # abort on unbound variable
set -o pipefail  # don't hide errors within pipes

script_dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

python_test_dirs="$(find "${script_dir}/src/" -maxdepth 2 -type f -name __init__.py -printf '%h\n')"
python_test_dirs="$(find $python_test_dirs -maxdepth 1 -type d -name tests -printf '%h\n')"

for dir in $python_test_dirs; do
  if [ -d "${dir}/tests" ]; then
    dir_basename="$(basename "${dir}")"
    echo "[${dir_basename}] running Python unit test"
    docker run -it --rm -v "${dir}/tests:/app/tests" "bos-${dir_basename}" bash -c "ln -s /app /${dir_basename} && python -m pytest -v --rootdir=/${dir_basename} -p no:warnings"
  else
    echo "No directory found at ${dir}/tests - skipping tests"
  fi
done

if "${script_dir}/mvnw" -v > /dev/null; then
  echo "Using Maven wrapper to build java projects"
  mvn_build_cmd="${script_dir}/mvnw"
else
  echo "Using Maven from Docker image to build java projects"
  mvn_build_cmd="docker run -it --rm --name mvn_temp -v "$(pwd)":/usr/src/mymaven -w /usr/src/mymaven maven:3.3-jdk-8 mvn"
fi

${mvn_build_cmd} -v

maven_dirs_to_build="$(find "${script_dir}/src/" -maxdepth 2 -type f -name pom.xml -printf '%h\n')"
for dir in $maven_dirs_to_build; do
  cd "${dir}"
  $mvn_build_cmd test
done
