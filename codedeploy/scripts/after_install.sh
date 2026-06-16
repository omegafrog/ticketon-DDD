#!/usr/bin/env bash
set -euo pipefail

test -r /etc/ticketon-deploy.env
test -x "$(command -v aws)"
