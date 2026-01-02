import { useState, useCallback, useMemo } from 'react'
import {
  Title,
  Text,
  Stack,
  Group,
  Button,
  Card,
  Badge,
  Select,
  Loader,
  Alert,
  Tooltip,
  Modal,
  TextInput,
  Divider,
  ScrollArea,
  SimpleGrid,
} from '@mantine/core'
import {
  IconReload,
  IconZoomIn,
  IconZoomOut,
  IconSearch,
  IconAlertTriangle,
  IconGraph,
  IconGitBranch,
  IconHierarchy,
  IconSettings,
  IconMaximize,
} from '@tabler/icons-react'
import ReactFlow, {
  Node,
  Edge,
  Controls,
  Background,
  useNodesState,
  useEdgesState,
  addEdge,
  Connection,
  NodeTypes,
  Position,
  MarkerType,
} from 'reactflow'
import 'reactflow/dist/style.css'
import { useQuery } from '@tanstack/react-query'
import { codeGraphApi } from '@/lib/v2/api'
import { useV2Store } from '@/store/v2-store'
import type { CodeGraphData, CodeGraphNode } from '@/types/v2'

const COLORS = {
  class: '#3b82f6',
  interface: '#a855f7',
  enum: '#f97316',
  function: '#22c55e',
  variable: '#6b7280',
  critical: '#ef4444',
  high: '#f97316',
  medium: '#eab308',
  low: '#22c55e',
}

// Custom Node Component
interface CustomNodeData {
  label: string
  type: string
  filePath: string
  complexity?: number
  dependencies: string[]
  dependents: string[]
  metadata?: Record<string, unknown>
  isAffected?: boolean
  impactLevel?: 'critical' | 'high' | 'medium' | 'low'
}

function CustomNode({ data, selected }: { data: CustomNodeData; selected: boolean }) {
  const nodeColor = COLORS[data.type as keyof typeof COLORS] || COLORS.class
  const impactColor = data.impactLevel ? COLORS[data.impactLevel] : undefined

  return (
    <div
      className={`custom-node ${selected ? 'selected' : ''} ${data.isAffected ? 'affected' : ''}`}
      style={{
        padding: '12px 16px',
        borderRadius: '8px',
        border: `2px solid ${selected ? nodeColor : '#e5e7eb'}`,
        borderLeft: `4px solid ${data.isAffected ? (impactColor || COLORS.critical) : nodeColor}`,
        backgroundColor: 'white',
        minWidth: '180px',
        maxWidth: '240px',
        boxShadow: data.isAffected ? '0 4px 12px rgba(239, 68, 68, 0.3)' : '0 2px 4px rgba(0,0,0,0.1)',
      }}
    >
      <Group gap={8} mb={data.complexity ? 8 : 0}>
        <div
          style={{
            width: '12px',
            height: '12px',
            borderRadius: '50%',
            backgroundColor: nodeColor,
          }}
        />
        <Text fw={600} size="sm" lineClamp={1}>
          {data.label}
        </Text>
        {data.isAffected && (
          <Badge color={impactColor || 'red'} size="xs" variant="filled">
            Impact
          </Badge>
        )}
      </Group>

      <Group gap={4}>
        <Badge size="xs" variant="light" c={nodeColor}>
          {data.type}
        </Badge>
        {data.complexity !== undefined && (
          <Tooltip label="Cyclomatic Complexity">
            <Badge
              size="xs"
              color={data.complexity > 15 ? 'red' : data.complexity > 10 ? 'yellow' : 'green'}
            >
              C: {data.complexity}
            </Badge>
          </Tooltip>
        )}
      </Group>

      <Text size="xs" c="dimmed" mt={8} lineClamp={1}>
        {data.filePath.split('/').slice(-2).join('/')}
      </Text>

      {data.metadata?.linesOfCode != null && (
        <Text size="xs" c="dimmed">
          {Number(data.metadata.linesOfCode)} LOC
        </Text>
      )}
    </div>
  )
}

const nodeTypes: NodeTypes = {
  custom: CustomNode,
}

interface CodeGraphPageProps {
  projectId?: number
}

