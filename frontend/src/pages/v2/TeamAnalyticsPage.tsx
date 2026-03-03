import { useState } from 'react'
import {
  Title,
  Text,
  Stack,
  Group,
  Button,
  Card,
  Badge,
  Select,
  Table,
  Progress,
  Avatar,
  SimpleGrid,
  Tabs,
  Box,
} from '@mantine/core'
import {
  IconUsers,
  IconCode,
  IconBug,
  IconTrophy,
  IconRefresh,
  IconDownload,
  IconChartBar,
  IconChecks,
} from '@tabler/icons-react'
import { useQuery } from '@tanstack/react-query'
import { analyticsApi } from '@/lib/v2/api'
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip as RechartsTooltip,
  Legend,
  ResponsiveContainer,
  RadarChart,
  PolarGrid,
  PolarAngleAxis,
  PolarRadiusAxis,
  Radar,
} from 'recharts'

interface TeamAnalyticsData {
  teams: TeamData[]
  trends: TeamTrendData[]
  comparisons: ComparisonData[]
  leaderboard: LeaderboardEntry[]
}

interface TeamData {
  teamId: string
  teamName: string
  members: TeamMember[]
  stats: TeamStats
  performance: PerformanceMetrics
}

interface TeamMember {
  id: string
  name: string
  avatar?: string
  role: string
  stats: MemberStats
}

interface TeamStats {
  totalReviews: number
  totalIssues: number
  criticalIssues: number
  avgReviewTime: number
  avgQuality: number
  autoFixRate: number
  prsReviewed: number
  codeCoverage: number
}

interface PerformanceMetrics {
  velocity: number
  efficiency: number
  quality: number
  collaboration: number
  innovation: number
}

interface MemberStats {
  reviews: number
  issuesFound: number
  avgReviewTime: number
  quality: number
  autoFixApplied: number
}

interface TeamTrendData {
  date: string
  teams: Record<string, {
    reviews: number
    issues: number
    quality: number
  }>
}

interface ComparisonData {
  metric: string
  teams: Record<string, number>
}

interface LeaderboardEntry {
  userId: string
  userName: string
  teamId: string
  teamName: string
  avatar?: string
  score: number
  reviews: number
  issuesFound: number
  quality: number
}

