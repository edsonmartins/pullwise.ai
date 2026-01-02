import { Star, Container, Plug, Users } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { useLanguage } from '@/components/language-provider'
import type { Language } from '@/lib/translations'

// Featured plugins data (same across languages, only names need i18n)
const pluginsData = [
  { nameKey: 'eslint', author: '@community-dev', downloads: '5K+' },
  { nameKey: 'security', author: '@security-guru', downloads: '3K+' },
  { nameKey: 'java', author: '@java-expert', downloads: '2K+' },
] as const

const pluginNames: Record<Language, Record<typeof pluginsData[number]['nameKey'], string>> = {
  en: { eslint: 'ESLint Custom Rules', security: 'Security Patterns', java: 'Java Best Practices' },
  pt: { eslint: 'Regras ESLint Customizadas', security: 'Padrões de Segurança', java: 'Melhores Práticas Java' },
  es: { eslint: 'Reglas ESLint Personalizadas', security: 'Patrones de Seguridad', java: 'Mejores Prácticas Java' },
}

/**
 * CommunityShowcase component
 * Displays community statistics and featured plugins
 * Part of the Community-First landing page strategy
 */
export default function CommunityShowcase() {
  const { t, language } = useLanguage()
  const showcase = t.communityShowcase

  const stats = [
    { icon: Star, value: '5,000+', label: showcase.stats.stars, color: 'text-yellow-500' },
    { icon: Container, value: '10,000+', label: showcase.stats.pulls, color: 'text-blue-500' },
    { icon: Plug, value: '200+', label: showcase.stats.plugins, color: 'text-purple-500' },
    { icon: Users, value: '1,000+', label: showcase.stats.members, color: 'text-indigo-500' },
  ]

  const by: Record<Language, string> = {
    en: 'by',
    pt: 'por',
    es: 'por',
  }

  const downloadsLabel: Record<Language, string> = {
    en: 'downloads',
    pt: 'downloads',
    es: 'descargas',
  }

  return (
    <section className="py-16 bg-purple-600 dark:bg-purple-950 text-white">
      <div className="container mx-auto px-6">
        <div className="text-center mb-12">
          <h2 className="text-4xl font-bold mb-4">
            {showcase.headline}
          </h2>
          <p className="text-xl text-purple-100 dark:text-purple-200">
            {showcase.subheadline}
          </p>
        </div>

        {/* Stats */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-8 max-w-4xl mx-auto mb-12">
          {stats.map((stat) => (
            <div key={stat.label} className="text-center">
              <stat.icon className={`h-8 w-8 mx-auto mb-2 ${stat.color}`} />
              <div className="text-3xl font-bold">{stat.value}</div>
              <div className="text-purple-100 dark:text-purple-200">{stat.label}</div>
            </div>
          ))}
        </div>

        {/* Featured Plugins */}
        <div className="max-w-4xl mx-auto mb-12">
          <h3 className="text-xl font-semibold mb-6 text-center">
            {showcase.featuredPlugins}
          </h3>
          <div className="grid md:grid-cols-3 gap-6">
            {pluginsData.map((plugin) => (
              <div
                key={plugin.nameKey}
                className="bg-purple-500/30 dark:bg-purple-900/30 backdrop-blur rounded-lg p-6 border border-purple-400/30 dark:border-purple-700"
              >
                <div className="text-2xl mb-3">🔌</div>
                <h4 className="font-semibold mb-1">{pluginNames[language][plugin.nameKey] || plugin.nameKey}</h4>
                <p className="text-sm text-purple-100 dark:text-purple-200 mb-2">{by[language]} {plugin.author}</p>
                <p className="text-sm text-purple-200 dark:text-purple-300">{plugin.downloads} {downloadsLabel[language]}</p>
              </div>
            ))}
          </div>
        </div>

        {/* CTA */}
        <div className="text-center">
          <Button
            variant="outline"
            className="border-white !bg-white !text-purple-600 hover:!bg-purple-50 hover:!text-purple-700 dark:!bg-transparent dark:!text-white dark:hover:!bg-white/10 dark:hover:!text-white"
            asChild
          >
            <a href="https://discord.gg/pullwise" target="_blank" rel="noopener noreferrer">
              {showcase.cta}
            </a>
          </Button>
        </div>
      </div>
    </section>
  )
}
