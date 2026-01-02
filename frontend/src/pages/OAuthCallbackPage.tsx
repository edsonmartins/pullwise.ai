import { useEffect, useState } from 'react'
import { Container, Stack, Title, Text, Loader, Alert } from '@mantine/core'
import { IconCheck, IconX } from '@tabler/icons-react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth } from '@/contexts/AuthContext'

export function OAuthCallbackPage() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const { login } = useAuth()
  const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading')
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const handleCallback = async () => {
      const token = searchParams.get('token')
      const errorParam = searchParams.get('error')
      const errorDescription = searchParams.get('error_description')

      if (errorParam || errorDescription) {
        setStatus('error')
        setError(errorDescription || errorParam || 'Erro desconhecido')
        setTimeout(() => navigate('/landing'), 3000)
        return
      }

      if (!token) {
        setStatus('error')
        setError('Token não encontrado na resposta')
        setTimeout(() => navigate('/landing'), 3000)
        return
      }

      try {
        await login(token)
        setStatus('success')
        setTimeout(() => navigate('/'), 1000)
      } catch (err) {
        setStatus('error')
        setError('Falha ao autenticar. Tente novamente.')
        setTimeout(() => navigate('/landing'), 3000)
      }
    }

    handleCallback()
  }, [searchParams, login, navigate])

  return (
    <Container size="sm" py="xl">
      <Stack align="center" gap="md">
        {status === 'loading' && (
          <>
            <Loader size="xl" />
            <Title order={3}>Autenticando...</Title>
            <Text c="dimmed">Por favor, aguarde enquanto verificamos suas credenciais.</Text>
          </>
        )}

        {status === 'success' && (
          <>
            <Alert
              variant="light"
              color="green"
              title="Sucesso!"
              icon={<IconCheck />}
              w="100%"
            >
              Você será redirecionado em instantes...
            </Alert>
          </>
        )}

        {status === 'error' && (
          <>
            <Alert
              variant="light"
              color="red"
              title="Erro na autenticação"
              icon={<IconX />}
              w="100%"
            >
              {error}
            </Alert>
            <Text c="dimmed" size="sm">
              Você será redirecionado para a página inicial...
            </Text>
          </>
        )}
      </Stack>
    </Container>
  )
}
