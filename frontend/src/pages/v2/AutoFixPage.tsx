import { useState, useCallback, useEffect } from 'react'
import {
  Title,
  Text,
  Stack,
  Group,
  Button,
  Card,
  Badge,
  Select,
  ScrollArea,
  Loader,
  Tooltip,
  Modal,
  Textarea,
  SimpleGrid,
  Box,
} from '@mantine/core'
import {
  IconCheck,
  IconX,
  IconCode,
  IconTool,
  IconEye,
  IconDownload,
  IconCopy,
  IconPlayerPlay,
  IconClock,
  IconCircleDot,
} from '@tabler/icons-react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { autofixApi, sandboxApi } from '@/lib/v2/api'
import { useV2Store } from '@/store/v2-store'
import type { FixSuggestion, SandboxResult } from '@/types/v2'
import DiffViewer from 'react-diff-viewer-continued'

interface AutoFixPageProps {
  reviewId?: number
}

export function AutoFixPage({ reviewId }: AutoFixPageProps) {
  const queryClient = useQueryClient()
  const {
    fixSuggestions,
    setFixSuggestions,
    selectedFixSuggestion,
    setSelectedFixSuggestion,
    isApplyingFix,
    setIsApplyingFix,
  } = useV2Store()

  const [rejectReason, setRejectReason] = useState('')
  const [rejectModalOpen, setRejectModalOpen] = useState(false)
  const [previewMode, setPreviewMode] = useState<'split' | 'unified'>('split')
  const [sandboxResult, setSandboxResult] = useState<SandboxResult | null>(null)

  // Fetch fix suggestions for the review
  const { data: suggestions, isLoading } = useQuery<FixSuggestion[]>({
    queryKey: ['fixSuggestions', reviewId],
    queryFn: () => autofixApi.listByReview(reviewId || 0),
    enabled: !!reviewId,
  })

  // Update store when suggestions change
  useEffect(() => {
    if (suggestions) {
      setFixSuggestions(suggestions)
    }
  }, [suggestions, setFixSuggestions])

  // Apply fix mutation
  const applyFixMutation = useMutation({
    mutationFn: (suggestionId: number) => autofixApi.applyFix(suggestionId),
    onMutate: () => {
      setIsApplyingFix(true)
    },
    onSuccess: () => {
      setIsApplyingFix(false)
      queryClient.invalidateQueries({ queryKey: ['fixSuggestions'] })
    },
    onError: () => {
      setIsApplyingFix(false)
    },
  })

  // Approve fix mutation
  const approveMutation = useMutation({
    mutationFn: ({ suggestionId, reviewedBy }: {
      suggestionId: number
      reviewedBy: string
    }) => autofixApi.approveSuggestion(suggestionId, reviewedBy),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['fixSuggestions'] })
    },
  })

  // Reject fix mutation
  const rejectMutation = useMutation({
    mutationFn: ({ suggestionId, reviewedBy, reason }: {
      suggestionId: number
      reviewedBy: string
      reason: string
    }) => autofixApi.rejectSuggestion(suggestionId, reviewedBy, reason),
    onSuccess: () => {
      setRejectModalOpen(false)
      setRejectReason('')
      queryClient.invalidateQueries({ queryKey: ['fixSuggestions'] })
    },
  })

  // Validate fix in sandbox
  const validateMutation = useMutation({
    mutationFn: ({ originalCode, fixedCode, language }: {
      originalCode: string
      fixedCode: string
      language: string
    }) => sandboxApi.validateFix(originalCode, fixedCode, language),
    onSuccess: (result) => {
      setSandboxResult(result)
    },
  })

  // Get status badge color
  const getStatusColor = (status: FixSuggestion['status']) => {
    switch (status) {
      case 'PENDING': return 'yellow'
      case 'APPROVED': return 'blue'
      case 'APPLIED': return 'green'
      case 'REJECTED': return 'red'
      case 'FAILED': return 'red'
      case 'EXPIRED': return 'gray'
      default: return 'gray'
    }
  }

  // Get confidence badge color
  const getConfidenceColor = (confidence: FixSuggestion['confidence']) => {
    switch (confidence) {
      case 'HIGH': return 'green'
      case 'MEDIUM': return 'yellow'
      case 'LOW': return 'red'
      default: return 'gray'
    }
  }

  // Handle apply fix
  const handleApplyFix = useCallback((suggestion: FixSuggestion) => {
    setSelectedFixSuggestion(suggestion)
    applyFixMutation.mutate(suggestion.id)
  }, [applyFixMutation, setSelectedFixSuggestion])

  // Handle approve fix
  const handleApproveFix = useCallback((suggestion: FixSuggestion) => {
    approveMutation.mutate({
      suggestionId: suggestion.id,
      reviewedBy: 'current-user',
    })
  }, [approveMutation])

  // Handle reject fix
  const handleRejectFix = useCallback((suggestion: FixSuggestion) => {
    setSelectedFixSuggestion(suggestion)
    setRejectModalOpen(true)
  }, [setSelectedFixSuggestion])

  // Confirm reject
  const confirmReject = useCallback(() => {
    if (selectedFixSuggestion && rejectReason) {
      rejectMutation.mutate({
        suggestionId: selectedFixSuggestion.id,
        reviewedBy: 'current-user',
        reason: rejectReason,
      })
    }
  }, [selectedFixSuggestion, rejectReason, rejectMutation])

  // Handle validate fix
  const handleValidateFix = useCallback((suggestion: FixSuggestion) => {
    if (!suggestion.originalCode || !suggestion.fixedCode) return

    const language = detectLanguage(suggestion.filePath)
    validateMutation.mutate({
      originalCode: suggestion.originalCode,
      fixedCode: suggestion.fixedCode,
      language,
    })
  }, [validateMutation])

  // Detect language from file path
  const detectLanguage = (filePath?: string): string => {
    if (!filePath) return 'text'
    const ext = filePath.split('.').pop()?.toLowerCase()
    switch (ext) {
      case 'java': return 'java'
      case 'ts': return 'typescript'
      case 'js': return 'javascript'
      case 'py': return 'python'
      case 'go': return 'go'
      default: return 'text'
    }
  }

  // Filter suggestions by status
  const allSuggestions = fixSuggestions.length > 0 ? fixSuggestions : (suggestions || [])
  const pendingSuggestions = allSuggestions.filter((s: FixSuggestion) => s.status === 'PENDING')
  const approvedSuggestions = allSuggestions.filter((s: FixSuggestion) => s.status === 'APPROVED')
  const appliedSuggestions = allSuggestions.filter((s: FixSuggestion) => s.status === 'APPLIED')
  const rejectedSuggestions = allSuggestions.filter((s: FixSuggestion) => s.status === 'REJECTED')

  const currentSuggestion = selectedFixSuggestion || allSuggestions[0]

  return (
    <Stack gap="md">
      {/* Header */}
      <Group justify="space-between">
        <div>
          <Title order={2}>Auto-Fix</Title>
          <Text c="dimmed">Sugestões de correção automática para issues</Text>
        </div>
        <Group>
          <Select
            placeholder="Visualização"
            value={previewMode}
            onChange={(v) => setPreviewMode(v as 'split' | 'unified')}
            data={[
              { value: 'split', label: 'Split View' },
              { value: 'unified', label: 'Unified View' },
            ]}
          />
        </Group>
      </Group>

      {/* Summary Cards */}
      <SimpleGrid cols={{ base: 2, md: 4 }}>
        <SummaryCard
          label="Pendentes"
          value={pendingSuggestions.length}
          color="yellow"
          icon={<IconClock size={20} />}
        />
        <SummaryCard
          label="Aprovados"
          value={approvedSuggestions.length}
          color="blue"
          icon={<IconCheck size={20} />}
        />
        <SummaryCard
          label="Aplicados"
          value={appliedSuggestions.length}
          color="green"
          icon={<IconTool size={20} />}
        />
        <SummaryCard
          label="Rejeitados"
          value={rejectedSuggestions.length}
          color="red"
          icon={<IconX size={20} />}
        />
      </SimpleGrid>

      {/* Main Content */}
      <SimpleGrid cols={{ base: 1, lg: 3 }}>
        {/* Suggestions List */}
        <Card padding="md" withBorder h={700}>
          <Group justify="space-between" mb="md">
            <Title order={5}>Sugestões</Title>
            <Badge>{allSuggestions.length}</Badge>
          </Group>

          <ScrollArea h={600}>
            <Stack gap="sm">
              {isLoading ? (
                <Stack align="center" py="xl">
                  <Loader size="sm" />
                  <Text size="sm" c="dimmed">Carregando...</Text>
                </Stack>
              ) : allSuggestions.length === 0 ? (
                <Text c="dimmed" ta="center" py="xl">
                  Nenhuma sugestão disponível
                </Text>
              ) : (
                allSuggestions.map((suggestion: FixSuggestion) => (
                  <FixSuggestionCard
                    key={suggestion.id}
                    suggestion={suggestion}
                    isSelected={selectedFixSuggestion?.id === suggestion.id}
                    onClick={() => setSelectedFixSuggestion(suggestion)}
                    onApply={() => handleApplyFix(suggestion)}
                    onApprove={() => handleApproveFix(suggestion)}
                    onReject={() => handleRejectFix(suggestion)}
                    onValidate={() => handleValidateFix(suggestion)}
                    isValidating={validateMutation.isPending}
                    isApplying={applyFixMutation.isPending}
                  />
                ))
              )}
            </Stack>
          </ScrollArea>
        </Card>

        {/* Diff Viewer */}
        <Card padding="0" withBorder h={700}>
          {!currentSuggestion ? (
            <Stack align="center" justify="center" h="100%">
              <IconCode size={48} style={{ color: 'var(--mantine-color-dimmed)' }} />
              <Text c="dimmed">Selecione uma sugestão para ver o diff</Text>
            </Stack>
          ) : (
            <Stack h="100%">
              {/* Suggestion Header */}
              <Box p="md" bg="dark.0">
                <Group justify="space-between">
                  <Group>
                    <Badge color={getStatusColor(currentSuggestion.status)}>
                      {currentSuggestion.status}
                    </Badge>
                    <Badge color={getConfidenceColor(currentSuggestion.confidence)}>
                      Confiança: {currentSuggestion.confidence}
                    </Badge>
                    {currentSuggestion.modelUsed && (
                      <Badge variant="light" leftSection={<IconPlayerPlay size={12} />}>
                        {currentSuggestion.modelUsed}
                      </Badge>
                    )}
                  </Group>
                  <Group gap="xs">
                    <Tooltip label="Copiar código">
                      <Button variant="subtle" size="xs">
                        <IconCopy size={16} />
                      </Button>
                    </Tooltip>
                    <Tooltip label="Baixar patch">
                      <Button variant="subtle" size="xs">
                        <IconDownload size={16} />
                      </Button>
                    </Tooltip>
                  </Group>
                </Group>

                {currentSuggestion.filePath && (
                  <Text size="sm" c="dimmed" mt="sm">
                    {currentSuggestion.filePath}
                    {currentSuggestion.startLine && `:${currentSuggestion.startLine}`}
                    {currentSuggestion.endLine && `-${currentSuggestion.endLine}`}
                  </Text>
                )}

                {currentSuggestion.explanation && (
                  <Box mt="sm" p="sm" style={{ backgroundColor: 'var(--mantine-color-dark-1)', borderRadius: '4px' }}>
                    <Group gap="sm">
                      <IconCircleDot size={16} />
                      <Text size="sm" fw={600}>Explicação</Text>
                    </Group>
                    <Text size="sm" mt="xs">{currentSuggestion.explanation}</Text>
                  </Box>
                )}

                {/* Cost Estimate */}
                {currentSuggestion.inputTokens && (
                  <Group gap="xl" mt="sm">
                    <Text size="xs" c="dimmed">
                      Input: {currentSuggestion.inputTokens.toLocaleString()} tokens
                    </Text>
                    <Text size="xs" c="dimmed">
                      Output: {currentSuggestion.outputTokens?.toLocaleString() || 0} tokens
                    </Text>
                    <Text size="xs" c="dimmed">
                      Custo: ${(currentSuggestion.estimatedCost || 0).toFixed(4)}
                    </Text>
                  </Group>
                )}

                {/* Actions */}
                <Group mt="md">
                  {currentSuggestion.status === 'PENDING' && (
                    <>
                      <Button
                        size="sm"
                        leftSection={<IconCheck size={16} />}
                        onClick={() => handleApproveFix(currentSuggestion)}
                        loading={approveMutation.isPending}
                      >
                        Aprovar
                      </Button>
                      <Button
                        size="sm"
                        variant="light"
                        color="red"
                        leftSection={<IconX size={16} />}
                        onClick={() => handleRejectFix(currentSuggestion)}
                      >
                        Rejeitar
                      </Button>
                    </>
                  )}
                  {currentSuggestion.status === 'APPROVED' && (
                    <Button
                      size="sm"
                      leftSection={<IconTool size={16} />}
                      onClick={() => handleApplyFix(currentSuggestion)}
                      loading={isApplyingFix}
                    >
                      Aplicar Correção
                    </Button>
                  )}
                  <Button
                    size="sm"
                    variant="light"
                    leftSection={<IconEye size={16} />}
                    onClick={() => handleValidateFix(currentSuggestion)}
                    loading={validateMutation.isPending}
                  >
                    Validar
                  </Button>
                </Group>
              </Box>

              {/* Diff View */}
              <ScrollArea flex={1}>
                <Box p="md">
                  {currentSuggestion.originalCode && currentSuggestion.fixedCode ? (
                    <DiffViewer
                      oldValue={currentSuggestion.originalCode}
                      newValue={currentSuggestion.fixedCode}
                      splitView={previewMode === 'split'}
                      useDarkTheme={false}
                    />
                  ) : (
                    <Box p="md" style={{ backgroundColor: 'var(--mantine-color-yellow-0)', borderRadius: '4px' }}>
                      <Text size="sm">Código não disponível para comparação</Text>
                    </Box>
                  )}

                  {/* Sandbox Validation Result */}
                  {sandboxResult && (
                    <Box
                      mt="md"
                      p="md"
                      style={{
                        backgroundColor: sandboxResult.success ? 'var(--mantine-color-green-0)' : 'var(--mantine-color-red-0)',
                        borderRadius: '4px',
                      }}
                    >
                      <Group gap="sm">
                        {sandboxResult.success ? <IconCheck size={16} /> : <IconX size={16} />}
                        <Text fw={600}>Resultado da Validação</Text>
                      </Group>
                      <Text size="sm" mt="xs">{sandboxResult.output}</Text>
                      {sandboxResult.error && (
                        <Text size="sm" c="red">{sandboxResult.error}</Text>
                      )}
                      <Text size="xs" c="dimmed" mt="sm">
                        Tempo: {sandboxResult.durationMs}ms | Exit Code: {sandboxResult.exitCode}
                      </Text>
                    </Box>
                  )}
                </Box>
              </ScrollArea>
            </Stack>
          )}
        </Card>
      </SimpleGrid>

      {/* Reject Modal */}
      <Modal
        opened={rejectModalOpen}
        onClose={() => setRejectModalOpen(false)}
        title={<Title order={4}>Rejeitar Sugestão</Title>}
        size="md"
        centered
      >
        <Stack gap="md">
          <Text size="sm">
            Por favor, forneça uma razão para rejeitar esta sugestão de correção.
          </Text>
          <Textarea
            placeholder="Descreva o motivo da rejeição..."
            value={rejectReason}
            onChange={(e) => setRejectReason(e.currentTarget.value)}
            minRows={3}
            required
          />
          <Group justify="flex-end">
            <Button variant="light" onClick={() => setRejectModalOpen(false)}>
              Cancelar
            </Button>
            <Button
              color="red"
              onClick={confirmReject}
              disabled={!rejectReason.trim()}
              loading={rejectMutation.isPending}
            >
              Rejeitar
            </Button>
          </Group>
        </Stack>
      </Modal>
    </Stack>
  )
}

