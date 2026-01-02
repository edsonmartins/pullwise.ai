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

  const { refetch } = useQuery({
    queryKey: ['teamAnalytics', selectedTimeRange],
    queryFn: () => analyticsApi.getTeamStats(),
  })

  // Mock data for demonstration
  const mockData: TeamAnalyticsData = {
    teams: [
      {
        teamId: 'team-1',
        teamName: 'Platform Core',
        members: [
          { id: 'u1', name: 'Alice Silva', role: 'Tech Lead', stats: { reviews: 45, issuesFound: 123, avgReviewTime: 180, quality: 92, autoFixApplied: 56 } },
          { id: 'u2', name: 'Bob Santos', role: 'Senior Dev', stats: { reviews: 38, issuesFound: 87, avgReviewTime: 210, quality: 88, autoFixApplied: 42 } },
          { id: 'u3', name: 'Carol Lima', role: 'Senior Dev', stats: { reviews: 52, issuesFound: 145, avgReviewTime: 165, quality: 94, autoFixApplied: 68 } },
          { id: 'u4', name: 'David Costa', role: 'Dev', stats: { reviews: 28, issuesFound: 64, avgReviewTime: 240, quality: 82, autoFixApplied: 34 } },
        ],
        stats: {
          totalReviews: 163,
          totalIssues: 419,
          criticalIssues: 12,
          avgReviewTime: 199,
          avgQuality: 89,
          autoFixRate: 0.68,
          prsReviewed: 89,
          codeCoverage: 78,
        },
        performance: {
          velocity: 85,
          efficiency: 78,
          quality: 92,
          collaboration: 88,
          innovation: 72,
        },
      },
      {
        teamId: 'team-2',
        teamName: 'Frontend Squad',
        members: [
          { id: 'u5', name: 'Eva Oliveira', role: 'Tech Lead', stats: { reviews: 41, issuesFound: 98, avgReviewTime: 175, quality: 90, autoFixApplied: 51 } },
          { id: 'u6', name: 'Frank Mendes', role: 'Senior Dev', stats: { reviews: 35, issuesFound: 76, avgReviewTime: 195, quality: 86, autoFixApplied: 39 } },
          { id: 'u7', name: 'Grace Almeida', role: 'Dev', stats: { reviews: 29, issuesFound: 58, avgReviewTime: 220, quality: 84, autoFixApplied: 32 } },
        ],
        stats: {
          totalReviews: 105,
          totalIssues: 232,
          criticalIssues: 8,
          avgReviewTime: 197,
          avgQuality: 87,
          autoFixRate: 0.62,
          prsReviewed: 67,
          codeCoverage: 82,
        },
        performance: {
          velocity: 78,
          efficiency: 82,
          quality: 88,
          collaboration: 92,
          innovation: 85,
        },
      },
      {
        teamId: 'team-3',
        teamName: 'Backend Services',
        members: [
          { id: 'u8', name: 'Henry Pereira', role: 'Tech Lead', stats: { reviews: 48, issuesFound: 134, avgReviewTime: 170, quality: 91, autoFixApplied: 59 } },
          { id: 'u9', name: 'Iris Rodrigues', role: 'Senior Dev', stats: { reviews: 43, issuesFound: 112, avgReviewTime: 185, quality: 89, autoFixApplied: 47 } },
          { id: 'u10', name: 'Jack Ferreira', role: 'Senior Dev', stats: { reviews: 39, issuesFound: 95, avgReviewTime: 200, quality: 87, autoFixApplied: 41 } },
          { id: 'u11', name: 'Kate Barbosa', role: 'Dev', stats: { reviews: 25, issuesFound: 52, avgReviewTime: 235, quality: 80, autoFixApplied: 28 } },
        ],
        stats: {
          totalReviews: 155,
          totalIssues: 393,
          criticalIssues: 15,
          avgReviewTime: 198,
          avgQuality: 87,
          autoFixRate: 0.65,
          prsReviewed: 82,
          codeCoverage: 71,
        },
        performance: {
          velocity: 82,
          efficiency: 75,
          quality: 89,
          collaboration: 85,
          innovation: 78,
        },
      },
    ],
    trends: [],
    comparisons: [],
    leaderboard: [
      { userId: 'u3', userName: 'Carol Lima', teamId: 'team-1', teamName: 'Platform Core', score: 94, reviews: 52, issuesFound: 145, quality: 94 },
      { userId: 'u8', userName: 'Henry Pereira', teamId: 'team-3', teamName: 'Backend Services', score: 91, reviews: 48, issuesFound: 134, quality: 91 },
      { userId: 'u1', userName: 'Alice Silva', teamId: 'team-1', teamName: 'Platform Core', score: 92, reviews: 45, issuesFound: 123, quality: 92 },
      { userId: 'u5', userName: 'Eva Oliveira', teamId: 'team-2', teamName: 'Frontend Squad', score: 90, reviews: 41, issuesFound: 98, quality: 90 },
      { userId: 'u9', userName: 'Iris Rodrigues', teamId: 'team-3', teamName: 'Backend Services', score: 89, reviews: 43, issuesFound: 112, quality: 89 },
    ],
  }

  const teamsData = mockData.teams
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
          value={`${Math.round(teamsData.reduce((sum, t) => sum + t.stats.avgQuality, 0) / teamsData.length)}%`}
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
              {mockData.leaderboard.map((entry, index) => (
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
