# Frontend V2 - Extensões e Melhorias do SaaS de Code Review

## 📋 Visão Geral

Este documento **estende** o frontend React já em desenvolvimento com novas features baseadas nos insights de mercado. O foco é em:

1. **Dashboard Analítico** - Métricas em tempo real e histórico
2. **Code Graph Visualização** - Análise de impacto visual
3. **Auto-Fix Interface** - Aplicação one-click de correções
4. **Plugin Marketplace** - Gerenciamento de plugins
5. **Real-time Updates** - WebSockets para feedback instantâneo
6. **Advanced Filtering** - Filtros inteligentes de issues
7. **Multi-Model Insights** - Visualização de decisões de LLM
8. **Team Analytics** - Métricas por equipe e desenvolvedor

---

## 🏗️ Arquitetura Frontend Estendida

```
┌────────────────────────────────────────────────────────────┐
│                    FRONTEND ARCHITECTURE V2                 │
├────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────────────────────────────────────────────┐ │
│  │              React App (Vite)                         │ │
│  ├──────────────────────────────────────────────────────┤ │
│  │  • Zustand (State Management)                        │ │
│  │  • TanStack Query (Server State)                     │ │
│  │  • React Router (Navigation)                         │ │
│  │  • Socket.io Client (Real-time)                      │ │
│  └──────────────────────────────────────────────────────┘ │
│                           │                                │
│                           ▼                                │
│  ┌──────────────────────────────────────────────────────┐ │
│  │              UI Components Layer                      │ │
│  ├──────────────────────────────────────────────────────┤ │
│  │                                                       │ │
│  │  ┌──────────────┐  ┌──────────────┐  ┌────────────┐ │ │
│  │  │  Dashboard   │  │  Code Graph  │  │  Auto-Fix  │ │ │
│  │  │  Analytics   │  │  Visualizer  │  │  Manager   │ │ │
│  │  └──────────────┘  └──────────────┘  └────────────┘ │ │
│  │                                                       │ │
│  │  ┌──────────────┐  ┌──────────────┐  ┌────────────┐ │ │
│  │  │   Plugin     │  │   Review     │  │   Team     │ │ │
│  │  │ Marketplace  │  │   Timeline   │  │ Analytics  │ │ │
│  │  └──────────────┘  └──────────────┘  └────────────┘ │ │
│  │                                                       │ │
│  └──────────────────────────────────────────────────────┘ │
│                           │                                │
│                           ▼                                │
│  ┌──────────────────────────────────────────────────────┐ │
│  │              Visualization Libraries                  │ │
│  ├──────────────────────────────────────────────────────┤ │
│  │  • Recharts (Charts & Graphs)                        │ │
│  │  • D3.js (Code Graph Visualization)                  │ │
│  │  • React Flow (Dependency Graphs)                    │ │
│  │  • Monaco Editor (Code Diff Viewer)                  │ │
│  └──────────────────────────────────────────────────────┘ │
│                           │                                │
│                           ▼                                │
│  ┌──────────────────────────────────────────────────────┐ │
│  │              API Layer                                │ │
│  ├──────────────────────────────────────────────────────┤ │
│  │  • Axios Interceptors                                │ │
│  │  • WebSocket Handler                                 │ │
│  │  • Query Invalidation                                │ │
│  └──────────────────────────────────────────────────────┘ │
│                                                             │
└────────────────────────────────────────────────────────────┘
```

---

## 🎯 Novas Features Detalhadas

### 1. Dashboard Analítico

**Objetivo:** Visualização em tempo real de métricas de code review.

#### 1.1 Main Dashboard Component

