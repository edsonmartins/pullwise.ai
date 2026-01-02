# 🦉 Pullwise - Brand Identity Guide

## Nome e Tagline

**Nome:** Pullwise  
**Tagline Principal:** "Wise reviews for every pull"  
**Taglines Alternativos:**
- "Code reviews that learn from your team"
- "AI that understands your codebase"
- "Where wisdom meets automation"

---

## Domínios

- **Principal:** pullwise.ai (produto, marketing, app)
- **Developer Docs:** pullwise.dev (documentação técnica, API, blog)
- **Redirect:** pullwise.dev → pullwise.ai (opcional)

---

## Mascote: Wisey the Code Owl 🦉

### Por que Coruja?

1. **Símbolo de Sabedoria** - "Wise" no nome
2. **Visão Noturna** - Detecta bugs escondidos
3. **Vigilante** - Sempre atento ao código
4. **Diferenciação** - Competidores usam rabbits, ants, etc
5. **Simpático** - Memorável e amigável

### Características do Wisey

- **Estilo:** Moderno, clean, friendly (não realista)
- **Elementos:** Óculos (opcional), lupa, código no fundo
- **Expressão:** Sábio mas acessível, não intimidador
- **Cores:** Tons de azul/roxo (marca) com detalhes verdes

### Uso do Mascote

- ✅ Logo principal: texto + ícone coruja
- ✅ Favicon: apenas coruja
- ✅ Loading states: coruja animada
- ✅ Empty states: coruja com mensagem amigável
- ✅ Erro 404: coruja confusa (cute)
- ✅ Marketing materials: Wisey como personagem

---

## Paleta de Cores

### Cores Principais

```css
/* Primary - Trust Blue */
--pullwise-primary: #2563EB;
--pullwise-primary-hover: #1D4ED8;
--pullwise-primary-light: #DBEAFE;

/* Secondary - Approval Green */
--pullwise-secondary: #10B981;
--pullwise-secondary-hover: #059669;
--pullwise-secondary-light: #D1FAE5;

/* Accent - AI Purple */
--pullwise-accent: #8B5CF6;
--pullwise-accent-hover: #7C3AED;
--pullwise-accent-light: #EDE9FE;
```

### Cores de Sistema

```css
/* Dark/Gray Scale */
--pullwise-dark: #1E293B;
--pullwise-gray-900: #0F172A;
--pullwise-gray-700: #334155;
--pullwise-gray-500: #64748B;
--pullwise-gray-300: #CBD5E1;
--pullwise-gray-100: #F1F5F9;

/* Background */
--pullwise-bg: #F8FAFC;
--pullwise-bg-white: #FFFFFF;

/* Status Colors */
--pullwise-success: #10B981;
--pullwise-warning: #F59E0B;
--pullwise-error: #EF4444;
--pullwise-info: #3B82F6;
```

### Uso das Cores

- **Primary (Blue):** CTA buttons, links, active states
- **Secondary (Green):** Success states, aprovações, checkmarks
- **Accent (Purple):** AI features, badges "AI-powered", highlights
- **Dark:** Texto principal, headers
- **Gray:** Texto secundário, borders, backgrounds

---

## Tipografia

### Família de Fontes

