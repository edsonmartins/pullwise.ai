import { Button } from "@/components/ui/button"
import { ArrowRight, Play } from "lucide-react"
import { ThemeToggle } from "@/components/theme-toggle"
import { LanguageToggle } from "@/components/language-toggle"
import { Link } from "react-router-dom"
import { useLanguage } from "@/components/language-provider"

export default function Hero() {
  const { t } = useLanguage()

  return (
    <section className="relative overflow-hidden bg-gradient-to-b from-purple-50 to-white dark:from-gray-900 dark:to-gray-950 pt-4 pb-16 md:pt-6 md:pb-20">
      {/* Navigation */}
      <nav className="container mx-auto px-6 mb-6 md:mb-8">
        <div className="flex items-center justify-between">
          <Link to="/landing" className="flex items-center gap-2">
            <span className="text-3xl">🦉</span>
            <span className="text-2xl font-bold text-gray-900 dark:text-white">Pullwise</span>
            <span className="ml-2 px-2 py-1 bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-300 text-xs font-semibold rounded">
              MIT Licensed
            </span>
          </Link>
          <div className="hidden md:flex items-center gap-4">
            <a href="https://docs.pullwise.ai" target="_blank" rel="noopener noreferrer" className="text-gray-600 dark:text-gray-300 hover:text-gray-900 dark:hover:text-white transition-colors text-sm">
              Docs
            </a>
            <a href="https://github.com/integralltech/pullwise-ai" target="_blank" rel="noopener noreferrer" className="text-gray-600 dark:text-gray-300 hover:text-gray-900 dark:hover:text-white transition-colors text-sm">
              GitHub
            </a>
            <a href="#comparison" className="text-gray-600 dark:text-gray-300 hover:text-gray-900 dark:hover:text-white transition-colors text-sm">
              {t.nav.comparison}
            </a>
            <a href="#pricing" className="text-gray-600 dark:text-gray-300 hover:text-gray-900 dark:hover:text-white transition-colors text-sm">
              {t.nav.pricing}
            </a>
            <ThemeToggle />
            <LanguageToggle />
            <Link to="/login">
              <Button variant="ghost" className="dark:text-gray-200 text-sm h-9">{t.nav.signIn}</Button>
            </Link>
            <Link to="/download">
              <Button className="text-sm h-9 bg-purple-600 hover:bg-purple-700">{t.nav.startTrial}</Button>
            </Link>
          </div>
        </div>
      </nav>

      <div className="container mx-auto px-6">
        <div className="max-w-4xl mx-auto text-center">
          {/* Badge */}
          <div className="inline-flex items-center gap-2 px-4 py-2 bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-300 rounded-full mb-8">
            <span className="font-semibold">{t.hero.badge}</span>
            <span>{t.hero.badgeText}</span>
            <ArrowRight className="h-4 w-4" />
          </div>

          {/* Headline */}
          <h1 className="text-5xl md:text-6xl font-bold text-gray-900 dark:text-white mb-6 leading-tight">
            {t.hero.headline}{" "}
            <span className="text-purple-600 dark:text-purple-400">{t.hero.highlight}</span>
          </h1>

          {/* Subheadline */}
          <p className="text-xl text-gray-600 dark:text-gray-300 mb-10 max-w-2xl mx-auto">
            {t.hero.subheadline}
          </p>

          {/* CTAs */}
          <div className="flex flex-col sm:flex-row items-center justify-center gap-4 mb-8">
            <Link to="/download">
              <Button size="lg" className="text-lg px-8 bg-purple-600 hover:bg-purple-700">
                {t.hero.ctaPrimary}
                <ArrowRight className="ml-2 h-5 w-5" />
              </Button>
            </Link>
            <Link to="/demo">
              <Button size="lg" variant="outline" className="text-lg px-8 dark:border-gray-600 dark:text-gray-200">
                <Play className="mr-2 h-5 w-5" />
                {t.hero.ctaSecondary}
              </Button>
            </Link>
          </div>

          {/* Supporting text */}
          <div className="flex flex-wrap items-center justify-center gap-6 text-sm text-gray-600 dark:text-gray-400">
            <span className="flex items-center gap-1">{t.hero.trust1}</span>
            <span className="flex items-center gap-1">{t.hero.trust2}</span>
            <span className="flex items-center gap-1">{t.hero.trust3}</span>
          </div>
        </div>

        {/* Hero Image / Demo */}
        <div className="mt-16 max-w-6xl mx-auto">
          <div className="rounded-xl shadow-2xl overflow-hidden border border-gray-200 dark:border-gray-700">
            <div className="aspect-video bg-gradient-to-br from-purple-100 to-blue-100 dark:from-purple-900/30 dark:to-blue-900/30 flex items-center justify-center">
              <div className="text-center">
                <span className="text-8xl mb-4 block animate-float">🦉</span>
                <p className="text-gray-600 dark:text-gray-300 text-lg">{t.hero.demoTitle}</p>
                <p className="text-gray-500 dark:text-gray-400 text-sm mt-2">{t.hero.demoSubtitle}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}
