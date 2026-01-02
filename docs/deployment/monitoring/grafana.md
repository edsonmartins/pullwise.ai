# Grafana Dashboards

Create and configure Grafana dashboards for Pullwise.

## Overview

Pre-built Grafana dashboards provide visibility into:
- Review performance and throughput
- Issue detection rates
- LLM API usage
- Database health
- System resources

## Quick Start

### Import Dashboard

1. Navigate to Grafana
2. Go to **Dashboards** → **Import**
3. Enter Dashboard ID: `18486` (Pullwise overview)
4. Or upload JSON file
5. Select Prometheus data source
6. Click **Import**

## Pre-Built Dashboards

### 1. Overview Dashboard

**Dashboard ID:** `18486`

Shows:
- Reviews per minute
- Average review duration
- Issues found by severity
- Active reviews
- System health

```json
{
  "dashboard": {
    "title": "Pullwise Overview",
    "panels": [
      {
        "title": "Reviews/min",
        "type": "stat",
        "targets": [
          {
            "expr": "sum(rate(review_total[1m]))",
            "legendFormat": "Reviews/min"
          }
        ]
      },
      {
        "title": "Avg Duration",
        "type": "gauge",
        "targets": [
          {
            "expr": "avg(review_duration_seconds_sum / review_duration_seconds_count)",
            "legendFormat": "Seconds"
          }
        ]
      },
      {
        "title": "Issues (24h)",
        "type": "piechart",
        "targets": [
          {
            "expr": "sum(increase(issues_total{severity=\"CRITICAL\"}[24h]))",
            "legendFormat": "Critical"
          },
          {
            "expr": "sum(increase(issues_total{severity=\"HIGH\"}[24h]))",
            "legendFormat": "High"
          },
          {
            "expr": "sum(increase(issues_total{severity=\"MEDIUM\"}[24h]))",
            "legendFormat": "Medium"
          },
          {
            "expr": "sum(increase(issues_total{severity=\"LOW\"}[24h]))",
            "legendFormat": "Low"
          }
        ]
      }
    ]
  }
}
```

### 2. Performance Dashboard

**Dashboard ID:** `18487`

Shows:
- Request rate and latency
- Response time percentiles
- Error rate
- Throughput

```json
{
  "dashboard": {
    "title": "Pullwise Performance",
    "panels": [
      {
        "title": "Request Rate",
        "type": "graph",
        "targets": [
          {
            "expr": "sum(rate(http_server_requests_seconds_count[5m]))",
            "legendFormat": "Requests/sec"
          }
        ]
      },
      {
        "title": "Response Time (p95)",
        "type": "graph",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le))",
            "legendFormat": "p95"
          }
        ]
      },
      {
        "title": "Error Rate",
        "type": "graph",
        "targets": [
          {
            "expr": "sum(rate(http_server_requests_seconds_count{status=~\"5..\"}[5m])) / sum(rate(http_server_requests_seconds_count[5m]))",
            "legendFormat": "Error Rate"
          }
        ]
      }
    ]
  }
}
```

### 3. LLM Usage Dashboard

**Dashboard ID:** `18488`

Shows:
- LLM request rate
- Token usage
- Cost tracking
- Response times by provider

```json
{
  "dashboard": {
    "title": "LLM Usage",
    "panels": [
      {
        "title": "Requests/min by Provider",
        "type": "graph",
        "targets": [
          {
            "expr": "sum(rate(llm_requests_total[1m])) by (provider)",
            "legendFormat": "{{provider}}"
          }
        ]
      },
      {
        "title": "Tokens/min",
        "type": "graph",
        "targets": [
          {
            "expr": "sum(rate(llm_tokens_total[1m]))",
            "legendFormat": "Tokens"
          }
        ]
      },
      {
        "title": "Estimated Cost (24h)",
        "type": "stat",
        "targets": [
          {
            "expr": "sum(increase(llm_tokens_total[24h])) * 0.00001",
            "legendFormat": "USD"
          }
        ]
      },
      {
        "title": "Response Time by Model",
        "type": "heatmap",
        "targets": [
          {
            "expr": "avg(llm_response_time_seconds) by (model)",
            "legendFormat": "{{model}}"
          }
        ]
      }
    ]
  }
}
```

### 4. Database Dashboard

**Dashboard ID:** `18489`

Shows:
- Connection pool usage
- Query performance
- Lock statistics
- Transaction rates

```json
{
  "dashboard": {
    "title": "Database Health",
    "panels": [
      {
        "title": "Pool Usage",
        "type": "gauge",
        "targets": [
          {
            "expr": "hikaricp_connections_active / hikaricp_connections_max * 100",
            "legendFormat": "Usage %"
          }
        ]
      },
      {
        "title": "Idle Connections",
        "type": "graph",
        "targets": [
          {
            "expr": "hikaricp_connections_idle",
            "legendFormat": "Idle"
          }
        ]
      },
      {
        "title": "Active Connections",
        "type": "graph",
        "targets": [
          {
            "expr": "hikaricp_connections_active",
            "legendFormat": "Active"
          }
        ]
      },
      {
        "title": "Wait Time",
        "type": "graph",
        "targets": [
          {
            "expr": "avg(hikaricp_connections_wait_seconds)",
            "legendFormat": "Wait (s)"
          }
        ]
      }
    ]
  }
}
```

## Dashboard Variables

### Variable Configuration

