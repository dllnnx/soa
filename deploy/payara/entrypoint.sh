#!/usr/bin/env bash
set -euo pipefail

readonly secret_path=/run/secrets/payara.jks
readonly keystore_path=/opt/payara/appserver/glassfish/domains/domain1/config/keystore.p12

if [[ ! -r "$secret_path" ]]; then
  echo "Payara TLS keystore is missing or unreadable: $secret_path" >&2
  exit 1
fi

install -o payara -g payara -m 600 "$secret_path" "$keystore_path"

exec runuser -u payara -- "$@"
