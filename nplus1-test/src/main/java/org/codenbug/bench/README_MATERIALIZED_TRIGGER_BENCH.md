# Materialized Trigger Benchmark (JDBC)

Runner: `org.codenbug.bench.MaterializedTriggerBenchmarkMain`

It runs two scenarios against the same DB:
- `NO_MATERIALIZED`: list query computes `COUNT/MIN/MAX` via correlated subqueries.
- `MATERIALIZED_TRIGGER`: maintains `seat_layout_stats` on `seat` INSERT (trigger) and list query LEFT JOINs stats.

## H2 (default)

```bash
./gradlew :nplus1-test:run --args="--layouts 1000 --events 10000 --seats 200000 --repeats 5"
```

## MySQL

1) Create an empty database (example: `bench`) and ensure your user can create/drop tables and triggers.

2) Run with a MySQL JDBC URL:

```bash
./gradlew :nplus1-test:run --args="--jdbc jdbc:mysql://127.0.0.1:3306/bench?rewriteBatchedStatements=true&useServerPrepStmts=true&cachePrepStmts=true&useSSL=false&allowPublicKeyRetrieval=true --user root --pass password --layouts 10000 --events 100000 --seats 10000000 --batch 10000 --progressEvery 1000000 --repeats 5"
```

To avoid inserting 10M rows twice, run scenarios separately:

```bash
./gradlew :nplus1-test:run --args="--only no --jdbc jdbc:mysql://127.0.0.1:3306/bench?rewriteBatchedStatements=true --user root --pass password --layouts 10000 --events 100000 --seats 10000000"
./gradlew :nplus1-test:run --args="--only trigger --jdbc jdbc:mysql://127.0.0.1:3306/bench?rewriteBatchedStatements=true --user root --pass password --layouts 10000 --events 100000 --seats 10000000"
```

Notes:
- This runner drops/recreates `seat_layout`, `seat`, `event`, `seat_layout_stats`, and `seat_ai_stats` trigger.
- For insert throughput, the JDBC URL flags above matter a lot (`rewriteBatchedStatements=true`).