```json
{
  "dashboard": {
    "templating": {
      "list": [
        {
          "name": "namespace",
          "type": "query",
          "query": "label_values(kube_pod_info, namespace)",
          "multi": false,
          "includeAll": false,
          "allValue": ""
        },
        {
          "name": "pod",
          "type": "query",
          "query": "label_values(kube_pod_info{namespace=\"$namespace\"}, pod)",
          "multi": true,
          "includeAll": true,
          "allValue": ".*"
        },
        {
          "name": "severity",
          "type": "custom",
          "query": "CRITICAL,HIGH,MEDIUM,LOW",
          "multi": true,
          "includeAll": true
        },
        {
          "name": "resolution",
          "type": "interval",
          "query": "10s,1m,5m,15m,1h",
          "current": {
            "text": "1m",
            "value": "1m"
          }
        }
      ]
    }
  }
}
```

## Annotations

### Configure Annotations

```json
{
  "dashboard": {
    "annotations": {
      "list": [
        {
          "name": "Deployments",
          "datasource": "Prometheus",
          "expr": "changes(kube Deployment labels)",
          "title": "Deployment",
          "text": "{{ $labels }}"
        },
        {
          "name": "Pod Restarts",
          "datasource": "Prometheus",
          "expr": "increase(kube_pod_container_status_restarts_total[1m]) > 0",
          "title": "Pod Restart",
          "text": "{{ $labels.namespace }}/{{ $labels.pod }}"
        }
      ]
    }
  }
}
```

## Alert Notifications

### Configure Alert

```json
{
  "dashboard": {
    "panels": [
      {
        "title": "Error Rate",
        "type": "graph",
        "alert": {
          "conditions": [
            {
              "evaluator": {
                "params": [0.05],
                "type": "gt"
              },
              "operator": {
                "type": "and"
              },
              "query": {
                "params": ["A", "5m", "now"]
              },
              "reducer": {
                "params": [],
                "type": "avg"
              },
              "type": "query"
            }
          ],
          "name": "High Error Rate",
          "message": "Error rate is {{ $value }}",
          "frequency": "1m",
          "handler": 1,
          "notifications": []
        },
        "targets": [
          {
            "expr": "rate(http_server_requests_seconds_count{status=~\"5..\"}[5m]) / rate(http_server_requests_seconds_count[5m])"
          }
        ]
      }
    ]
  }
}
```

## Provisioning Dashboards

### Kubernetes ConfigMap

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: grafana-dashboards
  namespace: monitoring
  labels:
    grafana_dashboard: "1"
data:
  pullwise-overview.json: |
    {
      "dashboard": {
        "title": "Pullwise Overview",
        "tags": ["pullwise"],
        "timezone": "browser",
        "panels": [...]
      }
    }
```

### Provisioning Configuration

```yaml
# grafana-values.yaml
dashboardProviders:
  dashboards:
    orgId: 1
    name: 'pullwise'
    folder: 'Pullwise'
    type: file
    disableDeletion: false
    editable: true
    options:
      path: /var/lib/grafana/dashboards/pullwise

dashboards:
  pullwise:
    pullwise-overview:
      json: |
        {
          "dashboard": {
            "title": "Pullwise Overview",
            "panels": [...]
          }
        }
```

## Custom Dashboards

### Create Custom Dashboard

1. Go to **Dashboards** → **New Dashboard**
2. Add panels:
   - Click **Add Visualization**
   - Select query type
   - Enter PromQL query
   - Configure visualization
3. Set variables:
   - Go to Dashboard settings → Variables
   - Add query/custom variables
   - Use in queries: `$variable`
4. Save dashboard

### Example: Project-Specific Dashboard

```json
{
  "dashboard": {
    "title": "Project Review Stats",
    "variables": [
      {
        "name": "project",
        "type": "query",
        "query": "label_values(review_total, project)"
      }
    ],
    "panels": [
      {
        "title": "Reviews for $project",
        "targets": [
          {
            "expr": "sum(rate(review_total{project=\"$project\"}[5m]))"
          }
        ]
      },
      {
        "title": "Issues by Severity",
        "targets": [
          {
            "expr": "sum(issues_total{project=\"$project\"}) by (severity)"
          }
        ]
      }
    ]
  }
}
```

## Exporting Dashboards

### Export Dashboard

```bash
# Via Grafana UI
# Dashboard → Settings → JSON Model → Copy

# Via API
curl -X GET \
  "http://grafana:3000/api/dashboards/uid/abc123" \
  -u "admin:password" \
  -H "Content-Type: application/json" \
  | jq '.dashboard' > dashboard.json
```

### Import Dashboard

```bash
# Via API
curl -X POST \
  "http://grafana:3000/api/dashboards/db" \
  -u "admin:password" \
  -H "Content-Type: application/json" \
  -d @dashboard.json
```

## Best Practices

### 1. Use Folders

```
Pullwise/
├── Overview
├── Performance
├── LLM Usage
├── Database
└── System Resources
```

### 2. Consistent Naming

- Use descriptive panel titles
- Include units in legend
- Use consistent colors

### 3. Responsive Time Ranges

```json
{
  "time": {
    "from": "now-6h",
    "to": "now"
  },
  "refresh": "30s"
}
```

### 4. Meaningful Thresholds

```json
{
  "panel": {
    "fieldConfig": {
      "defaults": {
        "thresholds": {
          "mode": "absolute",
          "steps": [
            { "value": null, "color": "green" },
            { "value": 70, "color": "yellow" },
            { "value": 90, "color": "red" }
          ]
        }
      }
    }
  }
}
```

## Next Steps

- [Prometheus](/docs/deployment/monitoring/prometheus) - Prometheus setup
- [Jaeger](/docs/deployment/monitoring/jaeger) - Distributed tracing
- [Production](/docs/deployment/docker/production) - Production setup
