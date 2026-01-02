import {
  Title,
  Text,
  Stack,
  Card,
  Tabs,
  TextInput,
  Select,
  Button,
  Group,
  SimpleGrid,
  Badge,
  Code,
  Alert,
  PasswordInput,
  Anchor,
} from '@mantine/core'
import {
  IconUser,
  IconBuilding,
  IconKey,
  IconWebhook,
  IconCheck,
  IconExternalLink,
} from '@tabler/icons-react'
import { useQuery } from '@tanstack/react-query'
import { organizationsApi } from '@/lib/api'
import { useAuth } from '@/contexts/AuthContext'
import { notifications } from '@mantine/notifications'
import { useNavigate } from 'react-router-dom'
import { useState } from 'react'

export function SettingsPage() {
  const { user, refresh } = useAuth()
  const navigate = useNavigate()

  const { data: organizations } = useQuery({
    queryKey: ['organizations'],
    queryFn: organizationsApi.list,
  })

  // Profile form state
  const [profileName, setProfileName] = useState(user?.displayName || '')
  const [profileEmail, setProfileEmail] = useState(user?.email || '')
  const [profileSaving, setProfileSaving] = useState(false)

  // Integrations state
  const [sonarUrl, setSonarUrl] = useState('')
  const [sonarToken, setSonarToken] = useState('')
  const [openRouterKey, setOpenRouterKey] = useState('')
  const [savingIntegrations, setSavingIntegrations] = useState(false)

  const handleSaveProfile = async () => {
    setProfileSaving(true)
    try {
      // Simular chamada à API
      await new Promise((resolve) => setTimeout(resolve, 1000))
      notifications.show({
        title: 'Sucesso',
        message: 'Perfil atualizado com sucesso',
        color: 'green',
      })
      await refresh()
    } catch {
      notifications.show({
        title: 'Erro',
        message: 'Erro ao atualizar perfil',
        color: 'red',
      })
    } finally {
      setProfileSaving(false)
    }
  }

  const handleSaveIntegrations = async () => {
    setSavingIntegrations(true)
    try {
      // Simular chamada à API
      await new Promise((resolve) => setTimeout(resolve, 1000))
      notifications.show({
        title: 'Sucesso',
        message: 'Integrações salvas com sucesso',
        color: 'green',
      })
    } catch {
      notifications.show({
        title: 'Erro',
        message: 'Erro ao salvar integrações',
        color: 'red',
      })
    } finally {
      setSavingIntegrations(false)
    }
  }

  return (
    <Stack gap="xl">
      <Group justify="space-between">
        <div>
          <Title order={2}>Configurações</Title>
          <Text c="dimmed">Gerencie sua conta e configurações</Text>
        </div>
      </Group>

      <Tabs defaultValue="profile">
        <Tabs.List>
          <Tabs.Tab value="profile" leftSection={<IconUser size={16} />}>
            Perfil
          </Tabs.Tab>
          <Tabs.Tab value="organizations" leftSection={<IconBuilding size={16} />}>
            Organizações
          </Tabs.Tab>
          <Tabs.Tab value="integrations" leftSection={<IconKey size={16} />}>
            Integrações
          </Tabs.Tab>
          <Tabs.Tab value="webhooks" leftSection={<IconWebhook size={16} />}>
            Webhooks
          </Tabs.Tab>
        </Tabs.List>

        {/* Profile Tab */}
        <Tabs.Panel value="profile">
          <Card padding="lg" radius="md" withBorder mt="md">
            <Stack>
              <Group>
                <div>
                  <Title order={4}>Informações do Perfil</Title>
                  <Text c="dimmed" size="sm">
                    Atualize suas informações pessoais
                  </Text>
                </div>
              </Group>

              <SimpleGrid cols={{ base: 1, sm: 2 }}>
                <TextInput
                  label="Nome"
                  placeholder="Seu nome"
                  value={profileName}
                  onChange={(e) => setProfileName(e.currentTarget.value)}
                />

                <TextInput
                  label="Email"
                  placeholder="seu@email.com"
                  value={profileEmail}
                  onChange={(e) => setProfileEmail(e.currentTarget.value)}
                  disabled
                  description="O email não pode ser alterado"
                />

                <TextInput
                  label="Nome de usuário"
                  placeholder="@username"
                  defaultValue={user?.username}
                  disabled
                  description="O username é gerado automaticamente"
                />
              </SimpleGrid>

              <Group justify="flex-end">
                <Button onClick={handleSaveProfile} loading={profileSaving} leftSection={<IconCheck size={16} />}>
                  Salvar alterações
                </Button>
              </Group>
            </Stack>
          </Card>
        </Tabs.Panel>

        {/* Organizations Tab */}
        <Tabs.Panel value="organizations">
          <Card padding="lg" radius="md" withBorder mt="md">
            <Stack>
              <Group justify="space-between">
                <div>
                  <Title order={4}>Minhas Organizações</Title>
                  <Text c="dimmed" size="sm">
                    Gerencie suas organizações e planos
                  </Text>
                </div>
                <Button onClick={() => navigate('/organizations')} variant="light">
                  Ver todas
                </Button>
              </Group>

              {organizations && organizations.length > 0 ? (
                <Stack gap="sm">
                  {organizations.map((org) => (
                    <Card key={org.id} padding="md" radius="sm" withBorder>
                      <Group justify="space-between">
                        <div>
                          <Text fw={500}>{org.name}</Text>
                          <Text size="sm" c="dimmed">
                            @{org.slug}
                          </Text>
                        </div>
                        <Badge
                          color={org.planType === 'FREE' ? 'gray' : org.planType === 'PRO' ? 'blue' : 'purple'}
                        >
                          {org.planType}
                        </Badge>
                      </Group>
                      {org.repoCount !== undefined && org.reviewCount !== undefined && (
                        <Group mt="sm" gap="xl">
                          <Text size="sm">
                            <strong>{org.repoCount}</strong> repositórios
                          </Text>
                          <Text size="sm">
                            <strong>{org.reviewCount}</strong> reviews este mês
                          </Text>
                        </Group>
                      )}
                    </Card>
                  ))}
                </Stack>
              ) : (
                <Alert variant="light">
                  <Text size="sm">
                    Nenhuma organização encontrada. <Anchor onClick={() => navigate('/organizations')}>Criar organização</Anchor>
                  </Text>
                </Alert>
              )}
            </Stack>
          </Card>
        </Tabs.Panel>

        {/* Integrations Tab */}
        <Tabs.Panel value="integrations">
          <Stack gap="md" mt="md">
            {/* GitHub Integration */}
            <Card padding="lg" radius="md" withBorder>
              <Stack gap="sm">
                <Group justify="space-between">
                  <Group>
                    <Text fw={500} size="lg">GitHub</Text>
                    <Badge color="green">Conectado</Badge>
                  </Group>
                </Group>
                <Text size="sm" c="dimmed">
                  Sua conta do GitHub está conectada via OAuth. Você pode criar projetos GitHub
                  automaticamente.
                </Text>
                <Group>
                  <Button variant="light" size="sm" component="a" href="https://github.com/settings/apps" target="_blank" rightSection={<IconExternalLink size={14} />}>
                  Gerenciar no GitHub
                </Button>
                </Group>
              </Stack>
            </Card>

            {/* SonarQube Integration */}
            <Card padding="lg" radius="md" withBorder>
              <Stack gap="sm">
                <Group justify="space-between">
                  <Text fw={500} size="lg">SonarQube</Text>
                  <Badge color="gray">Não configurado</Badge>
                </Group>
                <Text size="sm" c="dimmed">
                  Configure a integração com SonarQube para análise SAST estática.
                </Text>

                <TextInput
                  label="URL do SonarQube"
                  placeholder="https://sonarqube.example.com"
                  value={sonarUrl}
                  onChange={(e) => setSonarUrl(e.currentTarget.value)}
                />

                <PasswordInput
                  label="Token de API"
                  placeholder="sqp_xxxxxxxxxxxx"
                  value={sonarToken}
                  onChange={(e) => setSonarToken(e.currentTarget.value)}
                />

                <Group justify="flex-end">
                  <Button onClick={handleSaveIntegrations} loading={savingIntegrations} size="sm">
                    Salvar configuração
                  </Button>
                </Group>
              </Stack>
            </Card>

            {/* OpenRouter Integration */}
            <Card padding="lg" radius="md" withBorder>
              <Stack gap="sm">
                <Group justify="space-between">
                  <Text fw={500} size="lg">OpenRouter</Text>
                  <Badge color="gray">Não configurado</Badge>
                </Group>
                <Text size="sm" c="dimmed">
                  Configure a chave da API OpenRouter para análise com LLM.
                </Text>

                <PasswordInput
                  label="Chave da API OpenRouter"
                  placeholder="sk-or-v1-..."
                  value={openRouterKey}
                  onChange={(e) => setOpenRouterKey(e.currentTarget.value)}
                />

                <Select
                  label="Modelo padrão"
                  data={[
                    { value: 'anthropic/claude-3.5-sonnet', label: 'Claude 3.5 Sonnet' },
                    { value: 'anthropic/claude-3-haiku', label: 'Claude 3 Haiku' },
                    { value: 'openai/gpt-4o', label: 'GPT-4o' },
                    { value: 'openai/gpt-4o-mini', label: 'GPT-4o Mini' },
                    { value: 'google/gemini-pro-1.5', label: 'Gemini Pro 1.5' },
                  ]}
                  defaultValue="anthropic/claude-3.5-sonnet"
                />

                <Group justify="flex-end">
                  <Button onClick={handleSaveIntegrations} loading={savingIntegrations} size="sm">
                    Salvar configuração
                  </Button>
                </Group>
              </Stack>
            </Card>

            {/* BitBucket Integration */}
            <Card padding="lg" radius="md" withBorder>
              <Stack gap="sm">
                <Group justify="space-between">
                  <Text fw={500} size="lg">BitBucket</Text>
                  <Badge color="gray">Não configurado</Badge>
                </Group>
                <Text size="sm" c="dimmed">
                  Configure as credenciais do BitBucket para análise de PRs.
                </Text>

                <TextInput
                  label="Workspace do BitBucket"
                  placeholder="meu-workspace"
                />

                <PasswordInput
                  label="App Password"
                  placeholder="App password do BitBucket"
                />

                <Group justify="flex-end">
                  <Button onClick={handleSaveIntegrations} loading={savingIntegrations} size="sm">
                    Salvar configuração
                  </Button>
                </Group>
              </Stack>
            </Card>
          </Stack>
        </Tabs.Panel>

        {/* Webhooks Tab */}
        <Tabs.Panel value="webhooks">
          <Card padding="lg" radius="md" withBorder mt="md">
            <Stack gap="md">
              <div>
                <Title order={4}>Webhooks</Title>
                <Text c="dimmed" size="sm">
                  Configure webhooks para análise automática de Pull Requests
                </Text>
              </div>

              <Stack gap="sm">
                <Card padding="md" radius="sm" withBorder bg="gray.0">
                  <Text fw={500} mb="xs">
                    GitHub Webhook URL
                  </Text>
                  <Code block>{window.location.origin}/webhooks/github</Code>
                  <Text size="xs" c="dimmed" mt="xs">
                    Eventos: pull_request, pull_request_review
                  </Text>
                </Card>

                <Card padding="md" radius="sm" withBorder bg="gray.0">
                  <Text fw={500} mb="xs">
                    BitBucket Webhook URL
                  </Text>
                  <Code block>{window.location.origin}/webhooks/bitbucket</Code>
                  <Text size="xs" c="dimmed" mt="xs">
                    Eventos: pullrequest:created, pullrequest:updated
                  </Text>
                </Card>

                <Alert>
                  <Text size="sm">
                    Configure estas URLs nas configurações de webhook do seu provedor Git para
                    habilitar a análise automática de Pull Requests. Os eventos serão processados
                    em tempo real.
                  </Text>
                </Alert>

                <Card padding="md" radius="sm" withBorder>
                  <Group justify="space-between">
                    <div>
                      <Text fw={500} mb="xs">Segurança</Text>
                      <Text size="xs" c="dimmed">
                        Configure um segredo para validar webhooks recebidos
                      </Text>
                    </div>
                    <Button variant="light" size="sm">
                      Configurar segredo
                    </Button>
                  </Group>
                </Card>
              </Stack>
            </Stack>
          </Card>
        </Tabs.Panel>
      </Tabs>
    </Stack>
  )
}
