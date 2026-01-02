/**
 * Integrations component
 * Displays available platform integrations
 */
export default function Integrations() {
  const categories = [
    {
      title: "Version Control",
      items: [
        { name: "GitHub", desc: "App + Webhook" },
        { name: "BitBucket", desc: "Cloud + Server" },
        { name: "GitLab", desc: "Coming soon" },
      ],
    },
    {
      title: "SAST Tools",
      items: [
        { name: "SonarQube", desc: "Full integration" },
        { name: "Checkstyle", desc: "Java style rules" },
        { name: "PMD", desc: "Anti-patterns" },
        { name: "SpotBugs", desc: "Bug detection" },
        { name: "ESLint", desc: "JavaScript/TypeScript" },
      ],
    },
    {
      title: "AI Providers",
      items: [
        { name: "OpenRouter", desc: "GPT-4o, Claude Sonnet 4.5" },
        { name: "Ollama", desc: "Local deployment" },
        { name: "Custom", desc: "Enterprise models" },
      ],
    },
    {
      title: "Security",
      items: [
        { name: "OAuth2", desc: "GitHub, BitBucket" },
        { name: "SAML SSO", desc: "Enterprise" },
        { name: "On-premise", desc: "Enterprise deployment" },
      ],
    },
  ]

  return (
    <section className="py-24 bg-white dark:bg-gray-950">
      <div className="container mx-auto px-6">
        <div className="max-w-3xl mx-auto text-center mb-16">
          <h2 className="text-4xl font-bold text-gray-900 dark:text-white mb-4">
            Integrations
          </h2>
          <p className="text-xl text-gray-600 dark:text-gray-300">
            Connect with the tools you already use
          </p>
        </div>

        <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-8 max-w-6xl mx-auto">
          {categories.map((category) => (
            <div key={category.title} className="bg-gray-50 dark:bg-gray-800 rounded-xl p-6">
              <h3 className="font-bold text-gray-900 dark:text-white mb-4">{category.title}</h3>
              <ul className="space-y-3">
                {category.items.map((item) => (
                  <li key={item.name}>
                    <div className="font-medium text-gray-900 dark:text-white">{item.name}</div>
                    <div className="text-sm text-gray-500 dark:text-gray-400">{item.desc}</div>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}
