import { useMemo } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import {
  Card,
  Stack,
  Group,
  Badge,
  Text,
  Title,
  Button,
  SimpleGrid,
  Tabs,
  Table,
  Loader,
  ActionIcon,
  Tooltip,
} from '@mantine/core'
import {
  IconGitPullRequest,
  IconEye,
  IconCheck,
  IconAlertTriangle,
  IconExternalLink,
  IconSettings,
  IconArrowLeft,
  IconRobot,
  IconRobotOff,
} from '@tabler/icons-react'
import { format, formatDistanceToNow } from 'date-fns'
import { projectsApi, pullRequestsApi, reviewsApi } from '@/lib/api'

interface Project {
  id: number
  name: string
  description?: string
  platform: string
  repositoryUrl?: string
  repositoryId?: string
  githubInstallationId?: number
  autoReviewEnabled: boolean
  isActive: boolean
  createdAt: string
  organization?: { id: number; name: string }
}

interface PullRequest {
  id: number
  projectId: number
  platform: string
  prId: number
  prNumber: number
  title: string
  description?: string
  sourceBranch: string
  targetBranch: string
  authorName: string
  reviewUrl?: string
  isClosed: boolean
  isMerged: boolean
  createdAt: string
  updatedAt: string
}

interface Review {
  id: number
  pullRequestId: number
  status: string
  filesAnalyzed?: number
  startedAt?: string
  completedAt?: string
  createdAt: string
  stats?: {
    total: number
    critical: number
    high: number
    medium: number
    low: number
    info: number
  }
}

const platformColors: Record<string, string> = {
  GITHUB: 'dark',
  BITBUCKET: 'blue',
  GITLAB: 'orange',
  AZURE_DEVOPS: 'cyan',
}

const reviewStatusColors: Record<string, string> = {
  COMPLETED: 'green',
  FAILED: 'red',
  IN_PROGRESS: 'blue',
  PENDING: 'gray',
}

