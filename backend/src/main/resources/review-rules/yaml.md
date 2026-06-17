## YAML / config review checklist

- **Secrets**: hardcoded passwords, tokens, API keys, or connection strings that should come from a secret store / env vars.
- **Indentation**: structural mistakes (tabs vs spaces, wrong nesting) that change meaning.
- **Booleans / types**: unquoted values like `yes`/`no`/`on`/`off` coerced to booleans unexpectedly; numbers parsed as strings or vice versa.
- **Environment exposure**: debug flags, verbose logging, permissive CORS, or `*` wildcards enabled in production configs.
- **Resource limits**: missing CPU/memory limits, replicas, timeouts, or health checks (k8s/compose).
- **Image/version pinning**: `latest` tags or unpinned dependencies.
- **Duplicate keys** silently overriding earlier values.
