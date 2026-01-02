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
  SimpleGrid,
  Box,
  Code,
  Alert,
} from '@mantine/core'
import { IconPlus, IconTrash, IconEdit, IconBuilding, IconUsers, IconCreditCard } from '@tabler/icons-react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useState, useEffect } from 'react'
import { notifications } from '@mantine/notifications'
import { organizationsApi } from '@/lib/api'
import { modals } from '@mantine/modals'
import { BillingSection } from '@/components/billing/BillingSection'

type Organization = {
  id: number
  name: string
  slug: string
  planType: 'FREE' | 'PRO' | 'ENTERPRISE'
  repoCount?: number
  reviewCount?: number
  memberCount?: number
  createdAt?: string
}

const planLimits = {
  FREE: { repos: 3, reviewsPerMonth: 50, price: 0 },
  PRO: { repos: -1, reviewsPerMonth: -1, price: 24 },
  ENTERPRISE: { repos: -1, reviewsPerMonth: -1, price: 299 },
}

export function OrganizationsPage() {
  const queryClient = useQueryClient()
  const [opened, setOpened] = useState(false)
  const [billingOpened, setBillingOpened] = useState(false)
  const [selectedOrg, setSelectedOrg] = useState<Organization | null>(null)
  const [editingOrg, setEditingOrg] = useState<Organization | null>(null)

  const { data: organizations, isLoading } = useQuery({
    queryKey: ['organizations'],
    queryFn: organizationsApi.list,
  })

  const createMutation = useMutation({
    mutationFn: organizationsApi.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['organizations'] })
      setOpened(false)
      setEditingOrg(null)
      notifications.show({
        title: 'Sucesso',
        message: 'Organização criada com sucesso',
        color: 'green',
      })
    },
    onError: () => {
      notifications.show({
        title: 'Erro',
        message: 'Erro ao criar organização',
        color: 'red',
      })
    },
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: Partial<Organization> }) =>
      organizationsApi.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['organizations'] })
      setOpened(false)
      setEditingOrg(null)
      notifications.show({
        title: 'Sucesso',
        message: 'Organização atualizada',
        color: 'green',
      })
    },
  })

  const deleteMutation = useMutation({
    mutationFn: organizationsApi.delete,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['organizations'] })
      notifications.show({
        title: 'Sucesso',
        message: 'Organização removida',
        color: 'green',
      })
    },
  })

  const handleDelete = (org: Organization) => {
    modals.openConfirmModal({
      title: 'Excluir organização',
      children: (
        <Text size="sm">
          Tem certeza que deseja excluir a organização <b>{org.name}</b>? Esta ação não pode ser
          desfeita.
        </Text>
      ),
      labels: { confirm: 'Excluir', cancel: 'Cancelar' },
      confirmProps: { color: 'red' },
      onConfirm: () => deleteMutation.mutate(org.id),
    })
  }

  const handleEdit = (org: Organization) => {
    setEditingOrg(org)
    setOpened(true)
  }

  const handleCreate = () => {
    setEditingOrg(null)
    setOpened(true)
  }

  const handleBilling = (org: Organization) => {
    setSelectedOrg(org)
    setBillingOpened(true)
  }

  const handleSubscriptionChange = () => {
    queryClient.invalidateQueries({ queryKey: ['organizations'] })
  }

  return (
    <Stack gap="xl">
      <Group justify="space-between">
        <div>
          <Title order={2}>Organizações</Title>
          <Text c="dimmed">Gerencie suas organizações e equipes</Text>
        </div>
        <Button leftSection={<IconPlus size={16} />} onClick={handleCreate}>
          Nova Organização
        </Button>
      </Group>

      {isLoading ? (
        <Text>Carregando...</Text>
      ) : organizations && organizations.length > 0 ? (
        <SimpleGrid cols={{ base: 1, sm: 2, lg: 3 }}>
          {organizations.map((org) => (
            <OrganizationCard
              key={org.id}
              organization={org}
              onEdit={() => handleEdit(org)}
              onDelete={() => handleDelete(org)}
              onBilling={() => handleBilling(org)}
            />
          ))}
        </SimpleGrid>
      ) : (
        <Card padding="xl" radius="md" withBorder>
          <Stack align="center" gap="sm">
            <Box c="dimmed">
              <IconBuilding size={48} opacity={0.5} />
            </Box>
            <Text size="lg" fw={500}>
              Nenhuma organização
            </Text>
            <Text c="dimmed" size="sm">
              Crie sua primeira organização para começar
            </Text>
            <Button leftSection={<IconPlus size={16} />} onClick={handleCreate}>
              Criar Organização
            </Button>
          </Stack>
        </Card>
      )}

      <OrganizationFormModal
        opened={opened}
        onClose={() => {
          setOpened(false)
          setEditingOrg(null)
        }}
        onSubmit={(data) => {
          if (editingOrg) {
            updateMutation.mutate({ id: editingOrg.id, data })
          } else {
            createMutation.mutate(data as any)
          }
        }}
        organization={editingOrg}
        loading={createMutation.isPending || updateMutation.isPending}
      />

      {selectedOrg && (
        <Modal
          opened={billingOpened}
          onClose={() => setBillingOpened(false)}
          title={`Billing - ${selectedOrg.name}`}
          size="xl"
        >
          <BillingSection
            organizationId={selectedOrg.id}
            organizationName={selectedOrg.name}
            currentPlan={selectedOrg.planType}
            onSubscriptionChange={handleSubscriptionChange}
          />
        </Modal>
      )}
    </Stack>
  )
}

interface OrganizationCardProps {
  organization: Organization
  onEdit: () => void
  onDelete: () => void
  onBilling: () => void
}