```tsx
// src/pages/Dashboard/AnalyticsDashboard.tsx
import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { Card, Grid, Select, DateRangePicker } from '@/components/ui';
import { 
  IssuesTrendChart, 
  ReviewTimelineChart,
  SeverityDistribution,
  TopReviewers,
  CostAnalysis,
  ModelUsageChart
} from './components';

interface DashboardProps {
  organizationId: string;
}

export const AnalyticsDashboard: React.FC<DashboardProps> = ({ organizationId }) => {
  const [timeRange, setTimeRange] = React.useState({ start: '-30d', end: 'now' });
  const [selectedRepo, setSelectedRepo] = React.useState<string>('all');

  // Fetch metrics
  const { data: metrics, isLoading } = useQuery({
    queryKey: ['analytics', organizationId, timeRange, selectedRepo],
    queryFn: () => fetchAnalytics({ organizationId, timeRange, repo: selectedRepo }),
    refetchInterval: 30000, // Atualizar a cada 30s
  });

  const { data: realtimeStats } = useQuery({
    queryKey: ['realtime-stats', organizationId],
    queryFn: () => fetchRealtimeStats(organizationId),
    refetchInterval: 5000, // Atualizar a cada 5s
  });

  if (isLoading) {
    return <DashboardSkeleton />;
  }

  return (
    <div className="dashboard-container">
      {/* Header com filtros */}
      <div className="dashboard-header">
        <h1>Code Review Analytics</h1>
        <div className="filters">
          <DateRangePicker
            value={timeRange}
            onChange={setTimeRange}
          />
          <Select
            options={metrics?.repositories || []}
            value={selectedRepo}
            onChange={setSelectedRepo}
            placeholder="All Repositories"
          />
        </div>
      </div>

      {/* KPI Cards */}
      <Grid cols={4} gap={4}>
        <StatCard
          title="Total Reviews"
          value={metrics?.totalReviews}
          change={metrics?.reviewsChange}
          icon={<CheckCircle />}
        />
        <StatCard
          title="Issues Found"
          value={metrics?.totalIssues}
          change={metrics?.issuesChange}
          icon={<AlertTriangle />}
          trend={metrics?.issuesTrend}
        />
        <StatCard
          title="Avg Review Time"
          value={formatDuration(metrics?.avgReviewTime)}
          change={metrics?.timeChange}
          icon={<Clock />}
        />
        <StatCard
          title="LLM Cost (30d)"
          value={formatCurrency(metrics?.llmCost)}
          change={metrics?.costChange}
          icon={<DollarSign />}
        />
      </Grid>

      {/* Charts */}
      <Grid cols={2} gap={4} className="mt-6">
        <Card>
          <CardHeader>
            <h3>Issues Trend</h3>
            <span className="subtitle">Last 30 days</span>
          </CardHeader>
          <IssuesTrendChart data={metrics?.issuesTrend} />
        </Card>

        <Card>
          <CardHeader>
            <h3>Severity Distribution</h3>
            <span className="subtitle">Current period</span>
          </CardHeader>
          <SeverityDistribution data={metrics?.severityDistribution} />
        </Card>

        <Card>
          <CardHeader>
            <h3>Model Usage & Cost</h3>
            <ModelBreakdownButton onClick={() => setShowModelDetails(true)} />
          </CardHeader>
          <ModelUsageChart data={metrics?.modelUsage} />
        </Card>

        <Card>
          <CardHeader>
            <h3>Top Contributors</h3>
            <span className="subtitle">By review participation</span>
          </CardHeader>
          <TopReviewers data={metrics?.topReviewers} />
        </Card>
      </Grid>

      {/* Review Timeline */}
      <Card className="mt-6">
        <CardHeader>
          <h3>Review Timeline</h3>
          <LiveIndicator active={true} />
        </CardHeader>
        <ReviewTimelineChart 
          data={metrics?.timeline}
          realtime={realtimeStats}
        />
      </Card>

      {/* Recent Activity Stream */}
      <Card className="mt-6">
        <CardHeader>
          <h3>Recent Activity</h3>
        </CardHeader>
        <ActivityStream organizationId={organizationId} />
      </Card>
    </div>
  );
};

// Real-time activity stream
const ActivityStream: React.FC<{ organizationId: string }> = ({ organizationId }) => {
  const [activities, setActivities] = React.useState<Activity[]>([]);

  React.useEffect(() => {
    const socket = io(SOCKET_URL);

    socket.on('connect', () => {
      socket.emit('subscribe', { type: 'activities', organizationId });
    });

    socket.on('activity', (activity: Activity) => {
      setActivities(prev => [activity, ...prev].slice(0, 50));
    });

    return () => {
      socket.disconnect();
    };
  }, [organizationId]);

  return (
    <div className="activity-stream">
      {activities.map(activity => (
        <ActivityItem key={activity.id} activity={activity} />
      ))}
    </div>
  );
};
```

#### 1.2 Issues Trend Chart

```tsx
// src/pages/Dashboard/components/IssuesTrendChart.tsx
import React from 'react';
import { 
  LineChart, 
  Line, 
  XAxis, 
  YAxis, 
  CartesianGrid, 
  Tooltip, 
  Legend,
  ResponsiveContainer 
} from 'recharts';

interface IssuesTrendData {
  date: string;
  critical: number;
  high: number;
  medium: number;
  low: number;
  total: number;
}

export const IssuesTrendChart: React.FC<{ data: IssuesTrendData[] }> = ({ data }) => {
  return (
    <ResponsiveContainer width="100%" height={300}>
      <LineChart data={data}>
        <CartesianGrid strokeDasharray="3 3" />
        <XAxis 
          dataKey="date" 
          tickFormatter={(date) => new Date(date).toLocaleDateString()}
        />
        <YAxis />
        <Tooltip 
          labelFormatter={(date) => new Date(date).toLocaleDateString()}
          content={<CustomTooltip />}
        />
        <Legend />
        <Line 
          type="monotone" 
          dataKey="critical" 
          stroke="#dc2626" 
          strokeWidth={2}
          name="Critical"
        />
        <Line 
          type="monotone" 
          dataKey="high" 
          stroke="#f59e0b" 
          strokeWidth={2}
          name="High"
        />
        <Line 
          type="monotone" 
          dataKey="medium" 
          stroke="#3b82f6" 
          strokeWidth={2}
          name="Medium"
        />
        <Line 
          type="monotone" 
          dataKey="low" 
          stroke="#10b981" 
          strokeWidth={2}
          name="Low"
        />
      </LineChart>
    </ResponsiveContainer>
  );
};
```

