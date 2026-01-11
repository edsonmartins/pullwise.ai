import { useState } from "react"
import { Button } from "@/components/ui/button"
import { Copy, Check, Download, Container, Github } from "lucide-react"
import { Link } from "react-router-dom"
import logo from "@/assets/logo.png"

/**
 * DownloadPage component
 * Provides download options and installation instructions for Community Edition
 */
export default function DownloadPage() {
  const [copied, setCopied] = useState(false)

  const dockerComposeYml = `version: '3.8'

services:
  pullwise:
    image: pullwise/pullwise-ce:latest
    container_name: pullwise
    ports:
      - "8080:8080"
    environment:
      - PULLWISE_DATABASE_URL=postgresql://pullwise:pullwise@postgres:5432/pullwise
      - PULLWISE_REDIS_URL=redis://redis:6379
      - PULLWISE_OPENROUTER_API_KEY=\${OPENROUTER_API_KEY:-}
      - PULLWISE_OLLAMA_URL=http://ollama:11434
    depends_on:
      - postgres
      - redis
      - ollama
    restart: unless-stopped

  postgres:
    image: postgres:16-alpine
    container_name: pullwise-postgres
    environment:
      - POSTGRES_DB=pullwise
      - POSTGRES_USER=pullwise
      - POSTGRES_PASSWORD=pullwise
    volumes:
      - postgres_data:/var/lib/postgresql/data
    restart: unless-stopped

  redis:
    image: redis:7-alpine
    container_name: pullwise-redis
    restart: unless-stopped

  ollama:
    image: ollama/ollama:latest
    container_name: pullwise-ollama
    volumes:
      - ollama_data:/root/.ollama
    restart: unless-stopped

volumes:
  postgres_data:
  ollama_data:
`

  const installSteps = [
    {
      title: '1. Install Docker',
      description: 'Make sure you have Docker and Docker Compose installed on your system.',
      command: 'docker --version && docker-compose --version',
    },
    {
      title: '2. Create docker-compose.yml',
      description: 'Copy the configuration below to a file named docker-compose.yml',
    },
    {
      title: '3. Set API Key (Optional)',
      description: 'For cloud LLM support, set your OpenRouter API key as environment variable.',
      command: 'export OPENROUTER_API_KEY=your_key_here',
    },
    {
      title: '4. Start Pullwise',
      description: 'Run the docker-compose command to start all services.',
      command: 'docker-compose up -d',
    },
    {
      title: '5. Access Pullwise',
      description: 'Open your browser and navigate to the Pullwise web interface.',
      command: 'open http://localhost:8080',
    },
  ]

  const handleCopy = () => {
    navigator.clipboard.writeText(dockerComposeYml)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  return (
    <div className="min-h-screen bg-gradient-to-b from-purple-50 to-white dark:from-gray-900 dark:to-gray-950">
      {/* Header */}
      <nav className="container mx-auto px-6 py-6">
        <Link to="/landing" className="flex items-center gap-2">
          <img src={logo} alt="Pullwise Logo" className="h-12 w-12" />
          <span className="text-2xl font-bold text-gray-900 dark:text-white">Pullwise</span>
          <span className="ml-2 px-2 py-1 bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-300 text-xs font-semibold rounded">
            MIT Licensed
          </span>
        </Link>
      </nav>

      <div className="container mx-auto px-6 py-12">
        <div className="max-w-4xl mx-auto">
          {/* Header */}
          <div className="text-center mb-12">
            <h1 className="text-4xl md:text-5xl font-bold text-gray-900 dark:text-white mb-4">
              Download Community Edition
            </h1>
            <p className="text-xl text-gray-600 dark:text-gray-300">
              Get started with Pullwise in 5 minutes. Free forever.
            </p>
          </div>

          {/* Quick Start */}
          <div className="bg-white dark:bg-gray-800 rounded-xl shadow-lg p-8 mb-8 border border-gray-200 dark:border-gray-700">
            <h2 className="text-2xl font-bold text-gray-900 dark:text-white mb-6 flex items-center gap-2">
              <Download className="h-6 w-6 text-purple-600" />
              Quick Start
            </h2>

            <div className="bg-gray-900 rounded-lg p-4 mb-6 overflow-x-auto">
              <pre className="text-green-400 text-sm">
                <code>mkdir pullwise && cd pullwise</code><br/>
                <code>wget https://pullwise.ai/docker-compose.yml</code><br/>
                <code>docker-compose up -d</code>
              </pre>
            </div>

            <div className="flex flex-wrap gap-4">
              <Button size="lg" className="bg-purple-600 hover:bg-purple-700" onClick={handleCopy}>
                {copied ? <Check className="mr-2 h-5 w-5" /> : <Copy className="mr-2 h-5 w-5" />}
                {copied ? 'Copied!' : 'Copy Configuration'}
              </Button>
              <Button size="lg" variant="outline" asChild>
                <a href="https://github.com/integralltech/pullwise-ai" target="_blank" rel="noopener noreferrer">
                  <Github className="mr-2 h-5 w-5" />
                  View on GitHub
                </a>
              </Button>
              <Button size="lg" variant="outline" asChild>
                <a href="https://hub.docker.com/r/pullwise/pullwise-ce" target="_blank" rel="noopener noreferrer">
                  <Container className="mr-2 h-5 w-5" />
                  Docker Hub
                </a>
              </Button>
            </div>
          </div>

          {/* Installation Steps */}
          <div className="mb-8">
            <h2 className="text-2xl font-bold text-gray-900 dark:text-white mb-6">
              Installation Steps
            </h2>
            <div className="space-y-4">
              {installSteps.map((step, index) => (
                <div key={index} className="bg-white dark:bg-gray-800 rounded-lg p-6 border border-gray-200 dark:border-gray-700">
                  <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-2">
                    {step.title}
                  </h3>
                  <p className="text-gray-600 dark:text-gray-400 mb-4">
                    {step.description}
                  </p>
                  {step.command && (
                    <div className="bg-gray-900 rounded-lg p-3">
                      <code className="text-green-400 text-sm">{step.command}</code>
                    </div>
                  )}
                </div>
              ))}
            </div>
          </div>

          {/* docker-compose.yml */}
          <div className="mb-8">
            <h2 className="text-2xl font-bold text-gray-900 dark:text-white mb-6">
              docker-compose.yml
            </h2>
            <div className="bg-gray-900 rounded-lg p-4 overflow-x-auto">
              <pre className="text-green-400 text-xs">
                <code>{dockerComposeYml}</code>
              </pre>
            </div>
            <Button
              variant="outline"
              className="mt-4"
              onClick={handleCopy}
            >
              {copied ? <Check className="mr-2 h-4 w-4" /> : <Copy className="mr-2 h-4 w-4" />}
              {copied ? 'Copied!' : 'Copy to Clipboard'}
            </Button>
          </div>

          {/* Requirements */}
          <div className="bg-blue-50 dark:bg-blue-900/20 rounded-lg p-6 border border-blue-200 dark:border-blue-800">
            <h3 className="text-lg font-semibold text-blue-900 dark:text-blue-300 mb-4">
              System Requirements
            </h3>
            <ul className="space-y-2 text-blue-800 dark:text-blue-200">
              <li>• Docker 20.10+ and Docker Compose 2.0+</li>
              <li>• 2 GB RAM minimum (4 GB recommended)</li>
              <li>• 10 GB disk space</li>
              <li>• Linux, macOS, or Windows with WSL2</li>
            </ul>
          </div>

          {/* Need Help? */}
          <div className="text-center mt-12">
            <p className="text-gray-600 dark:text-gray-400 mb-4">
              Need help? Join our community or check the documentation.
            </p>
            <div className="flex flex-wrap justify-center gap-4">
              <Button variant="outline" asChild>
                <a href="https://discord.gg/pullwise" target="_blank" rel="noopener noreferrer">
                  Join Discord
                </a>
              </Button>
              <Button variant="outline" asChild>
                <a href="https://docs.pullwise.ai" target="_blank" rel="noopener noreferrer">
                  Documentation
                </a>
              </Button>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