export default function ProjectDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()

  const { data: project, isLoading: loadingProject } = useQuery<Project>({
    queryKey: ['project', id],
    queryFn: () => projectsApi.get(Number(id)),
    enabled: !!id,
  })

  const { data: pullRequests } = useQuery<PullRequest[]>({
    queryKey: ['pull-requests', id],
    queryFn: () => pullRequestsApi.list(Number(id)),
    enabled: !!id,
  })

  const { data: reviews } = useQuery<Review[]>({
    queryKey: ['reviews'],
    queryFn: () => reviewsApi.list(),
  })

  const pullRequestIds = useMemo(
    () => new Set((pullRequests ?? []).map((pr) => pr.id)),
    [pullRequests]
  )

  const projectReviews = useMemo(
    () => (reviews ?? []).filter((r) => pullRequestIds.has(r.pullRequestId)),
    [reviews, pullRequestIds]
  )

  const completedReviews = projectReviews.filter((r) => r.status === 'COMPLETED')
  const totalIssues = projectReviews.reduce(
    (sum, r) => sum + (r.stats?.total ?? 0),
    0
  )

  if (loadingProject) {
    return (
      <Stack align="center" justify="center" style={{ minHeight: '60vh' }}>
        <Loader size="lg" />
      </Stack>
    )
  }

  if (!project) {
    return (
      <Stack align="center" justify="center" style={{ minHeight: '60vh' }}>
        <Title order={3}>Project not found</Title>
        <Button variant="light" leftSection={<IconArrowLeft size={16} />} onClick={() => navigate('/projects')}>
          Back to Projects
        </Button>
      </Stack>
    )
  }

  return (
    <Stack gap="lg" p="md">
      {/* Header Section */}
      <Card withBorder shadow="sm" padding="lg">
        <Group justify="space-between" align="flex-start">
          <Stack gap="xs">
            <Group gap="sm">
              <Title order={2}>{project.name}</Title>
              <Badge color={platformColors[project.platform] ?? 'gray'} variant="filled" size="lg">
                {project.platform}
              </Badge>
              <Badge color={project.isActive ? 'green' : 'red'} variant="light" size="lg">
                {project.isActive ? 'Active' : 'Inactive'}
              </Badge>
            </Group>

            {project.description && (
              <Text c="dimmed" size="sm">
                {project.description}
              </Text>
            )}

            <Group gap="md" mt="xs">
              {project.repositoryUrl && (
                <Tooltip label="Open repository">
                  <Button
                    variant="subtle"
                    size="xs"
                    component="a"
                    href={project.repositoryUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    leftSection={<IconExternalLink size={14} />}
                  >
                    Repository
                  </Button>
                </Tooltip>
              )}

              <Tooltip label={project.autoReviewEnabled ? 'Auto-review enabled' : 'Auto-review disabled'}>
                <Badge
                  variant="dot"
                  color={project.autoReviewEnabled ? 'green' : 'gray'}
                  size="lg"
                  leftSection={
                    project.autoReviewEnabled ? (
                      <IconRobot size={14} />
                    ) : (
                      <IconRobotOff size={14} />
                    )
                  }
                >
                  {project.autoReviewEnabled ? 'Auto-review on' : 'Auto-review off'}
                </Badge>
              </Tooltip>

              <Text size="xs" c="dimmed">
                Created {formatDistanceToNow(new Date(project.createdAt), { addSuffix: true })}
              </Text>
            </Group>
          </Stack>

          <Group gap="xs">
            <Button
              variant="light"
              leftSection={<IconSettings size={16} />}
              onClick={() => navigate(`/projects/${id}/settings`)}
            >
              Edit Project
            </Button>
          </Group>
        </Group>
      </Card>

      {/* Stats Cards */}
      <SimpleGrid cols={{ base: 1, xs: 2, md: 4 }}>
        <Card withBorder shadow="sm" padding="lg">
          <Group justify="space-between">
            <Stack gap={0}>
              <Text size="xs" c="dimmed" tt="uppercase" fw={700}>
                Total Pull Requests
              </Text>
              <Title order={2}>{pullRequests?.length ?? 0}</Title>
            </Stack>
            <ActionIcon variant="light" color="blue" size="xl" radius="md">
              <IconGitPullRequest size={24} />
            </ActionIcon>
          </Group>
        </Card>

        <Card withBorder shadow="sm" padding="lg">
          <Group justify="space-between">
            <Stack gap={0}>
              <Text size="xs" c="dimmed" tt="uppercase" fw={700}>
                Total Reviews
              </Text>
              <Title order={2}>{projectReviews.length}</Title>
            </Stack>
            <ActionIcon variant="light" color="violet" size="xl" radius="md">
              <IconEye size={24} />
            </ActionIcon>
          </Group>
        </Card>

        <Card withBorder shadow="sm" padding="lg">
          <Group justify="space-between">
            <Stack gap={0}>
              <Text size="xs" c="dimmed" tt="uppercase" fw={700}>
                Completed Reviews
              </Text>
              <Title order={2}>{completedReviews.length}</Title>
            </Stack>
            <ActionIcon variant="light" color="green" size="xl" radius="md">
              <IconCheck size={24} />
            </ActionIcon>
          </Group>
        </Card>

        <Card withBorder shadow="sm" padding="lg">
          <Group justify="space-between">
            <Stack gap={0}>
              <Text size="xs" c="dimmed" tt="uppercase" fw={700}>
                Active Issues
              </Text>
              <Title order={2}>{totalIssues}</Title>
            </Stack>
            <ActionIcon variant="light" color="orange" size="xl" radius="md">
              <IconAlertTriangle size={24} />
            </ActionIcon>
          </Group>
        </Card>
      </SimpleGrid>

      {/* Tabs Section */}
      <Card withBorder shadow="sm" padding="lg">
        <Tabs defaultValue="pull-requests">
          <Tabs.List>
            <Tabs.Tab value="pull-requests" leftSection={<IconGitPullRequest size={16} />}>
              Pull Requests
            </Tabs.Tab>
            <Tabs.Tab value="reviews" leftSection={<IconEye size={16} />}>
              Reviews
            </Tabs.Tab>
          </Tabs.List>

          <Tabs.Panel value="pull-requests" pt="md">
            {pullRequests && pullRequests.length > 0 ? (
              <>
                <Table striped highlightOnHover>
                  <Table.Thead>
                    <Table.Tr>
                      <Table.Th>#</Table.Th>
                      <Table.Th>Title</Table.Th>
                      <Table.Th>Author</Table.Th>
                      <Table.Th>Branches</Table.Th>
                      <Table.Th>Status</Table.Th>
                      <Table.Th>Created</Table.Th>
                    </Table.Tr>
                  </Table.Thead>
                  <Table.Tbody>
                    {pullRequests.map((pr) => (
                      <Table.Tr
                        key={pr.id}
                        style={{ cursor: 'pointer' }}
                        onClick={() => navigate(`/projects/${id}/pull-requests/${pr.id}`)}
                      >
                        <Table.Td>
                          <Text size="sm" fw={600}>
                            #{pr.prNumber}
                          </Text>
                        </Table.Td>
                        <Table.Td>
                          <Text size="sm" lineClamp={1}>
                            {pr.title}
                          </Text>
                        </Table.Td>
                        <Table.Td>
                          <Text size="sm">{pr.authorName}</Text>
                        </Table.Td>
                        <Table.Td>
                          <Group gap={4}>
                            <Badge variant="outline" size="sm">
                              {pr.sourceBranch}
                            </Badge>
                            <Text size="xs" c="dimmed">
                              &rarr;
                            </Text>
                            <Badge variant="outline" size="sm">
                              {pr.targetBranch}
                            </Badge>
                          </Group>
                        </Table.Td>
                        <Table.Td>
                          {pr.isMerged ? (
                            <Badge color="violet" variant="filled" size="sm">
                              Merged
                            </Badge>
                          ) : pr.isClosed ? (
                            <Badge color="red" variant="filled" size="sm">
                              Closed
                            </Badge>
                          ) : (
                            <Badge color="green" variant="filled" size="sm">
                              Open
                            </Badge>
                          )}
                        </Table.Td>
                        <Table.Td>
                          <Tooltip label={format(new Date(pr.createdAt), 'PPpp')}>
                            <Text size="sm" c="dimmed">
                              {formatDistanceToNow(new Date(pr.createdAt), { addSuffix: true })}
                            </Text>
                          </Tooltip>
                        </Table.Td>
                      </Table.Tr>
                    ))}
                  </Table.Tbody>
                </Table>

                <Group justify="flex-end" mt="md">
                  <Button
                    variant="subtle"
                    onClick={() => navigate(`/projects/${id}/pull-requests`)}
                  >
                    View All Pull Requests
                  </Button>
                </Group>
              </>
            ) : (
              <Text c="dimmed" ta="center" py="xl">
                No pull requests found for this project.
              </Text>
            )}
          </Tabs.Panel>

          <Tabs.Panel value="reviews" pt="md">
            {projectReviews.length > 0 ? (
              <Table striped highlightOnHover>
                <Table.Thead>
                  <Table.Tr>
                    <Table.Th>ID</Table.Th>
                    <Table.Th>Status</Table.Th>
                    <Table.Th>Files Analyzed</Table.Th>
                    <Table.Th>Issues Found</Table.Th>
                    <Table.Th>Started</Table.Th>
                    <Table.Th>Completed</Table.Th>
                  </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                  {projectReviews.map((review) => (
                    <Table.Tr key={review.id}>
                      <Table.Td>
                        <Text size="sm" fw={600}>
                          #{review.id}
                        </Text>
                      </Table.Td>
                      <Table.Td>
                        <Badge
                          color={reviewStatusColors[review.status] ?? 'gray'}
                          variant="filled"
                          size="sm"
                        >
                          {review.status}
                        </Badge>
                      </Table.Td>
                      <Table.Td>
                        <Text size="sm">{review.filesAnalyzed ?? '-'}</Text>
                      </Table.Td>
                      <Table.Td>
                        <Text size="sm" fw={review.stats?.total ? 600 : 400}>
                          {review.stats?.total ?? 0}
                        </Text>
                      </Table.Td>
                      <Table.Td>
                        {review.startedAt ? (
                          <Tooltip label={format(new Date(review.startedAt), 'PPpp')}>
                            <Text size="sm" c="dimmed">
                              {formatDistanceToNow(new Date(review.startedAt), { addSuffix: true })}
                            </Text>
                          </Tooltip>
                        ) : (
                          <Text size="sm" c="dimmed">-</Text>
                        )}
                      </Table.Td>
                      <Table.Td>
                        {review.completedAt ? (
                          <Tooltip label={format(new Date(review.completedAt), 'PPpp')}>
                            <Text size="sm" c="dimmed">
                              {formatDistanceToNow(new Date(review.completedAt), { addSuffix: true })}
                            </Text>
                          </Tooltip>
                        ) : (
                          <Text size="sm" c="dimmed">-</Text>
                        )}
                      </Table.Td>
                    </Table.Tr>
                  ))}
                </Table.Tbody>
              </Table>
            ) : (
              <Text c="dimmed" ta="center" py="xl">
                No reviews found for this project.
              </Text>
            )}
          </Tabs.Panel>
        </Tabs>
      </Card>
    </Stack>
  )
}