#### 1.3 Model Usage Analytics

```tsx
// src/pages/Dashboard/components/ModelUsageChart.tsx
import React from 'react';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip } from 'recharts';

interface ModelUsage {
  model: string;
  requests: number;
  tokens: number;
  cost: number;
}

const MODEL_COLORS = {
  'o3-mini': '#8b5cf6',
  'claude-3.5-sonnet': '#f59e0b',
  'gpt-4.1-turbo': '#10b981',
  'gemma-3-4b': '#3b82f6',
};

export const ModelUsageChart: React.FC<{ data: ModelUsage[] }> = ({ data }) => {
  const chartData = data.map(item => ({
    name: item.model,
    value: item.cost,
    requests: item.requests,
    tokens: item.tokens,
  }));

  return (
    <div className="model-usage-chart">
      <ResponsiveContainer width="100%" height={250}>
        <PieChart>
          <Pie
            data={chartData}
            cx="50%"
            cy="50%"
            labelLine={false}
            label={renderCustomLabel}
            outerRadius={80}
            fill="#8884d8"
            dataKey="value"
          >
            {chartData.map((entry, index) => (
              <Cell 
                key={`cell-${index}`} 
                fill={MODEL_COLORS[entry.name as keyof typeof MODEL_COLORS]} 
              />
            ))}
          </Pie>
          <Tooltip content={<ModelTooltip />} />
        </PieChart>
      </ResponsiveContainer>

      {/* Legenda com detalhes */}
      <div className="model-legend">
        {data.map(model => (
          <div key={model.model} className="model-legend-item">
            <div 
              className="color-indicator" 
              style={{ backgroundColor: MODEL_COLORS[model.model as keyof typeof MODEL_COLORS] }}
            />
            <div className="model-details">
              <span className="model-name">{model.model}</span>
              <div className="model-stats">
                <span>{model.requests} requests</span>
                <span>{formatTokens(model.tokens)} tokens</span>
                <span className="cost">${model.cost.toFixed(2)}</span>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

const ModelTooltip: React.FC<any> = ({ active, payload }) => {
  if (active && payload && payload.length) {
    const data = payload[0].payload;
    return (
      <div className="custom-tooltip">
        <p className="label">{data.name}</p>
        <p>Requests: {data.requests}</p>
        <p>Tokens: {formatTokens(data.tokens)}</p>
        <p className="cost">Cost: ${data.value.toFixed(2)}</p>
      </div>
    );
  }
  return null;
};
```

---

### 2. Code Graph Visualização

**Objetivo:** Visualizar o impacto das mudanças através de grafos de dependências.

#### 2.1 Code Graph Viewer

