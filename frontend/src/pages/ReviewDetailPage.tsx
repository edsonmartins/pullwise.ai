import { useParams, useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  Card,
  Stack,
  Group,
  Badge,
  Text,
  Title,
  Accordion,
  Code,
  Loader,
  ActionIcon,
  Tooltip,
  Progress,
  SimpleGrid,
  Divider,
  Paper,
} from '@mantine/core'
import {
  IconArrowLeft,
  IconBug,
  IconAlertTriangle,
  IconAlertCircle,
  IconInfoCircle,
  IconShieldCheck,
  IconFlag,
} from '@tabler/icons-react'
import { format, formatDistanceToNow } from 'date-fns'
import { reviewsApi } from '@/lib/api'

interface Review {
  id: number
  pullRequestId: number
  status: string
  sastEnabled: boolean
  llmEnabled: boolean
  ragEnabled: boolean
  filesAnalyzed?: number
  linesAddedAnalyzed?: number
  linesRemovedAnalyzed?: number
  reviewCommentId?: string
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

interface Issue {
  id: number
  reviewId: number
  severity: 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW' | 'INFO'
  type: string
  source: string
  title: string
  description?: string
  filePath?: string
  lineStart?: number
  lineEnd?: number
  ruleId?: string
  suggestion?: string
  isFalsePositive: boolean
}

const severityColor = (s: string) => {
  switch (s) {
    case 'CRITICAL':
      return 'red'
    case 'HIGH':
      return 'orange'
    case 'MEDIUM':
      return 'yellow'
    case 'LOW':
      return 'blue'
    default:
      return 'gray'
  }
}

const statusColor = (status: string) => {
  switch (status) {
    case 'COMPLETED':
      return 'green'
    case 'FAILED':
      return 'red'
    case 'IN_PROGRESS':
      return 'blue'
    case 'PENDING':
      return 'gray'
    case 'CANCELLED':
      return 'orange'
    default:
      return 'gray'
  }
}

const sourceColor = (source: string) => {
  switch (source) {
    case 'SAST':
      return 'violet'
    case 'LLM':
      return 'cyan'
    case 'RAG':
      return 'teal'
    default:
      return 'gray'
  }
}

export default function ReviewDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const { data: review, isLoading: loadingReview } = useQuery<Review>({
    queryKey: ['review', id],
    queryFn: () => reviewsApi.get(Number(id)),
    enabled: !!id,
  })

  const { data: issues, isLoading: loadingIssues } = useQuery<Issue[]>({
    queryKey: ['review-issues', id],
    queryFn: () => reviewsApi.getIssues(Number(id)),
    enabled: !!id,
  })

