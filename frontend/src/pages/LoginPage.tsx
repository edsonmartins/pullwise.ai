import { Button, Container, Paper, Title, Text, Stack, Anchor } from '@mantine/core'
import { IconBrandGithub } from '@tabler/icons-react'
import { useNavigate } from 'react-router-dom'

export function LoginPage() {
  const navigate = useNavigate()

  const handleGitHubLogin = () => {
    // Redirecionar para OAuth do GitHub
    window.location.href = `${import.meta.env.VITE_API_URL || '/api'}/oauth2/authorization/github`
  }

  return (
    <Container size="sm" py="xl">
      <Stack align="center" gap="xl">
        <Paper withBorder shadow="md" p="xl" radius="md" w="100%">
          <Stack gap="md">
            <Stack align="center" gap="xs">
              <Title order={2}>Bem-vindo ao Pullwise</Title>
              <Text c="dimmed" size="sm">
                Code Review com IA para seus Pull Requests
              </Text>
            </Stack>

            <Button
              leftSection={<IconBrandGithub size={20} />}
              size="lg"
              fullWidth
              onClick={handleGitHubLogin}
            >
              Entrar com GitHub
            </Button>

            <Text c="dimmed" size="xs" ta="center">
              Ao entrar, você concorda com nossos Termos de Serviço e Política de Privacidade
            </Text>
          </Stack>
        </Paper>

        <Stack align="center" gap={4}>
          <Text c="dimmed" size="sm">
            Pullwise ajuda equipes a fazer code review mais rápido e com mais qualidade.
          </Text>
          <Anchor size="sm" onClick={() => navigate('/landing')}>
            Saiba mais sobre como funciona
          </Anchor>
        </Stack>
      </Stack>
    </Container>
  )
}