```tsx
// src/pages/PullRequest/components/CodeGraphViewer.tsx
import React from 'react';
import ReactFlow, {
  Node,
  Edge,
  Controls,
  Background,
  MiniMap,
  useNodesState,
  useEdgesState,
} from 'reactflow';
import 'reactflow/dist/style.css';

interface CodeGraphProps {
  pullRequestId: string;
}

export const CodeGraphViewer: React.FC<CodeGraphProps> = ({ pullRequestId }) => {
  const { data: graphData } = useQuery({
    queryKey: ['code-graph', pullRequestId],
    queryFn: () => fetchCodeGraph(pullRequestId),
  });

  const [nodes, setNodes, onNodesChange] = useNodesState([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState([]);

  React.useEffect(() => {
    if (graphData) {
      const { nodes: graphNodes, edges: graphEdges } = transformGraphData(graphData);
      setNodes(graphNodes);
      setEdges(graphEdges);
    }
  }, [graphData]);

  return (
    <div className="code-graph-container" style={{ height: '600px' }}>
      <div className="graph-header">
        <h3>Impact Analysis</h3>
        <div className="graph-legend">
          <div className="legend-item">
            <div className="node-indicator changed" />
            <span>Directly Changed</span>
          </div>
          <div className="legend-item">
            <div className="node-indicator affected" />
            <span>Potentially Affected</span>
          </div>
          <div className="legend-item">
            <div className="node-indicator safe" />
            <span>No Impact</span>
          </div>
        </div>
      </div>

      <ReactFlow
        nodes={nodes}
        edges={edges}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        nodeTypes={customNodeTypes}
        fitView
      >
        <Background />
        <Controls />
        <MiniMap 
          nodeColor={(node) => getNodeColor(node)}
          maskColor="rgba(0, 0, 0, 0.2)"
        />
      </ReactFlow>

      <ImpactSummary data={graphData?.impactAnalysis} />
    </div>
  );
};

// Custom Node Component
const CodeFileNode: React.FC<{ data: any }> = ({ data }) => {
  const { label, type, linesChanged, impactScore } = data;

  return (
    <div className={`code-file-node ${type}`}>
      <div className="node-header">
        <FileIcon type={getFileType(label)} />
        <span className="file-name">{label}</span>
      </div>
      {linesChanged && (
        <div className="node-stats">
          <span className="lines-changed">
            +{linesChanged.added} -{linesChanged.removed}
          </span>
        </div>
      )}
      {impactScore && (
        <div className="impact-score">
          <ImpactBadge score={impactScore} />
        </div>
      )}
    </div>
  );
};

const customNodeTypes = {
  codeFile: CodeFileNode,
};

// Transform backend graph data to React Flow format
function transformGraphData(graphData: any) {
  const nodes: Node[] = graphData.nodes.map((node: any, index: number) => ({
    id: node.id,
    type: 'codeFile',
    position: calculatePosition(index, graphData.nodes.length),
    data: {
      label: node.fileName,
      type: node.changeType, // 'changed' | 'affected' | 'safe'
      linesChanged: node.linesChanged,
      impactScore: node.impactScore,
    },
  }));

  const edges: Edge[] = graphData.edges.map((edge: any) => ({
    id: `${edge.from}-${edge.to}`,
    source: edge.from,
    target: edge.to,
    type: 'smoothstep',
    animated: edge.type === 'direct-dependency',
    style: {
      stroke: getEdgeColor(edge.type),
      strokeWidth: 2,
    },
  }));

  return { nodes, edges };
}

function getNodeColor(node: Node) {
  switch (node.data.type) {
    case 'changed': return '#dc2626';
    case 'affected': return '#f59e0b';
    default: return '#10b981';
  }
}

// Impact Summary Panel
const ImpactSummary: React.FC<{ data: any }> = ({ data }) => {
  if (!data) return null;

  return (
    <div className="impact-summary">
      <h4>Impact Analysis Summary</h4>
      <div className="impact-metrics">
        <div className="metric">
          <span className="label">Files Directly Changed:</span>
          <span className="value">{data.directlyChanged}</span>
        </div>
        <div className="metric">
          <span className="label">Files Potentially Affected:</span>
          <span className="value">{data.potentiallyAffected}</span>
        </div>
        <div className="metric">
          <span className="label">Components Impacted:</span>
          <span className="value">{data.componentsImpacted.join(', ')}</span>
        </div>
        <div className="metric">
          <span className="label">Risk Score:</span>
          <RiskScoreBadge score={data.riskScore} />
        </div>
      </div>
      <div className="recommendation">
        <AlertTriangle />
        <p>{data.recommendation}</p>
      </div>
    </div>
  );
};
```

---

### 3. Auto-Fix Interface

**Objetivo:** Interface para visualizar e aplicar correções automáticas.

#### 3.1 Auto-Fix Manager

