import { Button } from "@/components/ui/button"
import { ArrowRight } from "lucide-react"
import { Link } from "react-router-dom"
import { useLanguage } from "@/components/language-provider"

/**
 * FinalCTA component
 * Final call-to-action section for Community-First landing page
 */
export default function FinalCTA() {
  const { t } = useLanguage()
  const finalCta = t.finalCta

  return (
    <section className="py-24 bg-gradient-to-r from-purple-600 to-blue-600 dark:from-purple-700 dark:to-blue-700">
      <div className="container mx-auto px-6 text-center">
        <div className="max-w-3xl mx-auto">
          <h2 className="text-4xl md:text-5xl font-bold text-white mb-6">
            {finalCta.headline}
          </h2>
          <p className="text-xl text-purple-100 dark:text-purple-200 mb-10">
            {finalCta.subheadline}
          </p>

          <div className="flex flex-col sm:flex-row items-center justify-center gap-4 mb-8">
            <Button
              size="lg"
              variant="secondary"
              className="text-lg px-8"
              asChild
            >
              <Link to="/download">
                {finalCta.ctaPrimary}
                <ArrowRight className="ml-2 h-5 w-5" />
              </Link>
            </Button>
            <Button
              size="lg"
              variant="outline"
              className="text-lg px-8 border-white text-white hover:bg-white/10"
              asChild
            >
              <Link to="/demo">
                {finalCta.ctaSecondary}
              </Link>
            </Button>
          </div>

          <div className="flex flex-wrap items-center justify-center gap-6 text-sm text-purple-100 dark:text-purple-200">
            <span>{finalCta.trust1}</span>
            <span>{finalCta.trust2}</span>
            <span>{finalCta.trust3}</span>
          </div>
        </div>
      </div>
    </section>
  )
}
