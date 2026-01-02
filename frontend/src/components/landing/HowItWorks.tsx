import { Download, Link as LinkIcon, CheckCircle, Zap } from 'lucide-react'
import { useLanguage } from '@/components/language-provider'

/**
 * HowItWorks component
 * Shows the 4-step process from PR to approval
 * Part of the Community-First landing page strategy
 */
export default function HowItWorks() {
  const { t } = useLanguage()
  const howItWorks = t.howItWorks

  const stepKeys = ['step1', 'step2', 'step3', 'step4'] as const
  const icons = [Download, LinkIcon, CheckCircle, Zap]

  return (
    <section className="py-24 bg-gray-50 dark:bg-gray-900">
      <div className="container mx-auto px-6">
        <div className="max-w-3xl mx-auto text-center mb-16">
          <h2 className="text-4xl font-bold text-gray-900 dark:text-white mb-4">
            {howItWorks.headline}
          </h2>
          <p className="text-xl text-gray-600 dark:text-gray-300">
            {howItWorks.subheadline}
          </p>
        </div>

        <div className="max-w-4xl mx-auto">
          <div className="grid md:grid-cols-2 gap-6">
            {stepKeys.map((key, index) => {
              const step = howItWorks.steps[key]
              const Icon = icons[index]
              return (
                <div key={key} className="bg-white dark:bg-gray-800 rounded-xl p-6 border border-gray-200 dark:border-gray-700">
                  <div className="flex items-start gap-4">
                    <div className="p-3 bg-purple-100 dark:bg-purple-900/30 rounded-lg">
                      <Icon className="h-6 w-6 text-purple-600 dark:text-purple-400" />
                    </div>
                    <div className="flex-1">
                      <span className="text-sm font-semibold text-purple-600 dark:text-purple-400">{step.label}</span>
                      <h3 className="text-lg font-bold text-gray-900 dark:text-white mt-1 mb-2">{step.title}</h3>
                      <p className="text-gray-600 dark:text-gray-400">{step.description}</p>
                    </div>
                  </div>
                </div>
              )
            })}
          </div>
        </div>
      </div>
    </section>
  )
}
