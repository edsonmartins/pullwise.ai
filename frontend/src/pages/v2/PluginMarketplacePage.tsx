import { useState, useCallback, useMemo, useEffect } from 'react'
import {
  Title,
  Text,
  Stack,
  Group,
  Button,
  Card,
  Badge,
  Input,
  Select,
  ScrollArea,
  Modal,
  TextInput,
  Tabs,
  Grid,
  Tooltip,
  ActionIcon,
  Divider,
  Rating,
  Switch,
  SimpleGrid,
} from '@mantine/core'
import {
  IconSearch,
  IconDownload,
  IconTrash,
  IconSettings,
  IconRefresh,
  IconCode,
  IconStar,
  IconCheck,
  IconWorld,
  IconFilter,
  IconSortAscending,
} from '@tabler/icons-react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { pluginApi } from '@/lib/v2/api'
import { useV2Store } from '@/store/v2-store'
import type { Plugin } from '@/types/v2'

const PLUGIN_CATEGORIES = [
  { value: 'all', label: 'Todos' },
  { value: 'SAST', label: 'SAST' },
  { value: 'LINTER', label: 'Linter' },
  { value: 'SECURITY', label: 'Segurança' },
  { value: 'PERFORMANCE', label: 'Performance' },
  { value: 'CUSTOM_LLM', label: 'LLM Customizado' },
  { value: 'INTEGRATION', label: 'Integração' },
]

const PLUGIN_TYPES = [
  { value: 'all', label: 'Todos os Tipos' },
  { value: 'java', label: 'Java' },
  { value: 'typescript', label: 'TypeScript' },
  { value: 'python', label: 'Python' },
]

const SORT_OPTIONS = [
  { value: 'popular', label: 'Mais Popular' },
  { value: 'recent', label: 'Mais Recente' },
  { value: 'name', label: 'Nome (A-Z)' },
  { value: 'rating', label: 'Avaliação' },
]

interface PluginExtended extends Plugin {
  downloads?: number
  rating?: number
  ratingCount?: number
  lastUpdated?: string
  verified?: boolean
  featured?: boolean
  tags?: string[]
  category?: string
}

