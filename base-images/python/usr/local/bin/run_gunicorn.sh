#!/usr/bin/env bash

if [ -z "${PORT}" ]; then
    >&2 echo "PORT not set"
    exit 1
fi

if [ -z "${THREADS}" ]; then
    >&2 echo "THREADS not set"
    exit 1
fi

if [ -z "${LOG_LEVEL}" ]; then
  LOG_LEVEL="info"
fi

ADDITIONAL_PARAMS=""

if [ -n "${STATSD_HOST}" ]; then
  ADDITIONAL_PARAMS="${ADDITIONAL_PARAMS} --statsd-host ${STATSD_HOST}"
fi

if [ -n "${STATSD_PREFIX}" ]; then
  ADDITIONAL_PARAMS="${ADDITIONAL_PARAMS} --statsd-prefix ${STATSD_PREFIX}"
fi

exec gunicorn \
    --bind ":${PORT}" \
    --threads "${THREADS}" \
    --log-level "${LOG_LEVEL}" \
    ${ADDITIONAL_PARAMS}
