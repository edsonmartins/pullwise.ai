import {
  Title,
  Text,
  Stack,
  Card,
  Table,
  Badge,
  Group,
  Button,
  Alert,
  ActionIcon,
} from '@mantine/core'
import {
  IconGitPullRequest,
  IconAlertCircle,
  IconPlayerPlay,
} from '@tabler/icons-react'
import { useParams, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { pullRequestsApi, reviewsApi, projectsApi } from '@/lib/api'

export function PullRequestsPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()

  const { data: project } = useQuery({
    queryKey: ['project', id],
    queryFn: () => projectsApi.get(Number(id)),
    enabled: !!id,
  })

  const { data: prs, isLoading } = useQuery({
    queryKey: ['pull-requests', id],
    queryFn: () => pullRequestsApi.list(Number(id)),
    enabled: !!id,
  })

  const { data: reviews } = useQuery({
    queryKey: ['reviews'],
    queryFn: () => reviewsApi.list(),
  })

  const getStatusBadge = (pr: any) => {
    if (pr.isMerged) return <Badge color="cyan">Merged</Badge>
    if (pr.isClosed) return <Badge color="red">Fechado</Badge>
    return <Badge color="green">Aberto</Badge>
  }

  const getReviewStatus = (prId: number) => {
    const prReviews = reviews?.filter((r) => r.pullRequestId === prId)
    if (!prReviews || prReviews.length === 0) return null

    const latestReview = prReviews[0]
    return (
      <Badge
        color={
          latestReview.status === 'COMPLETED'
            ? 'green'
            : latestReview.status === 'FAILED'
              ? 'red'
              : 'blue'
        }
      >
        {latestReview.status}
      </Badge>
    )
  }

  const startReview = async (prId: number) => {
    try {
      await reviewsApi.create({
        pullRequestId: prId,
        sastEnabled: true,
        llmEnabled: true,
        ragEnabled: false,
      })
      window.location.reload()
    } catch (error) {
      console.error('Error starting review:', error)
    }
  }

  return (
    <Stack gap="xl">
      <Group>
        <Button variant="light" onClick={() => navigate('/projects')}>
          Voltar
        </Button>
        <div>
          <Title order={2}>Pull Requests</Title>
          <Text c="dimmed">{project?.name}</Text>
        </div>
      </Group>

      {isLoading ? (
        <Text>Carregando...</Text>
      ) : prs && prs.length > 0 ? (
        <Card padding="lg" radius="md" withBorder>
          <Table>
            <Table.Thead>
              <Table.Tr>
                <Table.Th>PR</Table.Th>
                <Table.Th>Branches</Table.Th>
                <Table.Th>Status</Table.Th>
                <Table.Th>Review</Table.Th>
                <Table.Th>Autor</Table.Th>
                <Table.Th>Ações</Table.Th>
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {prs.map((pr) => (
                <Table.Tr key={pr.id}>
                  <Table.Td>
                    <Group gap="xs">
                      <IconGitPullRequest size={16} />
                      <Text fw={500}>#{pr.prNumber}</Text>
                      <Text>{pr.title}</Text>
                    </Group>
                  </Table.Td>
                  <Table.Td>
                    <Text size="sm">
                      {pr.sourceBranch} → {pr.targetBranch}
                    </Text>
                  </Table.Td>
                  <Table.Td>{getStatusBadge(pr)}</Table.Td>
                  <Table.Td>{getReviewStatus(pr.id)}</Table.Td>
                  <Table.Td>
                    <Text size="sm">{pr.authorName}</Text>
                  </Table.Td>
                  <Table.Td>
                    {!pr.isClosed && !pr.isMerged && (
                      <ActionIcon
                        variant="light"
                        color="green"
                        onClick={() => startReview(pr.id)}
                      >
                        <IconPlayerPlay size={16} />
                      </ActionIcon>
                    )}
                  </Table.Td>
                </Table.Tr>
              ))}
            </Table.Tbody>
          </Table>
        </Card>
      ) : (
        <Alert icon={<IconAlertCircle size={16} />}>
          Nenhum pull request encontrado neste projeto
        </Alert>
      )}
    </Stack>
  )
}
