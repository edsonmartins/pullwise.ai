import { useLanguage } from "@/components/language-provider"
import logo from "@/assets/logo.png"

export default function Footer() {
  const { t } = useLanguage()

  return (
    <footer className="bg-gray-900 dark:bg-black text-gray-400 py-12">
      <div className="container mx-auto px-6">
        <div className="grid grid-cols-2 md:grid-cols-4 gap-8 mb-8">
          <div>
            <h3 className="text-white font-semibold mb-4">{t.footer.product}</h3>
            <ul className="space-y-2">
              <li><a href="#features" className="hover:text-white dark:hover:text-gray-200 transition-colors">{t.footer.features}</a></li>
              <li><a href="#pricing" className="hover:text-white dark:hover:text-gray-200 transition-colors">{t.footer.pricing}</a></li>
              <li><a href="/integrations" className="hover:text-white dark:hover:text-gray-200 transition-colors">{t.footer.integrations}</a></li>
              <li><a href="/changelog" className="hover:text-white dark:hover:text-gray-200 transition-colors">{t.footer.changelog}</a></li>
            </ul>
          </div>

          <div>
            <h3 className="text-white font-semibold mb-4">{t.footer.resources}</h3>
            <ul className="space-y-2">
              <li><a href="https://pullwise.dev/docs" className="hover:text-white dark:hover:text-gray-200 transition-colors">{t.footer.documentation}</a></li>
              <li><a href="https://pullwise.dev/api" className="hover:text-white dark:hover:text-gray-200 transition-colors">{t.footer.api}</a></li>
              <li><a href="/guides" className="hover:text-white dark:hover:text-gray-200 transition-colors">{t.footer.guides}</a></li>
              <li><a href="/blog" className="hover:text-white dark:hover:text-gray-200 transition-colors">{t.footer.blog}</a></li>
            </ul>
          </div>

          <div>
            <h3 className="text-white font-semibold mb-4">{t.footer.company}</h3>
            <ul className="space-y-2">
              <li><a href="/about" className="hover:text-white dark:hover:text-gray-200 transition-colors">{t.footer.about}</a></li>
              <li><a href="/careers" className="hover:text-white dark:hover:text-gray-200 transition-colors">{t.footer.careers}</a></li>
              <li><a href="/contact" className="hover:text-white dark:hover:text-gray-200 transition-colors">{t.footer.contact}</a></li>
              <li><a href="/brand" className="hover:text-white dark:hover:text-gray-200 transition-colors">{t.footer.brand}</a></li>
            </ul>
          </div>

          <div>
            <h3 className="text-white font-semibold mb-4">{t.footer.legal}</h3>
            <ul className="space-y-2">
              <li><a href="/privacy" className="hover:text-white dark:hover:text-gray-200 transition-colors">{t.footer.privacy}</a></li>
              <li><a href="/terms" className="hover:text-white dark:hover:text-gray-200 transition-colors">{t.footer.terms}</a></li>
              <li><a href="/security" className="hover:text-white dark:hover:text-gray-200 transition-colors">{t.footer.security}</a></li>
            </ul>
          </div>
        </div>

        <div className="border-t border-gray-800 dark:border-gray-700 pt-8 flex flex-col md:flex-row items-center justify-between">
          <div className="flex items-center gap-2 mb-4 md:mb-0">
            <img src={logo} alt="Pullwise Logo" className="h-10 w-10" />
            <span className="text-white font-semibold">Pullwise</span>
          </div>

          <div className="flex items-center gap-6">
            <a href="https://twitter.com/pullwise" className="hover:text-white dark:hover:text-gray-200 transition-colors">Twitter</a>
            <a href="https://github.com/pullwise" className="hover:text-white dark:hover:text-gray-200 transition-colors">GitHub</a>
            <a href="https://linkedin.com/company/pullwise" className="hover:text-white dark:hover:text-gray-200 transition-colors">LinkedIn</a>
            <a href="https://discord.gg/pullwise" className="hover:text-white dark:hover:text-gray-200 transition-colors">Discord</a>
          </div>
        </div>

        <p className="text-center text-gray-500 dark:text-gray-600 text-sm mt-8">
          {t.footer.copyright}
        </p>
      </div>
    </footer>
  )
}
