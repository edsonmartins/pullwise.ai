import {
  Title,
  Text,
  Button,
  Stack,
  Card,
  Group,
  Badge,
  ActionIcon,
  Modal,
  TextInput,
  Select,
  Switch,
  Code,
  SimpleGrid,
  Box,
} from '@mantine/core'
import { IconPlus, IconTrash, IconEdit, IconBrandGithub, IconBrandBitbucket } from '@tabler/icons-react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { notifications } from '@mantine/notifications'
import { projectsApi } from '@/lib/api'
import { useNavigate } from 'react-router-dom'
import { modals } from '@mantine/modals'

export function ProjectsPage() {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const [opened, setOpened] = useState(false)

  const { data: projects, isLoading } = useQuery({
    queryKey: ['projects'],
    queryFn: projectsApi.list,
  })

  const createMutation = useMutation({
    mutationFn: projectsApi.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['projects'] })
      close()
      notifications.show({
        title: 'Sucesso',
        message: 'Projeto criado com sucesso',
        color: 'green',
      })
    },
    onError: () => {
      notifications.show({
        title: 'Erro',
        message: 'Erro ao criar projeto',
        color: 'red',
      })
    },
  })

  const deleteMutation = useMutation({
    mutationFn: projectsApi.delete,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['projects'] })
      notifications.show({
        title: 'Sucesso',
        message: 'Projeto removido',
        color: 'green',
      })
    },
  })

  const handleDelete = (id: number, name: string) => {
    modals.openConfirmModal({
      title: 'Excluir projeto',
      children: (
        <Text size="sm">
          Tem certeza que deseja excluir o projeto <b>{name}</b>? Esta ação não pode ser desfeita.
        </Text>
      ),
      labels: { confirm: 'Excluir', cancel: 'Cancelar' },
      confirmProps: { color: 'red' },
      onConfirm: () => deleteMutation.mutate(id),
    })
  }

  return (
    <Stack gap="xl">
      <Group justify="space-between">
        <div>
          <Title order={2}>Projetos</Title>
          <Text c="dimmed">Gerencie seus repositórios conectados</Text>
        </div>
        <Button leftSection={<IconPlus size={16} />} onClick={() => setOpened(true)}>
          Novo Projeto
        </Button>
      </Group>

      {isLoading ? (
        <Text>Carregando...</Text>
      ) : projects && projects.length > 0 ? (
        <SimpleGrid cols={{ base: 1, sm: 2, lg: 3 }}>
          {projects.map((project) => (
            <Card key={project.id} padding="lg" radius="md" withBorder>
              <Group justify="space-between" mb="sm">
                <Group>
                  {project.platform === 'GITHUB' ? (
                    <IconBrandGithub size={24} />
                  ) : (
                    <IconBrandBitbucket size={24} />
                  )}
                  <div>
                    <Text fw={500}>{project.name}</Text>
                    {project.repositoryUrl && (
                      <Text size="xs" c="dimmed">
                        {project.repositoryUrl.replace('https://github.com/', '')}
                      </Text>
                    )}
                  </div>
                </Group>
                <Badge color={project.isActive ? 'green' : 'gray'}>
                  {project.isActive ? 'Ativo' : 'Inativo'}
                </Badge>
              </Group>

              {project.description && (
                <Text size="sm" c="dimmed" mb="sm" lineClamp={2}>
                  {project.description}
                </Text>
              )}

              <Group mt="sm">
                <Badge variant="light" size="sm">
                  Auto-review: {project.autoReviewEnabled ? 'Ligado' : 'Desligado'}
                </Badge>
              </Group>

              <Group mt="md" justify="flex-end">
                <ActionIcon
                  variant="light"
                  color="blue"
                  onClick={() => navigate(`/projects/${project.id}/pull-requests`)}
                >
                  <IconEdit size={16} />
                </ActionIcon>
                <ActionIcon
                  variant="light"
                  color="red"
                  onClick={() => handleDelete(project.id, project.name)}
                >
                  <IconTrash size={16} />
                </ActionIcon>
              </Group>
            </Card>
          ))}
        </SimpleGrid>
      ) : (
        <Card padding="xl" radius="md" withBorder>
          <Stack align="center" gap="sm">
            <Box c="dimmed">
              <IconBrandGithub size={48} opacity={0.5} />
            </Box>
            <Text size="lg" fw={500}>
              Nenhum projeto ainda
            </Text>
            <Text c="dimmed" size="sm">
              Conecte seu primeiro repositório para começar
            </Text>
            <Button leftSection={<IconPlus size={16} />} onClick={() => setOpened(true)}>
              Criar Projeto
            </Button>
          </Stack>
        </Card>
      )}

      <ProjectFormModal
        opened={opened}
        onClose={() => setOpened(false)}
        onSubmit={(data) => createMutation.mutate(data)}
        loading={createMutation.isPending}
      />
    </Stack>
  )
}

interface ProjectFormModalProps {
  opened: boolean
  onClose: () => void
  onSubmit: (data: {
    name: string
    description?: string
    platform: string
    repositoryUrl?: string
    autoReviewEnabled?: boolean
  }) => void
  loading?: boolean
}

function ProjectFormModal({ opened, onClose, onSubmit, loading }: ProjectFormModalProps) {
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [platform, setPlatform] = useState('GITHUB')
  const [repositoryUrl, setRepositoryUrl] = useState('')
  const [autoReviewEnabled, setAutoReviewEnabled] = useState(true)

  const handleSubmit = () => {
    onSubmit({
      name,
      description: description || undefined,
      platform,
      repositoryUrl: repositoryUrl || undefined,
      autoReviewEnabled,
    })
    setName('')
    setDescription('')
    setRepositoryUrl('')
  }

  return (
    <Modal opened={opened} onClose={onClose} title="Novo Projeto" size="md">
      <Stack>
        <TextInput
          label="Nome"
          placeholder="meu-projeto"
          value={name}
          onChange={(e) => setName(e.currentTarget.value)}
          required
        />

        <TextInput
          label="Descrição"
          placeholder="Descrição do projeto"
          value={description}
          onChange={(e) => setDescription(e.currentTarget.value)}
        />

        <Select
          label="Plataforma"
          data={[
            { value: 'GITHUB', label: 'GitHub' },
            { value: 'BITBUCKET', label: 'BitBucket' },
          ]}
          value={platform}
          onChange={(value) => setPlatform(value || 'GITHUB')}
        />

        <TextInput
          label="URL do Repositório"
          placeholder="https://github.com/usuario/repo"
          value={repositoryUrl}
          onChange={(e) => setRepositoryUrl(e.currentTarget.value)}
          leftSection={<Code fz="xs" />}
        />

        <Switch
          label="Auto-review habilitado"
          description="Análise automática de PRs"
          checked={autoReviewEnabled}
          onChange={(e) => setAutoReviewEnabled(e.currentTarget.checked)}
        />

        <Group justify="flex-end" mt="md">
          <Button variant="default" onClick={onClose}>
            Cancelar
          </Button>
          <Button onClick={handleSubmit} loading={loading}>
            Criar
          </Button>
        </Group>
      </Stack>
    </Modal>
  )
}
