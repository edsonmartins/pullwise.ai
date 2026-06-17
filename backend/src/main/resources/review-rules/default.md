## General review checklist

- **Correctness**: off-by-one errors, wrong operators, inverted conditions, unhandled return values.
- **Null / undefined safety**: dereferences without a null check, missing optional handling.
- **Error handling**: swallowed exceptions, missing error propagation, empty catch blocks.
- **Resource management**: files, sockets, streams, locks, or connections not closed/released.
- **Concurrency**: shared mutable state without synchronization, race conditions, deadlocks.
- **Input validation**: untrusted input used without validation or sanitization.
- **Secrets**: hardcoded credentials, tokens, or keys.
- **Dead / duplicated code**: copy-paste blocks, unreachable branches, unused variables.
- **Readability**: unclear names, deeply nested logic, overly long functions.
