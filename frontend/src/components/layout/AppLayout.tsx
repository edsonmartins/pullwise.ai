import {
  AppShell,
  Burger,
  Group,
  UnstyledButton,
  Text,
  Avatar,
  Menu,
  rem,
  Container,
  ScrollArea,
  Tooltip,
  Badge,
} from '@mantine/core'
import { useDisclosure } from '@mantine/hooks'
import {
  IconDashboard,
  IconSourceCode,
  IconGitPullRequest,
  IconFileSearch,
  IconSettings,
  IconLogout,
  IconChevronRight,
  IconBuilding,
  IconWifi,
  IconWifiOff,
} from '@tabler/icons-react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '@/contexts/AuthContext'
import { useWebSocket } from '@/contexts/WebSocketContext'
import classes from './AppLayout.module.css'

interface AppLayoutProps {
  children: React.ReactNode
}

interface NavLink {
  icon: typeof IconDashboard
  label: string
  path: string
}

const navLinks: NavLink[] = [
  { icon: IconDashboard, label: 'Dashboard', path: '/' },
  { icon: IconBuilding, label: 'Organizações', path: '/organizations' },
  { icon: IconSourceCode, label: 'Projetos', path: '/projects' },
  { icon: IconGitPullRequest, label: 'Reviews', path: '/reviews' },
  { icon: IconSettings, label: 'Configurações', path: '/settings' },
]

export function AppLayout({ children }: AppLayoutProps) {
  const [opened, { toggle }] = useDisclosure()
  const location = useLocation()
  const navigate = useNavigate()
  const { user, logout } = useAuth()
  const { connected: wsConnected } = useWebSocket()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  const navItems = navLinks.map((link) => {
    const isActive = location.pathname === link.path || location.pathname.startsWith(link.path + '/')
    return (
      <Link
        key={link.label}
        to={link.path}
        className={classes.navLink}
        data-active={isActive || undefined}
      >
        <link.icon className={classes.navLinkIcon} stroke={1.5} />
        <span>{link.label}</span>
      </Link>
    )
  })

  return (
    <AppShell
      header={{ height: 60 }}
      navbar={{
        width: 280,
        breakpoint: 'sm',
        collapsed: { mobile: !opened, desktop: !opened },
      }}
      padding="md"
    >
      <AppShell.Header>
        <Group h="100%" px="md" justify="space-between">
          <Group>
            <Burger opened={opened} onClick={toggle} hiddenFrom="sm" size="sm" />
            <IconFileSearch size={28} className={classes.logo} />
            <Text fw={700} size="lg" className={classes.logoText}>
              Pullwise
            </Text>
          </Group>

          <Group gap="md">
            <Tooltip label={wsConnected ? 'Conectado em tempo real' : 'Desconectado'}>
              <Badge
                leftSection={wsConnected ? <IconWifi size={12} /> : <IconWifiOff size={12} />}
                color={wsConnected ? 'green' : 'gray'}
                variant="light"
              >
                {wsConnected ? 'Ao vivo' : 'Offline'}
              </Badge>
            </Tooltip>

            <Menu shadow="md" width={200}>
              <Menu.Target>
                <UnstyledButton className={classes.userButton}>
                  <Group>
                    <Avatar src={user?.avatarUrl} alt={user?.displayName} radius="xl" size="sm" />
                    <div style={{ flex: 1 }}>
                      <Text size="sm" fw={500}>
                        {user?.displayName || user?.username}
                      </Text>
                      <Text c="dimmed" size="xs">
                        {user?.email}
                      </Text>
                    </div>
                    <IconChevronRight style={{ width: rem(14), height: rem(14) }} stroke={1.5} />
                  </Group>
                </UnstyledButton>
              </Menu.Target>

              <Menu.Dropdown>
                <Menu.Item
                  leftSection={<IconSettings style={{ width: rem(16), height: rem(16) }} stroke={1.5} />}
                  onClick={() => navigate('/settings')}
                >
                  Configurações
                </Menu.Item>
                <Menu.Divider />
                <Menu.Item
                  leftSection={<IconLogout style={{ width: rem(16), height: rem(16) }} stroke={1.5} />}
                  onClick={handleLogout}
                  color="red"
                >
                  Sair
                </Menu.Item>
              </Menu.Dropdown>
            </Menu>
          </Group>
        </Group>
      </AppShell.Header>

      <AppShell.Navbar p="md">
        <AppShell.Section grow my="md" component={ScrollArea}>
          <Group className={classes.navSection} mb="xs">
            <Text size="xs" fw={500} c="dimmed">
              MENU
            </Text>
          </Group>
          {navItems}
        </AppShell.Section>
      </AppShell.Navbar>

      <AppShell.Main>
        <Container fluid>{children}</Container>
      </AppShell.Main>
    </AppShell>
  )
}
