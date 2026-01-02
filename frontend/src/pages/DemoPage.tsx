import { useState } from "react"
import { Button } from "@/components/ui/button"
import { ArrowRight, Check, Calendar, Users, Building2, Mail, MessageSquare } from "lucide-react"
import { Link } from "react-router-dom"

/**
 * DemoPage component
 * Captures leads for Enterprise demo bookings
 */
export default function DemoPage() {
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    company: '',
    companySize: '',
    useCase: '',
    message: '',
  })
  const [submitted, setSubmitted] = useState(false)

  const companySizes = [
    '1-10 developers',
    '11-50 developers',
    '51-200 developers',
    '201-500 developers',
    '500+ developers',
  ]

  const useCases = [
    'Improve code review quality',
    'Reduce review time',
    'Compliance & security requirements',
    'On-premise / air-gapped deployment',
    'Custom LLM integration',
    'Other',
  ]

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
    setFormData(prev => ({ ...prev, [e.target.name]: e.target.value }))
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    // Here you would typically send the data to your backend
    console.log('Demo request:', formData)
    setSubmitted(true)
  }

  const benefits = [
    {
      icon: Users,
      title: 'Expert Guidance',
      description: 'Walkthrough of features tailored to your team\'s needs',
    },
    {
      icon: Building2,
      title: 'Enterprise Deep Dive',
      description: 'See SOC2, SSO, and air-gapped deployment options',
    },
    {
      icon: Calendar,
      title: 'Flexible Scheduling',
      description: '30-minute session at your convenience',
    },
  ]

  if (submitted) {
    return (
      <div className="min-h-screen bg-gradient-to-b from-purple-50 to-white dark:from-gray-900 dark:to-gray-950 flex items-center justify-center">
        <div className="container mx-auto px-6 py-12">
          <div className="max-w-2xl mx-auto text-center">
            <div className="w-20 h-20 bg-green-100 dark:bg-green-900/30 rounded-full flex items-center justify-center mx-auto mb-6">
              <Check className="h-10 w-10 text-green-600 dark:text-green-400" />
            </div>
            <h1 className="text-4xl font-bold text-gray-900 dark:text-white mb-4">
              Thank You!
            </h1>
            <p className="text-xl text-gray-600 dark:text-gray-300 mb-8">
              We've received your demo request. A member of our team will reach out within 24 hours.
            </p>
            <div className="flex flex-wrap justify-center gap-4">
              <Button size="lg" className="bg-purple-600 hover:bg-purple-700" asChild>
                <Link to="/landing">Back to Home</Link>
              </Button>
              <Button size="lg" variant="outline" asChild>
                <Link to="/download">Download Community Edition</Link>
              </Button>
            </div>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gradient-to-b from-purple-50 to-white dark:from-gray-900 dark:to-gray-950">
      {/* Header */}
      <nav className="container mx-auto px-6 py-6">
        <Link to="/landing" className="flex items-center gap-2">
          <span className="text-3xl">🦉</span>
          <span className="text-2xl font-bold text-gray-900 dark:text-white">Pullwise</span>
        </Link>
      </nav>

      <div className="container mx-auto px-6 py-12">
        <div className="max-w-6xl mx-auto">
          <div className="grid lg:grid-cols-2 gap-12">
            {/* Left Column - Content */}
            <div>
              <h1 className="text-4xl md:text-5xl font-bold text-gray-900 dark:text-white mb-6">
                Book an Enterprise Demo
              </h1>
              <p className="text-xl text-gray-600 dark:text-gray-300 mb-8">
                See how Pullwise Enterprise can transform your code review process with AI-powered automation.
              </p>

              {/* Benefits */}
              <div className="space-y-6 mb-8">
                {benefits.map((benefit) => (
                  <div key={benefit.title} className="flex gap-4">
                    <div className="p-3 bg-purple-100 dark:bg-purple-900/30 rounded-lg">
                      <benefit.icon className="h-6 w-6 text-purple-600 dark:text-purple-400" />
                    </div>
                    <div>
                      <h3 className="font-semibold text-gray-900 dark:text-white mb-1">
                        {benefit.title}
                      </h3>
                      <p className="text-gray-600 dark:text-gray-400">
                        {benefit.description}
                      </p>
                    </div>
                  </div>
                ))}
              </div>

              {/* Alternative Contact */}
              <div className="bg-gray-100 dark:bg-gray-800 rounded-lg p-6">
                <h3 className="font-semibold text-gray-900 dark:text-white mb-4">
                  Prefer to reach out directly?
                </h3>
                <div className="space-y-3">
                  <a href="mailto:enterprise@pullwise.ai" className="flex items-center gap-3 text-gray-600 dark:text-gray-400 hover:text-purple-600 dark:hover:text-purple-400">
                    <Mail className="h-5 w-5" />
                    enterprise@pullwise.ai
                  </a>
                  <a href="https://discord.gg/pullwise" target="_blank" rel="noopener noreferrer" className="flex items-center gap-3 text-gray-600 dark:text-gray-400 hover:text-purple-600 dark:hover:text-purple-400">
                    <MessageSquare className="h-5 w-5" />
                    Join our Discord
                  </a>
                </div>
              </div>
            </div>

            {/* Right Column - Form */}
            <div className="bg-white dark:bg-gray-800 rounded-xl shadow-lg p-8 border border-gray-200 dark:border-gray-700">
              <h2 className="text-2xl font-bold text-gray-900 dark:text-white mb-6">
                Request Your Demo
              </h2>

              <form onSubmit={handleSubmit} className="space-y-6">
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                    Full Name *
                  </label>
                  <input
                    type="text"
                    name="name"
                    required
                    value={formData.name}
                    onChange={handleChange}
                    className="w-full px-4 py-3 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-purple-500 focus:border-transparent"
                    placeholder="John Doe"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                    Work Email *
                  </label>
                  <input
                    type="email"
                    name="email"
                    required
                    value={formData.email}
                    onChange={handleChange}
                    className="w-full px-4 py-3 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-purple-500 focus:border-transparent"
                    placeholder="john@company.com"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                    Company *
                  </label>
                  <input
                    type="text"
                    name="company"
                    required
                    value={formData.company}
                    onChange={handleChange}
                    className="w-full px-4 py-3 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-purple-500 focus:border-transparent"
                    placeholder="Acme Corp"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                    Company Size *
                  </label>
                  <select
                    name="companySize"
                    required
                    value={formData.companySize}
                    onChange={handleChange}
                    className="w-full px-4 py-3 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-purple-500 focus:border-transparent"
                  >
                    <option value="">Select size...</option>
                    {companySizes.map((size) => (
                      <option key={size} value={size}>{size}</option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                    Primary Use Case *
                  </label>
                  <select
                    name="useCase"
                    required
                    value={formData.useCase}
                    onChange={handleChange}
                    className="w-full px-4 py-3 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-purple-500 focus:border-transparent"
                  >
                    <option value="">Select use case...</option>
                    {useCases.map((useCase) => (
                      <option key={useCase} value={useCase}>{useCase}</option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                    Message (Optional)
                  </label>
                  <textarea
                    name="message"
                    rows={4}
                    value={formData.message}
                    onChange={handleChange}
                    className="w-full px-4 py-3 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-purple-500 focus:border-transparent"
                    placeholder="Tell us about your current code review process..."
                  />
                </div>

                <Button
                  type="submit"
                  size="lg"
                  className="w-full bg-purple-600 hover:bg-purple-700"
                >
                  Request Demo
                  <ArrowRight className="ml-2 h-5 w-5" />
                </Button>

                <p className="text-xs text-gray-500 dark:text-gray-400 text-center">
                  By submitting, you agree to our privacy policy. We'll respond within 24 hours.
                </p>
              </form>
            </div>
          </div>

          {/* Also Available - Community Edition */}
          <div className="mt-16 text-center bg-green-50 dark:bg-green-900/20 rounded-lg p-8 border border-green-200 dark:border-green-800">
            <h3 className="text-2xl font-bold text-gray-900 dark:text-white mb-4">
              Not ready for Enterprise? Start with Community Edition!
            </h3>
            <p className="text-gray-600 dark:text-gray-400 mb-6 max-w-2xl mx-auto">
              Get started with our free, MIT-licensed Community Edition. Self-hosted, production-ready, and forever free.
            </p>
            <Button size="lg" variant="outline" className="border-green-600 text-green-600 hover:bg-green-50" asChild>
              <Link to="/download">
                Download Community Edition
                <ArrowRight className="ml-2 h-5 w-5" />
              </Link>
            </Button>
          </div>
        </div>
      </div>
    </div>
  )
}
