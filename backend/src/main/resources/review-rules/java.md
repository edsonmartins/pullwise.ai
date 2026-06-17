## Java / Kotlin review checklist

- **NullPointerException**: dereferencing values that can be null; prefer `Optional`, `@Nullable` contracts, or explicit checks.
- **Resource leaks**: streams, JDBC `Connection`/`Statement`/`ResultSet`, or `Closeable` not in try-with-resources.
- **Collections**: modifying a collection while iterating; returning mutable internal collections; using `==` instead of `.equals()`.
- **equals/hashCode**: overriding one without the other; mutable fields used in `hashCode`.
- **Concurrency**: non-thread-safe fields in singletons/beans; missing `volatile`/`synchronized`; `SimpleDateFormat` shared across threads.
- **Streams**: side effects inside `map`/`filter`; infinite or unbounded streams; reusing a consumed stream.
- **Spring specifics**: field injection over constructor injection; `@Transactional` on private/self-invoked methods; N+1 queries from lazy associations; missing `@Transactional(readOnly=true)`.
- **Exceptions**: catching `Exception`/`Throwable` broadly; losing the cause when rethrowing; logging and rethrowing (double logging).
- **Resource-heavy work** inside loops (DB calls, allocations) that should be batched or hoisted.