// Sub-components
function SummaryCard({
  label,
  value,
  icon,
  color = 'blue',
}: {
  label: string
  value: number
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

function FixSuggestionCard({
  suggestion,
  isSelected,
  onClick,
  onApply,
  onApprove,
  onReject,
  onValidate,
  isValidating,
  isApplying,
}: {
  suggestion: FixSuggestion
  isSelected: boolean
  onClick: () => void
  onApply: () => void
  onApprove: () => void
  onReject: () => void
  onValidate: () => void
  isValidating: boolean
  isApplying: boolean
}) {
  const getStatusColor = (status: FixSuggestion['status']) => {
    switch (status) {
      case 'PENDING': return 'yellow'
      case 'APPROVED': return 'blue'
      case 'APPLIED': return 'green'
      case 'REJECTED': return 'red'
      case 'FAILED': return 'red'
      case 'EXPIRED': return 'gray'
      default: return 'gray'
    }
  }

  const getConfidenceColor = (confidence: FixSuggestion['confidence']) => {
    switch (confidence) {
      case 'HIGH': return 'green'
      case 'MEDIUM': return 'yellow'
      case 'LOW': return 'red'
      default: return 'gray'
    }
  }

  return (
    <Card
      padding="sm"
      withBorder
      style={{
        borderColor: isSelected ? 'var(--mantine-color-blue-5)' : undefined,
        cursor: 'pointer',
      }}
      onClick={onClick}
    >
      <Stack gap="xs">
        <Group justify="space-between">
          <Group gap={4}>
            <Badge size="xs" color={getStatusColor(suggestion.status)}>
              {suggestion.status}
            </Badge>
            <Badge size="xs" color={getConfidenceColor(suggestion.confidence)}>
              {suggestion.confidence}
            </Badge>
          </Group>
          <Text size="xs" c="dimmed">
            #{suggestion.id}
          </Text>
        </Group>

        {suggestion.filePath && (
          <Text size="xs" c="dimmed" lineClamp={1}>
            {suggestion.filePath.split('/').slice(-2).join('/')}
          </Text>
        )}

        {suggestion.explanation && (
          <Text size="xs" lineClamp={2}>
            {suggestion.explanation}
          </Text>
        )}

        <Group gap={4}>
          {suggestion.status === 'PENDING' && (
            <>
              <Button
                size="xs"
                variant="light"
                leftSection={<IconCheck size={14} />}
                onClick={(e) => { e.stopPropagation(); onApprove(); }}
              >
                Aprovar
              </Button>
              <Button
                size="xs"
                variant="light"
                color="red"
                leftSection={<IconX size={14} />}
                onClick={(e) => { e.stopPropagation(); onReject(); }}
              >
                Rejeitar
              </Button>
            </>
          )}
          {suggestion.status === 'APPROVED' && (
            <Button
              size="xs"
              variant="light"
              leftSection={<IconTool size={14} />}
              onClick={(e) => { e.stopPropagation(); onApply(); }}
              loading={isApplying}
            >
              Aplicar
            </Button>
          )}
          <Button
            size="xs"
            variant="subtle"
            leftSection={<IconEye size={14} />}
            onClick={(e) => { e.stopPropagation(); onValidate(); }}
            loading={isValidating}
          >
            Validar
          </Button>
        </Group>
      </Stack>
    </Card>
  )
}