```tsx
// src/pages/PullRequest/components/AutoFixManager.tsx
import React from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { DiffEditor } from '@monaco-editor/react';
import { Button, Badge, Tabs } from '@/components/ui';

interface AutoFixManagerProps {
  pullRequestId: string;
}

export const AutoFixManager: React.FC<AutoFixManagerProps> = ({ pullRequestId }) => {
  const queryClient = useQueryClient();
  const [selectedFix, setSelectedFix] = React.useState<FixSuggestion | null>(null);

  const { data: suggestions } = useQuery({
    queryKey: ['fix-suggestions', pullRequestId],
    queryFn: () => fetchFixSuggestions(pullRequestId),
  });

  const applyFixMutation = useMutation({
    mutationFn: (suggestionId: string) => applyFix(suggestionId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['fix-suggestions', pullRequestId] });
      toast.success('Fix applied successfully!');
    },
    onError: (error) => {
      toast.error(`Failed to apply fix: ${error.message}`);
    },
  });

  const groupedSuggestions = groupByConfidence(suggestions || []);

  return (
    <div className="auto-fix-manager">
      <div className="manager-header">
        <h3>Auto-Fix Suggestions</h3>
        <div className="stats">
          <Badge variant="success">
            {suggestions?.filter(s => s.confidence === 'HIGH').length || 0} High Confidence
          </Badge>
          <Badge variant="warning">
            {suggestions?.filter(s => s.confidence === 'MEDIUM').length || 0} Medium
          </Badge>
          <Badge variant="info">
            {suggestions?.filter(s => s.confidence === 'LOW').length || 0} Low
          </Badge>
        </div>
      </div>

      <Tabs>
        <TabList>
          <Tab>High Confidence ({groupedSuggestions.high?.length || 0})</Tab>
          <Tab>Medium Confidence ({groupedSuggestions.medium?.length || 0})</Tab>
          <Tab>Low Confidence ({groupedSuggestions.low?.length || 0})</Tab>
        </TabList>

        <TabPanel>
          <FixSuggestionList
            suggestions={groupedSuggestions.high}
            onSelect={setSelectedFix}
            onApply={(id) => applyFixMutation.mutate(id)}
            canAutoApply={true}
          />
        </TabPanel>

        <TabPanel>
          <FixSuggestionList
            suggestions={groupedSuggestions.medium}
            onSelect={setSelectedFix}
            onApply={(id) => applyFixMutation.mutate(id)}
            canAutoApply={false}
          />
        </TabPanel>

        <TabPanel>
          <FixSuggestionList
            suggestions={groupedSuggestions.low}
            onSelect={setSelectedFix}
            onApply={(id) => applyFixMutation.mutate(id)}
            canAutoApply={false}
          />
        </TabPanel>
      </Tabs>

      {selectedFix && (
        <FixPreviewModal
          fix={selectedFix}
          onClose={() => setSelectedFix(null)}
          onApply={() => {
            applyFixMutation.mutate(selectedFix.id);
            setSelectedFix(null);
          }}
        />
      )}
    </div>
  );
};

// Fix Suggestion List Component
const FixSuggestionList: React.FC<{
  suggestions: FixSuggestion[];
  onSelect: (fix: FixSuggestion) => void;
  onApply: (id: string) => void;
  canAutoApply: boolean;
}> = ({ suggestions, onSelect, onApply, canAutoApply }) => {
  if (!suggestions || suggestions.length === 0) {
    return (
      <div className="empty-state">
        <p>No suggestions available</p>
      </div>
    );
  }

  return (
    <div className="fix-suggestion-list">
      {suggestions.map(suggestion => (
        <FixSuggestionCard
          key={suggestion.id}
          suggestion={suggestion}
          onSelect={() => onSelect(suggestion)}
          onApply={() => onApply(suggestion.id)}
          canAutoApply={canAutoApply}
        />
      ))}
    </div>
  );
};

// Fix Suggestion Card
const FixSuggestionCard: React.FC<{
  suggestion: FixSuggestion;
  onSelect: () => void;
  onApply: () => void;
  canAutoApply: boolean;
}> = ({ suggestion, onSelect, onApply, canAutoApply }) => {
  return (
    <div className="fix-suggestion-card">
      <div className="card-header">
        <div className="issue-info">
          <IssueSeverityBadge severity={suggestion.issue.severity} />
          <span className="issue-title">{suggestion.issue.title}</span>
        </div>
        <ConfidenceBadge confidence={suggestion.confidence} />
      </div>

      <div className="file-info">
        <FileIcon type={getFileType(suggestion.filePath)} />
        <span className="file-path">{suggestion.filePath}</span>
        <span className="line-range">
          Lines {suggestion.lineStart}-{suggestion.lineEnd}
        </span>
      </div>

      <div className="explanation">
        <p>{suggestion.explanation}</p>
      </div>

      <div className="code-preview">
        <div className="before">
          <span className="label">Original:</span>
          <code>{truncateCode(suggestion.originalCode)}</code>
        </div>
        <ArrowRight className="arrow" />
        <div className="after">
          <span className="label">Suggested:</span>
          <code>{truncateCode(suggestion.suggestedCode)}</code>
        </div>
      </div>

      <div className="card-actions">
        <Button variant="outline" onClick={onSelect}>
          View Diff
        </Button>
        {canAutoApply && suggestion.status === 'PENDING' && (
          <Button variant="primary" onClick={onApply}>
            <Zap className="icon" />
            Apply Fix
          </Button>
        )}
        {!canAutoApply && (
          <Button variant="primary" onClick={onSelect}>
            Review & Apply
          </Button>
        )}
      </div>

      {suggestion.applied && (
        <div className="applied-badge">
          <CheckCircle />
          <span>Applied in commit {suggestion.appliedCommitSha?.substring(0, 7)}</span>
        </div>
      )}
    </div>
  );
};

// Fix Preview Modal
const FixPreviewModal: React.FC<{
  fix: FixSuggestion;
  onClose: () => void;
  onApply: () => void;
}> = ({ fix, onClose, onApply }) => {
  return (
    <Modal onClose={onClose} size="large">
      <ModalHeader>
        <h3>Preview Fix: {fix.issue.title}</h3>
        <ConfidenceBadge confidence={fix.confidence} />
      </ModalHeader>

      <ModalBody>
        <div className="fix-details">
          <div className="detail-row">
            <span className="label">File:</span>
            <span className="value">{fix.filePath}</span>
          </div>
          <div className="detail-row">
            <span className="label">Lines:</span>
            <span className="value">{fix.lineStart}-{fix.lineEnd}</span>
          </div>
          <div className="detail-row">
            <span className="label">Issue Type:</span>
            <span className="value">{fix.issue.type}</span>
          </div>
        </div>

        <div className="explanation-section">
          <h4>Explanation</h4>
          <p>{fix.explanation}</p>
        </div>

        <div className="diff-viewer">
          <DiffEditor
            height="400px"
            language={getLanguage(fix.filePath)}
            original={fix.originalCode}
            modified={fix.suggestedCode}
            options={{
              readOnly: true,
              minimap: { enabled: false },
              renderSideBySide: true,
            }}
          />
        </div>

        <div className="warning-section">
          <AlertTriangle />
          <p>
            Please review the changes carefully before applying. 
            This will create a new commit in your repository.
          </p>
        </div>
      </ModalBody>

      <ModalFooter>
        <Button variant="outline" onClick={onClose}>
          Cancel
        </Button>
        <Button variant="primary" onClick={onApply}>
          <Zap /> Apply Fix
        </Button>
      </ModalFooter>
    </Modal>
  );
};
```

