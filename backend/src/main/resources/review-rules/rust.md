## Rust review checklist

- **panics**: `unwrap()`/`expect()` on `Option`/`Result` that can realistically be `None`/`Err`; indexing that can be out of bounds.
- **Error handling**: swallowing errors; using `unwrap` instead of propagating with `?`; overly broad error types.
- **Ownership / borrowing**: unnecessary `clone()`; holding a lock guard across an `.await`; lifetimes that could be simplified.
- **Concurrency**: `Arc<Mutex<...>>` contention; potential deadlocks from lock ordering; `unsafe` blocks without justification.
- **Integer math**: overflow in release builds; casts (`as`) that truncate; missing `checked_`/`saturating_` where needed.
- **unsafe**: any `unsafe` block — verify the invariants are actually upheld and documented.
- **Iterators**: collecting where a lazy iterator suffices; allocations in hot paths.
