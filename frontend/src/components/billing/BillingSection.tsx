import {
  Stack,
  Card,
  Title,
  Text,
  Badge,
  Button,
  Group,
  SimpleGrid,
  Alert,
  Progress,
  Anchor,
} from '@mantine/core'
import {
  IconCheck,
  IconX,
  IconCreditCard,
  IconLicense,
  IconRefresh,
  IconExternalLink,
} from '@tabler/icons-react'
import { useState, useEffect } from 'react'
import { notifications } from '@mantine/notifications'
import { organizationsApi, SubscriptionResponse } from '@/lib/api'

interface BillingSectionProps {
  organizationId: number
  organizationName: string
  currentPlan: string
  onSubscriptionChange: () => void
}

const planFeatures = {
  FREE: {
    name: 'Free',
    price: 0,
    period: 'para sempre',
    features: [
      { included: true, text: 'Até 3 repositórios' },
      { included: true, text: '50 reviews por mês' },
      { included: true, text: 'Análise SAST básica' },
      { included: true, text: 'Análise com LLM (rate limited)' },
      { included: false, text: 'Suporte prioritário' },
      { included: false, text: 'API access' },
    ],
    cta: 'Plano atual',
    color: 'gray',
  },
  PRO: {
    name: 'Pro',
    price: 24,
    period: 'por mês',
    features: [
      { included: true, text: 'Repositórios ilimitados' },
      { included: true, text: 'Reviews ilimitados' },
      { included: true, text: 'Análise SAST completa' },
      { included: true, text: 'Análise com LLM avançada' },
      { included: true, text: 'Suporte por email' },
      { included: false, text: 'SLA garantido' },
    ],
    cta: 'Fazer upgrade',
    color: 'blue',
  },
  ENTERPRISE: {
    name: 'Enterprise',
    price: 299,
    period: 'por mês',
    features: [
      { included: true, text: 'Tudo do Pro' },
      { included: true, text: 'SSO & SAML' },
      { included: true, text: 'SLA garantido (99.9%)' },
      { included: true, text: 'Suporte dedicado' },
      { included: true, text: 'API access completo' },
      { included: true, text: 'Deploy on-premise' },
    ],
    cta: 'Fazer upgrade',
    color: 'purple',
  },
}