---

### 4. Plugin Marketplace

**Objetivo:** Interface para gerenciar e instalar plugins.

#### 4.1 Plugin Marketplace Component

```tsx
// src/pages/Settings/PluginMarketplace.tsx
import React from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';

export const PluginMarketplace: React.FC = () => {
  const queryClient = useQueryClient();
  const [searchQuery, setSearchQuery] = React.useState('');
  const [selectedCategory, setSelectedCategory] = React.useState<string>('all');

  const { data: plugins } = useQuery({
    queryKey: ['marketplace-plugins', searchQuery, selectedCategory],
    queryFn: () => fetchMarketplacePlugins({ search: searchQuery, category: selectedCategory }),
  });

  const { data: installedPlugins } = useQuery({
    queryKey: ['installed-plugins'],
    queryFn: fetchInstalledPlugins,
  });

  const installMutation = useMutation({
    mutationFn: (pluginId: string) => installPlugin(pluginId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['installed-plugins'] });
      toast.success('Plugin installed successfully!');
    },
  });

  const uninstallMutation = useMutation({
    mutationFn: (pluginId: string) => uninstallPlugin(pluginId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['installed-plugins'] });
      toast.success('Plugin uninstalled');
    },
  });

  const categories = ['all', 'sast', 'linters', 'security', 'custom-llm', 'integrations'];

  return (
    <div className="plugin-marketplace">
      <div className="marketplace-header">
        <h1>Plugin Marketplace</h1>
        <p>Extend Pullwise.ai with community plugins</p>
      </div>

      <div className="marketplace-filters">
        <SearchInput
          value={searchQuery}
          onChange={setSearchQuery}
          placeholder="Search plugins..."
        />

        <div className="category-filters">
          {categories.map(category => (
            <Button
              key={category}
              variant={selectedCategory === category ? 'primary' : 'outline'}
              onClick={() => setSelectedCategory(category)}
            >
              {category.toUpperCase()}
            </Button>
          ))}
        </div>
      </div>

      <Tabs>
        <TabList>
          <Tab>Marketplace ({plugins?.length || 0})</Tab>
          <Tab>Installed ({installedPlugins?.length || 0})</Tab>
          <Tab>My Plugins</Tab>
        </TabList>

        <TabPanel>
          <PluginGrid
            plugins={plugins}
            installedPlugins={installedPlugins}
            onInstall={(id) => installMutation.mutate(id)}
          />
        </TabPanel>

        <TabPanel>
          <InstalledPluginsList
            plugins={installedPlugins}
            onUninstall={(id) => uninstallMutation.mutate(id)}
            onConfigure={(id) => navigate(`/settings/plugins/${id}/configure`)}
          />
        </TabPanel>

        <TabPanel>
          <MyPluginsList />
        </TabPanel>
      </Tabs>
    </div>
  );
};

// Plugin Card Component
const PluginCard: React.FC<{
  plugin: Plugin;
  isInstalled: boolean;
  onInstall: () => void;
}> = ({ plugin, isInstalled, onInstall }) => {
  return (
    <div className="plugin-card">
      <div className="plugin-icon">
        {plugin.icon ? (
          <img src={plugin.icon} alt={plugin.name} />
        ) : (
          <PackageIcon />
        )}
      </div>

      <div className="plugin-info">
        <div className="plugin-header">
          <h3>{plugin.name}</h3>
          <Badge variant={getTypeBadgeVariant(plugin.type)}>
            {plugin.type}
          </Badge>
        </div>

        <p className="plugin-description">{plugin.description}</p>

        <div className="plugin-meta">
          <span className="author">
            <User className="icon" />
            {plugin.author}
          </span>
          <span className="version">v{plugin.version}</span>
          <span className="downloads">
            <Download className="icon" />
            {formatNumber(plugin.downloads)}
          </span>
          <div className="rating">
            <Star className="icon" />
            {plugin.rating.toFixed(1)}
          </div>
        </div>

        <div className="plugin-languages">
          {plugin.supportedLanguages.map(lang => (
            <Badge key={lang} size="sm">
              {lang}
            </Badge>
          ))}
        </div>
      </div>

      <div className="plugin-actions">
        {isInstalled ? (
          <Button variant="outline" disabled>
            <CheckCircle /> Installed
          </Button>
        ) : (
          <Button variant="primary" onClick={onInstall}>
            <Download /> Install
          </Button>
        )}
        <Button variant="ghost" onClick={() => navigate(`/marketplace/plugins/${plugin.id}`)}>
          Details
        </Button>
      </div>
    </div>
  );
};

// Plugin Configuration Panel
const PluginConfigPanel: React.FC<{ pluginId: string }> = ({ pluginId }) => {
  const { data: plugin } = useQuery({
    queryKey: ['plugin-details', pluginId],
    queryFn: () => fetchPluginDetails(pluginId),
  });

  const { data: config } = useQuery({
    queryKey: ['plugin-config', pluginId],
    queryFn: () => fetchPluginConfig(pluginId),
  });

  const saveMutation = useMutation({
    mutationFn: (settings: any) => savePluginConfig(pluginId, settings),
    onSuccess: () => toast.success('Configuration saved!'),
  });

  if (!plugin || !config) {
    return <Skeleton />;
  }

  return (
    <div className="plugin-config-panel">
      <div className="config-header">
        <h2>Configure {plugin.name}</h2>
        <Toggle
          checked={config.enabled}
          onChange={(enabled) => saveMutation.mutate({ ...config, enabled })}
          label="Enabled"
        />
      </div>

      <Form
        schema={plugin.configSchema}
        initialValues={config.settings}
        onSubmit={(values) => saveMutation.mutate({ ...config, settings: values })}
      >
        <DynamicFormFields schema={plugin.configSchema} />

        <div className="form-actions">
          <Button type="submit" variant="primary">
            Save Configuration
          </Button>
          <Button type="button" variant="outline" onClick={() => resetForm()}>
            Reset to Defaults
          </Button>
        </div>
      </Form>
    </div>
  );
};
```

