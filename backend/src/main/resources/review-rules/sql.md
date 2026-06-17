## SQL / migration review checklist

- **Injection**: dynamic SQL built by string concatenation instead of parameterized queries.
- **Destructive migrations**: `DROP`/`TRUNCATE`/`ALTER ... DROP COLUMN` without a backout plan; data loss on rollback.
- **Locking**: schema changes on large tables that take long locks (adding non-null columns with defaults, index builds) without `CONCURRENTLY`/online strategy.
- **Indexes**: missing indexes on FK/`WHERE`/`JOIN` columns; redundant or duplicate indexes.
- **Transactions**: multi-statement changes not wrapped atomically; mixing DDL and DML unsafely.
- **NULL handling**: `NOT NULL` added without a default/backfill; `= NULL` instead of `IS NULL`.
- **Performance**: `SELECT *`, implicit cartesian joins, functions on indexed columns in `WHERE`.
- **Idempotency / reversibility** of the migration (Flyway: matching down or forward-fix plan).
