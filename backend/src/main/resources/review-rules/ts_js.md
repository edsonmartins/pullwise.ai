## TypeScript / JavaScript review checklist

- **Equality**: `==` vs `===` (prefer strict); coercion surprises with `0`, `""`, `null`, `undefined`, `NaN`.
- **Async**: missing `await`; unhandled promise rejections; `await` inside loops that should run in parallel (`Promise.all`); floating promises.
- **null/undefined**: optional chaining (`?.`) and nullish coalescing (`??`) used correctly; not confusing `??` with `||` for falsy values.
- **Types**: use of `any` that hides bugs; unsafe non-null assertions (`!`); type casts that bypass checks.
- **React** (if applicable): missing/incorrect `useEffect` dependency arrays; state mutation instead of immutable updates; missing `key` in lists; expensive work not memoized.
- **Closures**: stale variables captured in callbacks/loops; `var` hoisting issues.
- **Security**: `dangerouslySetInnerHTML`, `eval`, building DOM/HTML from untrusted input (XSS); injection in template strings used for queries.
- **Error handling**: empty `catch`; swallowing errors; not narrowing `unknown` in catch clauses.
