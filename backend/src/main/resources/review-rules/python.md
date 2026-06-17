## Python review checklist

- **Mutable default arguments**: `def f(x=[])` / `={}` shared across calls.
- **Exception handling**: bare `except:`; catching `Exception` too broadly; swallowing errors; not chaining with `raise ... from`.
- **Resource management**: files/sockets/locks not opened with `with`.
- **Comparisons**: `is` vs `==` (use `is` only for `None`/singletons); truthiness of empty collections.
- **Concurrency**: GIL assumptions; shared state across threads; blocking calls inside async coroutines.
- **f-strings / formatting** built from untrusted input used in SQL/shell/paths (injection).
- **Typing**: missing or misleading type hints on public functions; `Optional` not handled.
- **Performance**: building large lists where a generator suffices; repeated work in loops; `+` string concatenation in tight loops.
- **Imports / side effects** at module import time that can fail or slow startup.
