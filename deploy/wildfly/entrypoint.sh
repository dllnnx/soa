#!/usr/bin/env bash
set -euo pipefail

readonly secret_path=/run/secrets/wildfly.p12
readonly keystore_path=/opt/jboss/wildfly/standalone/configuration/wildfly.p12

if [[ ! -r "$secret_path" ]]; then
  echo "WildFly TLS keystore is missing or unreadable: $secret_path" >&2
  exit 1
fi

for dir in data tmp log; do
  mkdir -p "/opt/jboss/wildfly/standalone/$dir"
done
chown -R jboss:root /opt/jboss/wildfly/standalone/data \
                     /opt/jboss/wildfly/standalone/tmp \
                     /opt/jboss/wildfly/standalone/log \
                     /opt/jboss/wildfly/standalone/configuration

install -o jboss -g root -m 600 "$secret_path" "$keystore_path"

exec runuser -u jboss -- /opt/jboss/wildfly/bin/standalone.sh "$@"
