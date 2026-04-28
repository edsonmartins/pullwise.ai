import { useQuery } from '@tanstack/react-query'
import { Card, Stack, Group, Badge, Text, Title, Loader, Code, Divider } from '@mantine/core'
import { IconTarget, IconAlertCircle } from '@tabler/icons-react'
import { blastRadiusApi, reviewsApi, type BlastRadiusResult, type ImpactedNode } from '@/lib/api'

type BlastRadiusCardProps =
  | { reviewId: number; projectId?: never; changedFiles?: never; topN?: number }
  | { projectId: number; changedFiles: string[]; reviewId?: never; topN?: number }

function riskBadgeColor(risk: number): string {
  if (risk >= 0.75) return 'red'
  if (risk >= 0.55) return 'orange'
  if (risk >= 0.35) return 'yellow'
  return 'gray'
}

function shortPath(path: string | null): string {
  if (!path) return '—'
  const parts = path.split('/')
  return parts.length > 3 ? parts.slice(-3).join('/') : path
}

export function BlastRadiusCard(props: BlastRadiusCardProps) {
  const topN = props.topN ?? 5
  const enabled = 'reviewId' in props
    ? !!props.reviewId
    : props.projectId > 0 && props.changedFiles.length > 0

  const queryKey = 'reviewId' in props
    ? ['blast-radius', 'review', props.reviewId]
    : ['blast-radius', 'project', props.projectId, [...props.changedFiles].sort()]

  const { data, isLoading, error } = useQuery<BlastRadiusResult>({
    queryKey,
    queryFn: () => {
      if ('reviewId' in props && props.reviewId) {
        return reviewsApi.getBlastRadius(props.reviewId)
      }
      if ('projectId' in props && props.projectId && props.changedFiles) {
        return blastRadiusApi.compute(props.projectId, props.changedFiles)
      }
      return Promise.resolve<BlastRadiusResult>({
        impactedNodes: [],
        impactedFiles: [],
        truncated: false,
        seedCount: 0,
      })
    },
    enabled,
    staleTime: 1000 * 60 * 10, // 10 min — alinha com o cache Redis do backend
  })

  if (!enabled) return null

  if (isLoading) {
    return (
      <Card withBorder padding="md">
        <Group gap="xs">
          <IconTarget size={18} />
          <Title order={5}>Blast Radius</Title>
          <Loader size="xs" />
        </Group>
      </Card>
    )
  }

  if (error) {
    return (
      <Card withBorder padding="md">
        <Group gap="xs">
          <IconAlertCircle size={18} color="orange" />
          <Text c="dimmed" size="sm">Could not compute blast radius</Text>
        </Group>
      </Card>
    )
  }

  if (!data || data.impactedNodes.length === 0) {
    return (
      <Card withBorder padding="md">
        <Group gap="xs" mb={4}>
          <IconTarget size={18} />
          <Title order={5}>Blast Radius</Title>
        </Group>
        <Text c="dimmed" size="sm">No downstream impact detected for the changed files.</Text>
      </Card>
    )
  }

  const top: ImpactedNode[] = data.impactedNodes.slice(0, topN)

  return (
    <Card withBorder padding="md">
      <Stack gap="sm">
        <Group justify="space-between">
          <Group gap="xs">
            <IconTarget size={18} />
            <Title order={5}>Blast Radius</Title>
          </Group>
          {data.truncated && (
            <Badge color="orange" variant="light" size="sm">
              truncated
            </Badge>
          )}
        </Group>

        <Group gap="md">
          <Text size="sm">
            <Text span fw={600}>{data.seedCount}</Text> seeds
          </Text>
          <Text size="sm">
            <Text span fw={600}>{data.impactedNodes.length}</Text> impacted nodes
          </Text>
          <Text size="sm">
            <Text span fw={600}>{data.impactedFiles.length}</Text> files
          </Text>
        </Group>

        <Divider />

        <Stack gap="xs">
          <Text size="xs" c="dimmed" tt="uppercase" fw={500}>
            Top impacted symbols
          </Text>
          {top.map((node) => (
            <Group key={node.qualifiedName} justify="space-between" wrap="nowrap" gap="sm">
              <Stack gap={2} style={{ minWidth: 0, flex: 1 }}>
                <Code style={{ background: 'transparent', padding: 0, fontSize: 13 }}>
                  {node.qualifiedName}
                </Code>
                <Text size="xs" c="dimmed">
                  {shortPath(node.filePath)} · depth {node.depth} · confidence{' '}
                  {(node.propagatedConfidence * 100).toFixed(0)}%
                </Text>
              </Stack>
              <Badge color={riskBadgeColor(node.riskScore)} variant="light" size="sm">
                risk {node.riskScore.toFixed(2)}
              </Badge>
            </Group>
          ))}
          {data.impactedNodes.length > topN && (
            <Text size="xs" c="dimmed" ta="center">
              … and {data.impactedNodes.length - topN} more
            </Text>
          )}
        </Stack>
      </Stack>
    </Card>
  )
}
