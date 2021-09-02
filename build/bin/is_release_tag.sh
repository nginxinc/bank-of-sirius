#!/usr/bin/env bash

set -o nounset   # abort on unbound variable
set -o pipefail  # don't hide errors within pipes

BRANCH="$(git branch --show-current)"
BRANCH="${BRANCH//[$'\t\r\n']}"

if [ "${BRANCH}" != "master" ]; then
  >&2 echo "not on master branch"
  exit 1
fi

if [ -z "${VERSION:-}" ]; then
  >&2 echo "reading version from file .version"
  VERSION="$(cat .version)"
  VERSION="${VERSION//[$'\t\r\n']}"

  if [ -z "${VERSION:-}" ]; then
    >&2 echo ".version file contents are blank"
    exit 1
  fi
else
  >&2 echo "reading version from file env var VERSION"
fi

TAG_COMMIT="$(git rev-list -n1 "v${VERSION}" 2> /dev/null)"
TAG_COMMIT="${TAG_COMMIT//[$'\t\r\n']}"

if [ -z "${TAG_COMMIT:-}" ]; then
  >&2 echo "no tag associated with version [${VERSION}]"
  exit 1
fi

CURRENT_COMMIT="$(git rev-parse HEAD)"
CURRENT_COMMIT="${CURRENT_COMMIT//[$'\t\r\n']/}"

if [ -z "${CURRENT_COMMIT:-}" ]; then
  >&2 echo "no commit hash returned from git"
  exit 1
fi

if [ "${TAG_COMMIT}" == "${CURRENT_COMMIT}" ]; then
  >&2 echo "version tag commit hash matches current commit hash"
  exit 0
else
  >&2 echo "version tag commit hash does not match current commit hash"
  exit 1
fi
