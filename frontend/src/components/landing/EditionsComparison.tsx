import { Check, X } from 'lucide-react'
import { useLanguage } from '@/components/language-provider'
import type { Language } from '@/lib/translations'

// Edition data (shared structure, names only need i18n)
const editionsData = [
  { key: 'ce', color: 'bg-green-50 dark:bg-green-900/30 border-green-200 dark:border-green-800' },
  { key: 'pro', color: 'bg-blue-50 dark:bg-blue-900/30 border-blue-200 dark:border-blue-800' },
  { key: 'ee', color: 'bg-purple-50 dark:bg-purple-900/30 border-purple-200 dark:border-purple-800' },
  { key: 'eep', color: 'bg-orange-50 dark:bg-orange-900/30 border-orange-200 dark:border-orange-800' },
] as const

// Features comparison data (values are localized in translations)
const featuresTemplate = [
  { nameKey: 'users' },
  { nameKey: 'organizations' },
  { nameKey: 'license' },
  { nameKey: 'selfHosted', type: 'boolean' as const },
  { nameKey: 'pipeline' },
  { nameKey: 'codeGraph', type: 'boolean' as const },
  { nameKey: 'ssoSaml', type: 'boolean' as const },
  { nameKey: 'rbac' },
  { nameKey: 'auditLogs' },
  { nameKey: 'airGapped', type: 'boolean' as const },
  { nameKey: 'sla' },
  { nameKey: 'csm', type: 'boolean' as const },
  { nameKey: 'sourceAccess' },
] as const

type FeatureNameKey = typeof featuresTemplate[number]['nameKey']

const featureNames: Record<Language, Record<FeatureNameKey, string>> = {
  en: {
    users: 'Users', organizations: 'Organizations', license: 'License', selfHosted: 'Self-Hosted',
    pipeline: 'Pipeline', codeGraph: 'Code Graph', ssoSaml: 'SSO/SAML', rbac: 'RBAC',
    auditLogs: 'Audit Logs', airGapped: 'Air-Gapped', sla: 'SLA', csm: 'CSM', sourceAccess: 'Source Access',
  },
  pt: {
    users: 'Usuários', organizations: 'Organizações', license: 'Licença', selfHosted: 'Self-Hosted',
    pipeline: 'Pipeline', codeGraph: 'Grafo de Código', ssoSaml: 'SSO/SAML', rbac: 'RBAC',
    auditLogs: 'Audit Logs', airGapped: 'Air-Gapped', sla: 'SLA', csm: 'CSM', sourceAccess: 'Acesso ao Source',
  },
  es: {
    users: 'Usuarios', organizations: 'Organizaciones', license: 'Licencia', selfHosted: 'Self-Hosted',
    pipeline: 'Pipeline', codeGraph: 'Grafo de Código', ssoSaml: 'SSO/SAML', rbac: 'RBAC',
    auditLogs: 'Audit Logs', airGapped: 'Air-Gapped', sla: 'SLA', csm: 'CSM', sourceAccess: 'Acceso al Source',
  },
}

