#!/bin/sh

/opt/jboss/container/java/run/run-java.sh --bootstrap-server "${BOOTSTRAP_HOST}" \
      --bootstrap-server-port "${BOOTSTRAP_PORT}" \
      --step "${STEP}" \
      --consumes-from "${CONSUMES_FROM}" \
      --produces-to "${PRODUCES_TO}"

