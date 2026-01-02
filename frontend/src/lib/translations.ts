export const translations = {
  en: {
    // Navigation
    nav: {
      features: "Features",
      pricing: "Pricing",
      faq: "FAQ",
      comparison: "Compare",
      signIn: "Sign In",
      startTrial: "Start Free Trial",
    },
    // Hero (Community-First)
    hero: {
      badge: "v1.0 Now Available:",
      badgeText: "Community Edition with MIT License",
      headline: "The Open Code Review",
      highlight: "Platform",
      subheadline: "Self-hosted AI code review. Start free, scale enterprise. Trusted by 10,000+ developers worldwide.",
      ctaPrimary: "Get Started Free",
      ctaSecondary: "Book a Demo",
      trust1: "MIT License - Forever free",
      trust2: "Self-hosted",
      trust3: "5-minute setup",
      demoTitle: "Product Demo",
      demoSubtitle: "Self-hosted AI code review in action",
    },
    // Community Showcase
    communityShowcase: {
      headline: "Join 10,000+ Developers Worldwide",
      subheadline: "Production-Grade. Free Forever. Community-Powered.",
      stats: {
        stars: "GitHub Stars",
        pulls: "Docker Pulls",
        plugins: "Community Plugins",
        members: "Discord Members",
      },
      featuredPlugins: "Featured Community Plugins",
      cta: "Join Community →",
    },
    // Why Community Edition
    whyCommunity: {
      headline: "Production-Grade. Free Forever. No Strings Attached.",
      subheadline: "Everything you need for professional code reviews, nothing you don't.",
      benefits: {
        mit: {
          title: "MIT License",
          description: "Use commercially, modify, distribute. No vendor lock-in. Forever free.",
        },
        selfHosted: {
          title: "Self-Hosted",
          description: "Your data, your infrastructure. Docker, K8s, or air-gapped.",
        },
        coreComplete: {
          title: "Core Complete",
          description: "Multi-model AI + SAST + auto-fix. Production-ready from day one.",
        },
        plugins: {
          title: "200+ Plugins",
          description: "Free community plugins. Extend functionality. Create your own.",
        },
        setup: {
          title: "5-Minute Setup",
          description: "Docker Compose one-liner. docker-compose up and you're ready.",
        },
      },
      cta: "Download Community Edition",
      trust: "MIT License • No credit card required • Runs on your infrastructure",
    },
    // Features (Community-First)
    features: {
      headline: "Why Developers Choose Pullwise",
      subheadline: "Start with open source, upgrade when you need enterprise features.",
      items: {
        openSource: {
          headline: "100% Open Source",
          title: "MIT Licensed Core",
          description: "No vendor lock-in. Forever free. Modify and distribute freely.",
          badge: "Community Edition",
        },
        selfHosted: {
          headline: "Self-Hosted",
          title: "Your Infrastructure, Your Rules",
          description: "Docker, K8s, or air-gapped. Complete data control.",
        },
        aiPowered: {
          headline: "AI-Powered",
          title: "Multi-Model Intelligence",
          description: "Gemma local + GPT-4 + Claude. Optimized for cost.",
        },
        scale: {
          headline: "Scale When Ready",
          title: "Upgrade Path: CE → Pro → EE",
          description: "Seamless upgrade. No reinstall. License key only.",
        },
        enterprise: {
          headline: "Enterprise Ready",
          title: "SOC2, SAML, Air-Gapped",
          description: "When you need compliance, we're ready.",
        },
        extensible: {
          headline: "Extensible",
          title: "200+ Community Plugins",
          description: "Free plugins. Create custom. Marketplace available.",
        },
      },
    },
    // How It Works
    howItWorks: {
      headline: "From Zero to Code Reviews in Minutes",
      subheadline: "Get started with 3 commands. No credit card required.",
      steps: {
        step1: {
          label: "Step 1",
          title: "Download & Run",
          description: "docker-compose up and you're ready",
        },
        step2: {
          label: "Step 2",
          title: "Connect Your Repo",
          description: "Install GitHub app or configure webhook",
        },
        step3: {
          label: "Step 3",
          title: "Get Instant Reviews",
          description: "Every PR analyzed in <3 minutes with AI + SAST",
        },
        step4: {
          label: "Step 4",
          title: "Fix & Merge",
          description: "Auto-fix suggestions, one-click apply, ship faster",
        },
      },
    },
    // Editions Comparison
    editionsComparison: {
      headline: "Pullwise.ai Editions",
      subheadline: "Compare features across all editions",
    },
    // Pricing (Community-First - 4 tiers)
    pricing: {
      headline: "Simple, Transparent Pricing. Start Free.",
      subheadline: "Community Edition is free forever. Upgrade when you need enterprise features.",
      plans: {
        ce: {
          name: "Community Edition",
          shortName: "CE",
          price: "$0",
          period: "forever",
          description: "For startups, OSS, individual developers",
          badge: "MOST POPULAR",
          cta: "Download Now",
          features: [
            "5 users per instance",
            "1 organization",
            "Multi-model LLM support",
            "SAST integrations",
            "Auto-fix (basic)",
            "Plugin system (200+ plugins)",
            "Community support (Discord, GitHub)",
            "MIT License - use commercially",
          ],
        },
        pro: {
          name: "Professional",
          price: "$49",
          period: "per developer/month",
          description: "For growing teams (10-50 devs)",
          cta: "Start 14-Day Trial",
          features: [
            "Everything in CE, plus:",
            "50 users per instance",
            "3 organizations",
            "4-pass pipeline",
            "Code graph analysis",
            "SSO/SAML",
            "Basic RBAC",
            "Advanced analytics",
            "Jira/Linear integration",
            "RAG knowledge base",
            "Email support (48h response)",
          ],
        },
        ee: {
          name: "Enterprise",
          price: "$99",
          period: "per developer/month",
          description: "For large enterprises (50+ devs)",
          badge: "BEST VALUE",
          cta: "Contact Sales",
          features: [
            "Everything in Pro, plus:",
            "Unlimited users",
            "Unlimited organizations",
            "Air-gapped deployment",
            "SOC2/ISO/GDPR ready",
            "Advanced RBAC",
            "Audit logs (1 year)",
            "Priority support (4h SLA)",
            "Compliance features",
            "Custom model fine-tuning",
            "Private LLM support",
          ],
        },
        eep: {
          name: "Enterprise Plus",
          price: "$149",
          period: "per developer/month",
          description: "For Fortune 500, Government",
          cta: "Contact Sales",
          features: [
            "Everything in EE, plus:",
            "Source code access",
            "Dedicated Customer Success Manager",
            "24/7 Premium support",
            "1-hour SLA",
            "Custom SLA (99.99%)",
            "Roadmap influence",
            "On-site implementation",
            "Executive business reviews",
          ],
        },
      },
      footer: "All plans include self-hosted deployment.",
      contact: "Need a custom plan?",
      contactLink: "Contact us",
    },
    // FAQ (Community-First)
    faq: {
      headline: "Frequently Asked Questions",
      subheadline: "Got questions? We've got answers.",
      questions: [
        {
          q: "Is Community Edition really free forever?",
          a: "Yes. Community Edition is MIT licensed and free forever. You can use it commercially, modify the source code, and distribute it. No hidden costs, no time limits, no user quotas beyond the 5-user limit per instance.",
        },
        {
          q: "What's the difference between Community Edition and Professional?",
          a: "Community Edition has the core AI + SAST features but is limited to 5 users and 1 organization. Professional adds 50 users, 3 organizations, 4-pass pipeline, code graph analysis, SSO/SAML, and email support. Think of CE as 'perfect for small teams' and Pro as 'for growing teams needing governance.'",
        },
        {
          q: "Can I upgrade from Community Edition to Enterprise later?",
          a: "Yes! Upgrading is seamless - just apply a license key, no reinstallation needed. Your data, configuration, and settings are preserved. CE → Pro → EE → EE+ is a smooth upgrade path.",
        },
        {
          q: "Do I need self-hosted for Community Edition?",
          a: "Yes, Community Edition is self-hosted only (Docker or K8s). This ensures your code never leaves your infrastructure. Professional and Enterprise can optionally use our managed cloud, but self-hosted is available for all editions.",
        },
        {
          q: "How does the plugin marketplace work?",
          a: "All plugins in Community Edition are free. Paid plugins (Enterprise features) use a 70/30 revenue split: 70% to the plugin developer, 30% to Pullwise. You can also create custom plugins for your organization without publishing to the marketplace.",
        },
        {
          q: "What support comes with Community Edition?",
          a: "Community Edition includes Discord community support, GitHub issues (bug reports), comprehensive documentation, and community-contributed plugins. Paid plans add email support (48h for Pro, 4h for EE, 1h for EE+).",
        },
        {
          q: "Can I use Community Edition for commercial projects?",
          a: "Absolutely! The MIT license allows commercial use. You can use Pullwise CE in your company, in client projects, or in commercial products without paying anything. The only limit is 5 users per instance.",
        },
        {
          q: "What AI models are supported in Community Edition?",
          a: "CE supports all models through OpenRouter or self-hosted: Local via Ollama (Gemma, Llama, Mistral, etc.), Cloud via OpenRouter (GPT-4, Claude, etc.), Custom models via API. Professional adds intelligent model routing for cost optimization.",
        },
      ],
      contact: "Still have questions?",
      discord: "Join our Discord community",
      email: "contact us",
    },
    // Final CTA (Community-First)
    finalCta: {
      headline: "Ready to Ship Better Code?",
      subheadline: "Join 10,000+ developers using Pullwise for code reviews. Start free, upgrade when ready.",
      ctaPrimary: "Download Community Edition",
      ctaSecondary: "Book Enterprise Demo",
      trust1: "MIT License - Forever free",
      trust2: "5-minute setup with Docker",
      trust3: "10,000+ active installations",
    },
    // Footer
    footer: {
      product: "Product",
      resources: "Resources",
      company: "Company",
      legal: "Legal",
      features: "Features",
      pricing: "Pricing",
      integrations: "Integrations",
      changelog: "Changelog",
      documentation: "Documentation",
      api: "API Reference",
      guides: "Guides",
      blog: "Blog",
      about: "About",
      careers: "Careers",
      contact: "Contact",
      brand: "Brand",
      privacy: "Privacy",
      terms: "Terms",
      security: "Security",
      copyright: "© 2025 IntegrAllTech - Pullwise. All rights reserved.",
    },
  },
  pt: {
    // Navigation
    nav: {
      features: "Recursos",
      pricing: "Preços",
      faq: "FAQ",
      comparison: "Comparar",
      signIn: "Entrar",
      startTrial: "Começar Grátis",
    },
    // Hero (Community-First)
    hero: {
      badge: "v1.0 Agora Disponível:",
      badgeText: "Community Edition com Licença MIT",
      headline: "A Plataforma Aberta de",
      highlight: "Code Review",
      subheadline: "Code review com IA self-hosted. Comece grátis, escale para enterprise. Usado por mais de 10.000 desenvolvedores ao redor do mundo.",
      ctaPrimary: "Começar Grátis",
      ctaSecondary: "Agendar Demo",
      trust1: "Licença MIT - Grátis para sempre",
      trust2: "Self-hosted",
      trust3: "Setup em 5 minutos",
      demoTitle: "Demo do Produto",
      demoSubtitle: "Code review com IA self-hosted em ação",
    },
    // Community Showcase
    communityShowcase: {
      headline: "Junte-se a 10.000+ Desenvolvedores Globalmente",
      subheadline: "Produção. Grátis Para Sempre. Movido pela Comunidade.",
      stats: {
        stars: "Estrelas no GitHub",
        pulls: "Docker Pulls",
        plugins: "Plugins da Comunidade",
        members: "Membros Discord",
      },
      featuredPlugins: "Plugins em Destaque da Comunidade",
      cta: "Juntar-se à Comunidade →",
    },
    // Why Community Edition
    whyCommunity: {
      headline: "Produção. Grátis Para Sempre. Sem Amarras.",
      subheadline: "Tudo que você precisa para code reviews profissionais, nada que não precise.",
      benefits: {
        mit: {
          title: "Licença MIT",
          description: "Uso comercial, modifique, distribua. Sem vendor lock-in. Para sempre grátis.",
        },
        selfHosted: {
          title: "Self-Hosted",
          description: "Seus dados, sua infraestrutura. Docker, K8s, ou air-gapped.",
        },
        coreComplete: {
          title: "Core Completo",
          description: "IA multi-modelo + SAST + auto-fix. Produção desde o primeiro dia.",
        },
        plugins: {
          title: "200+ Plugins",
          description: "Plugins grátis da comunidade. Estenda funcionalidades. Crie os seus.",
        },
        setup: {
          title: "Setup em 5 Minutos",
          description: "Uma linha Docker Compose. docker-compose up e pronto.",
        },
      },
      cta: "Baixar Community Edition",
      trust: "Licença MIT • Sem cartão de crédito • Roda na sua infraestrutura",
    },
    // Features (Community-First)
    features: {
      headline: "Por Que Desenvolvedores Escolhem Pullwise",
      subheadline: "Comece com open source, atualize quando precisar de enterprise.",
      items: {
        openSource: {
          headline: "100% Open Source",
          title: "Core com Licença MIT",
          description: "Sem vendor lock-in. Para sempre grátis. Modifique e distribua livremente.",
          badge: "Community Edition",
        },
        selfHosted: {
          headline: "Self-Hosted",
          title: "Sua Infraestrutura, Suas Regras",
          description: "Docker, K8s, ou air-gapped. Controle completo dos dados.",
        },
        aiPowered: {
          headline: "IA-Poderado",
          title: "Inteligência Multi-Modelo",
          description: "Gemma local + GPT-4 + Claude. Otimizado para custo.",
        },
        scale: {
          headline: "Escale Quando Pronto",
          title: "Upgrade: CE → Pro → EE",
          description: "Upgrade transparente. Sem reinstalar. Apenas chave de licença.",
        },
        enterprise: {
          headline: "Enterprise Ready",
          title: "SOC2, SAML, Air-Gapped",
          description: "Quando precisar de compliance, estamos prontos.",
        },
        extensible: {
          headline: "Extensível",
          title: "200+ Plugins da Comunidade",
          description: "Plugins grátis. Crie customizados. Marketplace disponível.",
        },
      },
    },
    // How It Works
    howItWorks: {
      headline: "Do Zero a Code Reviews em Minutos",
      subheadline: "Comece com 3 comandos. Sem cartão de crédito.",
      steps: {
        step1: {
          label: "Passo 1",
          title: "Baixar e Executar",
          description: "docker-compose up e pronto",
        },
        step2: {
          label: "Passo 2",
          title: "Conecte Seu Repo",
          description: "Instale o app do GitHub ou configure webhook",
        },
        step3: {
          label: "Passo 3",
          title: "Reviews Instantâneos",
          description: "Cada PR analisado em <3 minutos com IA + SAST",
        },
        step4: {
          label: "Passo 4",
          title: "Corrija e Merge",
          description: "Sugestões de auto-fix, um clique para aplicar, ship mais rápido",
        },
      },
    },
    // Editions Comparison
    editionsComparison: {
      headline: "Edições Pullwise.ai",
      subheadline: "Compare recursos entre todas as edições",
    },
    // Pricing (Community-First - 4 tiers)
    pricing: {
      headline: "Preços Simples e Transparentes. Comece Grátis.",
      subheadline: "Community Edition é grátis para sempre. Atualize quando precisar de enterprise.",
      plans: {
        ce: {
          name: "Community Edition",
          shortName: "CE",
          price: "R$0",
          period: "para sempre",
          description: "Para startups, OSS, desenvolvedores individuais",
          badge: "MAIS POPULAR",
          cta: "Baixar Agora",
          features: [
            "5 usuários por instância",
            "1 organização",
            "Suporte a LLM multi-modelo",
            "Integrações SAST",
            "Auto-fix (básico)",
            "Sistema de plugins (200+ plugins)",
            "Suporte comunidade (Discord, GitHub)",
            "Licença MIT - uso comercial",
          ],
        },
        pro: {
          name: "Professional",
          price: "R$240",
          period: "por desenvolvedor/mês",
          description: "Para times em crescimento (10-50 devs)",
          cta: "Iniciar Trial 14 Dias",
          features: [
            "Tudo do CE, mais:",
            "50 usuários por instância",
            "3 organizações",
            "Pipeline 4-pass",
            "Análise de code graph",
            "SSO/SAML",
            "RBAC básico",
            "Analytics avançados",
            "Integração Jira/Linear",
            "Base de conhecimento RAG",
            "Suporte por email (48h)",
          ],
        },
        ee: {
          name: "Enterprise",
          price: "R$490",
          period: "por desenvolvedor/mês",
          description: "Para grandes empresas (50+ devs)",
          badge: "MELHOR VALOR",
          cta: "Falar com Vendas",
          features: [
            "Tudo do Pro, mais:",
            "Usuários ilimitados",
            "Organizações ilimitadas",
            "Deploy air-gapped",
            "SOC2/ISO/GDPR ready",
            "RBAC avançado",
            "Audit logs (1 ano)",
            "Suporte prioritário (4h SLA)",
            "Features de compliance",
            "Fine-tuning de modelos customizados",
            "Suporte a LLM privado",
          ],
        },
        eep: {
          name: "Enterprise Plus",
          price: "R$740",
          period: "por desenvolvedor/mês",
          description: "Para Fortune 500, Governo",
          cta: "Falar com Vendas",
          features: [
            "Tudo do EE, mais:",
            "Acesso ao código fonte",
            "Gerente de Suporte Dedicado",
            "Suporte 24/7 Premium",
            "SLA de 1 hora",
            "SLA customizado (99,99%)",
            "Influência no roadmap",
            "Implementação on-site",
            "Reviews de negócios executivos",
          ],
        },
      },
      footer: "Todos os planos incluem deployment self-hosted.",
      contact: "Precisa de um plano customizado?",
      contactLink: "Fale conosco",
    },
    // FAQ (Community-First)
    faq: {
      headline: "Perguntas Frequentes",
      subheadline: "Tem dúvidas? Temos respostas.",
      questions: [
        {
          q: "Community Edition é realmente grátis para sempre?",
          a: "Sim. Community Edition é licenciada com MIT e grátis para sempre. Você pode usar comercialmente, modificar o código fonte e distribuir. Sem custos escondidos, sem limites de tempo, sem cotas além do limite de 5 usuários por instância.",
        },
        {
          q: "Qual a diferença entre Community Edition e Professional?",
          a: "Community Edition tem as features core de IA + SAST mas é limitada a 5 usuários e 1 organização. Professional adiciona 50 usuários, 3 organizações, pipeline 4-pass, análise de code graph, SSO/SAML e suporte por email. Pense no CE como 'perfeito para times pequenos' e Pro como 'para times em crescimento que precisam de governança'.",
        },
        {
          q: "Posso fazer upgrade de Community Edition para Enterprise depois?",
          a: "Sim! O upgrade é transparente - apenas aplique uma chave de licença, sem necessidade de reinstalação. Seus dados, configurações e ajustes são preservados. CE → Pro → EE → EE+ é um caminho de upgrade suave.",
        },
        {
          q: "Preciso de self-hosted para Community Edition?",
          a: "Sim, Community Edition é apenas self-hosted (Docker ou K8s). Isso garante que seu código nunca sai de sua infraestrutura. Professional e Enterprise podem usar opcionalmente nossa nuvem gerenciada, mas self-hosted está disponível para todas as edições.",
        },
        {
          q: "Como funciona o marketplace de plugins?",
          a: "Todos os plugins em Community Edition são grátis. Plugins pagos (features Enterprise) usam split de receita 70/30: 70% para o desenvolvedor do plugin, 30% para Pullwise. Você também pode criar plugins customizados para sua organização sem publicar no marketplace.",
        },
        {
          q: "Que suporte vem com Community Edition?",
          a: "Community Edition inclui suporte da comunidade Discord, issues do GitHub (bug reports), documentação completa e plugins contribuídos pela comunidade. Planos pagos adicionam suporte por email (48h para Pro, 4h para EE, 1h para EE+).",
        },
        {
          q: "Posso usar Community Edition para projetos comerciais?",
          a: "Absolutamente! A licença MIT permite uso comercial. Você pode usar Pullwise CE na sua empresa, em projetos de clientes, ou em produtos comerciais sem pagar nada. O único limite é 5 usuários por instância.",
        },
        {
          q: "Quais modelos de IA são suportados no Community Edition?",
          a: "CE suporta todos os modelos através do OpenRouter ou self-hosted: Local via Ollama (Gemma, Llama, Mistral, etc.), Cloud via OpenRouter (GPT-4, Claude, etc.), Modelos customizados via API. Professional adiciona roteamento inteligente de modelos para otimização de custo.",
        },
      ],
      contact: "Ainda tem dúvidas?",
      discord: "Junte-se à nossa comunidade Discord",
      email: "entre em contato",
    },
    // Final CTA (Community-First)
    finalCta: {
      headline: "Pronto Para Entregar Código Melhor?",
      subheadline: "Junte-se a 10.000+ desenvolvedores usando Pullwise para code reviews. Comece grátis, escale quando precisar.",
      ctaPrimary: "Baixar Community Edition",
      ctaSecondary: "Agendar Demo Enterprise",
      trust1: "Licença MIT - Para sempre grátis",
      trust2: "Setup em 5 minutos com Docker",
      trust3: "10.000+ instalações ativas",
    },
    // Footer
    footer: {
      product: "Produto",
      resources: "Recursos",
      company: "Empresa",
      legal: "Legal",
      features: "Recursos",
      pricing: "Preços",
      integrations: "Integrações",
      changelog: "Changelog",
      documentation: "Documentação",
      api: "Referência de API",
      guides: "Guias",
      blog: "Blog",
      about: "Sobre",
      careers: "Vagas",
      contact: "Contato",
      brand: "Marca",
      privacy: "Privacidade",
      terms: "Termos",
      security: "Segurança",
      copyright: "© 2025 IntegrAllTech - Pullwise. Todos os direitos reservados.",
    },
  },
  es: {
    // Navigation
    nav: {
      features: "Características",
      pricing: "Precios",
      faq: "Preguntas",
      comparison: "Comparar",
      signIn: "Iniciar Sesión",
      startTrial: "Prueba Gratis",
    },
    // Hero (Community-First)
    hero: {
      badge: "v1.0 Ahora Disponible:",
      badgeText: "Community Edition con Licencia MIT",
      headline: "La Plataforma Abierta de",
      highlight: "Code Review",
      subheadline: "Code review con IA auto-alojado. Empieza gratis, escala a enterprise. Usado por más de 10.000 desarrolladores en todo el mundo.",
      ctaPrimary: "Comenzar Gratis",
      ctaSecondary: "Agendar Demo",
      trust1: "Licencia MIT - Gratis para siempre",
      trust2: "Self-hosted",
      trust3: "Configuración en 5 minutos",
      demoTitle: "Demo del Producto",
      demoSubtitle: "Code review con IA auto-alojado en acción",
    },
    // Community Showcase
    communityShowcase: {
      headline: "Únete a 10.000+ Desarrolladores Globalmente",
      subheadline: "Producción. Gratis Para Siempre. Impulsado por la Comunidad.",
      stats: {
        stars: "Estrellas en GitHub",
        pulls: "Docker Pulls",
        plugins: "Plugins de la Comunidad",
        members: "Miembros Discord",
      },
      featuredPlugins: "Plugins Destacados de la Comunidad",
      cta: "Únete a la Comunidad →",
    },
    // Why Community Edition
    whyCommunity: {
      headline: "Producción. Gratis Para Siempre. Sin Ataduras.",
      subheadline: "Todo lo que necesitas para code reviews profesionales, nada que no necesites.",
      benefits: {
        mit: {
          title: "Licencia MIT",
          description: "Uso comercial, modifica, distribuye. Sin vendor lock-in. Para siempre gratis.",
        },
        selfHosted: {
          title: "Self-Hosted",
          description: "Tus datos, tu infraestructura. Docker, K8s, o air-gapped.",
        },
        coreComplete: {
          title: "Core Completo",
          description: "IA multi-modelo + SAST + auto-fix. Producción desde el primer día.",
        },
        plugins: {
          title: "200+ Plugins",
          description: "Plugins gratis de la comunidad. Extiende funcionalidades. Crea los tuyos.",
        },
        setup: {
          title: "Configuración en 5 Minutos",
          description: "Una línea Docker Compose. docker-compose up y listo.",
        },
      },
      cta: "Descargar Community Edition",
      trust: "Licencia MIT • Sin tarjeta de crédito • Funciona en tu infraestructura",
    },
    // Features (Community-First)
    features: {
      headline: "Por Qué Desarrolladores Eligen Pullwise",
      subheadline: "Empieza con open source, actualiza cuando necesites enterprise.",
      items: {
        openSource: {
          headline: "100% Open Source",
          title: "Core con Licencia MIT",
          description: "Sin vendor lock-in. Para siempre gratis. Modifica y distribuye libremente.",
          badge: "Community Edition",
        },
        selfHosted: {
          headline: "Self-Hosted",
          title: "Tu Infraestructura, Tus Reglas",
          description: "Docker, K8s, o air-gapped. Control completo de datos.",
        },
        aiPowered: {
          headline: "IA-Potenciado",
          title: "Inteligencia Multi-Modelo",
          description: "Gemma local + GPT-4 + Claude. Optimizado para costo.",
        },
        scale: {
          headline: "Escala Cuando Estés Listo",
          title: "Upgrade: CE → Pro → EE",
          description: "Upgrade transparente. Sin reinstalar. Solo clave de licencia.",
        },
        enterprise: {
          headline: "Enterprise Ready",
          title: "SOC2, SAML, Air-Gapped",
          description: "Cuando necesites compliance, estamos listos.",
        },
        extensible: {
          headline: "Extensible",
          title: "200+ Plugins de la Comunidad",
          description: "Plugins gratis. Crea personalizados. Marketplace disponible.",
        },
      },
    },
    // How It Works
    howItWorks: {
      headline: "De Cero a Code Reviews en Minutos",
      subheadline: "Empieza con 3 comandos. Sin tarjeta de crédito.",
      steps: {
        step1: {
          label: "Paso 1",
          title: "Descargar y Ejecutar",
          description: "docker-compose up y listo",
        },
        step2: {
          label: "Paso 2",
          title: "Conecta Tu Repo",
          description: "Instala la app de GitHub o configura webhook",
        },
        step3: {
          label: "Paso 3",
          title: "Reviews Instantáneos",
          description: "Cada PR analizado en <3 minutos con IA + SAST",
        },
        step4: {
          label: "Paso 4",
          title: "Corrige y Merge",
          description: "Sugerencias de auto-fix, un clic para aplicar, envía más rápido",
        },
      },
    },
    // Editions Comparison
    editionsComparison: {
      headline: "Ediciones Pullwise.ai",
      subheadline: "Compara características entre todas las ediciones",
    },
    // Pricing (Community-First - 4 tiers)
    pricing: {
      headline: "Precios Simples y Transparentes. Empieza Gratis.",
      subheadline: "Community Edition es gratis para siempre. Actualiza cuando necesites enterprise.",
      plans: {
        ce: {
          name: "Community Edition",
          shortName: "CE",
          price: "$0",
          period: "para siempre",
          description: "Para startups, OSS, desarrolladores individuales",
          badge: "MÁS POPULAR",
          cta: "Descargar Ahora",
          features: [
            "5 usuarios por instancia",
            "1 organización",
            "Soporte LLM multi-modelo",
            "Integraciones SAST",
            "Auto-fix (básico)",
            "Sistema de plugins (200+ plugins)",
            "Soporte comunidad (Discord, GitHub)",
            "Licencia MIT - uso comercial",
          ],
        },
        pro: {
          name: "Professional",
          price: "$49",
          period: "por desarrollador/mes",
          description: "Para equipos en crecimiento (10-50 devs)",
          cta: "Iniciar Trial 14 Días",
          features: [
            "Todo en CE, más:",
            "50 usuarios por instancia",
            "3 organizaciones",
            "Pipeline 4-pass",
            "Análisis de code graph",
            "SSO/SAML",
            "RBAC básico",
            "Analytics avanzados",
            "Integración Jira/Linear",
            "Base de conocimiento RAG",
            "Soporte por email (48h)",
          ],
        },
        ee: {
          name: "Enterprise",
          price: "$99",
          period: "por desarrollador/mes",
          description: "Para grandes empresas (50+ devs)",
          badge: "MEJOR VALOR",
          cta: "Contactar Ventas",
          features: [
            "Todo en Pro, más:",
            "Usuarios ilimitados",
            "Organizaciones ilimitadas",
            "Deploy air-gapped",
            "SOC2/ISO/GDPR ready",
            "RBAC avanzado",
            "Audit logs (1 año)",
            "Soporte prioritario (4h SLA)",
            "Features de compliance",
            "Fine-tuning de modelos personalizados",
            "Soporte a LLM privado",
          ],
        },
        eep: {
          name: "Enterprise Plus",
          price: "$149",
          period: "por desarrollador/mes",
          description: "Para Fortune 500, Gobierno",
          cta: "Contactar Ventas",
          features: [
            "Todo en EE, más:",
            "Acceso al código fuente",
            "Gerente de Soporte Dedicado",
            "Soporte 24/7 Premium",
            "SLA de 1 hora",
            "SLA personalizado (99.99%)",
            "Influencia en el roadmap",
            "Implementación on-site",
            "Reviews de negocios ejecutivos",
          ],
        },
      },
      footer: "Todos los planes incluyen deployment self-hosted.",
      contact: "¿Necesitas un plan personalizado?",
      contactLink: "Contáctanos",
    },
    // FAQ (Community-First)
    faq: {
      headline: "Preguntas Frecuentes",
      subheadline: "¿Tienes preguntas? Tenemos respuestas.",
      questions: [
        {
          q: "¿Community Edition es realmente gratis para siempre?",
          a: "Sí. Community Edition está licenciada con MIT y es gratis para siempre. Puedes usarla comercialmente, modificar el código fuente y distribuirla. Sin costos ocultos, sin límites de tiempo, sin cuotas más allá del límite de 5 usuarios por instancia.",
        },
        {
          q: "¿Cuál es la diferencia entre Community Edition y Professional?",
          a: "Community Edition tiene las features core de IA + SAST pero está limitada a 5 usuarios y 1 organización. Professional añade 50 usuarios, 3 organizaciones, pipeline 4-pass, análisis de code graph, SSO/SAML y soporte por email. Piensa en CE como 'perfecto para equipos pequeños' y Pro como 'para equipos en crecimiento que necesitan gobernanza'.",
        },
        {
          q: "¿Puedo hacer upgrade de Community Edition a Enterprise después?",
          a: "¡Sí! El upgrade es transparente - solo aplica una clave de licencia, sin necesidad de reinstalación. Tus datos, configuraciones y ajustes se preservan. CE → Pro → EE → EE+ es un camino de upgrade suave.",
        },
        {
          q: "¿Necesito self-hosted para Community Edition?",
          a: "Sí, Community Edition es solo self-hosted (Docker o K8s). Esto garantiza que tu código nunca salga de tu infraestructura. Professional y Enterprise pueden usar opcionalmente nuestra nube administrada, pero self-hosted está disponible para todas las ediciones.",
        },
        {
          q: "¿Cómo funciona el marketplace de plugins?",
          a: "Todos los plugins en Community Edition son gratis. Los plugins pagos (features Enterprise) usan split de ingresos 70/30: 70% para el desarrollador del plugin, 30% para Pullwise. También puedes crear plugins personalizados para tu organización sin publicar en el marketplace.",
        },
        {
          q: "¿Qué soporte viene con Community Edition?",
          a: "Community Edition incluye soporte de la comunidad Discord, issues de GitHub (reportes de bugs), documentación completa y plugins contribuidos por la comunidad. Los planes pagados añaden soporte por email (48h para Pro, 4h para EE, 1h para EE+).",
        },
        {
          q: "¿Puedo usar Community Edition para proyectos comerciales?",
          a: "¡Absolutamente! La licencia MIT permite uso comercial. Puedes usar Pullwise CE en tu empresa, en proyectos de clientes, o en productos comerciales sin pagar nada. El único límite es 5 usuarios por instancia.",
        },
        {
          q: "¿Qué modelos de IA son soportados en Community Edition?",
          a: "CE soporta todos los modelos a través de OpenRouter o self-hosted: Local vía Ollama (Gemma, Llama, Mistral, etc.), Cloud vía OpenRouter (GPT-4, Claude, etc.), Modelos personalizados vía API. Professional añade enrutamiento inteligente de modelos para optimización de costo.",
        },
      ],
      contact: "¿Aún tienes preguntas?",
      discord: "Únete a nuestra comunidad Discord",
      email: "contáctanos",
    },
    // Final CTA (Community-First)
    finalCta: {
      headline: "¿Listo Para Entregar Código Mejor?",
      subheadline: "Únete a 10.000+ desarrolladores usando Pullwise para code reviews. Empieza gratis, actualiza cuando estés listo.",
      ctaPrimary: "Descargar Community Edition",
      ctaSecondary: "Agendar Demo Enterprise",
      trust1: "Licencia MIT - Para siempre gratis",
      trust2: "Configuración en 5 minutos con Docker",
      trust3: "10.000+ instalaciones activas",
    },
    // Footer
    footer: {
      product: "Producto",
      resources: "Recursos",
      company: "Empresa",
      legal: "Legal",
      features: "Características",
      pricing: "Precios",
      integrations: "Integraciones",
      changelog: "Changelog",
      documentation: "Documentación",
      api: "Referencia de API",
      guides: "Guías",
      blog: "Blog",
      about: "Acerca de",
      careers: "Carreras",
      contact: "Contacto",
      brand: "Marca",
      privacy: "Privacidad",
      terms: "Términos",
      security: "Seguridad",
      copyright: "© 2025 IntegrAllTech - Pullwise. Todos los derechos reservados.",
    },
  },
}

export type Language = "en" | "pt" | "es"
export type Translations = typeof translations.en
