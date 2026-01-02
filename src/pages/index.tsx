import Layout from '@theme/Layout'
import useDocusaurusContext from '@docusaurus/useDocusaurusContext'
import Link from '@docusaurus/Link'
import { useColorMode } from '@docusaurus/theme-common'

export default function Home() {
  const { siteConfig } = useDocusaurusContext()
  const { colorMode } = useColorMode()
  const isDarkTheme = colorMode === 'dark'

  return (
    <Layout title={`${siteConfig.title}`} description="AI-Powered Code Reviews">
      <main
        style={{
          background: isDarkTheme ? '#0f0f0f' : '#faf5ff',
          minHeight: '100vh',
          padding: '2rem 0',
        }}
      >
        <div className="container">
          <div
            style={{
              textAlign: 'center',
              padding: '4rem 1rem',
              maxWidth: '900px',
              margin: '0 auto',
            }}
          >
            <div
              style={{
                display: 'inline-block',
                padding: '0.25rem 0.75rem',
                background: isDarkTheme ? 'rgba(168, 85, 247, 0.15)' : 'rgba(147, 51, 234, 0.1)',
                color: isDarkTheme ? '#a855f7' : '#9333ea',
                borderRadius: '9999px',
                fontSize: '0.875rem',
                fontWeight: 500,
                marginBottom: '1.5rem',
              }}
            >
              v1.0 — Community Edition (MIT Licensed)
            </div>

            <h1
              style={{
                fontSize: 'clamp(2.5rem, 5vw, 4rem)',
                fontWeight: 700,
                lineHeight: 1.1,
                marginBottom: '1.5rem',
                background: 'linear-gradient(135deg, #9333ea 0%, #7c3aed 100%)',
                WebkitBackgroundClip: 'text',
                WebkitTextFillColor: 'transparent',
                backgroundClip: 'text',
              }}
            >
              AI-Powered Code Reviews
            </h1>

            <p
              style={{
                fontSize: 'clamp(1.125rem, 2vw, 1.5rem)',
                color: 'var(--ifm-color-emphasis-700)',
                maxWidth: '650px',
                margin: '0 auto 2.5rem',
                lineHeight: 1.6,
              }}
            >
              Open-source, self-hosted platform combining SAST with LLMs for
              intelligent, automated code reviews.
            </p>

            <div
              style={{
                display: 'flex',
                gap: '1rem',
                justifyContent: 'center',
                flexWrap: 'wrap',
                marginBottom: '3rem',
              }}
            >
              <Link
                to="/docs/getting-started/quick-start"
                style={{
                  padding: '0.875rem 2rem',
                  background: 'linear-gradient(135deg, #9333ea 0%, #7c3aed 100%)',
                  color: 'white',
                  borderRadius: '0.5rem',
                  fontWeight: 600,
                  textDecoration: 'none',
                  transition: 'all 0.2s ease',
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '0.5rem',
                }}
              >
                Get Started
                <svg
                  width="16"
                  height="16"
                  viewBox="0 0 16 16"
                  fill="none"
                  xmlns="http://www.w3.org/2000/svg"
                >
                  <path
                    d="M3 8H13M13 8L9 4M13 8L9 12"
                    stroke="currentColor"
                    strokeWidth="2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  />
                </svg>
              </Link>

              <Link
                to="/docs/getting-started/intro"
                style={{
                  padding: '0.875rem 2rem',
                  background: isDarkTheme ? 'var(--ifm-color-emphasis-800)' : 'white',
                  color: '#9333ea',
                  border: '2px solid #9333ea',
                  borderRadius: '0.5rem',
                  fontWeight: 600,
                  textDecoration: 'none',
                  transition: 'all 0.2s ease',
                }}
              >
                View Docs
              </Link>
            </div>

            <div
              style={{
                display: 'flex',
                gap: '2rem',
                justifyContent: 'center',
                flexWrap: 'wrap',
                fontSize: '0.875rem',
                color: 'var(--ifm-color-emphasis-600)',
              }}
            >
              <span style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <svg
                  width="16"
                  height="16"
                  viewBox="0 0 16 16"
                  fill="#22c55e"
                  xmlns="http://www.w3.org/2000/svg"
                >
                  <circle cx="8" cy="8" r="8" />
                </svg>
                MIT Licensed
              </span>
              <span style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <svg
                  width="16"
                  height="16"
                  viewBox="0 0 16 16"
                  fill="#22c55e"
                  xmlns="http://www.w3.org/2000/svg"
                >
                  <circle cx="8" cy="8" r="8" />
                </svg>
                Self-Hosted
              </span>
              <span style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <svg
                  width="16"
                  height="16"
                  viewBox="0 0 16 16"
                  fill="#22c55e"
                  xmlns="http://www.w3.org/2000/svg"
                >
                  <circle cx="8" cy="8" r="8" />
                </svg>
                200+ Plugins
              </span>
            </div>
          </div>

          {/* Features Grid */}
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
              gap: '1.5rem',
              marginTop: '4rem',
            }}
          >
            {[
              {
                title: 'Hybrid SAST + AI',
                description:
                  'Combines static analysis tools with LLMs for comprehensive code reviews.',
                icon: '🔍',
              },
              {
                title: 'Auto-Fix',
                description:
                  'One-click apply AI-generated fixes with preview and rollback support.',
                icon: '✨',
              },
              {
                title: 'Multi-Model LLM',
                description:
                  'Support for GPT-4, Claude, Gemini, and local models via Ollama.',
                icon: '🤖',
              },
              {
                title: 'Plugin System',
                description:
                  '200+ community plugins for custom linters and security rules.',
                icon: '🔌',
              },
              {
                title: 'GitHub Integration',
                description:
                  'Seamless integration with GitHub webhooks for automatic PR reviews.',
                icon: '🐙',
              },
              {
                title: 'Enterprise Ready',
                description:
                  'SAML SSO, RBAC, audit logs, and dedicated support available.',
                icon: '🏢',
              },
            ].map((feature) => (
              <div
                key={feature.title}
                style={{
                  padding: '1.5rem',
                  borderRadius: '1rem',
                  border: '1px solid var(--ifm-color-emphasis-200)',
                  background: isDarkTheme ? 'var(--ifm-color-emphasis-100)' : 'white',
                  transition: 'all 0.2s ease',
                }}
              >
                <div
                  style={{
                    fontSize: '2rem',
                    marginBottom: '1rem',
                  }}
                >
                  {feature.icon}
                </div>
                <h3
                  style={{
                    fontSize: '1.125rem',
                    fontWeight: 600,
                    marginBottom: '0.5rem',
                  }}
                >
                  {feature.title}
                </h3>
                <p
                  style={{
                    color: 'var(--ifm-color-emphasis-600)',
                    lineHeight: 1.6,
                    margin: 0,
                  }}
                >
                  {feature.description}
                </p>
              </div>
            ))}
          </div>

          {/* Quick Start Section */}
          <div
            style={{
              marginTop: '4rem',
              padding: '2rem',
              background: 'linear-gradient(135deg, #9333ea 0%, #7c3aed 100%)',
              borderRadius: '1rem',
              textAlign: 'center',
              color: 'white',
            }}
          >
            <h2
              style={{
                fontSize: '1.75rem',
                fontWeight: 600,
                marginBottom: '1rem',
                color: 'white',
              }}
            >
              Get Started in 5 Minutes
            </h2>
            <p
              style={{
                color: 'rgba(255,255,255,0.9)',
                marginBottom: '1.5rem',
                maxWidth: '600px',
                margin: '0 auto 1.5rem',
              }}
            >
              Run Pullwise locally with a single Docker command
            </p>
            <code
              style={{
                display: 'block',
                padding: '1rem',
                background: 'rgba(0,0,0,0.2)',
                borderRadius: '0.5rem',
                fontSize: '0.875rem',
                textAlign: 'left',
                maxWidth: '500px',
                margin: '0 auto 1.5rem',
                color: 'white',
                fontFamily: 'var(--ifm-font-family-monospace)',
              }}
            >
              docker run -d -p 8080:8080 pullwise/pullwise:latest
            </code>
            <Link
              to="/docs/getting-started/quick-start"
              style={{
                display: 'inline-block',
                padding: '0.75rem 1.5rem',
                background: 'white',
                color: '#9333ea',
                borderRadius: '0.5rem',
                fontWeight: 600,
                textDecoration: 'none',
              }}
            >
              Read the Quick Start Guide →
            </Link>
          </div>

          {/* Community Section */}
          <div
            style={{
              marginTop: '4rem',
              textAlign: 'center',
              padding: '2rem 0',
            }}
          >
            <h2
              style={{
                fontSize: '1.5rem',
                fontWeight: 600,
                marginBottom: '1rem',
              }}
            >
              Join the Community
            </h2>
            <p
              style={{
                color: 'var(--ifm-color-emphasis-600)',
                marginBottom: '1.5rem',
              }}
            >
              10,000+ developers using Pullwise
            </p>
            <div
              style={{
                display: 'flex',
                gap: '1rem',
                justifyContent: 'center',
                flexWrap: 'wrap',
              }}
            >
              <a
                href="https://github.com/integralltech/pullwise-ai"
                target="_blank"
                rel="noopener noreferrer"
                style={{
                  padding: '0.75rem 1.5rem',
                  background: 'var(--ifm-color-emphasis-100)',
                  borderRadius: '0.5rem',
                  textDecoration: 'none',
                  color: 'var(--ifm-color-emphasis-800)',
                  fontWeight: 500,
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '0.5rem',
                }}
              >
                <svg
                  width="20"
                  height="20"
                  viewBox="0 0 24 24"
                  fill="currentColor"
                  xmlns="http://www.w3.org/2000/svg"
                >
                  <path d="M12 .297c-6.63 0-12 5.373-12 12 0 5.303 3.438 9.8 8.205 11.385.6.113.82-.258.82-.577 0-.285-.01-1.04-.015-2.04-3.338.724-4.042-1.61-4.042-1.61C4.422 18.07 3.633 17.7 3.633 17.7c-1.087-.744.084-.729.084-.729 1.205.084 1.838 1.236 1.838 1.236 1.07 1.835 2.809 1.305 3.495.998.108-.776.417-1.305.76-1.605-2.665-.3-5.466-1.332-5.466-5.93 0-1.31.465-2.38 1.235-3.22-.135-.303-.54-1.523.105-3.176 0 0 1.005-.322 3.3 1.23.96-.267 1.98-.399 3-.405 1.02.006 2.04.138 3 .405 2.28-1.552 3.285-1.23 3.285-1.23.645 1.653.24 2.873.12 3.176.765.84 1.23 1.91 1.23 3.22 0 4.61-2.805 5.625-5.475 5.92.42.36.81 1.096.81 2.22 0 1.606-.015 2.896-.015 3.286 0 .315.21.69.825.57C20.565 22.092 24 17.592 24 12.297c0-6.627-5.373-12-12-12" />
                </svg>
                GitHub
              </a>
              <a
                href="https://discord.gg/pullwise"
                target="_blank"
                rel="noopener noreferrer"
                style={{
                  padding: '0.75rem 1.5rem',
                  background: 'var(--ifm-color-emphasis-100)',
                  borderRadius: '0.5rem',
                  textDecoration: 'none',
                  color: 'var(--ifm-color-emphasis-800)',
                  fontWeight: 500,
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '0.5rem',
                }}
              >
                <svg
                  width="20"
                  height="20"
                  viewBox="0 0 24 24"
                  fill="currentColor"
                  xmlns="http://www.w3.org/2000/svg"
                >
                  <path d="M20.317 4.37a19.791 19.791 0 0 0-4.885-1.515.074.074 0 0 0-.079.037c-.21.375-.444.864-.608 1.25a18.27 18.27 0 0 0-5.487 0 12.64 12.64 0 0 0-.617-1.25.077.077 0 0 0-.079-.037A19.736 19.736 0 0 0 3.677 4.37a.07.07 0 0 0-.032.027C.533 9.046-.32 13.58.099 18.057a.082.082 0 0 0 .031.057 19.9 19.9 0 0 0 5.993 3.03.078.078 0 0 0 .084-.028c.462-.63.874-1.295 1.226-1.994a.076.076 0 0 0-.041-.106 13.107 13.107 0 0 1-1.872-.892.077.077 0 0 1-.008-.128 10.2 10.2 0 0 0 .372-.292.074.074 0 0 1 .077-.01c3.928 1.793 8.18 1.793 12.062 0a.074.074 0 0 1 .078.01c.12.098.246.198.373.292a.077.077 0 0 1-.006.127 12.299 12.299 0 0 1-1.873.892.077.077 0 0 0-.041.107c.36.698.772 1.362 1.225 1.993a.076.076 0 0 0 .084.028 19.839 19.839 0 0 0 6.002-3.03.077.077 0 0 0 .032-.054c.5-5.177-.838-9.674-3.549-13.66a.061.061 0 0 0-.031-.03zM8.02 15.33c-1.183 0-2.157-1.085-2.157-2.419 0-1.333.956-2.419 2.157-2.419 1.21 0 2.176 1.096 2.157 2.42 0 1.333-.956 2.418-2.157 2.418zm7.975 0c-1.183 0-2.157-1.085-2.157-2.419 0-1.333.955-2.419 2.157-2.419 1.21 0 2.176 1.096 2.157 2.42 0 1.333-.946 2.418-2.157 2.418z" />
                </svg>
                Discord
              </a>
            </div>
          </div>
        </div>
      </main>
    </Layout>
  )
}
