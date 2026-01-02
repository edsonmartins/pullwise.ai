import { Button } from "@/components/ui/button"
import { Check } from "lucide-react"
import { Link } from "react-router-dom"
import { useLanguage } from "@/components/language-provider"

// Plan highlight configuration
const planConfig = {
  ce: { highlighted: true, highlightColor: 'ring-green-500 dark:ring-green-400' },
  pro: { highlighted: false },
  ee: { highlighted: true, highlightColor: 'ring-purple-600 dark:ring-purple-500' },
  eep: { highlighted: false },
} as const

/**
 * Pricing component
 * Displays 4 pricing tiers following GitLab-style model
 * Community Edition (CE) - Free, Professional ($49), Enterprise ($99), Enterprise Plus ($149)
 */
export default function Pricing() {
  const { t } = useLanguage()
  const pricing = t.pricing

  const planKeys = ['ce', 'pro', 'ee', 'eep'] as const

  return (
    <section id="pricing" className="py-24 bg-white dark:bg-gray-950">
      <div className="container mx-auto px-6">
        <div className="text-center mb-16">
          <h2 className="text-4xl font-bold text-gray-900 dark:text-white mb-4">
            {pricing.headline}
          </h2>
          <p className="text-xl text-gray-600 dark:text-gray-300 max-w-2xl mx-auto">
            {pricing.subheadline}
          </p>
        </div>

        <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-6 max-w-7xl mx-auto">
          {planKeys.map((key) => {
            const plan = pricing.plans[key]
            const config = planConfig[key]
            const hasBadge = 'badge' in plan && plan.badge
            const hasShortName = 'shortName' in plan && plan.shortName
            return (
              <div
                key={key}
                className={`rounded-xl p-6 flex flex-col ${
                  config.highlighted
                    ? key === 'ee'
                      ? 'bg-purple-600 dark:bg-purple-500 text-white ring-4 ring-purple-600 dark:ring-purple-500 ring-offset-4 ring-offset-white dark:ring-offset-gray-950 shadow-xl'
                      : 'bg-white dark:bg-gray-800 ring-4 ring-green-500 dark:ring-green-400 ring-offset-4 ring-offset-white dark:ring-offset-gray-950'
                    : 'bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700'
                }`}
              >
                {hasBadge && (
                  <div className={`inline-block px-3 py-1 ${
                    key === 'ce'
                      ? 'bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-300'
                      : 'bg-purple-100 dark:bg-purple-900/30 text-purple-700 dark:text-purple-300'
                  } rounded-full text-sm font-semibold mb-4`}>
                    {plan.badge}
                  </div>
                )}

                <h3
                  className={`text-xl font-bold mb-2 ${
                    config.highlighted && key !== 'ee' ? 'text-green-600 dark:text-green-400' : ''
                  }`}
                >
                  {hasShortName ? plan.shortName : plan.name}
                </h3>

                <div className="mb-4">
                  <span
                    className={`text-4xl font-bold ${
                      config.highlighted && key === 'ee' ? 'text-white' : 'text-gray-900 dark:text-white'
                    }`}
                  >
                    {plan.price}
                  </span>
                  <span
                    className={`text-sm ml-2 ${
                      config.highlighted && key === 'ee' ? 'text-purple-200' : 'text-gray-600 dark:text-gray-400'
                    }`}
                  >
                    {plan.period}
                  </span>
                </div>

                <p
                  className={`mb-6 text-sm ${
                    config.highlighted && key === 'ee' ? 'text-purple-100' : 'text-gray-600 dark:text-gray-400'
                  }`}
                >
                  {plan.description}
                </p>

                <Button
                  className={`w-full mb-6 ${
                    config.highlighted && key === 'ee'
                      ? 'bg-white text-purple-600 hover:bg-purple-50'
                      : config.highlighted
                      ? 'bg-green-600 dark:bg-green-500 text-white hover:bg-green-700 dark:hover:bg-green-600'
                      : ''
                  }`}
                  variant={config.highlighted ? 'default' : 'outline'}
                  asChild
                >
                  <Link to={key === 'ce' ? '/download' : key === 'pro' ? '/trial' : '/contact-sales'}>{plan.cta}</Link>
                </Button>

                <ul className="space-y-2 flex-grow">
                  {plan.features.map((feature, i) => (
                    <li key={i} className="flex items-start gap-2 text-sm">
                      <Check
                        className={`h-4 w-4 mt-0.5 flex-shrink-0 ${
                          config.highlighted && key === 'ee'
                            ? 'text-purple-200'
                            : config.highlighted
                            ? 'text-green-500'
                            : 'text-gray-400'
                        }`}
                      />
                      <span
                        className={
                          config.highlighted && key === 'ee' ? 'text-purple-50' : 'text-gray-700 dark:text-gray-300'
                        }
                      >
                        {feature}
                      </span>
                    </li>
                  ))}
                </ul>
              </div>
            )
          })}
        </div>

        <p className="text-center text-gray-600 dark:text-gray-400 mt-8">
          {pricing.footer}{' '}
          <Link to="/contact" className="text-purple-600 dark:text-purple-400 hover:underline">
            {pricing.contactLink}
          </Link>
        </p>
      </div>
    </section>
  )
}
