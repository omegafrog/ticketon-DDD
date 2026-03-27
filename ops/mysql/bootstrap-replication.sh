#!/bin/bash

set -euo pipefail

MASTER_HOST="${MASTER_HOST:-mysql-master}"
REPLICA_HOST="${REPLICA_HOST:-mysql-replica}"
ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-password}"
REPL_USER="${REPL_USER:-repl_user}"
REPL_PASSWORD="${REPL_PASSWORD:-repl_password}"

wait_for_mysql() {
  local host="$1"
  echo "[bootstrap] Waiting for MySQL at ${host}..."
  until mysqladmin ping -h"${host}" -uroot -p"${ROOT_PASSWORD}" --silent; do
    sleep 2
  done
  echo "[bootstrap] ${host} is ready"
}

read_status_field() {
  local field="$1"
  mysql -h"${REPLICA_HOST}" -uroot -p"${ROOT_PASSWORD}" -e "SHOW REPLICA STATUS\\G" 2>/dev/null \
    | awk -F': ' -v key="$field" '$1 ~ key {print $2; exit}'
}

wait_for_mysql "${MASTER_HOST}"
wait_for_mysql "${REPLICA_HOST}"

echo "[bootstrap] Ensuring replication user exists on master"
mysql -h"${MASTER_HOST}" -uroot -p"${ROOT_PASSWORD}" -e "
CREATE USER IF NOT EXISTS '${REPL_USER}'@'%' IDENTIFIED BY '${REPL_PASSWORD}';
ALTER USER '${REPL_USER}'@'%' IDENTIFIED BY '${REPL_PASSWORD}';
GRANT REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO '${REPL_USER}'@'%';
FLUSH PRIVILEGES;
"

MASTER_STATUS=$(mysql -Nse "SHOW BINARY LOG STATUS" -h"${MASTER_HOST}" -uroot -p"${ROOT_PASSWORD}" || true)
if [ -z "${MASTER_STATUS}" ]; then
  MASTER_STATUS=$(mysql -Nse "SHOW MASTER STATUS" -h"${MASTER_HOST}" -uroot -p"${ROOT_PASSWORD}")
fi

MASTER_LOG_FILE=$(echo "${MASTER_STATUS}" | awk '{print $1}')
MASTER_LOG_POS=$(echo "${MASTER_STATUS}" | awk '{print $2}')

if [ -z "${MASTER_LOG_FILE}" ] || [ -z "${MASTER_LOG_POS}" ]; then
  echo "[bootstrap] Failed to read master binlog coordinates"
  exit 1
fi

echo "[bootstrap] Master status: file=${MASTER_LOG_FILE}, pos=${MASTER_LOG_POS}"

CURRENT_HOST=$(read_status_field "Source_Host")
CURRENT_IO=$(read_status_field "Replica_IO_Running")
CURRENT_SQL=$(read_status_field "Replica_SQL_Running")

if [ "${CURRENT_HOST}" = "${MASTER_HOST}" ] && [ "${CURRENT_IO}" = "Yes" ] && [ "${CURRENT_SQL}" = "Yes" ]; then
  echo "[bootstrap] Replica already running against ${MASTER_HOST}. Skipping reconfiguration."
  exit 0
fi

echo "[bootstrap] Configuring replica source"
mysql -h"${REPLICA_HOST}" -uroot -p"${ROOT_PASSWORD}" -e "
STOP REPLICA;
RESET REPLICA ALL;
CHANGE REPLICATION SOURCE TO
  SOURCE_HOST='${MASTER_HOST}',
  SOURCE_USER='${REPL_USER}',
  SOURCE_PASSWORD='${REPL_PASSWORD}',
  SOURCE_LOG_FILE='${MASTER_LOG_FILE}',
  SOURCE_LOG_POS=${MASTER_LOG_POS},
  GET_SOURCE_PUBLIC_KEY=1,
  SOURCE_CONNECT_RETRY=5,
  SOURCE_RETRY_COUNT=86400;
START REPLICA;
"

echo "[bootstrap] Verifying replication health"
for _ in $(seq 1 30); do
  IO_RUNNING=$(read_status_field "Replica_IO_Running")
  SQL_RUNNING=$(read_status_field "Replica_SQL_Running")
  if [ "${IO_RUNNING}" = "Yes" ] && [ "${SQL_RUNNING}" = "Yes" ]; then
    echo "[bootstrap] Replication is healthy"
    exit 0
  fi
  sleep 2
done

echo "[bootstrap] Replication did not become healthy in time"
mysql -h"${REPLICA_HOST}" -uroot -p"${ROOT_PASSWORD}" -e "SHOW REPLICA STATUS\\G" || true
exit 1
