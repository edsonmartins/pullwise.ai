import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from "@/components/ui/accordion"
import { useLanguage } from "@/components/language-provider"

/**
 * FAQ component
 * Displays frequently asked questions focused on Community Edition
 * Part of the Community-First landing page strategy
 */
export default function FAQ() {
  const { t } = useLanguage()
  const faq = t.faq

  return (
    <section id="faq" className="py-24 bg-gray-50 dark:bg-gray-900">
      <div className="container mx-auto px-6">
        <div className="max-w-3xl mx-auto">
          <h2 className="text-4xl font-bold text-gray-900 dark:text-white mb-4 text-center">
            {faq.headline}
          </h2>
          <p className="text-xl text-gray-600 dark:text-gray-300 mb-12 text-center">
            {faq.subheadline}
          </p>

          <Accordion type="single" collapsible className="space-y-4">
            {faq.questions.map((item, index) => (
              <AccordionItem
                key={index}
                value={`item-${index}`}
                className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg px-6"
              >
                <AccordionTrigger className="text-left font-semibold text-gray-900 dark:text-white hover:text-purple-600 dark:hover:text-purple-400">
                  {item.q}
                </AccordionTrigger>
                <AccordionContent className="text-gray-600 dark:text-gray-400">
                  {item.a}
                </AccordionContent>
              </AccordionItem>
            ))}
          </Accordion>

          <p className="text-center mt-8 text-gray-600 dark:text-gray-400">
            {faq.contact}{' '}
            <a href="https://discord.gg/pullwise" target="_blank" rel="noopener noreferrer" className="text-purple-600 dark:text-purple-400 hover:underline">
              {faq.discord}
            </a>
            {' '}&rarr;{' '}
            <a href="mailto:hello@pullwise.ai" className="text-purple-600 dark:text-purple-400 hover:underline">
              {faq.email}
            </a>
          </p>
        </div>
      </div>
    </section>
  )
}