export function TeamAnalyticsPage() {
  const [selectedTimeRange, setSelectedTimeRange] = useState('30d')
  const [selectedTeam, setSelectedTeam] = useState<string | null>(null)

  const { data: apiData, refetch } = useQuery<TeamAnalyticsData>({
    queryKey: ['teamAnalytics', selectedTimeRange],
    queryFn: () => analyticsApi.getTeamStats(),
  })

  const emptyData: TeamAnalyticsData = {
    teams: [],
    trends: [],
    comparisons: [],
    leaderboard: [],
  }

  const teamData = apiData || emptyData

  const teamsData = teamData.teams
  const selectedTeamData = selectedTeam
    ? teamsData.find(t => t.teamId === selectedTeam)
    : teamsData[0]

  const timeRanges = [
    { value: '7d', label: '7 dias' },
    { value: '30d', label: '30 dias' },
    { value: '90d', label: '90 dias' },
    { value: '365d', label: '1 ano' },
  ]

  const COLORS = {
    team1: '#3b82f6',
    team2: '#a855f7',
    team3: '#22c55e',
    team4: '#f97316',
    critical: '#ef4444',
    high: '#f97316',
    medium: '#eab308',
    low: '#22c55e',
  }

  // Prepare performance radar data
  const radarData = selectedTeamData ? [
    { metric: 'Velocidade', value: selectedTeamData.performance.velocity, fullMark: 100 },
    { metric: 'Eficiência', value: selectedTeamData.performance.efficiency, fullMark: 100 },
    { metric: 'Qualidade', value: selectedTeamData.performance.quality, fullMark: 100 },
    { metric: 'Colaboração', value: selectedTeamData.performance.collaboration, fullMark: 100 },
    { metric: 'Inovação', value: selectedTeamData.performance.innovation, fullMark: 100 },
  ] : []

  // Prepare comparison data
  const comparisonData = teamsData.map(team => ({
    name: team.teamName,
    Reviews: team.stats.totalReviews,
    Issues: team.stats.totalIssues,
    Quality: team.stats.avgQuality,
    'Auto-Fix %': Math.round(team.stats.autoFixRate * 100),
  }))

  return (
    <Stack gap="md">
      {/* Header */}
      <Group justify="space-between">
        <div>
          <Title order={2}>Team Analytics</Title>
          <Text c="dimmed">Análise detalhada de performance por time e membro</Text>
        </div>
        <Group>
          <Select
            value={selectedTimeRange}
            onChange={(v) => setSelectedTimeRange(v || '30d')}
            data={timeRanges}
            w={100}
          />
          <Button
            leftSection={<IconRefresh size={16} />}
            onClick={() => refetch()}
            variant="light"
          >
            Atualizar
          </Button>
          <Button
            leftSection={<IconDownload size={16} />}
            variant="light"
          >
            Exportar
          </Button>
        </Group>
      </Group>

      {/* Overall Stats */}
      <SimpleGrid cols={{ base: 2, md: 4 }}>
        <StatCard
          label="Total de Times"
          value={teamsData.length}
          icon={<IconUsers size={20} />}
          color="blue"
        />
        <StatCard
          label="Total de Reviews"
          value={teamsData.reduce((sum, t) => sum + t.stats.totalReviews, 0)}
          icon={<IconCode size={20} />}
          color="cyan"
        />
        <StatCard
          label="Issues Encontrados"
          value={teamsData.reduce((sum, t) => sum + t.stats.totalIssues, 0)}
          icon={<IconBug size={20} />}
          color="purple"
        />
        <StatCard
          label="Qualidade Média"
          value={`${teamsData.length > 0 ? Math.round(teamsData.reduce((sum, t) => sum + t.stats.avgQuality, 0) / teamsData.length) : 0}%`}
          icon={<IconTrophy size={20} />}
          color="green"
        />
      </SimpleGrid>

      <Tabs defaultValue="overview">
        <Tabs.List>
          <Tabs.Tab value="overview" leftSection={<IconChartBar size={16} />}>
            Visão Geral
          </Tabs.Tab>
          <Tabs.Tab value="teams" leftSection={<IconUsers size={16} />}>
            Por Time
          </Tabs.Tab>
          <Tabs.Tab value="members" leftSection={<IconChecks size={16} />}>
            Membros
          </Tabs.Tab>
          <Tabs.Tab value="leaderboard" leftSection={<IconTrophy size={16} />}>
            Ranking
          </Tabs.Tab>
        </Tabs.List>

        {/* Overview Tab */}
        <Tabs.Panel value="overview" pt="md">
          <SimpleGrid cols={{ base: 1, md: 2 }}>
            {/* Team Comparison */}
            <Card padding="lg" withBorder h={400}>
              <Title order={5} mb="md">Comparativo entre Times</Title>
              <ResponsiveContainer width="100%" height={320}>
                <BarChart data={comparisonData}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="name" tick={{ fontSize: 12 }} />
                  <YAxis />
                  <RechartsTooltip />
                  <Legend />
                  <Bar dataKey="Reviews" fill={COLORS.team1} name="Reviews" />
                  <Bar dataKey="Issues" fill={COLORS.team2} name="Issues" />
                </BarChart>
              </ResponsiveContainer>
            </Card>

            {/* Quality Trend */}
            <Card padding="lg" withBorder h={400}>
              <Title order={5} mb="md">Qualidade por Time</Title>
              <ResponsiveContainer width="100%" height={320}>
                <BarChart data={comparisonData} layout="horizontal">
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis type="number" />
                  <YAxis dataKey="name" type="category" width={100} tick={{ fontSize: 12 }} />
                  <RechartsTooltip />
                  <Bar dataKey="Quality" fill={COLORS.team4} />
                </BarChart>
              </ResponsiveContainer>
            </Card>

            {/* Auto-Fix Rate */}
            <Card padding="lg" withBorder h={400}>
              <Title order={5} mb="md">Taxa de Auto-Fix</Title>
              <ResponsiveContainer width="100%" height={320}>
                <BarChart data={comparisonData}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="name" tick={{ fontSize: 12 }} />
                  <YAxis />
                  <RechartsTooltip />
                  <Bar dataKey="Auto-Fix %" fill={COLORS.team3} />
                </BarChart>
              </ResponsiveContainer>
            </Card>

            {/* Critical Issues */}
            <Card padding="lg" withBorder h={400}>
              <Title order={5} mb="md">Issues Críticos por Time</Title>
              <Box style={{ height: 320, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <Stack w="100%">
                  {teamsData.map(team => (
                    <Group key={team.teamId}>
                      <Text w={150} size="sm">{team.teamName}</Text>
                      <Progress
                        value={(team.stats.criticalIssues / Math.max(...teamsData.map(t => t.stats.criticalIssues))) * 100}
                        color={team.stats.criticalIssues > 10 ? 'red' : team.stats.criticalIssues > 5 ? 'yellow' : 'green'}
                        style={{ flex: 1 }}
                      />
                      <Text w={40} size="sm" ta="end">{team.stats.criticalIssues}</Text>
                    </Group>
                  ))}
                </Stack>
              </Box>
            </Card>
          </SimpleGrid>
        </Tabs.Panel>

        {/* By Team Tab */}
        <Tabs.Panel value="teams" pt="md">
          <Group mb="md">
            <Select
              placeholder="Selecione um time"
              data={teamsData.map(t => ({ value: t.teamId, label: t.teamName }))}
              value={selectedTeam}
              onChange={setSelectedTeam}
              w={250}
            />
          </Group>

          {selectedTeamData && (
            <Stack>
              <SimpleGrid cols={{ base: 1, md: 2 }}>
                {/* Team Stats */}
                <Card padding="lg" withBorder>
                  <Title order={5} mb="md">{selectedTeamData.teamName}</Title>
                  <SimpleGrid cols={2}>
                    <MiniStat label="Reviews" value={selectedTeamData.stats.totalReviews} />
                    <MiniStat label="Issues" value={selectedTeamData.stats.totalIssues} />
                    <MiniStat label="Críticos" value={selectedTeamData.stats.criticalIssues} color="red" />
                    <MiniStat label="PRs" value={selectedTeamData.stats.prsReviewed} />
                    <MiniStat label="Qualidade" value={`${selectedTeamData.stats.avgQuality}%`} />
                    <MiniStat label="Auto-Fix" value={`${Math.round(selectedTeamData.stats.autoFixRate * 100)}%`} />
                  </SimpleGrid>
                </Card>

                {/* Performance Radar */}
                <Card padding="lg" withBorder>
                  <Title order={5} mb="md">Performance</Title>
                  <ResponsiveContainer width="100%" height={250}>
                    <RadarChart data={radarData}>
                      <PolarGrid />
                      <PolarAngleAxis dataKey="metric" tick={{ fontSize: 11 }} />
                      <PolarRadiusAxis angle={90} domain={[0, 100]} />
                      <Radar
                        name={selectedTeamData.teamName}
                        dataKey="value"
                        stroke={COLORS.team1}
                        fill={COLORS.team1}
                        fillOpacity={0.6}
                      />
                    </RadarChart>
                  </ResponsiveContainer>
                </Card>
              </SimpleGrid>

              {/* Team Members - full width */}
              <Card padding="lg" withBorder>
                <Title order={5} mb="md">Membros</Title>
                <Table>
                  <Table.Thead>
                    <Table.Tr>
                      <Table.Th>Membro</Table.Th>
                      <Table.Th>Reviews</Table.Th>
                      <Table.Th>Issues</Table.Th>
                      <Table.Th>Tempo Médio</Table.Th>
                      <Table.Th>Qualidade</Table.Th>
                      <Table.Th>Auto-Fix</Table.Th>
                    </Table.Tr>
                  </Table.Thead>
                  <Table.Tbody>
                    {selectedTeamData.members.map(member => (
                      <Table.Tr key={member.id}>
                        <Table.Td>
                          <Group gap="sm">
                            <Avatar size="sm" radius="xl" name={member.name} color="initials">
                              {member.name.split(' ').map(n => n[0]).join('')}
                            </Avatar>
                            <div>
                              <Text size="sm" fw={500}>{member.name}</Text>
                              <Text size="xs" c="dimmed">{member.role}</Text>
                            </div>
                          </Group>
                        </Table.Td>
                        <Table.Td>{member.stats.reviews}</Table.Td>
                        <Table.Td>{member.stats.issuesFound}</Table.Td>
                        <Table.Td>{Math.round(member.stats.avgReviewTime / 60)}m</Table.Td>
                        <Table.Td>
                          <Badge
                            color={member.stats.quality >= 90 ? 'green' : member.stats.quality >= 80 ? 'yellow' : 'red'}
                          >
                            {member.stats.quality}%
                          </Badge>
                        </Table.Td>
                        <Table.Td>{member.stats.autoFixApplied}</Table.Td>
                      </Table.Tr>
                    ))}
                  </Table.Tbody>
                </Table>
              </Card>
            </Stack>
          )}
        </Tabs.Panel>

        {/* Members Tab */}
        <Tabs.Panel value="members" pt="md">
          <SimpleGrid cols={{ base: 1, md: 2, lg: 3 }}>
            {teamsData.flatMap(team =>
              team.members.map(member => (
                <Card key={member.id} padding="md" withBorder>
                  <Group gap="sm" mb="md">
                    <Avatar size="md" radius="xl" name={member.name}>
                      {member.name.split(' ').map(n => n[0]).join('')}
                    </Avatar>
                    <div>
                      <Text fw={600}>{member.name}</Text>
                      <Text size="xs" c="dimmed">{member.role}</Text>
                      <Badge size="xs" variant="light">{team.teamName}</Badge>
                    </div>
                  </Group>
                  <SimpleGrid cols={2} mt="md">
                    <MiniStat label="Reviews" value={member.stats.reviews} />
                    <MiniStat label="Issues" value={member.stats.issuesFound} />
                    <MiniStat label="Qualidade" value={`${member.stats.quality}%`} />
                    <MiniStat label="Auto-Fix" value={member.stats.autoFixApplied} />
                  </SimpleGrid>
                </Card>
              ))
            )}
          </SimpleGrid>
        </Tabs.Panel>

        {/* Leaderboard Tab */}
        <Tabs.Panel value="leaderboard" pt="md">
          <Card padding="lg" withBorder>
            <Title order={5} mb="md">Ranking de Desenvolvedores</Title>
            <Stack gap="sm">
              {teamData.leaderboard.map((entry, index) => (
                <LeaderboardItem key={entry.userId} entry={entry} rank={index + 1} />
              ))}
            </Stack>
          </Card>
        </Tabs.Panel>
      </Tabs>
    </Stack>
  )
}

// Sub-components
function StatCard({
  label,
  value,
  icon,
  color = 'blue',
}: {
  label: string
  value: number | string
  icon: React.ReactNode
  color?: string
}) {
  const colorMap = {
    blue: '#3b82f6',
    cyan: '#06b6d4',
    purple: '#a855f7',
    green: '#22c55e',
    red: '#ef4444',
    yellow: '#eab308',
  }

  return (
    <Card padding="md" withBorder>
      <Group>
        <div style={{ color: colorMap[color as keyof typeof colorMap] }}>
          {icon}
        </div>
        <div>
          <Text size="xs" c="dimmed">{label}</Text>
          <Text fz="xl" fw={700}>{value}</Text>
        </div>
      </Group>
    </Card>
  )
}

function MiniStat({
  label,
  value,
  color = 'blue',
}: {
  label: string
  value: number | string
  color?: string
}) {
  return (
    <Box>
      <Text size="xs" c="dimmed">{label}</Text>
      <Text size="lg" fw={600} c={color === 'red' ? 'red' : undefined}>{value}</Text>
    </Box>
  )
}

function LeaderboardItem({ entry, rank }: { entry: LeaderboardEntry; rank: number }) {
  const getRankColor = (rank: number) => {
    switch (rank) {
      case 1: return 'yellow'
      case 2: return 'gray'
      case 3: return 'orange'
      default: return 'blue'
    }
  }

  const getRankIcon = (rank: number) => {
    switch (rank) {
      case 1: return '🥇'
      case 2: return '🥈'
      case 3: return '🥉'
      default: return `#${rank}`
    }
  }

  return (
    <Card
      padding="sm"
      withBorder
      style={{
        borderColor: rank <= 3 ? `var(--mantine-color-${getRankColor(rank)}-5)` : undefined,
      }}
    >
      <Group justify="space-between">
        <Group gap="md">
          <Text size="xl" fw={700} c={getRankColor(rank)}>
            {getRankIcon(rank)}
          </Text>
          <Avatar size="md" radius="xl" name={entry.userName}>
            {entry.userName.split(' ').map(n => n[0]).join('')}
          </Avatar>
          <div>
            <Text fw={600}>{entry.userName}</Text>
            <Text size="xs" c="dimmed">{entry.teamName}</Text>
          </div>
        </Group>
        <Group gap="xl">
          <Group gap="sm">
            <Text size="sm" c="dimmed">Score</Text>
            <Text fw={700} size="lg">{entry.score}</Text>
          </Group>
          <Group gap="sm">
            <Text size="sm" c="dimmed">Reviews</Text>
            <Text fw={600}>{entry.reviews}</Text>
          </Group>
          <Group gap="sm">
            <Text size="sm" c="dimmed">Issues</Text>
            <Text fw={600}>{entry.issuesFound}</Text>
          </Group>
          <Badge
            color={entry.quality >= 90 ? 'green' : entry.quality >= 80 ? 'yellow' : 'red'}
            size="lg"
          >
            {entry.quality}% Qualidade
          </Badge>
        </Group>
      </Group>
    </Card>
  )
}
