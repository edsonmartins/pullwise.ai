import { useEffect } from 'react'
import {
  Title,
  Text,
  SimpleGrid,
  Card,
  Group,
  Button,
  Stack,
  Select,
  Badge,
  Loader,
} from '@mantine/core'
import {
  IconTrendingUp,
  IconTrendingDown,
  IconBug,
  IconAlertTriangle,
  IconCheck,
  IconClock,
  IconRefresh,
  IconCode,
} from '@tabler/icons-react'
import { useQuery } from '@tanstack/react-query'
import {
  AreaChart,
  Area,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
} from 'recharts'
import { analyticsApi } from '@/lib/v2/api'
import { useV2Store } from '@/store/v2-store'
import type { AnalyticsData } from '@/types/v2'

const COLORS = {
  critical: '#ef4444',
  high: '#f97316',
  medium: '#eab308',
  low: '#22c55e',
  info: '#3b82f6',
  purple: '#a855f7',
  cyan: '#06b6d4',
}

export function AnalyticsDashboardPage() {
  const { setAnalyticsData, selectedTimeRange, setSelectedTimeRange } = useV2Store()

  const { data, isLoading, refetch } = useQuery<AnalyticsData>({
    queryKey: ['analytics', selectedTimeRange],
    queryFn: () => analyticsApi.getOverview(selectedTimeRange),
  })

  // Update store when data changes
  useEffect(() => {
    if (data) {
      setAnalyticsData(data)
    }
  }, [data, setAnalyticsData])

  const timeRanges = [
    { value: '7d', label: '7 dias' },
    { value: '30d', label: '30 dias' },
    { value: '90d', label: '90 dias' },
    { value: '365d', label: '1 ano' },
  ]

  if (isLoading) {
    return (
      <Stack align="center" py="xl">
        <Loader size="lg" />
        <Text mt="md" c="dimmed">
          Carregando analytics...
        </Text>
      </Stack>
    )
  }

  const overview = data?.overview
  const trends = data?.trends || []
  const topIssues = data?.topIssues || []
  const teamStats = data?.teamStats || []

  // Prepare severity distribution data for pie chart
  const severityData = topIssues.reduce((acc, issue) => {
    const severity = issue.severity
    acc[severity] = (acc[severity] || 0) + issue.count
    return acc
  }, {} as Record<string, number>)

  const pieData = Object.entries(severityData).map(([name, value]) => ({ name, value }))

  return (
    <Stack gap="xl">
      {/* Header */}
      <Group justify="space-between">
        <div>
          <Title order={2}>Analytics Dashboard</Title>
          <Text c="dimmed">Métricas e insights dos seus code reviews</Text>
        </div>
        <Group>
          <Select
            value={selectedTimeRange}
            onChange={(value) => setSelectedTimeRange(value || '30d')}
            data={timeRanges}
            w={120}
          />
          <Button
            leftSection={<IconRefresh size={16} />}
            onClick={() => refetch()}
            variant="light"
          >
            Atualizar
          </Button>
        </Group>
      </Group>

      {/* Overview Metrics */}
      <SimpleGrid cols={{ base: 1, sm: 2, md: 4 }}>
        <MetricCard
          title="Total Reviews"
          value={overview?.totalReviews || 0}
          icon={<IconCode size={20} />}
          color="cyan"
          trend={trends.length > 1 ? calculateTrend(trends, 'reviews') : undefined}
        />
        <MetricCard
          title="Issues Encontrados"
          value={overview?.totalIssues || 0}
          icon={<IconBug size={20} />}
          color="purple"
          trend={trends.length > 1 ? calculateTrend(trends, 'issues') : undefined}
        />
        <MetricCard
          title="Críticos"
          value={overview?.criticalIssues || 0}
          icon={<IconAlertTriangle size={20} />}
          color="critical"
          trend={trends.length > 1 ? calculateTrend(trends, 'critical') : undefined}
        />
        <MetricCard
          title="Taxa de Auto-Fix"
          value={`${Math.round((overview?.autoFixRate || 0) * 100)}%`}
          icon={<IconCheck size={20} />}
          color="info"
        />
      </SimpleGrid>

      {/* Charts Row */}
      <SimpleGrid cols={{ base: 1, md: 2 }}>
        {/* Trend Chart */}
        <Card padding="lg" radius="md" withBorder h={400}>
          <Title order={4} mb="md">Tendência de Reviews e Issues</Title>
          <ResponsiveContainer width="100%" height={300}>
            <AreaChart data={trends}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis
                dataKey="date"
                tickFormatter={(value: string) => new Date(value).toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit' })}
              />
              <YAxis />
              <Tooltip
                contentStyle={{
                  backgroundColor: 'var(--mantine-color-body)',
                  borderRadius: '8px',
                  border: '1px solid var(--mantine-color-border)',
                }}
                labelFormatter={(value: unknown) => String(value)}
              />
              <Legend />
              <Area
                type="monotone"
                dataKey="reviews"
                stackId="1"
                stroke={COLORS.cyan}
                fill={COLORS.cyan}
                name="Reviews"
                fillOpacity={0.6}
              />
              <Area
                type="monotone"
                dataKey="issues"
                stackId="2"
                stroke={COLORS.purple}
                fill={COLORS.purple}
                name="Issues"
                fillOpacity={0.6}
              />
              <Area
                type="monotone"
                dataKey="critical"
                stackId="2"
                stroke={COLORS.critical}
                fill={COLORS.critical}
                name="Críticos"
                fillOpacity={0.6}
              />
            </AreaChart>
          </ResponsiveContainer>
        </Card>

        {/* Severity Distribution */}
        <Card padding="lg" radius="md" withBorder h={400}>
          <Title order={4} mb="md">Distribuição por Severidade</Title>
          <ResponsiveContainer width="100%" height={300}>
            <PieChart>
              <Pie
                data={pieData}
                cx="50%"
                cy="50%"
                labelLine={false}
                label={({ name, percent }: { name: string; percent: number }) =>
                  `${name}: ${(percent * 100).toFixed(0)}%`
                }
                dataKey="value"
              >
                <Cell fill={COLORS.critical} name="CRITICAL" />
                <Cell fill={COLORS.high} name="HIGH" />
                <Cell fill={COLORS.medium} name="MEDIUM" />
                <Cell fill={COLORS.low} name="LOW" />
                <Cell fill={COLORS.info} name="INFO" />
              </Pie>
              <Tooltip />
            </PieChart>
          </ResponsiveContainer>
        </Card>
      </SimpleGrid>

      {/* Top Issues */}
      <Card padding="lg" radius="md" withBorder>
        <Title order={4} mb="md">Top Issues por Tipo</Title>
        <ResponsiveContainer width="100%" height={300}>
          <BarChart data={topIssues} layout="vertical">
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis type="number" />
            <YAxis
              dataKey="type"
              type="category"
              width={100}
              tickFormatter={(value: string) => value.replace('_', ' ')}
            />
            <Tooltip
              contentStyle={{
                backgroundColor: 'var(--mantine-color-body)',
                borderRadius: '8px',
                border: '1px solid var(--mantine-color-border)',
              }}
              formatter={(value: number) => [value, 'count'] as const}
            />
            <Bar dataKey="count" fill={COLORS.purple} />
          </BarChart>
        </ResponsiveContainer>
      </Card>

      {/* Team Stats */}
      <Card padding="lg" radius="md" withBorder>
        <Title order={4} mb="md">Métricas por Time</Title>
        {teamStats.length === 0 ? (
          <Text c="dimmed" ta="center" py="xl">
            Nenhum dado de time disponível
          </Text>
        ) : (
          <SimpleGrid cols={{ base: 1, sm: 2, md: 3 }}>
            {teamStats.map((team) => (
              <Card key={team.teamId} padding="md" radius="sm" withBorder>
                <Group justify="space-between" mb="xs">
                  <Text fw={600}>{team.teamName}</Text>
                  <Badge
                    color={team.avgQuality >= 80 ? 'green' : team.avgQuality >= 60 ? 'yellow' : 'red'}
                  >
                    {Math.round(team.avgQuality)}% qualidade
                  </Badge>
                </Group>
                <Stack gap={4}>
                  <MetricRow
                    label="Reviews"
                    value={team.reviews}
                    icon={<IconCode size={14} />}
                  />
                  <MetricRow
                    label="Issues"
                    value={team.issues}
                    icon={<IconBug size={14} />}
                  />
                  <MetricRow
                    label="Tempo Médio"
                    value={`${Math.round(team.avgReviewTime)}s`}
                    icon={<IconClock size={14} />}
                  />
                </Stack>
              </Card>
            ))}
          </SimpleGrid>
        )}
      </Card>
    </Stack>
  )
}

// Helper Components
function MetricCard({
  title,
  value,
  icon,
  color,
  trend,
}: {
  title: string
  value: number | string
  icon: React.ReactNode
  color: string
  trend?: { value: number; isPositive: boolean }
}) {
  const colorMap = {
    critical: '#ef4444',
    high: '#f97316',
    medium: '#eab308',
    low: '#22c55e',
    info: '#3b82f6',
    cyan: '#06b6d4',
    purple: '#a855f7',
  }

  return (
    <Card padding="lg" radius="md" withBorder>
      <Group justify="space-between" mb="xs">
        <Text tt="uppercase" size="xs" fw={700} c="dimmed">
          {title}
        </Text>
        <div style={{ color: colorMap[color as keyof typeof colorMap] }}>
          {icon}
        </div>
      </Group>
      <Group align="flex-end">
        <Text fz="xl" fw={700}>
          {value}
        </Text>
        {trend && (
          <Badge
            color={trend.isPositive ? 'green' : 'red'}
            variant="light"
            size="xs"
            leftSection={
              trend.isPositive ? (
                <IconTrendingUp size={12} />
              ) : (
                <IconTrendingDown size={12} />
              )
            }
          >
            {Math.abs(trend.value)}%
          </Badge>
        )}
      </Group>
    </Card>
  )
}

function MetricRow({
  label,
  value,
  icon,
}: {
  label: string
  value: number | string
  icon: React.ReactNode
}) {
  return (
    <Group justify="space-between">
      <Group gap={4} c="dimmed" style={{ fontSize: '0.875rem' }}>
        {icon}
        <span>{label}</span>
      </Group>
      <Text fw={600}>{value}</Text>
    </Group>
  )
}

function calculateTrend(
  trends: Array<{ reviews: number; issues: number; critical: number }>,
  key: 'reviews' | 'issues' | 'critical'
) {
  if (trends.length < 2) return undefined

  const latest = trends[trends.length - 1][key]
  const previous = trends[0][key]

  if (previous === 0) return undefined

  const value = ((latest - previous) / previous) * 100
  return {
    value: Math.round(value),
    isPositive: value >= 0,
  }
}