export function CodeGraphPage({ projectId }: CodeGraphPageProps) {
  const {
    codeGraphData,
    setCodeGraphData,
    isCodeGraphLoading,
    setIsCodeGraphLoading,
    selectedGraphNode,
    setSelectedGraphNode,
  } = useV2Store()

  const [nodes, setNodes, onNodesChange] = useNodesState([])
  const [edges, setEdges, onEdgesChange] = useEdgesState([])
  const [selectedNode, setSelectedNode] = useState<CodeGraphNode | null>(null)
  const [searchQuery, setSearchQuery] = useState('')
  const [layoutType, setLayoutType] = useState<'dagre' | 'force' | 'hierarchical'>('dagre')

  const { data, isLoading, refetch } = useQuery<CodeGraphData>({
    queryKey: ['codeGraph', projectId],
    queryFn: () => codeGraphApi.analyze(projectId || 1),
    enabled: !!projectId,
  })

  // Update store and transform when data changes
  useMemo(() => {
    if (data) {
      setCodeGraphData(data)
      transformToReactFlow(data)
    }
  }, [data])

  // Transform code graph data to React Flow format
  const transformToReactFlow = useCallback((graphData: CodeGraphData) => {
    const flowNodes: Node[] = graphData.nodes.map((node) => {
      const flowNode: Node = {
        id: node.id,
        type: 'custom',
        position: { x: 0, y: 0 },
        data: {
          label: node.name,
          type: node.type,
          filePath: node.filePath,
          complexity: node.complexity,
          dependencies: node.dependencies,
          dependents: node.dependents,
          metadata: node.metadata,
          isAffected: false,
          impactLevel: undefined,
        } as CustomNodeData,
        sourcePosition: Position.Right,
        targetPosition: Position.Left,
      }
      return flowNode
    })

    const flowEdges: Edge[] = graphData.edges.map((edge) => ({
      id: edge.id,
      source: edge.source,
      target: edge.target,
      type: 'smoothstep',
      animated: edge.type === 'implements',
      style: {
        stroke: getEdgeColor(edge.type),
        strokeWidth: edge.weight ? Math.min(4, 1 + edge.weight) : 2,
      },
      markerEnd: {
        type: MarkerType.ArrowClosed,
        color: getEdgeColor(edge.type),
      },
      label: edge.label,
      labelStyle: { fontSize: 10, fontWeight: 600 },
    }))

    const { nodes: layoutedNodes, edges: layoutedEdges } = applyLayout(
      flowNodes,
      flowEdges,
      layoutType
    )

    setNodes(layoutedNodes)
    setEdges(layoutedEdges)
  }, [layoutType, setNodes, setEdges])

  const applyLayout = useCallback((
    nodes: Node[],
    edges: Edge[],
    type: 'dagre' | 'force' | 'hierarchical'
  ): { nodes: Node[]; edges: Edge[] } => {
    if (type === 'force') {
      return applyForceLayout(nodes, edges)
    }
    return applyHierarchicalLayout(nodes, edges, type === 'hierarchical')
  }, [])

  const applyHierarchicalLayout = useCallback((
    nodes: Node[],
    edges: Edge[],
    _strict: boolean
  ): { nodes: Node[]; edges: Edge[] } => {
    const nodeMap = new Map(nodes.map(n => [n.id, n]))
    const inDegree = new Map<string, number>()
    const adjacency = new Map<string, string[]>()

    nodes.forEach(n => {
      inDegree.set(n.id, 0)
      adjacency.set(n.id, [])
    })

    edges.forEach(e => {
      adjacency.get(e.source)?.push(e.target)
      inDegree.set(e.target, (inDegree.get(e.target) || 0) + 1)
    })

    const layers: string[][] = []
    const visited = new Set<string>()
    const queue = nodes.filter(n => (inDegree.get(n.id) || 0) === 0).map(n => n.id)

    while (queue.length > 0) {
      const currentLayer = [...queue]
      layers.push(currentLayer)
      queue.length = 0

      currentLayer.forEach(nodeId => {
        visited.add(nodeId)
        adjacency.get(nodeId)?.forEach(targetId => {
          if (!visited.has(targetId)) {
            inDegree.set(targetId, (inDegree.get(targetId) || 0) - 1)
            if ((inDegree.get(targetId) || 0) === 0) {
              queue.push(targetId)
            }
          }
        })
      })
    }

    nodes.forEach(n => {
      if (!visited.has(n.id)) {
        layers.push([n.id])
      }
    })

    const NODE_WIDTH = 200
    const NODE_HEIGHT = 120
    const HORIZONTAL_SPACING = 80
    const VERTICAL_SPACING = 100

    const layoutedNodes = nodes.map(node => {
      const layerIndex = layers.findIndex(layer => layer.includes(node.id))
      const layer = layers[layerIndex] || []

      const layerNodes = layer.map(id => nodeMap.get(id)!).filter(Boolean)
      const sortedLayerNodes = layerNodes

      const positionInLayer = sortedLayerNodes.findIndex(n => n.id === node.id)

      return {
        ...node,
        position: {
          x: positionInLayer * (NODE_WIDTH + HORIZONTAL_SPACING) + 50,
          y: layerIndex * (NODE_HEIGHT + VERTICAL_SPACING) + 50,
        },
      }
    })

    return { nodes: layoutedNodes, edges }
  }, [])

  const applyForceLayout = useCallback((
    nodes: Node[],
    edges: Edge[]
  ): { nodes: Node[]; edges: Edge[] } => {
    const WIDTH = 2000
    const HEIGHT = 1500
    const iterations = 100

    const positions = new Map<string, { x: number; y: number }>()

    nodes.forEach(node => {
      positions.set(node.id, {
        x: Math.random() * WIDTH,
        y: Math.random() * HEIGHT,
      })
    })

    for (let i = 0; i < iterations; i++) {
      nodes.forEach(a => {
        const posA = positions.get(a.id)!
        nodes.forEach(b => {
          if (a.id === b.id) return
          const posB = positions.get(b.id)!
          const dx = posA.x - posB.x
          const dy = posA.y - posB.y
          const dist = Math.sqrt(dx * dx + dy * dy) || 1
          const force = 500 / (dist * dist)
          posA.x += (dx / dist) * force
          posA.y += (dy / dist) * force
        })
      })

      edges.forEach(edge => {
        const posA = positions.get(edge.source)!
        const posB = positions.get(edge.target)!
        const dx = posB.x - posA.x
        const dy = posB.y - posA.y
        const dist = Math.sqrt(dx * dx + dy * dy) || 1
        const force = (dist - 150) * 0.05

        posA.x += (dx / dist) * force
        posA.y += (dy / dist) * force
        posB.x -= (dx / dist) * force
        posB.y -= (dy / dist) * force
      })

      nodes.forEach(node => {
        const pos = positions.get(node.id)!
        pos.x += (WIDTH / 2 - pos.x) * 0.01
        pos.y += (HEIGHT / 2 - pos.y) * 0.01
      })
    }

    const layoutedNodes = nodes.map(node => ({
      ...node,
      position: positions.get(node.id) || { x: 0, y: 0 },
    }))

    return { nodes: layoutedNodes, edges }
  }, [])

  const onConnect = useCallback(
    (params: Connection) => setEdges((eds) => addEdge(params, eds)),
    [setEdges]
  )

  const onNodeClick = useCallback((_unknown: unknown, node: Node) => {
    const graphNode = codeGraphData?.nodes.find(n => n.id === node.id)
    if (graphNode) {
      setSelectedNode(graphNode)
      setSelectedGraphNode(node.id)
    }
  }, [codeGraphData, setSelectedGraphNode])

  const handleCalculateBlastRadius = useCallback(async () => {
    if (!selectedGraphNode || !projectId) return

    setIsCodeGraphLoading(true)

    try {
      const result = await codeGraphApi.getBlastRadius(projectId, selectedGraphNode)

      setNodes((prevNodes) =>
        prevNodes.map((node) => {
          const impact = result.affectedNodes.find((n: { nodeId: string }) => n.nodeId === node.id)
          if (impact) {
            return {
              ...node,
              data: {
                ...node.data,
                isAffected: true,
                impactLevel: getImpactLevel(impact.distance || 0),
              } as CustomNodeData,
            }
          }
          return node
        })
      )
    } catch {
      console.error('Failed to calculate blast radius')
    } finally {
      setIsCodeGraphLoading(false)
    }
  }, [selectedGraphNode, projectId, setIsCodeGraphLoading, setNodes])

  const getImpactLevel = (distance: number): 'critical' | 'high' | 'medium' | 'low' => {
    if (distance === 0) return 'critical'
    if (distance === 1) return 'high'
    if (distance === 2) return 'medium'
    return 'low'
  }

  const getEdgeColor = (type: string): string => {
    switch (type) {
      case 'depends_on': return '#94a3b8'
      case 'used_by': return '#22c55e'
      case 'implements': return '#a855f7'
      case 'extends': return '#3b82f6'
      default: return '#64748b'
    }
  }

  const filteredNodes = useMemo(() => {
    if (!searchQuery) return nodes
    const query = searchQuery.toLowerCase()
    return nodes.filter(n =>
      (n.data as CustomNodeData).label.toLowerCase().includes(query) ||
      (n.data as CustomNodeData).filePath.toLowerCase().includes(query)
    )
  }, [nodes, searchQuery])

  const filteredEdges = useMemo(() => {
    if (!searchQuery) return edges
    const nodeIds = new Set(filteredNodes.map(n => n.id))
    return edges.filter(e => nodeIds.has(e.source) && nodeIds.has(e.target))
  }, [edges, filteredNodes, searchQuery])

  const metrics = codeGraphData?.metrics
  const complexityScore = metrics ? Math.round(metrics.avgComplexity) : 0

  return (
    <Stack gap="md">
      {/* Header */}
      <Group justify="space-between">
        <div>
          <Title order={2}>Code Graph Visualizer</Title>
          <Text c="dimmed">Visualize e analise dependências do código</Text>
        </div>
        <Group>
          <Select
            value={layoutType}
            onChange={(value) => setLayoutType(value as any)}
            data={[
              { value: 'dagre', label: 'Hierárquico' },
              { value: 'force', label: 'Force-Directed' },
            ]}
            leftSection={<IconGraph size={16} />}
          />
          <Button
            leftSection={<IconReload size={16} />}
            onClick={() => refetch()}
            variant="light"
            loading={isLoading}
          >
            Recarregar
          </Button>
        </Group>
      </Group>

      {/* Metrics Cards */}
      <SimpleGrid cols={{ base: 2, md: 4 }}>
        <MetricCard
          label="Total Nodes"
          value={metrics?.totalNodes || nodes.length}
          icon={<IconHierarchy size={18} />}
        />
        <MetricCard
          label="Total Edges"
          value={metrics?.totalEdges || edges.length}
          icon={<IconGitBranch size={18} />}
        />
        <MetricCard
          label="Complexidade Média"
          value={complexityScore}
          icon={<IconSettings size={18} />}
          color={complexityScore > 15 ? 'red' : complexityScore > 10 ? 'yellow' : 'green'}
        />
        <MetricCard
          label="Ciclos Detectados"
          value={metrics?.circularDependencies?.length || 0}
          icon={<IconAlertTriangle size={18} />}
          color={(metrics?.circularDependencies?.length || 0) > 0 ? 'red' : 'green'}
        />
      </SimpleGrid>

      {/* Circular Dependencies Warning */}
      {metrics && metrics.circularDependencies.length > 0 && (
        <Alert color="red" icon={<IconAlertTriangle size={18} />}>
          <Text fw={600}>Dependências Circulares Detectadas</Text>
          <Text size="sm">{metrics.circularDependencies.length} ciclos encontrados</Text>
        </Alert>
      )}

      {/* Controls */}
      <Card padding="sm" withBorder>
        <Group justify="space-between">
          <Group>
            <TextInput
              placeholder="Buscar nó..."
              leftSection={<IconSearch size={16} />}
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.currentTarget.value)}
              w={250}
            />
            {selectedGraphNode && (
              <Button
                variant="light"
                size="sm"
                onClick={handleCalculateBlastRadius}
                leftSection={<IconGraph size={16} />}
              >
                Calcular Impacto
              </Button>
            )}
          </Group>
          <Group>
            <Tooltip label="Zoom In">
              <Button variant="subtle" size="sm">
                <IconZoomIn size={18} />
              </Button>
            </Tooltip>
            <Tooltip label="Zoom Out">
              <Button variant="subtle" size="sm">
                <IconZoomOut size={18} />
              </Button>
            </Tooltip>
            <Tooltip label="Fit View">
              <Button variant="subtle" size="sm">
                <IconMaximize size={18} />
              </Button>
            </Tooltip>
          </Group>
        </Group>
      </Card>

      {/* Graph Visualization */}
      <Card padding={0} withBorder style={{ height: '600px' }}>
        {isLoading || isCodeGraphLoading ? (
          <Stack align="center" justify="center" h="100%">
            <Loader size="lg" />
            <Text c="dimmed">Analisando código...</Text>
          </Stack>
        ) : nodes.length === 0 ? (
          <Stack align="center" justify="center" h="100%">
            <IconGraph size={48} style={{ color: 'var(--mantine-color-dimmed)' }} />
            <Text c="dimmed">Nenhum dado de grafo disponível</Text>
            <Button onClick={() => refetch()}>Analisar Código</Button>
          </Stack>
        ) : (
          <ReactFlow
            nodes={filteredNodes}
            edges={filteredEdges}
            onNodesChange={onNodesChange}
            onEdgesChange={onEdgesChange}
            onConnect={onConnect}
            onNodeClick={onNodeClick}
            nodeTypes={nodeTypes}
            fitView
            minZoom={0.2}
            maxZoom={2}
            defaultEdgeOptions={{
              animated: false,
              style: { stroke: '#94a3b8', strokeWidth: 2 },
            }}
          >
            <Background />
            <Controls />
          </ReactFlow>
        )}
      </Card>

      {/* Node Detail Modal */}
      <Modal
        opened={!!selectedNode}
        onClose={() => setSelectedNode(null)}
        title={<Title order={4}>{selectedNode?.name}</Title>}
        size="lg"
      >
        {selectedNode && (
          <Stack gap="md">
            <Group>
              <Badge color={COLORS[selectedNode.type as keyof typeof COLORS]}>
                {selectedNode.type}
              </Badge>
              {selectedNode.complexity !== undefined && (
                <Badge
                  color={selectedNode.complexity > 15 ? 'red' : selectedNode.complexity > 10 ? 'yellow' : 'green'}
                >
                  Complexidade: {selectedNode.complexity}
                </Badge>
              )}
            </Group>

            <Divider label="Localização" labelPosition="left" />
            <Text size="sm" c="dimmed">
              {selectedNode.filePath}
            </Text>
            {selectedNode.startLine && selectedNode.endLine && (
              <Text size="sm">
                Linhas: {selectedNode.startLine} - {selectedNode.endLine}
              </Text>
            )}

            <Divider label="Dependências" labelPosition="left" />
            <ScrollArea h={100}>
              <Stack gap={4}>
                {selectedNode.dependencies.length === 0 ? (
                  <Text c="dimmed">Nenhuma dependência</Text>
                ) : (
                  selectedNode.dependencies.map(dep => (
                    <Text key={dep} size="sm">{dep}</Text>
                  ))
                )}
              </Stack>
            </ScrollArea>

            <Divider label="Dependentes" labelPosition="left" />
            <ScrollArea h={100}>
              <Stack gap={4}>
                {selectedNode.dependents.length === 0 ? (
                  <Text c="dimmed">Nenhum dependente</Text>
                ) : (
                  selectedNode.dependents.map(dep => (
                    <Text key={dep} size="sm">{dep}</Text>
                  ))
                )}
              </Stack>
            </ScrollArea>

            {selectedNode.metadata && Object.keys(selectedNode.metadata).length > 0 && (
              <>
                <Divider label="Metadata" labelPosition="left" />
                <Stack gap={4}>
                  {Object.entries(selectedNode.metadata).map(([key, value]) => (
                    <Group key={key} justify="space-between">
                      <Text size="sm" fw={600}>{key}</Text>
                      <Text size="sm">{String(value)}</Text>
                    </Group>
                  ))}
                </Stack>
              </>
            )}

            <Group>
              <Button
                fullWidth
                onClick={() => {
                  setSelectedGraphNode(selectedNode.id)
                  handleCalculateBlastRadius()
                  setSelectedNode(null)
                }}
              >
                Ver Impacto
              </Button>
            </Group>
          </Stack>
        )}
      </Modal>
    </Stack>
  )
}

// Helper Components
function MetricCard({
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
  return (
    <Card padding="md" withBorder>
      <Group gap="sm">
        <div style={{ color }} className="metric-icon">
          {icon}
        </div>
        <div>
          <Text size="xs" c="dimmed">{label}</Text>
          <Text fz="lg" fw={700}>{value}</Text>
        </div>
      </Group>
    </Card>
  )
}
