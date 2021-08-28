#!/usr/bin/env bash

set -o errexit   # abort on nonzero exit status
set -o nounset   # abort on unbound variable
set -o pipefail  # don't hide errors within pipes

# This script attempts to read a comment containing a md5 checksum from a
# requirements.in file. If the checksum matches the current value of the
# checksum for the requirements.txt file, then no STDOUT is returned. If it
# does not match the path to the requirements.in to be processed is returned.

dir="$(dirname "${1}")"
in_file="${dir}/requirements.in"
txt_file="${dir}/requirements.txt"
match_text="^# requirements checksum:"
grep_cmd="$(shell which ggrep 2> /dev/null || which grep 2> /dev/null)"

# If there are no requirements files, then there is nothing to process
if [ ! -f "${in_file}" ]; then
  >&2 echo "${in_file} not found"
  exit 1
fi
if [ ! -f "${txt_file}" ]; then
  >&2 echo "${txt_file} not found - marking for creation"
  echo "${in_file}"
  exit 0
fi

in_checksum="$(${grep_cmd} "${match_text}" "${in_file}" | cut -f4 -d' ' || true)"

if [ "${in_checksum}" == "" ]; then
  >&2 echo "no checksum recorded in ${in_file}"
  echo "${in_file}"
  exit 0
fi

reqs_checksum_output="$(${grep_cmd} -v "${match_text}" "${in_file}" | openssl md5 -r)"
reqs_checksum="${reqs_checksum_output:0:32}"

if [ "${in_checksum}" != "${reqs_checksum}" ]; then
  >&2 echo "checksum [${in_checksum}] recorded in ${in_file} does not match checksum [${reqs_checksum}] of previously compiled requirements"

  echo "${in_file}"
  exit 0
fi
