import {
  Title,
  Text,
  Stack,
  Card,
  Badge,
  Group,
  Alert,
  Accordion,
  Code,
  ScrollArea,
} from '@mantine/core'
import { IconFileSearch, IconAlertCircle } from '@tabler/icons-react'
import { useQuery } from '@tanstack/react-query'
import { reviewsApi } from '@/lib/api'
import { formatDistanceToNow } from 'date-fns'
import { ptBR } from 'date-fns/locale'

const getStatusColor = (status: string) => {
  switch (status) {
    case 'COMPLETED':
      return 'green'
    case 'FAILED':
      return 'red'
    case 'IN_PROGRESS':
      return 'blue'
    case 'CANCELLED':
      return 'gray'
    default:
      return 'gray'
  }
}

const getSeverityColor = (severity: string) => {
  switch (severity) {
    case 'CRITICAL':
      return 'red'
    case 'HIGH':
      return 'orange'
    case 'MEDIUM':
      return 'yellow'
    case 'LOW':
      return 'blue'
    case 'INFO':
      return 'gray'
    default:
      return 'gray'
  }
}

export function ReviewsPage() {
  const { data: reviews, isLoading } = useQuery({
    queryKey: ['reviews'],
    queryFn: () => reviewsApi.list(),
  })

  return (
    <Stack gap="xl">
      <div>
        <Title order={2}>Reviews</Title>
        <Text c="dimmed">Histórico de análises de código</Text>
      </div>

      {isLoading ? (
        <Text>Carregando...</Text>
      ) : reviews && reviews.length > 0 ? (
        <Stack>
          {reviews.map((review) => (
            <ReviewCard key={review.id} review={review} />
          ))}
        </Stack>
      ) : (
        <Alert icon={<IconAlertCircle size={16} />}>
          Nenhum review encontrado
        </Alert>
      )}
    </Stack>
  )
}

interface ReviewCardProps {
  review: {
    id: number
    pullRequestId: number
    status: string
    sastEnabled: boolean
    llmEnabled: boolean
    ragEnabled: boolean
    filesAnalyzed?: number
    createdAt: string
    startedAt?: string
    completedAt?: string
    stats?: {
      total: number
      critical: number
      high: number
      medium: number
      low: number
      info: number
    }
  }
}

function ReviewCard({ review }: ReviewCardProps) {
  const { data: issues } = useQuery({
    queryKey: ['review-issues', review.id],
    queryFn: () => reviewsApi.getIssues(review.id),
    enabled: review.status === 'COMPLETED',
  })

  return (
    <Card padding="lg" radius="md" withBorder>
      <Group justify="space-between" mb="sm">
        <Group>
          <IconFileSearch size={20} />
          <Text fw={500}>PR #{review.pullRequestId}</Text>
          <Badge color={getStatusColor(review.status)}>{review.status}</Badge>
        </Group>
        <Text size="sm" c="dimmed">
          {formatDistanceToNow(new Date(review.createdAt), { locale: ptBR })} atrás
        </Text>
      </Group>

      <Group gap="md" mb="sm">
        <Badge variant="light" color={review.sastEnabled ? 'green' : 'gray'}>
          SAST
        </Badge>
        <Badge variant="light" color={review.llmEnabled ? 'blue' : 'gray'}>
          LLM
        </Badge>
        <Badge variant="light" color={review.ragEnabled ? 'purple' : 'gray'}>
          RAG
        </Badge>
      </Group>

      {review.stats && review.stats.total > 0 && (
        <Group gap="xs" mb="sm">
          <Badge color="red">{review.stats.critical} Críticos</Badge>
          <Badge color="orange">{review.stats.high} Altos</Badge>
          <Badge color="yellow">{review.stats.medium} Médios</Badge>
          <Badge color="blue">{review.stats.low} Baixos</Badge>
        </Group>
      )}

      {issues && issues.length > 0 && (
        <Accordion>
          <Accordion.Item value="issues">
            <Accordion.Control>Ver issues ({issues.length})</Accordion.Control>
            <Accordion.Panel>
              <ScrollArea.Autosize mah={300}>
                <Stack gap="sm">
                  {issues.map((issue) => (
                    <Card key={issue.id} padding="sm" radius="sm" withBorder>
                      <Group justify="space-between" mb="xs">
                        <Group>
                          <Badge color={getSeverityColor(issue.severity)}>
                            {issue.severity}
                          </Badge>
                          <Badge variant="light">{issue.source}</Badge>
                        </Group>
                        {issue.ruleId && (
                          <Code fz="xs">{issue.ruleId}</Code>
                        )}
                      </Group>
                      <Text fw={500} size="sm">
                        {issue.title}
                      </Text>
                      {issue.description && (
                        <Text size="xs" c="dimmed" mt="xs">
                          {issue.description}
                        </Text>
                      )}
                      {issue.filePath && (
                        <Text size="xs" c="blue" mt="xs">
                          {issue.filePath}
                          {issue.lineStart && `:${issue.lineStart}`}
                          {issue.lineEnd && `-${issue.lineEnd}`}
                        </Text>
                      )}
                      {issue.suggestion && (
                        <Card padding="xs" radius="sm" mt="xs" bg="green.0">
                          <Text size="xs" c="green.9">
                            <strong>Sugestão:</strong> {issue.suggestion}
                          </Text>
                        </Card>
                      )}
                    </Card>
                  ))}
                </Stack>
              </ScrollArea.Autosize>
            </Accordion.Panel>
          </Accordion.Item>
        </Accordion>
      )}
    </Card>
  )
}