function OrganizationCard({ organization, onEdit, onDelete, onBilling }: OrganizationCardProps) {
  const plan = planLimits[organization.planType]
  const isFree = organization.planType === 'FREE'

  return (
    <Card padding="lg" radius="md" withBorder>
      <Group justify="space-between" mb="sm">
        <Group>
          <IconBuilding size={24} />
          <div>
            <Text fw={500}>{organization.name}</Text>
            <Text size="xs" c="dimmed">
              @{organization.slug}
            </Text>
          </div>
        </Group>
        <Badge
          color={organization.planType === 'FREE' ? 'gray' : organization.planType === 'PRO' ? 'blue' : 'purple'}
        >
          {organization.planType}
        </Badge>
      </Group>

      {isFree && (
        <Alert variant="light" color="blue" mb="sm">
          <Text size="xs">
            <strong>{plan.repos}</strong> repositórios • <strong>{plan.reviewsPerMonth}</strong>{' '}
            reviews/mês
          </Text>
        </Alert>
      )}

      <Stack gap="xs" mb="sm">
        {organization.memberCount !== undefined && (
          <Group gap="xs">
            <IconUsers size={14} />
            <Text size="sm">{organization.memberCount} membros</Text>
          </Group>
        )}
        {organization.repoCount !== undefined && (
          <Text size="sm">{organization.repoCount} repositórios</Text>
        )}
        {organization.reviewCount !== undefined && (
          <Text size="sm">{organization.reviewCount} reviews este mês</Text>
        )}
      </Stack>

      <Group mt="sm" justify="flex-end">
        <Button
          variant="light"
          size="sm"
          leftSection={<IconCreditCard size={14} />}
          onClick={onBilling}
        >
          {isFree ? 'Upgrade' : 'Gerenciar'}
        </Button>
        <ActionIcon variant="light" color="blue" onClick={onEdit}>
          <IconEdit size={16} />
        </ActionIcon>
        <ActionIcon variant="light" color="red" onClick={onDelete}>
          <IconTrash size={16} />
        </ActionIcon>
      </Group>
    </Card>
  )
}

interface OrganizationFormModalProps {
  opened: boolean
  onClose: () => void
  onSubmit: (data: any) => void
  organization: Organization | null
  loading?: boolean
}

function OrganizationFormModal({
  opened,
  onClose,
  onSubmit,
  organization,
  loading,
}: OrganizationFormModalProps) {
  const [name, setName] = useState('')
  const [slug, setSlug] = useState('')
  const [planType, setPlanType] = useState<'FREE' | 'PRO' | 'ENTERPRISE'>('FREE')

  useEffect(() => {
    if (organization) {
      setName(organization.name)
      setSlug(organization.slug)
      setPlanType(organization.planType)
    } else {
      setName('')
      setSlug('')
      setPlanType('FREE')
    }
  }, [organization, opened])

  const handleSubmit = () => {
    onSubmit({
      name,
      slug: slug.toLowerCase().replace(/\s+/g, '-'),
      planType,
    })
  }

  const selectedPlan = planLimits[planType]

  return (
    <Modal opened={opened} onClose={onClose} title={organization ? 'Editar Organização' : 'Nova Organização'} size="md">
      <Stack>
        <TextInput
          label="Nome"
          placeholder="Minha Empresa"
          value={name}
          onChange={(e) => setName(e.currentTarget.value)}
          required
        />

        <TextInput
          label="Slug"
          placeholder="minha-empresa"
          value={slug}
          onChange={(e) => setSlug(e.currentTarget.value.toLowerCase().replace(/\s+/g, '-'))}
          required
          leftSection={<Code fz="xs">@</Code>}
        />

        <Select
          label="Plano"
          data={[
            { value: 'FREE', label: 'Free - Grátis' },
            { value: 'PRO', label: `Pro - $${planLimits.PRO.price}/mês` },
            { value: 'ENTERPRISE', label: `Enterprise - $${planLimits.ENTERPRISE.price}/mês` },
          ]}
          value={planType}
          onChange={(value) => setPlanType((value || 'FREE') as any)}
        />

        {planType === 'FREE' && (
          <Alert variant="light" color="blue">
            <Stack gap="xs">
              <Text size="sm" fw={500}>
                Limites do plano Free
              </Text>
              <Text size="sm">• {selectedPlan.repos} repositórios</Text>
              <Text size="sm">• {selectedPlan.reviewsPerMonth} reviews por mês</Text>
            </Stack>
          </Alert>
        )}

        {planType === 'PRO' && (
          <Alert variant="light" color="indigo">
            <Stack gap="xs">
              <Text size="sm" fw={500}>
                Plano Pro - Repos ilimitados
              </Text>
              <Text size="sm">• ${selectedPlan.price}/mês</Text>
              <Text size="sm">• Repositórios ilimitados</Text>
              <Text size="sm">• Reviews ilimitados</Text>
              <Text size="sm">• Suporte prioritário</Text>
            </Stack>
          </Alert>
        )}

        {planType === 'ENTERPRISE' && (
          <Alert variant="light" color="purple">
            <Stack gap="xs">
              <Text size="sm" fw={500}>
                Plano Enterprise
              </Text>
              <Text size="sm">• ${selectedPlan.price}/mês</Text>
              <Text size="sm">• Tudo do Pro +</Text>
              <Text size="sm">• SSO & SAML</Text>
              <Text size="sm">• SLA garantido</Text>
              <Text size="sm">• Suporte dedicado</Text>
            </Stack>
          </Alert>
        )}

        <Group justify="flex-end" mt="md">
          <Button variant="default" onClick={onClose}>
            Cancelar
          </Button>
          <Button onClick={handleSubmit} loading={loading}>
            {organization ? 'Salvar' : 'Criar'}
          </Button>
        </Group>
      </Stack>
    </Modal>
  )
}
