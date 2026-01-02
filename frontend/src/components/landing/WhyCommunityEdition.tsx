import { Unlock, Home, CheckCircle, Plug, Zap } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Link } from 'react-router-dom'
import { useLanguage } from '@/components/language-provider'

/**
 * WhyCommunityEdition component
 * Highlights the benefits of Community Edition (MIT licensed)
 * Part of the Community-First landing page strategy
 */
export default function WhyCommunityEdition() {
  const { t } = useLanguage()
  const why = t.whyCommunity

  const benefitKeys = ['mit', 'selfHosted', 'coreComplete', 'plugins', 'setup'] as const
  const icons = [Unlock, Home, CheckCircle, Plug, Zap]

  return (
    <section className="py-24 bg-white dark:bg-gray-950">
      <div className="container mx-auto px-6">
        <div className="text-center mb-16">
          <h2 className="text-4xl font-bold text-gray-900 dark:text-white mb-4">
            {why.headline}
          </h2>
          <p className="text-xl text-gray-600 dark:text-gray-300 max-w-2xl mx-auto">
            {why.subheadline}
          </p>
        </div>

        <div className="grid md:grid-cols-3 lg:grid-cols-5 gap-8 max-w-6xl mx-auto mb-12">
          {benefitKeys.map((key, index) => {
            const benefit = why.benefits[key]
            const Icon = icons[index]
            return (
              <div
                key={key}
                className="text-center p-6 bg-gray-50 dark:bg-gray-900 rounded-xl border border-gray-200 dark:border-gray-800"
              >
                <div className="p-3 bg-purple-100 dark:bg-purple-900/30 rounded-lg inline-block mb-4">
                  <Icon className="h-6 w-6 text-purple-600 dark:text-purple-400" />
                </div>
                <h3 className="font-semibold text-gray-900 dark:text-white mb-2">{benefit.title}</h3>
                <p className="text-sm text-gray-600 dark:text-gray-400">{benefit.description}</p>
              </div>
            )
          })}
        </div>

        <div className="text-center">
          <Button
            size="lg"
            variant="default"
            className="!bg-purple-600 hover:!bg-purple-700 dark:!bg-purple-500 dark:hover:!bg-purple-600"
            asChild
          >
            <Link to="/download">{why.cta}</Link>
          </Button>
          <p className="text-sm text-gray-600 dark:text-gray-400 mt-4">
            {why.trust}
          </p>
        </div>
      </div>
    </section>
  )
}