const featureValues: Record<Language, Record<FeatureNameKey, Record<'ce' | 'pro' | 'ee' | 'eep', string | boolean>>> = {
  en: {
    users: { ce: '5', pro: '50', ee: 'Unlimited', eep: 'Unlimited' },
    organizations: { ce: '1', pro: '3', ee: 'Unlimited', eep: 'Unlimited' },
    license: { ce: 'MIT', pro: 'Proprietary', ee: 'Proprietary', eep: 'Proprietary' },
    selfHosted: { ce: true, pro: true, ee: true, eep: true },
    pipeline: { ce: '2-pass', pro: '4-pass', ee: '4-pass', eep: '4-pass' },
    codeGraph: { ce: false, pro: true, ee: true, eep: true },
    ssoSaml: { ce: false, pro: true, ee: true, eep: true },
    rbac: { ce: false, pro: 'Basic', ee: 'Advanced', eep: 'Advanced' },
    auditLogs: { ce: false, pro: '30 days', ee: '1 year', eep: 'Custom' },
    airGapped: { ce: false, pro: false, ee: true, eep: true },
    sla: { ce: 'Community', pro: '48h', ee: '4h', eep: '1h' },
    csm: { ce: false, pro: false, ee: false, eep: true },
    sourceAccess: { ce: 'Core', pro: false, ee: false, eep: true },
  },
  pt: {
    users: { ce: '5', pro: '50', ee: 'Ilimitados', eep: 'Ilimitados' },
    organizations: { ce: '1', pro: '3', ee: 'Ilimitadas', eep: 'Ilimitadas' },
    license: { ce: 'MIT', pro: 'Proprietária', ee: 'Proprietária', eep: 'Proprietária' },
    selfHosted: { ce: true, pro: true, ee: true, eep: true },
    pipeline: { ce: '2-pass', pro: '4-pass', ee: '4-pass', eep: '4-pass' },
    codeGraph: { ce: false, pro: true, ee: true, eep: true },
    ssoSaml: { ce: false, pro: true, ee: true, eep: true },
    rbac: { ce: false, pro: 'Básico', ee: 'Avançado', eep: 'Avançado' },
    auditLogs: { ce: false, pro: '30 dias', ee: '1 ano', eep: 'Custom' },
    airGapped: { ce: false, pro: false, ee: true, eep: true },
    sla: { ce: 'Comunidade', pro: '48h', ee: '4h', eep: '1h' },
    csm: { ce: false, pro: false, ee: false, eep: true },
    sourceAccess: { ce: 'Core', pro: false, ee: false, eep: true },
  },
  es: {
    users: { ce: '5', pro: '50', ee: 'Ilimitados', eep: 'Ilimitados' },
    organizations: { ce: '1', pro: '3', ee: 'Ilimitadas', eep: 'Ilimitadas' },
    license: { ce: 'MIT', pro: 'Propietaria', ee: 'Propietaria', eep: 'Propietaria' },
    selfHosted: { ce: true, pro: true, ee: true, eep: true },
    pipeline: { ce: '2-pass', pro: '4-pass', ee: '4-pass', eep: '4-pass' },
    codeGraph: { ce: false, pro: true, ee: true, eep: true },
    ssoSaml: { ce: false, pro: true, ee: true, eep: true },
    rbac: { ce: false, pro: 'Básico', ee: 'Avanzado', eep: 'Avanzado' },
    auditLogs: { ce: false, pro: '30 días', ee: '1 año', eep: 'Custom' },
    airGapped: { ce: false, pro: false, ee: true, eep: true },
    sla: { ce: 'Comunidad', pro: '48h', ee: '4h', eep: '1h' },
    csm: { ce: false, pro: false, ee: false, eep: true },
    sourceAccess: { ce: 'Core', pro: false, ee: false, eep: true },
  },
}

const editionNames: Record<Language, Record<'ce' | 'pro' | 'ee' | 'eep', string>> = {
  en: { ce: 'Community Edition', pro: 'Professional', ee: 'Enterprise', eep: 'Enterprise Plus' },
  pt: { ce: 'Community Edition', pro: 'Professional', ee: 'Enterprise', eep: 'Enterprise Plus' },
  es: { ce: 'Community Edition', pro: 'Professional', ee: 'Enterprise', eep: 'Enterprise Plus' },
}

/**
 * EditionsComparison component
 * Displays a 4-column comparison table of all editions (CE/Pro/EE/EE+)
 * Part of the Community-First landing page strategy
 */
export default function EditionsComparison() {
  const { t, language } = useLanguage()
  const comparison = t.editionsComparison

  return (
    <section id="comparison" className="py-24 bg-gray-50 dark:bg-gray-900">
      <div className="container mx-auto px-6">
        <div className="text-center mb-16">
          <h2 className="text-4xl font-bold text-gray-900 dark:text-white mb-4">
            {comparison.headline}
          </h2>
          <p className="text-xl text-gray-600 dark:text-gray-300">
            {comparison.subheadline}
          </p>
        </div>

        <div className="max-w-6xl mx-auto overflow-x-auto">
          <table className="w-full bg-white dark:bg-gray-800 rounded-xl overflow-hidden shadow-sm border border-gray-200 dark:border-gray-700">
            <thead>
              <tr className="border-b border-gray-200 dark:border-gray-700">
                <th className="text-left p-4 font-semibold text-gray-900 dark:text-white bg-gray-50 dark:bg-gray-900">
                  Feature
                </th>
                {editionsData.map((edition) => (
                  <th
                    key={edition.key}
                    className={`p-4 font-semibold text-gray-900 dark:text-white ${edition.color}`}
                  >
                    {editionNames[language][edition.key]}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {featuresTemplate.map((feature) => {
                const isBoolean = 'type' in feature && feature.type === 'boolean'
                return (
                  <tr key={feature.nameKey} className="border-b border-gray-100 dark:border-gray-700">
                    <td className="p-4 font-medium text-gray-900 dark:text-white">{featureNames[language][feature.nameKey]}</td>
                    {editionsData.map((edition) => {
                      const value = featureValues[language][feature.nameKey][edition.key]
                      const valueIsBoolean = isBoolean || typeof value === 'boolean'
                      return (
                        <td key={edition.key} className={`p-4 text-center ${edition.color}`}>
                          {valueIsBoolean ? (
                            value ? (
                              <Check className="h-5 w-5 text-green-500 mx-auto" />
                            ) : (
                              <X className="h-5 w-5 text-gray-300 dark:text-gray-600 mx-auto" />
                            )
                          ) : (
                            <span className="text-gray-700 dark:text-gray-300">{value as string | boolean}</span>
                          )}
                        </td>
                      )
                    })}
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      </div>
    </section>
  )
}