**Recomendado:** [Inter](https://fonts.google.com/specimen/Inter)
- ✅ Moderna, clean, excellent legibility
- ✅ Suporta variável font weights
- ✅ Open source e grátis
- ✅ Excelente para UI/código

**Alternativa:** [Manrope](https://fonts.google.com/specimen/Manrope)
- Mais arredondada, friendly
- Boa para headlines

### Hierarquia de Texto

```css
/* Headlines */
h1: 48px / 3rem - Bold (700)
h2: 36px / 2.25rem - Bold (700)
h3: 28px / 1.75rem - Semibold (600)
h4: 20px / 1.25rem - Semibold (600)

/* Body */
Body Large: 18px / 1.125rem - Regular (400)
Body: 16px / 1rem - Regular (400)
Body Small: 14px / 0.875rem - Regular (400)
Caption: 12px / 0.75rem - Regular (400)

/* Code/Mono */
Font: JetBrains Mono ou Fira Code
Size: 14px / 0.875rem
```

---

## Logo

### Versões do Logo

#### 1. Logo Completo (Horizontal)
```
🦉 PULLWISE
```
- Uso: Header, marketing, apresentações
- Mínimo: 120px largura

#### 2. Logo Icon Only
```
🦉
```
- Uso: Favicon, app icon, small spaces
- Mínimo: 32x32px

#### 3. Logo Stacked (Vertical)
```
   🦉
PULLWISE
```
- Uso: Redes sociais, profile pics
- Formato: Quadrado

### Variações de Cor

- **Primary:** Azul sobre branco/claro
- **Dark Mode:** Branco sobre dark
- **Monochrome:** Cinza escuro sobre branco

### Espaçamento

- Clear space ao redor: mínimo de 20% da altura do logo
- Não distorcer proporções
- Não adicionar efeitos (shadows, gradients)

---

## Componentes UI

### Buttons

```css
/* Primary Button */
background: var(--pullwise-primary);
color: white;
border-radius: 8px;
padding: 12px 24px;
font-weight: 600;
hover: background var(--pullwise-primary-hover);

/* Secondary Button */
background: white;
color: var(--pullwise-primary);
border: 2px solid var(--pullwise-primary);

/* Success Button */
background: var(--pullwise-secondary);
color: white;
```

### Cards

```css
background: white;
border: 1px solid var(--pullwise-gray-300);
border-radius: 12px;
box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
padding: 24px;
```

### Badges

```css
/* AI Badge */
background: var(--pullwise-accent-light);
color: var(--pullwise-accent);
border-radius: 16px;
padding: 4px 12px;
font-size: 12px;
font-weight: 600;
```

### Severity Badges

```css
Critical: #EF4444 (Red)
High: #F59E0B (Orange)
Medium: #FBBF24 (Yellow)
Low: #3B82F6 (Blue)
```

---

## Tone of Voice

### Personalidade da Marca

- **Sábio mas Acessível:** Expert sem ser intimidador
- **Helpful:** Sempre pronto para ajudar
- **Confiável:** Profissional e preciso
- **Amigável:** Conversacional, não robótico
- **Tech-Forward:** Moderno, inovador

### Exemplos de Comunicação

#### ✅ BOM
"Wisey found 3 critical issues in your PR. Let's fix them together!"
"Your code quality improved 23% this month. Nice work!"
"This PR looks great! Just one small suggestion..."

#### ❌ EVITAR
"ERROR: Multiple violations detected"
"Your code has problems"
"This implementation is wrong"

### Guidelines de Escrita

- Use "we" e "let's" (inclusivo)
- Evite jargão excessivo
- Explique o "por quê", não só o "o quê"
- Celebre wins, seja gentil com issues
- Use emojis moderadamente (🦉 ✅ 🚀 💡)

---

## Imagery & Graphics

### Style Guide

- **Estilo:** Ilustrações flat/semi-flat, modernas
- **Cores:** Paleta da marca
- **Elementos:** Código, PRs, reviews, times colaborando
- **Mood:** Profissional mas amigável

### Fotografias

Se usar fotos:
- Times diversos trabalhando juntos
- Desenvolvedores felizes/focados
- Ambientes tech modernos
- Evitar stock photos genéricas

### Ícones

- **Estilo:** Outline ou dual-tone
- **Biblioteca recomendada:** [Lucide Icons](https://lucide.dev)
- **Peso:** 2px stroke
- **Cores:** Seguir paleta da marca

---

## Marketing Copy

### Landing Page Hero

**Headline:** "Wise Reviews for Every Pull Request"

**Subheadline:** "AI-powered code reviews that learn from your team. Combine SAST + LLM for context-aware feedback in minutes."

**CTA Primary:** "Start Free Trial"  
**CTA Secondary:** "See How It Works"

### Feature Titles

- ⚡ **Hybrid Intelligence** - SAST + AI in perfect harmony
- 🧠 **Learns Your Team** - RAG-powered context from your docs
- 🎯 **Precision Reviews** - Catches what others miss
- 📚 **Knowledge Base** - Remembers your ADRs and patterns
- 🚀 **Ship Faster** - Reviews in minutes, not hours

### Value Props

1. **For Developers:** "Focus on building. Let Wisey handle the tedious reviews."
2. **For Tech Leads:** "Maintain code quality at scale without bottlenecks."
3. **For CTOs:** "Reduce review time by 60% while improving code quality."

---

## Social Media

### Twitter (@pullwise)

**Bio:** "AI-powered code reviews that learn from your team. SAST + LLM hybrid. Built for devs who ship quality code fast. 🦉"

**Profile Pic:** Wisey icon (round)  
**Header:** Product screenshot + tagline

**Post Style:**
- Tech tips & best practices
- Product updates
- Customer wins (with permission)
- Dev humor (appropriate)
- Behind-the-scenes

### LinkedIn (linkedin.com/company/pullwise)

**Description:** "Pullwise delivers context-aware code reviews by combining static analysis with AI that learns from your team's patterns and documentation."

**Post Style:**
- More professional/formal
- Case studies
- Engineering blog posts
- Hiring announcements
- Industry insights

### GitHub (github.com/pullwise)

**Profile:** Wisey icon  
**README:** Technical, feature-focused, links to docs

---

## File Naming Conventions

### Logos
```
pullwise-logo-primary.svg
pullwise-logo-white.svg
pullwise-logo-dark.svg
pullwise-icon.svg
pullwise-icon-square.png (512x512)
```

### Brand Assets
```
pullwise-colors.css
pullwise-brand-guide.pdf
pullwise-presentation-template.pptx
wisey-mascot-variations.ai
```

---

## Checklist de Uso da Marca

### ✅ Permitido

- Usar logo em documentação
- Criar conteúdo educacional sobre Pullwise
- Compartilhar experiências usando o produto
- Mencionar em apresentações técnicas
- Criar integrações (com aprovação)

### ❌ Não Permitido

- Modificar o logo/cores
- Usar marca para produtos concorrentes
- Implicar parceria oficial sem autorização
- Usar Wisey em outros contextos
- Criar subdomínios *.pullwise.* não autorizados

---

## Recursos para Download

Quando os assets estiverem prontos:

- **Brand Kit:** pullwise.ai/brand
- **Press Kit:** pullwise.ai/press
- **Logo Pack:** pullwise.ai/brand/logos.zip
- **Style Guide PDF:** pullwise.ai/brand/style-guide.pdf

---

## Contato de Branding

Para questões sobre uso da marca:  
**Email:** brand@pullwise.ai

---

## Versão

**Brand Guide Version:** 1.0  
**Last Updated:** December 31, 2025  
**Next Review:** March 2026

---

## Aprovações

Esta brand guide foi desenvolvida por IntegrAllTech para o produto Pullwise.

**Status:** 🎨 Ready for Implementation
