import {
  Title,
  Text,
  SimpleGrid,
  Card,
  Group,
  Badge,
  Button,
  Stack,
  Table,
} from '@mantine/core'
import {
  IconSourceCode,
  IconGitPullRequest,
  IconAlertTriangle,
  IconChevronRight,
} from '@tabler/icons-react'
import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { projectsApi, reviewsApi } from '@/lib/api'

export function DashboardPage() {
  const { data: projects } = useQuery({
    queryKey: ['projects'],
    queryFn: projectsApi.list,
  })

  const { data: reviews } = useQuery({
    queryKey: ['reviews'],
    queryFn: () => reviewsApi.list(),
  })

  const stats = [
    {
      label: 'Projetos',
      value: projects?.length || 0,
      icon: IconSourceCode,
      color: 'indigo',
    },
    {
      label: 'Reviews',
      value: reviews?.length || 0,
      icon: IconGitPullRequest,
      color: 'green',
    },
    {
      label: 'Issues Críticos',
      value: reviews?.reduce((acc, r) => acc + (r.stats?.critical || 0), 0) || 0,
      icon: IconAlertTriangle,
      color: 'red',
    },
  ]

  const recentReviews = reviews?.slice(0, 5) || []

  return (
    <Stack gap="xl">
      <Group justify="space-between">
        <div>
          <Title order={2}>Dashboard</Title>
          <Text c="dimmed">Visão geral dos seus projetos e reviews</Text>
        </div>
        <Button component={Link} to="/projects" leftSection={<IconSourceCode size={16} />}>
          Novo Projeto
        </Button>
      </Group>

      <SimpleGrid cols={{ base: 1, sm: 3 }}>
        {stats.map((stat) => (
          <Card key={stat.label} padding="lg" radius="md" withBorder>
            <Group justify="space-between" mb="xs">
              <Text tt="uppercase" size="xs" fw={700} c="dimmed">
                {stat.label}
              </Text>
              <stat.icon size={20} className={`text-${stat.color}-500`} />
            </Group>
            <Text fz="xl" fw={700}>
              {stat.value}
            </Text>
          </Card>
        ))}
      </SimpleGrid>

      <Card padding="lg" radius="md" withBorder>
        <Group justify="space-between" mb="md">
          <Title order={4}>Reviews Recentes</Title>
          <Button
            variant="subtle"
            component={Link}
            to="/reviews"
            rightSection={<IconChevronRight size={14} />}
          >
            Ver todos
          </Button>
        </Group>

        {recentReviews.length === 0 ? (
          <Text c="dimmed" ta="center" py="xl">
            Nenhum review encontrado
          </Text>
        ) : (
          <Table>
            <Table.Thead>
              <Table.Tr>
                <Table.Th>Status</Table.Th>
                <Table.Th>Projeto</Table.Th>
                <Table.Th>Issues</Table.Th>
                <Table.Th>Data</Table.Th>
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {recentReviews.map((review) => (
                <Table.Tr key={review.id}>
                  <Table.Td>
                    <Badge
                      color={
                        review.status === 'COMPLETED'
                          ? 'green'
                          : review.status === 'FAILED'
                            ? 'red'
                            : review.status === 'IN_PROGRESS'
                              ? 'blue'
                              : 'gray'
                      }
                    >
                      {review.status}
                    </Badge>
                  </Table.Td>
                  <Table.Td>PR #{review.pullRequestId}</Table.Td>
                  <Table.Td>{review.stats?.total || 0}</Table.Td>
                  <Table.Td>
                    {new Date(review.createdAt).toLocaleDateString('pt-BR')}
                  </Table.Td>
                </Table.Tr>
              ))}
            </Table.Tbody>
          </Table>
        )}
      </Card>
    </Stack>
  )
}
