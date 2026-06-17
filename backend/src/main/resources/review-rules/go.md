## Go review checklist

- **Error handling**: ignored errors (`_ =` or unchecked returns); not wrapping with `%w`; returning both a value and a nil error inconsistently.
- **nil**: nil map writes (panic); nil pointer/interface dereferences; nil slice vs empty slice assumptions.
- **Goroutines**: leaked goroutines; missing `context` cancellation; writing to a channel with no receiver; `WaitGroup` misuse.
- **defer**: `defer` inside loops accumulating until function return; deferring `Close()` without checking its error for writes.
- **Concurrency**: data races on shared variables; missing mutex; capturing loop variables in goroutines (pre-1.22 semantics).
- **Resource cleanup**: `Body`/files/rows not closed; `rows.Err()` not checked.
- **Slices/maps**: appending to a shared slice (aliasing); concurrent map access.
- **Defensive**: unchecked type assertions (`x.(T)` without the comma-ok form).
