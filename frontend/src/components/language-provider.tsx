import { createContext, useContext, useState } from "react"
import { translations, Translations, Language } from "@/lib/translations"

type LanguageProviderProps = {
  children: React.ReactNode
  defaultLanguage?: Language
  storageKey?: string
}

type LanguageProviderState = {
  language: Language
  setLanguage: (language: Language) => void
  t: Translations
}

const initialState: LanguageProviderState = {
  language: "en",
  setLanguage: () => null,
  t: translations.en,
}

const LanguageProviderContext = createContext<LanguageProviderState>(initialState)

export function LanguageProvider({
  children,
  defaultLanguage = "en",
  storageKey = "pullwise-language",
}: LanguageProviderProps) {
  const [language, setLanguageState] = useState<Language>(() => {
    const stored = localStorage.getItem(storageKey)
    if (stored && (stored === "en" || stored === "pt" || stored === "es")) {
      return stored as Language
    }
    return defaultLanguage
  })

  const setLanguage = (lang: Language) => {
    setLanguageState(lang)
    localStorage.setItem(storageKey, lang)
  }

  const t = translations[language]

  return (
    <LanguageProviderContext.Provider value={{ language, setLanguage, t }}>
      {children}
    </LanguageProviderContext.Provider>
  )
}

export const useLanguage = () => {
  const context = useContext(LanguageProviderContext)

  if (context === undefined)
    throw new Error("useLanguage must be used within a LanguageProvider")

  return context
}