export function BillingSection({
  organizationId,
  organizationName,
  currentPlan,
  onSubscriptionChange,
}: BillingSectionProps) {
  const [subscription, setSubscription] = useState<SubscriptionResponse | null>(null)
  const [loadingCheckout, setLoadingCheckout] = useState(false)
  const [loadingPortal, setLoadingPortal] = useState(false)

  useEffect(() => {
    loadSubscription()
  }, [organizationId])

  const loadSubscription = async () => {
    try {
      const sub = await organizationsApi.getSubscription(organizationId)
      setSubscription(sub)
    } catch (error) {
      // Subscription might not exist
    }
  }

  const handleUpgrade = async (plan: string) => {
    if (plan === 'FREE') return // Cannot upgrade to free
    setLoadingCheckout(true)
    try {
      const { checkoutUrl } = await organizationsApi.startCheckout(organizationId, plan)
      window.location.href = checkoutUrl
    } catch (error) {
      notifications.show({
        title: 'Erro',
        message: 'Erro ao iniciar checkout',
        color: 'red',
      })
    } finally {
      setLoadingCheckout(false)
    }
  }

  const handleManageSubscription = async () => {
    setLoadingPortal(true)
    try {
      const { portalUrl } = await organizationsApi.getPortalUrl(organizationId)
      window.location.href = portalUrl
    } catch (error) {
      notifications.show({
        title: 'Erro',
        message: 'Erro ao abrir portal de assinatura',
        color: 'red',
      })
    } finally {
      setLoadingPortal(false)
    }
  }

  const handleCancelSubscription = async () => {
    try {
      await organizationsApi.cancelSubscription(organizationId)
      notifications.show({
        title: 'Sucesso',
        message: 'Assinatura cancelada. Você permanecerá com acesso até o fim do período.',
        color: 'green',
      })
      await loadSubscription()
      onSubscriptionChange()
    } catch (error) {
      notifications.show({
        title: 'Erro',
        message: 'Erro ao cancelar assinatura',
        color: 'red',
      })
    }
  }

  const isActive = subscription?.status === 'ACTIVE' || subscription?.status === 'TRIALING'
  const isPastDue = subscription?.status === 'PAST_DUE'
  const isCancelled = subscription?.status === 'CANCELLED' || subscription?.cancelAtPeriodEnd

  // Calculate usage percentage for Free plan
  const usagePercent = currentPlan === 'FREE' ? 66 : 0 // Example: 33/50 reviews used
  const usageText = currentPlan === 'FREE' ? '33 de 50 reviews usados este mês' : null

  return (
    <Stack gap="xl">
      {/* Current Subscription */}
      <Card padding="lg" radius="md" withBorder>
        <Stack gap="md">
          <Group justify="space-between">
            <Group>
              <IconLicense size={24} />
              <div>
                <Title order={4}>Assinatura Atual</Title>
                <Text c="dimmed" size="sm">
                  {organizationName}
                </Text>
              </div>
            </Group>
            {isActive && (
              <Badge
                color={subscription?.status === 'TRIALING' ? 'cyan' : 'green'}
                size="lg"
              >
                {subscription?.status === 'TRIALING' ? 'Trial' : 'Ativa'}
              </Badge>
            )}
          </Group>

          {subscription && (
            <SimpleGrid cols={{ base: 1, sm: 2 }}>
              <Stack gap="xs">
                <Text size="sm" c="dimmed">Plano</Text>
                <Text fw={500}>{subscription.planType}</Text>
              </Stack>
              <Stack gap="xs">
                <Text size="sm" c="dimmed">Status</Text>
                <Badge
                  color={
                    isPastDue
                      ? 'red'
                      : isCancelled
                        ? 'orange'
                        : subscription.status === 'TRIALING'
                          ? 'cyan'
                          : 'green'
                  }
                >
                  {subscription.status === 'TRIALING'
                    ? `Trial até ${subscription.trialEnd ? new Date(subscription.trialEnd).toLocaleDateString('pt-BR') : ''}`
                    : subscription.status === 'ACTIVE' && subscription.cancelAtPeriodEnd
                      ? 'Cancela ao fim do período'
                      : subscription.status}
                </Badge>
              </Stack>
              {subscription.currentPeriodStart && subscription.currentPeriodEnd && (
                <>
                  <Stack gap="xs">
                    <Text size="sm" c="dimmed">Período atual</Text>
                    <Text size="sm">
                      {new Date(subscription.currentPeriodStart).toLocaleDateString('pt-BR')} -{' '}
                      {new Date(subscription.currentPeriodEnd).toLocaleDateString('pt-BR')}
                    </Text>
                  </Stack>
                </>
              )}
            </SimpleGrid>
          )}

          {usageText && (
            <Stack gap="xs">
              <Group justify="space-between">
                <Text size="sm">Uso este mês</Text>
                <Text size="sm" fw={500}>
                  {usageText}
                </Text>
              </Group>
              <Progress value={usagePercent} color={usagePercent > 80 ? 'red' : 'blue'} />
            </Stack>
          )}

          {isActive && !isCancelled && (
            <Group>
              <Button
                variant="light"
                leftSection={<IconRefresh size={16} />}
                onClick={handleManageSubscription}
                loading={loadingPortal}
              >
                Gerenciar assinatura
              </Button>
              <Button
                variant="outline"
                color="red"
                onClick={handleCancelSubscription}
              >
                Cancelar
              </Button>
            </Group>
          )}
        </Stack>
      </Card>

      {/* Pricing Plans */}
      <div>
        <Title order={4} mb="md">
          Planos disponíveis
        </Title>
        <SimpleGrid cols={{ base: 1, sm: 3 }} spacing="lg">
          {(Object.keys(planFeatures) as Array<keyof typeof planFeatures>).map((planKey) => {
            const plan = planFeatures[planKey]
            const isCurrentPlan = planKey === currentPlan
            const isUpgrade = ['PRO', 'ENTERPRISE'].includes(planKey)

            return (
              <Card
                key={planKey}
                padding="xl"
                radius="md"
                withBorder
                styles={{
                  root: {
                    borderColor: isCurrentPlan ? `var(--mantine-color-${plan.color}-filled)` : undefined,
                    backgroundColor: isCurrentPlan ? `var(--mantine-color-${plan.color}-0)` : undefined,
                  },
                }}
              >
                <Stack gap="md">
                  <Stack gap="xs">
                    <Group justify="space-between">
                      <Text fw={700} size="lg">
                        {plan.name}
                      </Text>
                      {isCurrentPlan && <Badge color={plan.color}>Atual</Badge>}
                    </Group>
                    <Group align="flex-end">
                      <Text size="xl" fw={700}>
                        ${plan.price}
                      </Text>
                      <Text size="sm" c="dimmed">
                        {plan.period}
                      </Text>
                    </Group>
                  </Stack>

                  <Stack gap="xs">
                    {plan.features.map((feature, idx) => (
                      <Group key={idx} gap="xs">
                        {feature.included ? (
                          <IconCheck size={16} color="var(--mantine-color-green-6)" />
                        ) : (
                          <IconX size={16} color="var(--mantine-color-gray-4)" />
                        )}
                        <Text size="sm" c={feature.included ? undefined : 'dimmed'}>
                          {feature.text}
                        </Text>
                      </Group>
                    ))}
                  </Stack>

                  {isUpgrade && !isCurrentPlan ? (
                    <Button
                      color={plan.color as any}
                      fullWidth
                      onClick={() => handleUpgrade(planKey)}
                      loading={loadingCheckout}
                    >
                      Fazer upgrade
                    </Button>
                  ) : isCurrentPlan ? (
                    <Button variant="light" fullWidth disabled>
                      {plan.cta}
                    </Button>
                  ) : (
                    <Button variant="outline" fullWidth disabled>
                      Indisponível
                    </Button>
                  )}
                </Stack>
              </Card>
            )
          })}
        </SimpleGrid>
      </div>

      {/* Billing Info */}
      <Card padding="md" radius="md" withBorder bg="gray.0">
        <Stack gap="sm">
          <Group gap="xs">
            <IconCreditCard size={16} />
            <Text fw={500} size="sm">
              Pagamentos processados via Stripe
            </Text>
          </Group>
          <Text size="xs" c="dimmed">
            Você pode gerenciar sua assinatura, método de pagamento e faturas através do portal do
            cliente. Os preços são cobrados em USD.
          </Text>
          <Group>
            <Anchor
              size="sm"
              onClick={loadingPortal ? undefined : handleManageSubscription}
            >
              {loadingPortal ? 'Abrindo...' : 'Abrir portal de pagamento'} <IconExternalLink size={12} style={{ display: 'inline-block', marginLeft: '4px' }} />
            </Anchor>
          </Group>
        </Stack>
      </Card>

      {/* Annual Discount */}
      {currentPlan === 'FREE' && (
        <Alert variant="light" color="indigo">
          <Group>
            <Text fw={500} size="sm">
              Economize 20% com pagamento anual!
            </Text>
          </Group>
          <Text size="sm" mt="xs">
            Ao fazer upgrade para plano anual, você recebe 2 meses grátis. O desconto é aplicado
            automaticamente no checkout.
          </Text>
        </Alert>
      )}
    </Stack>
  )
}