---

### 5. Real-time Updates com WebSocket

**Objetivo:** Updates em tempo real de reviews em andamento.

#### 5.1 WebSocket Hook

```tsx
// src/hooks/useReviewWebSocket.ts
import { useEffect, useState } from 'react';
import { io, Socket } from 'socket.io-client';

interface ReviewUpdate {
  type: 'progress' | 'issue-found' | 'completed' | 'failed';
  reviewId: string;
  data: any;
}

export const useReviewWebSocket = (reviewId: string) => {
  const [socket, setSocket] = useState<Socket | null>(null);
  const [updates, setUpdates] = useState<ReviewUpdate[]>([]);
  const [progress, setProgress] = useState(0);
  const [status, setStatus] = useState<'pending' | 'in-progress' | 'completed' | 'failed'>('pending');

  useEffect(() => {
    const newSocket = io(import.meta.env.VITE_SOCKET_URL, {
      auth: {
        token: getAuthToken(),
      },
    });

    newSocket.on('connect', () => {
      console.log('WebSocket connected');
      newSocket.emit('subscribe-review', { reviewId });
    });

    newSocket.on('review-update', (update: ReviewUpdate) => {
      setUpdates(prev => [...prev, update]);

      switch (update.type) {
        case 'progress':
          setProgress(update.data.percentage);
          setStatus('in-progress');
          break;
        case 'issue-found':
          // Trigger notification
          toast.info(`New ${update.data.severity} issue found`);
          break;
        case 'completed':
          setProgress(100);
          setStatus('completed');
          toast.success('Review completed!');
          break;
        case 'failed':
          setStatus('failed');
          toast.error('Review failed');
          break;
      }
    });

    newSocket.on('disconnect', () => {
      console.log('WebSocket disconnected');
    });

    setSocket(newSocket);

    return () => {
      newSocket.disconnect();
    };
  }, [reviewId]);

  return {
    socket,
    updates,
    progress,
    status,
  };
};
```

#### 5.2 Live Review Progress Component

```tsx
// src/pages/PullRequest/components/LiveReviewProgress.tsx
import React from 'react';
import { useReviewWebSocket } from '@/hooks/useReviewWebSocket';
import { Progress, Card, Badge } from '@/components/ui';

export const LiveReviewProgress: React.FC<{ reviewId: string }> = ({ reviewId }) => {
  const { updates, progress, status } = useReviewWebSocket(reviewId);

  const passUpdates = updates.filter(u => u.type === 'progress');
  const currentPass = passUpdates[passUpdates.length - 1]?.data?.pass || 'Initializing';

  return (
    <Card className="live-review-progress">
      <div className="progress-header">
        <h4>Review in Progress</h4>
        <StatusBadge status={status} />
      </div>

      <Progress value={progress} max={100} />

      <div className="current-activity">
        <Loader className="spinner" />
        <span>{currentPass}</span>
      </div>

      <div className="activity-log">
        {updates.map((update, index) => (
          <div key={index} className="activity-item">
            <div className="timestamp">
              {new Date(update.timestamp).toLocaleTimeString()}
            </div>
            <div className="activity-content">
              {renderUpdateContent(update)}
            </div>
          </div>
        ))}
      </div>
    </Card>
  );
};

function renderUpdateContent(update: ReviewUpdate) {
  switch (update.type) {
    case 'progress':
      return (
        <span>
          <Activity className="icon" />
          {update.data.message}
        </span>
      );
    case 'issue-found':
      return (
        <span className={`issue-${update.data.severity.toLowerCase()}`}>
          <AlertTriangle className="icon" />
          {update.data.severity} issue found in {update.data.file}
        </span>
      );
    default:
      return <span>{JSON.stringify(update.data)}</span>;
  }
}
```

---

### 6. Team Analytics

**Objetivo:** Métricas e insights por equipe e desenvolvedor.

