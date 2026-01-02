import Hero from "@/components/landing/Hero"
import CommunityShowcase from "@/components/landing/CommunityShowcase"
import WhyCommunityEdition from "@/components/landing/WhyCommunityEdition"
import Features from "@/components/landing/Features"
import HowItWorks from "@/components/landing/HowItWorks"
import EditionsComparison from "@/components/landing/EditionsComparison"
import Integrations from "@/components/landing/Integrations"
import Pricing from "@/components/landing/Pricing"
import FAQ from "@/components/landing/FAQ"
import FinalCTA from "@/components/landing/FinalCTA"
import Footer from "@/components/landing/Footer"

/**
 * LandingPage component
 * Community-First landing page with GitLab-style 4-tier pricing model
 */
export function LandingPage() {
  return (
    <div className="min-h-screen bg-white dark:bg-gray-950">
      <Hero />
      <CommunityShowcase />
      <WhyCommunityEdition />
      <Features />
      <HowItWorks />
      <EditionsComparison />
      <Integrations />
      <Pricing />
      <FAQ />
      <FinalCTA />
      <Footer />
    </div>
  )
}
