import { Button } from "@/components/ui/button"
import { useLanguage } from "@/components/language-provider"

const languages = [
  { code: "en" as const, label: "EN" },
  { code: "pt" as const, label: "PT" },
  { code: "es" as const, label: "ES" },
] as const

export function LanguageToggle() {
  const { language, setLanguage } = useLanguage()

  const cycleLanguage = () => {
    const currentIndex = languages.findIndex((lang) => lang.code === language)
    const nextIndex = (currentIndex + 1) % languages.length
    setLanguage(languages[nextIndex].code)
  }

  return (
    <Button
      variant="ghost"
      size="sm"
      onClick={cycleLanguage}
      className="font-mono font-medium min-w-[50px]"
    >
      {language.toUpperCase()}
    </Button>
  )
}