#### 6.1 Team Analytics Dashboard

```tsx
// src/pages/Analytics/TeamAnalytics.tsx
import React from 'react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend } from 'recharts';

export const TeamAnalytics: React.FC = () => {
  const [timeRange, setTimeRange] = React.useState('30d');
  const [selectedTeam, setSelectedTeam] = React.useState<string>('all');

  const { data: teamMetrics } = useQuery({
    queryKey: ['team-analytics', timeRange, selectedTeam],
    queryFn: () => fetchTeamAnalytics({ timeRange, team: selectedTeam }),
  });

  return (
    <div className="team-analytics">
      <h1>Team Analytics</h1>

      <Grid cols={3} gap={4}>
        <StatCard
          title="Total PRs Reviewed"
          value={teamMetrics?.totalPRs}
          icon={<GitPullRequest />}
        />
        <StatCard
          title="Avg Review Time"
          value={formatDuration(teamMetrics?.avgReviewTime)}
          icon={<Clock />}
        />
        <StatCard
          title="Issues Resolved"
          value={teamMetrics?.issuesResolved}
          change={teamMetrics?.issuesResolvedChange}
          icon={<CheckCircle />}
        />
      </Grid>

      <Card className="mt-6">
        <h3>Developer Leaderboard</h3>
        <DeveloperLeaderboard data={teamMetrics?.developers} />
      </Card>

      <Card className="mt-6">
        <h3>Review Quality Trends</h3>
        <ReviewQualityChart data={teamMetrics?.qualityTrend} />
      </Card>

      <Card className="mt-6">
        <h3>Issue Resolution Time</h3>
        <IssueResolutionChart data={teamMetrics?.resolutionTime} />
      </Card>
    </div>
  );
};

const DeveloperLeaderboard: React.FC<{ data: DeveloperMetrics[] }> = ({ data }) => {
  return (
    <table className="leaderboard-table">
      <thead>
        <tr>
          <th>Rank</th>
          <th>Developer</th>
          <th>PRs Created</th>
          <th>Reviews Given</th>
          <th>Issues Found</th>
          <th>Avg Review Time</th>
          <th>Quality Score</th>
        </tr>
      </thead>
      <tbody>
        {data?.map((dev, index) => (
          <tr key={dev.id}>
            <td className="rank">{index + 1}</td>
            <td className="developer">
              <Avatar src={dev.avatar} alt={dev.name} />
              <span>{dev.name}</span>
            </td>
            <td>{dev.prsCreated}</td>
            <td>{dev.reviewsGiven}</td>
            <td>{dev.issuesFound}</td>
            <td>{formatDuration(dev.avgReviewTime)}</td>
            <td>
              <QualityScoreBadge score={dev.qualityScore} />
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
};
```

---

## 📦 Dependências do Projeto

```json
{
  "name": "codereview-frontend",
  "version": "2.0.0",
  "dependencies": {
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "react-router-dom": "^6.20.0",
    
    "// State Management": "",
    "zustand": "^4.4.7",
    "@tanstack/react-query": "^5.14.2",
    
    "// UI Components": "",
    "@radix-ui/react-dialog": "^1.0.5",
    "@radix-ui/react-tabs": "^1.0.4",
    "@radix-ui/react-select": "^2.0.0",
    "lucide-react": "^0.294.0",
    
    "// Charts & Visualization": "",
    "recharts": "^2.10.3",
    "reactflow": "^11.10.1",
    "d3": "^7.8.5",
    
    "// Code Editor": "",
    "@monaco-editor/react": "^4.6.0",
    
    "// Real-time": "",
    "socket.io-client": "^4.6.1",
    
    "// HTTP Client": "",
    "axios": "^1.6.2",
    
    "// Utilities": "",
    "date-fns": "^3.0.0",
    "clsx": "^2.0.0",
    "tailwind-merge": "^2.1.0"
  },
  "devDependencies": {
    "@types/react": "^18.2.45",
    "@types/node": "^20.10.5",
    "@vitejs/plugin-react": "^4.2.1",
    "vite": "^5.0.8",
    "typescript": "^5.3.3",
    "tailwindcss": "^3.3.6",
    "autoprefixer": "^10.4.16",
    "postcss": "^8.4.32"
  }
}
```

---

## 🚀 Roadmap de Implementação Frontend

**Fase 1 - Core UI (2 semanas):**
- ✅ Dashboard básico com KPIs
- ✅ Review timeline
- ✅ Issue filtering

**Fase 2 - Advanced Features (3 semanas):**
- Code Graph Visualização
- Auto-Fix Interface
- Real-time updates

**Fase 3 - Enterprise (2 semanas):**
- Plugin Marketplace
- Team Analytics
- Advanced Reports

**Fase 4 - Polish (1 semana):**
- Performance optimization
- Mobile responsiveness
- Accessibility

**Total:** ~8 semanas (~2 meses)

---

Este documento serve como **guia de implementação** para as melhorias do frontend. Cada seção pode ser implementada de forma incremental e independente.