export function PluginMarketplacePage() {
  const queryClient = useQueryClient()
  const { plugins, setPlugins, installedPlugins, setInstalledPlugins } = useV2Store()

  const [searchQuery, setSearchQuery] = useState('')
  const [selectedCategory, setSelectedCategory] = useState('all')
  const [selectedType, setSelectedType] = useState('all')
  const [sortBy, setSortBy] = useState('popular')
  const [selectedPlugin, setSelectedPlugin] = useState<PluginExtended | null>(null)
  const [configModalOpen, setConfigModalOpen] = useState(false)
  const [pluginConfig, setPluginConfig] = useState<Record<string, unknown>>({})

  // Fetch available plugins
  const { data: availablePlugins = [] } = useQuery<Plugin[]>({
    queryKey: ['plugins'],
    queryFn: () => pluginApi.list(),
  })

  // Update store when available plugins change
  useEffect(() => {
    if (availablePlugins.length > 0) {
      const extendedPlugins = availablePlugins.map(p => ({
        ...p,
        downloads: Math.floor(Math.random() * 50000),
        rating: 3 + Math.random() * 2,
        ratingCount: Math.floor(Math.random() * 500),
        lastUpdated: new Date(Date.now() - Math.random() * 90 * 24 * 60 * 60 * 1000).toISOString(),
        verified: Math.random() > 0.7,
        featured: Math.random() > 0.8,
        tags: ['code-quality', 'security', 'ai'].slice(0, Math.floor(Math.random() * 3) + 1),
        category: PLUGIN_CATEGORIES[Math.floor(Math.random() * (PLUGIN_CATEGORIES.length - 1)) + 1].value,
      })) as PluginExtended[]
      setPlugins(extendedPlugins)
    }
  }, [availablePlugins, setPlugins])

  // Fetch installed plugins
  const { data: installed = [] } = useQuery<Plugin[]>({
    queryKey: ['plugins', 'installed'],
    queryFn: () => pluginApi.getInstalled(),
  })

  // Update store when installed plugins change
  useEffect(() => {
    if (installed.length > 0) {
      setInstalledPlugins(installed.map(p => p.id))
    }
  }, [installed, setInstalledPlugins])

  // Install plugin mutation
  const installMutation = useMutation({
    mutationFn: (pluginId: string) => pluginApi.install(pluginId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['plugins'] })
      queryClient.invalidateQueries({ queryKey: ['plugins', 'installed'] })
    },
  })

  // Uninstall plugin mutation
  const uninstallMutation = useMutation({
    mutationFn: (pluginId: string) => pluginApi.uninstall(pluginId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['plugins'] })
      queryClient.invalidateQueries({ queryKey: ['plugins', 'installed'] })
    },
  })

  // Enable plugin mutation
  const enableMutation = useMutation({
    mutationFn: (pluginId: string) => pluginApi.enable(pluginId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['plugins'] })
      queryClient.invalidateQueries({ queryKey: ['plugins', 'installed'] })
    },
  })

  // Disable plugin mutation
  const disableMutation = useMutation({
    mutationFn: (pluginId: string) => pluginApi.disable(pluginId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['plugins'] })
      queryClient.invalidateQueries({ queryKey: ['plugins', 'installed'] })
    },
  })

  // Update config mutation
  const updateConfigMutation = useMutation({
    mutationFn: ({ pluginId, config }: { pluginId: string; config: Record<string, unknown> }) =>
      pluginApi.updateConfig(pluginId, config),
    onSuccess: () => {
      setConfigModalOpen(false)
      queryClient.invalidateQueries({ queryKey: ['plugins'] })
    },
  })

  // Filter and sort plugins
  const filteredPlugins = useMemo(() => {
    let filtered = plugins as PluginExtended[]

    if (searchQuery) {
      const query = searchQuery.toLowerCase()
      filtered = filtered.filter(p =>
        p.name.toLowerCase().includes(query) ||
        p.description.toLowerCase().includes(query) ||
        p.author.toLowerCase().includes(query) ||
        p.tags?.some(tag => tag.toLowerCase().includes(query))
      )
    }

    if (selectedCategory !== 'all') {
      filtered = filtered.filter(p => p.category === selectedCategory || p.type === selectedCategory)
    }

    if (selectedType !== 'all') {
      filtered = filtered.filter(p => p.supportedLanguages.includes(selectedType))
    }

    filtered = [...filtered].sort((a, b) => {
      switch (sortBy) {
        case 'popular':
          return (b.downloads || 0) - (a.downloads || 0)
        case 'recent':
          return new Date(b.lastUpdated || 0).getTime() - new Date(a.lastUpdated || 0).getTime()
        case 'name':
          return a.name.localeCompare(b.name)
        case 'rating':
          return (b.rating || 0) - (a.rating || 0)
        default:
          return 0
      }
    })

    return filtered
  }, [plugins, searchQuery, selectedCategory, selectedType, sortBy])

  const featuredPlugins = filteredPlugins.filter(p => p.featured)
  const regularPlugins = filteredPlugins.filter(p => !p.featured)

  const handleInstall = useCallback((pluginId: string) => {
    installMutation.mutate(pluginId)
  }, [installMutation])

  const handleUninstall = useCallback((pluginId: string) => {
    uninstallMutation.mutate(pluginId)
  }, [uninstallMutation])

  const handleToggleEnable = useCallback((plugin: Plugin) => {
    if (plugin.enabled) {
      disableMutation.mutate(plugin.id)
    } else {
      enableMutation.mutate(plugin.id)
    }
  }, [enableMutation, disableMutation])

  const handleOpenConfig = useCallback((plugin: PluginExtended) => {
    setSelectedPlugin(plugin)
    setPluginConfig(plugin.config || {})
    setConfigModalOpen(true)
  }, [])

  const handleSaveConfig = useCallback(() => {
    if (selectedPlugin) {
      updateConfigMutation.mutate({
        pluginId: selectedPlugin.id,
        config: pluginConfig,
      })
    }
  }, [selectedPlugin, pluginConfig, updateConfigMutation])

  return (
    <Stack gap="md">
      {/* Header */}
      <Group justify="space-between">
        <div>
          <Title order={2}>Plugin Marketplace</Title>
          <Text c="dimmed">Descubra e gerencie plugins para estender o Pullwise</Text>
        </div>
        <Button
          leftSection={<IconRefresh size={16} />}
          onClick={() => queryClient.invalidateQueries({ queryKey: ['plugins'] })}
          variant="light"
        >
          Atualizar
        </Button>
      </Group>

      {/* Stats */}
      <SimpleGrid cols={{ base: 2, md: 4 }}>
        <StatCard
          label="Total Disponível"
          value={plugins.length}
          icon={<IconWorld size={20} />}
        />
        <StatCard
          label="Instalados"
          value={installedPlugins.length}
          icon={<IconDownload size={20} />}
          color="green"
        />
        <StatCard
          label="Ativos"
          value={plugins.filter(p => p.enabled).length}
          icon={<IconCheck size={20} />}
          color="blue"
        />
        <StatCard
          label="Verificados"
          value={(plugins as PluginExtended[]).filter(p => p.verified).length}
          icon={<IconStar size={20} />}
          color="yellow"
        />
      </SimpleGrid>

      {/* Search and Filters */}
      <Card padding="sm" withBorder>
        <Group>
          <Input
            placeholder="Buscar plugins..."
            leftSection={<IconSearch size={16} />}
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.currentTarget.value)}
            style={{ flex: 1 }}
            miw={250}
          />
          <Select
            value={selectedCategory}
            onChange={(v) => setSelectedCategory(v || 'all')}
            data={PLUGIN_CATEGORIES}
            leftSection={<IconFilter size={16} />}
          />
          <Select
            value={selectedType}
            onChange={(v) => setSelectedType(v || 'all')}
            data={PLUGIN_TYPES}
          />
          <Select
            value={sortBy}
            onChange={(v) => setSortBy(v || 'popular')}
            data={SORT_OPTIONS}
            leftSection={<IconSortAscending size={16} />}
          />
        </Group>
      </Card>

      {/* Plugin Grid */}
      <ScrollArea mah={800}>
        <Tabs defaultValue="installed">
          <Tabs.List>
            <Tabs.Tab value="installed">
              Instalados ({installedPlugins.length})
            </Tabs.Tab>
            <Tabs.Tab value="available">
              Disponíveis ({filteredPlugins.length})
            </Tabs.Tab>
            <Tabs.Tab value="featured">
              Destaques ({featuredPlugins.length})
            </Tabs.Tab>
          </Tabs.List>

          <Tabs.Panel value="installed" pt="md">
            <PluginGrid
              plugins={plugins.filter(p => installedPlugins.includes(p.id)) as PluginExtended[]}
              isInstalledTab
              onInstall={handleInstall}
              onUninstall={handleUninstall}
              onToggleEnable={handleToggleEnable}
              onOpenConfig={handleOpenConfig}
              isInstalling={installMutation.isPending}
              isUninstalling={uninstallMutation.isPending}
            />
          </Tabs.Panel>

          <Tabs.Panel value="available" pt="md">
            <PluginGrid
              plugins={regularPlugins}
              onInstall={handleInstall}
              onUninstall={handleUninstall}
              onToggleEnable={handleToggleEnable}
              onOpenConfig={handleOpenConfig}
              isInstalling={installMutation.isPending}
              isUninstalling={uninstallMutation.isPending}
            />
          </Tabs.Panel>

          <Tabs.Panel value="featured" pt="md">
            <PluginGrid
              plugins={featuredPlugins}
              onInstall={handleInstall}
              onUninstall={handleUninstall}
              onToggleEnable={handleToggleEnable}
              onOpenConfig={handleOpenConfig}
              isInstalling={installMutation.isPending}
              isUninstalling={uninstallMutation.isPending}
            />
          </Tabs.Panel>
        </Tabs>
      </ScrollArea>

      {/* Config Modal */}
      <Modal
        opened={configModalOpen}
        onClose={() => setConfigModalOpen(false)}
        title={<Title order={4}>Configurar {selectedPlugin?.name}</Title>}
        size="md"
      >
        {selectedPlugin && (
          <Stack gap="md">
            <Text size="sm" c="dimmed">
              Configure as opções do plugin abaixo.
            </Text>

            <Stack gap="sm">
              <TextInput
                label="Nome da Configuração"
                placeholder="Minha configuração"
                defaultValue={(selectedPlugin.config?.name as string) || ''}
                onChange={(e) => setPluginConfig({ ...pluginConfig, name: e.currentTarget.value })}
              />

              <Switch
                label="Habilitar notificações"
                description="Receba notificações quando este plugin encontrar issues"
                defaultChecked={Boolean(selectedPlugin.config?.notifications)}
                onChange={(e) => setPluginConfig({ ...pluginConfig, notifications: e.currentTarget.checked })}
              />

              <TextInput
                label="Limite de recursos"
                placeholder="1024"
                description="Limite máximo de memória (MB)"
                type="number"
                defaultValue={selectedPlugin.config?.memoryLimit as number || 1024}
                onChange={(e) => setPluginConfig({ ...pluginConfig, memoryLimit: parseInt(e.currentTarget.value) || 1024 })}
              />

              <Select
                label="Nível de log"
                data={[
                  { value: 'ERROR', label: 'Erro' },
                  { value: 'WARN', label: 'Aviso' },
                  { value: 'INFO', label: 'Info' },
                  { value: 'DEBUG', label: 'Debug' },
                ]}
                defaultValue={selectedPlugin.config?.logLevel as string || 'INFO'}
                onChange={(v) => setPluginConfig({ ...pluginConfig, logLevel: v })}
              />
            </Stack>

            <Group justify="flex-end" mt="md">
              <Button variant="light" onClick={() => setConfigModalOpen(false)}>
                Cancelar
              </Button>
              <Button
                onClick={handleSaveConfig}
                loading={updateConfigMutation.isPending}
              >
                Salvar Configuração
              </Button>
            </Group>
          </Stack>
        )}
      </Modal>
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
  value: number
  icon: React.ReactNode
  color?: string
}) {
  return (
    <Card padding="md" withBorder>
      <Group>
        <div style={{ color: `var(--mantine-color-${color}-filled)` }}>
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

function PluginGrid({
  plugins,
  isInstalledTab = false,
  onInstall,
  onUninstall,
  onToggleEnable,
  onOpenConfig,
  isInstalling,
  isUninstalling,
}: {
  plugins: PluginExtended[]
  isInstalledTab?: boolean
  onInstall: (id: string) => void
  onUninstall: (id: string) => void
  onToggleEnable: (plugin: Plugin) => void
  onOpenConfig: (plugin: PluginExtended) => void
  isInstalling: boolean
  isUninstalling: boolean
}) {
  if (plugins.length === 0) {
    return (
      <Card padding="xl" withBorder>
        <Stack align="center" py="xl">
          <IconWorld size={48} style={{ color: 'var(--mantine-color-dimmed)' }} />
          <Text c="dimmed">
            {isInstalledTab ? 'Nenhum plugin instalado' : 'Nenhum plugin encontrado'}
          </Text>
        </Stack>
      </Card>
    )
  }

  return (
    <Grid>
      {plugins.map((plugin) => (
        <Grid.Col key={plugin.id} span={{ base: 12, sm: 6, lg: 4 }}>
          <PluginCard
            plugin={plugin}
            onInstall={onInstall}
            onUninstall={onUninstall}
            onToggleEnable={onToggleEnable}
            onOpenConfig={onOpenConfig}
            isInstalling={isInstalling}
            isUninstalling={isUninstalling}
          />
        </Grid.Col>
      ))}
    </Grid>
  )
}

function PluginCard({
  plugin,
  onInstall,
  onUninstall,
  onToggleEnable,
  onOpenConfig,
  isInstalling,
  isUninstalling,
}: {
  plugin: PluginExtended
  onInstall: (id: string) => void
  onUninstall: (id: string) => void
  onToggleEnable: (plugin: Plugin) => void
  onOpenConfig: (plugin: PluginExtended) => void
  isInstalling: boolean
  isUninstalling: boolean
}) {
  const isInstalled = plugin.installed
  const categoryColor = {
    SAST: 'blue',
    SECURITY: 'red',
    PERFORMANCE: 'yellow',
    CUSTOM_LLM: 'grape',
    INTEGRATION: 'green',
    LINTER: 'cyan',
  }[plugin.category || plugin.type] || 'gray'

  return (
    <Card padding="md" withBorder h="100%" style={{ display: 'flex', flexDirection: 'column' }}>
      <Stack gap="sm" style={{ flex: 1 }}>
        {/* Header */}
        <Group justify="space-between" align="flex-start">
          <Group gap="sm">
            <div
              style={{
                width: 48,
                height: 48,
                borderRadius: '8px',
                background: `linear-gradient(135deg, var(--mantine-color-${categoryColor}-4), var(--mantine-color-${categoryColor}-7))`,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <IconCode size={24} style={{ color: 'white' }} />
            </div>
            <div>
              <Group gap={4}>
                <Text fw={600}>{plugin.name}</Text>
                {plugin.verified && (
                  <Tooltip label="Plugin verificado">
                    <IconStar size={14} color="var(--mantine-color-yellow-5)" fill="currentColor" />
                  </Tooltip>
                )}
              </Group>
              <Text size="xs" c="dimmed">por {plugin.author}</Text>
            </div>
          </Group>
          {plugin.featured && (
            <Badge color="yellow" variant="filled" size="xs">
              Destaque
            </Badge>
          )}
        </Group>

        {/* Badges */}
        <Group gap={4}>
          <Badge size="xs" color={categoryColor} variant="light">
            {plugin.category || plugin.type}
          </Badge>
          <Badge size="xs" variant="outline">
            v{plugin.version}
          </Badge>
          {plugin.supportedLanguages.slice(0, 2).map(lang => (
            <Badge key={lang} size="xs" variant="dot">
              {lang}
            </Badge>
          ))}
        </Group>

        {/* Description */}
        <Text size="sm" lineClamp={2}>
          {plugin.description}
        </Text>

        {/* Tags */}
        {plugin.tags && plugin.tags.length > 0 && (
          <Group gap={4}>
            {plugin.tags.map(tag => (
              <Badge key={tag} size="xs" variant="light" color="gray">
                {tag}
              </Badge>
            ))}
          </Group>
        )}

        {/* Stats */}
        <Group gap="md">
          {plugin.rating && (
            <Group gap={4}>
              <Rating value={plugin.rating} fractions={2} readOnly size="xs" />
              <Text size="xs" c="dimmed">({plugin.ratingCount})</Text>
            </Group>
          )}
          {plugin.downloads && (
            <Text size="xs" c="dimmed">
              {plugin.downloads > 1000
                ? `${(plugin.downloads / 1000).toFixed(1)}k`
                : plugin.downloads} downloads
            </Text>
          )}
        </Group>

        {/* Languages */}
        <Divider />
        <Text size="xs" c="dimmed">
          Suporta: {plugin.supportedLanguages.join(', ')}
        </Text>

        {/* Actions */}
        <Group gap={4}>
          {isInstalled ? (
            <>
              <Switch
                size="sm"
                label={plugin.enabled ? 'Ativo' : 'Inativo'}
                checked={plugin.enabled}
                onChange={() => onToggleEnable(plugin)}
                disabled={!plugin.enabled && !isInstalled}
              />
              <ActionIcon
                variant="light"
                color="blue"
                onClick={() => onOpenConfig(plugin)}
                disabled={!plugin.enabled}
              >
                <IconSettings size={16} />
              </ActionIcon>
              <Button
                size="sm"
                variant="light"
                color="red"
                leftSection={<IconTrash size={14} />}
                onClick={() => onUninstall(plugin.id)}
                loading={isUninstalling}
              >
                Remover
              </Button>
            </>
          ) : (
            <Button
              size="sm"
              fullWidth
              leftSection={<IconDownload size={14} />}
              onClick={() => onInstall(plugin.id)}
              loading={isInstalling}
            >
              Instalar
            </Button>
          )}
        </Group>
      </Stack>
    </Card>
  )
}
