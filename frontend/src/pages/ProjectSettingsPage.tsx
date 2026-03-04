import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  Card,
  Stack,
  Group,
  TextInput,
  Textarea,
  Switch,
  Button,
  Text,
  Title,
  Divider,
  Loader,
  Badge,
  Anchor,
  Center,
} from '@mantine/core'
import { notifications } from '@mantine/notifications'
import {
  IconArrowLeft,
  IconDeviceFloppy,
  IconTrash,
  IconExternalLink,
} from '@tabler/icons-react'
import { projectsApi } from '@/lib/api'

interface ProjectUpdate {
  name?: string
  description?: string
  isActive?: boolean
  autoReviewEnabled?: boolean
}

export default function ProjectSettingsPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [isActive, setIsActive] = useState(true)
  const [autoReviewEnabled, setAutoReviewEnabled] = useState(false)
  const [deleteConfirm, setDeleteConfirm] = useState(false)

  const { data: project, isLoading } = useQuery({
    queryKey: ['project', id],
    queryFn: () => projectsApi.get(Number(id)),
    enabled: !!id,
  })

  const updateMutation = useMutation({
    mutationFn: (data: ProjectUpdate) => projectsApi.update(Number(id), data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['project', id] })
      queryClient.invalidateQueries({ queryKey: ['projects'] })
      notifications.show({
        title: 'Settings saved',
        message: 'Project settings have been updated successfully.',
        color: 'green',
      })
    },
    onError: () => {
      notifications.show({
        title: 'Error',
        message: 'Failed to update project settings. Please try again.',
        color: 'red',
      })
    },
  })

  const deleteMutation = useMutation({
    mutationFn: () => projectsApi.delete(Number(id)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['projects'] })
      notifications.show({
        title: 'Project deleted',
        message: 'The project has been permanently deleted.',
        color: 'orange',
      })
      navigate('/projects')
    },
    onError: () => {
      notifications.show({
        title: 'Error',
        message: 'Failed to delete project. Please try again.',
        color: 'red',
      })
      setDeleteConfirm(false)
    },
  })

  useEffect(() => {
    if (project) {
      setName(project.name ?? '')
      setDescription(project.description ?? '')
      setIsActive(project.isActive ?? true)
      setAutoReviewEnabled(project.autoReviewEnabled ?? false)
    }
  }, [project])

  const handleSave = () => {
    if (!name.trim()) {
      notifications.show({
        title: 'Validation error',
        message: 'Project name is required.',
        color: 'red',
      })
      return
    }

    updateMutation.mutate({
      name: name.trim(),
      description: description.trim() || undefined,
      isActive,
      autoReviewEnabled,
    })
  }

  const handleDelete = () => {
    if (!deleteConfirm) {
      setDeleteConfirm(true)
      return
    }
    deleteMutation.mutate()
  }

  if (isLoading) {
    return (
      <Center style={{ minHeight: '60vh' }}>
        <Loader size="lg" />
      </Center>
    )
  }

  if (!project) {
    return (
      <Center style={{ minHeight: '60vh' }}>
        <Stack align="center" gap="md">
          <Title order={3}>Project not found</Title>
          <Text c="dimmed">
            The project you are looking for does not exist or you do not have access to it.
          </Text>
          <Button
            variant="light"
            leftSection={<IconArrowLeft size={16} />}
            onClick={() => navigate('/projects')}
          >
            Back to Projects
          </Button>
        </Stack>
      </Center>
    )
  }

  return (
    <Stack gap="lg" p="md" maw={720} mx="auto">
      <Button
        variant="subtle"
        leftSection={<IconArrowLeft size={16} />}
        onClick={() => navigate(`/projects/${id}`)}
        style={{ alignSelf: 'flex-start' }}
      >
        Back to Project
      </Button>

      <Title order={2}>Project Settings</Title>

      {/* General Settings */}
      <Card withBorder shadow="sm" radius="md" p="lg">
        <Stack gap="md">
          <Title order={4}>General Settings</Title>
          <Divider />

          <TextInput
            label="Project Name"
            placeholder="Enter project name"
            required
            value={name}
            onChange={(e) => setName(e.currentTarget.value)}
          />

          <Textarea
            label="Description"
            placeholder="Enter a description for this project"
            minRows={3}
            autosize
            value={description}
            onChange={(e) => setDescription(e.currentTarget.value)}
          />

          <Switch
            label="Active Status"
            description="When disabled, the project will not process any new reviews."
            checked={isActive}
            onChange={(e) => setIsActive(e.currentTarget.checked)}
          />

          <Switch
            label="Auto-Review Enabled"
            description="Automatically trigger code reviews on new pull requests."
            checked={autoReviewEnabled}
            onChange={(e) => setAutoReviewEnabled(e.currentTarget.checked)}
          />

          <Group justify="flex-end" mt="sm">
            <Button
              leftSection={<IconDeviceFloppy size={16} />}
              onClick={handleSave}
              loading={updateMutation.isPending}
            >
              Save Changes
            </Button>
          </Group>
        </Stack>
      </Card>

      {/* Repository Info */}
      <Card withBorder shadow="sm" radius="md" p="lg">
        <Stack gap="md">
          <Title order={4}>Repository Info</Title>
          <Divider />

          <Group gap="sm">
            <Text fw={500} size="sm" w={160}>Platform:</Text>
            <Badge variant="light" color="blue">{project.platform}</Badge>
          </Group>

          {project.repositoryUrl && (
            <Group gap="sm">
              <Text fw={500} size="sm" w={160}>Repository URL:</Text>
              <Anchor
                href={project.repositoryUrl}
                target="_blank"
                rel="noopener noreferrer"
                size="sm"
              >
                <Group gap={4}>
                  {project.repositoryUrl}
                  <IconExternalLink size={14} />
                </Group>
              </Anchor>
            </Group>
          )}

          {project.repositoryId && (
            <Group gap="sm">
              <Text fw={500} size="sm" w={160}>Repository ID:</Text>
              <Text size="sm" c="dimmed">{project.repositoryId}</Text>
            </Group>
          )}

          {project.githubInstallationId != null && (
            <Group gap="sm">
              <Text fw={500} size="sm" w={160}>GitHub Installation ID:</Text>
              <Text size="sm" c="dimmed">{project.githubInstallationId}</Text>
            </Group>
          )}
        </Stack>
      </Card>

      {/* Danger Zone */}
      <Card
        withBorder
        shadow="sm"
        radius="md"
        p="lg"
        style={{ borderColor: 'var(--mantine-color-red-6)' }}
      >
        <Stack gap="md">
          <Title order={4} c="red">Danger Zone</Title>
          <Divider />

          <Text size="sm" c="dimmed">
            Deleting this project is permanent and cannot be undone. All associated reviews,
            findings, and configuration will be removed.
          </Text>

          <Group justify="flex-end">
            {deleteConfirm && (
              <Button
                variant="default"
                onClick={() => setDeleteConfirm(false)}
              >
                Cancel
              </Button>
            )}
            <Button
              color="red"
              variant={deleteConfirm ? 'filled' : 'outline'}
              leftSection={<IconTrash size={16} />}
              onClick={handleDelete}
              loading={deleteMutation.isPending}
            >
              {deleteConfirm ? 'Confirm Delete' : 'Delete Project'}
            </Button>
          </Group>
        </Stack>
      </Card>
    </Stack>
  )
}