  const fpMutation = useMutation({
    mutationFn: (issueId: number) => reviewsApi.markFalsePositive(issueId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['review-issues', id] })
      queryClient.invalidateQueries({ queryKey: ['review', id] })
    },
  })

  if (loadingReview) {
    return (
      <Stack align="center" justify="center" style={{ minHeight: '60vh' }}>
        <Loader size="lg" />
        <Text c="dimmed">Loading review...</Text>
      </Stack>
    )
  }

  if (!review) {
    return (
      <Stack align="center" justify="center" style={{ minHeight: '60vh' }}>
        <IconAlertCircle size={48} color="gray" />
        <Title order={3} c="dimmed">
          Review not found
        </Title>
        <Text
          c="blue"
          style={{ cursor: 'pointer' }}
          onClick={() => navigate('/reviews')}
        >
          Back to Reviews
        </Text>
      </Stack>
    )
  }

  const stats = review.stats ?? {
    total: 0,
    critical: 0,
    high: 0,
    medium: 0,
    low: 0,
    info: 0,
  }

  const progressSections = stats.total > 0
    ? [
        { value: (stats.critical / stats.total) * 100, color: 'red', label: 'Critical' },
        { value: (stats.high / stats.total) * 100, color: 'orange', label: 'High' },
        { value: (stats.medium / stats.total) * 100, color: 'yellow', label: 'Medium' },
        { value: (stats.low / stats.total) * 100, color: 'blue', label: 'Low' },
        { value: (stats.info / stats.total) * 100, color: 'gray', label: 'Info' },
      ].filter((s) => s.value > 0)
    : []

  const formatLine = (issue: Issue) => {
    if (issue.lineStart && issue.lineEnd && issue.lineStart !== issue.lineEnd) {
      return `L${issue.lineStart}-L${issue.lineEnd}`
    }
    if (issue.lineStart) {
      return `L${issue.lineStart}`
    }
    return null
  }

  return (
    <Stack gap="lg" p="md">
      {/* Header Section */}
      <Card shadow="sm" padding="lg" radius="md" withBorder>
        <Group justify="space-between" align="flex-start">
          <Stack gap="xs">
            <Group gap="md">
              <ActionIcon
                variant="subtle"
                size="lg"
                onClick={() => navigate('/reviews')}
                aria-label="Back to Reviews"
              >
                <IconArrowLeft size={20} />
              </ActionIcon>
              <Title order={2}>Review #{review.id}</Title>
              <Badge
                color={statusColor(review.status)}
                variant="filled"
                size="lg"
              >
                {review.status}
              </Badge>
            </Group>
            <Group gap="lg" ml={52}>
              <Text size="sm" c="dimmed">
                Created: {format(new Date(review.createdAt), 'PPp')}
              </Text>
              {review.startedAt && (
                <Text size="sm" c="dimmed">
                  Started: {format(new Date(review.startedAt), 'PPp')}
                </Text>
              )}
              {review.completedAt && (
                <Text size="sm" c="dimmed">
                  Completed: {format(new Date(review.completedAt), 'PPp')}
                </Text>
              )}
              {review.startedAt && review.completedAt && (
                <Text size="sm" c="dimmed">
                  Duration:{' '}
                  {formatDistanceToNow(new Date(review.startedAt), {
                    addSuffix: false,
                  })}
                </Text>
              )}
            </Group>
            <Group gap="sm" ml={52}>
              {review.sastEnabled && (
                <Badge variant="outline" color="violet" size="sm">
                  SAST
                </Badge>
              )}
              {review.llmEnabled && (
                <Badge variant="outline" color="cyan" size="sm">
                  LLM
                </Badge>
              )}
              {review.ragEnabled && (
                <Badge variant="outline" color="teal" size="sm">
                  RAG
                </Badge>
              )}
              {review.filesAnalyzed != null && (
                <Text size="sm" c="dimmed">
                  {review.filesAnalyzed} files analyzed
                </Text>
              )}
            </Group>
          </Stack>
        </Group>
      </Card>

      {/* Stats Overview */}
      <SimpleGrid cols={{ base: 2, sm: 3, md: 6 }}>
        <Paper shadow="xs" p="md" radius="md" withBorder>
          <Stack align="center" gap={4}>
            <IconBug size={24} color="gray" />
            <Text size="xl" fw={700}>
              {stats.total}
            </Text>
            <Text size="sm" c="dimmed">
              Total Issues
            </Text>
          </Stack>
        </Paper>
        <Paper shadow="xs" p="md" radius="md" withBorder>
          <Stack align="center" gap={4}>
            <IconAlertTriangle size={24} color="red" />
            <Text size="xl" fw={700} c="red">
              {stats.critical}
            </Text>
            <Text size="sm" c="dimmed">
              Critical
            </Text>
          </Stack>
        </Paper>
        <Paper shadow="xs" p="md" radius="md" withBorder>
          <Stack align="center" gap={4}>
            <IconAlertTriangle size={24} color="orange" />
            <Text size="xl" fw={700} c="orange">
              {stats.high}
            </Text>
            <Text size="sm" c="dimmed">
              High
            </Text>
          </Stack>
        </Paper>
        <Paper shadow="xs" p="md" radius="md" withBorder>
          <Stack align="center" gap={4}>
            <IconAlertCircle size={24} color="yellow" />
            <Text size="xl" fw={700} c="yellow">
              {stats.medium}
            </Text>
            <Text size="sm" c="dimmed">
              Medium
            </Text>
          </Stack>
        </Paper>
        <Paper shadow="xs" p="md" radius="md" withBorder>
          <Stack align="center" gap={4}>
            <IconInfoCircle size={24} color="blue" />
            <Text size="xl" fw={700} c="blue">
              {stats.low}
            </Text>
            <Text size="sm" c="dimmed">
              Low
            </Text>
          </Stack>
        </Paper>
        <Paper shadow="xs" p="md" radius="md" withBorder>
          <Stack align="center" gap={4}>
            <IconShieldCheck size={24} color="gray" />
            <Text size="xl" fw={700} c="gray">
              {stats.info}
            </Text>
            <Text size="sm" c="dimmed">
              Info
            </Text>
          </Stack>
        </Paper>
      </SimpleGrid>

      {/* Severity Distribution Progress Bar */}
      {stats.total > 0 && (
        <Card shadow="sm" padding="lg" radius="md" withBorder>
          <Text fw={500} mb="sm">
            Severity Distribution
          </Text>
          <Progress.Root size="xl">
            {progressSections.map((section) => (
              <Tooltip key={section.label} label={`${section.label}: ${Math.round(section.value)}%`}>
                <Progress.Section value={section.value} color={section.color}>
                  <Progress.Label>{section.label}</Progress.Label>
                </Progress.Section>
              </Tooltip>
            ))}
          </Progress.Root>
        </Card>
      )}

      <Divider />

      {/* Issues List */}
      <Stack gap="sm">
        <Title order={3}>Issues</Title>

        {loadingIssues ? (
          <Stack align="center" py="xl">
            <Loader size="md" />
            <Text c="dimmed" size="sm">
              Loading issues...
            </Text>
          </Stack>
        ) : !issues || issues.length === 0 ? (
          <Card shadow="sm" padding="lg" radius="md" withBorder>
            <Text c="dimmed" ta="center">
              No issues found for this review.
            </Text>
          </Card>
        ) : (
          <Accordion variant="separated" radius="md">
            {issues.map((issue) => (
              <Accordion.Item
                key={issue.id}
                value={String(issue.id)}
                style={
                  issue.isFalsePositive
                    ? { opacity: 0.6 }
                    : undefined
                }
              >
                <Accordion.Control>
                  <Group justify="space-between" wrap="nowrap" gap="sm">
                    <Group gap="sm" wrap="nowrap" style={{ flex: 1, minWidth: 0 }}>
                      <Badge
                        color={severityColor(issue.severity)}
                        variant="filled"
                        size="sm"
                        style={{ flexShrink: 0 }}
                      >
                        {issue.severity}
                      </Badge>
                      <Badge
                        color="dark"
                        variant="outline"
                        size="sm"
                        style={{ flexShrink: 0 }}
                      >
                        {issue.type}
                      </Badge>
                      <Badge
                        color={sourceColor(issue.source)}
                        variant="light"
                        size="sm"
                        style={{ flexShrink: 0 }}
                      >
                        {issue.source}
                      </Badge>
                      <Text
                        size="sm"
                        fw={500}
                        truncate
                        style={
                          issue.isFalsePositive
                            ? { textDecoration: 'line-through' }
                            : undefined
                        }
                      >
                        {issue.title}
                      </Text>
                    </Group>
                    {issue.isFalsePositive && (
                      <Badge color="gray" variant="light" size="sm" style={{ flexShrink: 0 }}>
                        False Positive
                      </Badge>
                    )}
                  </Group>
                </Accordion.Control>
                <Accordion.Panel>
                  <Stack gap="md">
                    {/* File Path and Line Info */}
                    {issue.filePath && (
                      <Group gap="xs">
                        <Text size="sm" c="dimmed">
                          File:
                        </Text>
                        <Code>{issue.filePath}</Code>
                        {formatLine(issue) && (
                          <Badge variant="light" size="sm">
                            {formatLine(issue)}
                          </Badge>
                        )}
                      </Group>
                    )}

                    {/* Rule ID */}
                    {issue.ruleId && (
                      <Group gap="xs">
                        <Text size="sm" c="dimmed">
                          Rule:
                        </Text>
                        <Code>{issue.ruleId}</Code>
                      </Group>
                    )}

                    {/* Description */}
                    {issue.description && (
                      <Stack gap={4}>
                        <Text size="sm" fw={500}>
                          Description
                        </Text>
                        <Text
                          size="sm"
                          style={
                            issue.isFalsePositive
                              ? { textDecoration: 'line-through' }
                              : undefined
                          }
                        >
                          {issue.description}
                        </Text>
                      </Stack>
                    )}

                    {/* Suggestion */}
                    {issue.suggestion && (
                      <Stack gap={4}>
                        <Text size="sm" fw={500}>
                          Suggestion
                        </Text>
                        <Code block>{issue.suggestion}</Code>
                      </Stack>
                    )}

                    <Divider />

                    {/* Actions */}
                    <Group justify="flex-end">
                      {!issue.isFalsePositive && (
                        <Tooltip label="Mark as False Positive">
                          <ActionIcon
                            variant="subtle"
                            color="orange"
                            size="lg"
                            onClick={() => fpMutation.mutate(issue.id)}
                            loading={
                              fpMutation.isPending &&
                              fpMutation.variables === issue.id
                            }
                            aria-label="Mark as False Positive"
                          >
                            <IconFlag size={18} />
                          </ActionIcon>
                        </Tooltip>
                      )}
                    </Group>
                  </Stack>
                </Accordion.Panel>
              </Accordion.Item>
            ))}
          </Accordion>
        )}
      </Stack>
    </Stack>
  )
}
