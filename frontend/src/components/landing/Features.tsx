import { Unlock, Home, Bot, TrendingUp, Building, Plug } from "lucide-react"
import { useLanguage } from "@/components/language-provider"

/**
 * Features component
 * Displays 6 key features in 3x2 grid layout
 * Part of the Community-First landing page strategy
 */
export default function Features() {
  const { t } = useLanguage()
  const features = t.features

  const featureKeys = ['openSource', 'selfHosted', 'aiPowered', 'scale', 'enterprise', 'extensible'] as const
  const icons = [Unlock, Home, Bot, TrendingUp, Building, Plug]

  return (
    <section id="features" className="py-24 bg-gray-50 dark:bg-gray-900">
      <div className="container mx-auto px-6">
        <div className="text-center mb-16">
          <h2 className="text-4xl font-bold text-gray-900 dark:text-white mb-4">
            {features.headline}
          </h2>
          <p className="text-xl text-gray-600 dark:text-gray-300 max-w-2xl mx-auto">
            {features.subheadline}
          </p>
        </div>

        <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-8 max-w-6xl mx-auto">
          {featureKeys.map((key, index) => {
            const feature = features.items[key]
            const Icon = icons[index]
            return (
              <div
                key={key}
                className="bg-white dark:bg-gray-800 rounded-xl p-8 shadow-sm border border-gray-200 dark:border-gray-700"
              >
                <div className="flex items-center gap-3 mb-4">
                  <div className="p-3 bg-purple-100 dark:bg-purple-900/30 rounded-lg">
                    <Icon className="h-6 w-6 text-purple-600 dark:text-purple-400" />
                  </div>
                  <h3 className="text-lg font-bold text-gray-900 dark:text-white">
                    {feature.headline}
                  </h3>
                  {'badge' in feature && feature.badge && (
                    <span className="ml-auto px-2 py-1 bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-300 text-xs font-semibold rounded">
                      {feature.badge}
                    </span>
                  )}
                </div>

                <h4 className="text-base font-semibold text-gray-800 dark:text-gray-200 mb-3">
                  {feature.title}
                </h4>

                <p className="text-gray-600 dark:text-gray-400">{feature.description}</p>
              </div>
            )
          })}
        </div>
      </div>
    </section>
  )
}
