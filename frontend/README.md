# Pullwise Frontend

[![React](https://img.shields.io/badge/React-18-61DAFB?logo=react&logoColor=white)](https://reactjs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5-3178C6?logo=typescript&logoColor=white)](https://www.typescriptlang.org/)
[![Vite](https://img.shields.io/badge/Vite-5-646CFF?logo=vite&logoColor=white)](https://vitejs.dev/)

The React frontend for Pullwise - AI Code Review Platform.

---

## 🚀 Quick Start

```bash
# Install dependencies
npm install

# Start development server
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview
```

**Dev server:** `http://localhost:3000` (or next available port)

---

## 📁 Project Structure

```
src/
├── components/
│   ├── ui/                    # Shadcn/ui components
│   ├── landing/               # Landing page components
│   ├── language-provider.tsx  # i18n context (en/pt/es)
│   ├── theme-provider.tsx     # Dark mode context
│   └── ...
├── lib/
│   ├── translations.ts        # i18n translations
│   └── api.ts                 # API client
├── pages/
│   ├── LandingPage.tsx        # Main landing page
│   ├── DownloadPage.tsx       # Download/Install page
│   ├── DemoPage.tsx           # Demo request form
│   └── v2/                    # V2 app pages
│       ├── DashboardPage.tsx
│       ├── CodeGraphPage.tsx
│       ├── AutoFixPage.tsx
│       └── ...
├── store/
│   └── v2-store.ts            # Zustand state
└── main.tsx                   # App entry point
```

---

## 🛠️ Tech Stack

| Category | Technology |
|----------|------------|
| **Framework** | React 18 with TypeScript |
| **Build Tool** | Vite 5 |
| **Styling** | TailwindCSS + Shadcn/ui |
| **State** | Zustand + TanStack Query v5 |
| **Routing** | React Router v6 |
| **Forms** | React Hook Form + Zod |
| **Charts** | Recharts |
| **Icons** | Lucide React |
| **Code Editor** | Monaco Editor |
| **Graph Vis** | React Flow + D3.js |

---

## 🎨 Key Features

### Landing Page (Community-First)
- **Hero** - "The Open Code Review Platform"
- **Community Showcase** - Stats and featured plugins
- **Why Community Edition** - MIT benefits
- **Features** - 6 key features grid
- **How It Works** - 4-step setup
- **Editions Comparison** - 4-tier comparison table
- **Pricing** - CE/Pro/EE/EE+ tiers
- **FAQ** - Common questions
- **Multi-language** - English, Portuguese, Spanish

### V2 Application Pages
- **Dashboard** - Overview metrics and activity
- **Code Graph** - Interactive dependency visualization
- **Auto-Fix** - One-click fix application
- **Analytics** - Team performance metrics
- **Plugin Marketplace** - Browse and install plugins

### UI Components
- Dark mode support (system-aware)
- Responsive design (mobile-first)
- Accessible (ARIA labels, keyboard nav)
- Loading states and error handling
- Toast notifications

---

## 🔧 Development

### Available Scripts

```bash
npm run dev          # Start dev server (HMR)
npm run build        # Build for production
npm run preview      # Preview production build
npm run lint         # ESLint check
npm run lint:fix     # ESLint auto-fix
```

### Environment Variables

```bash
# API Configuration
VITE_API_URL=http://localhost:8080/api
VITE_WS_URL=ws://localhost:8080/ws

# Feature Flags
VITE_ENABLE_ANALYTICS=true
VITE_ENABLE_CODE_GRAPH=true
```

### Code Splitting

Pages are lazy-loaded for optimal performance:

```tsx
const DashboardPage = lazy(() => import('./pages/v2/DashboardPage'))
const CodeGraphPage = lazy(() => import('./pages/v2/CodeGraphPage'))
```

---

## 🌐 Internationalization (i18n)

The frontend supports 3 languages:

- **English (en)** - Default
- **Portuguese (pt)** - Português Brasileiro
- **Spanish (es)** - Español

### Adding Translations

1. Add keys to `src/lib/translations.ts`:

```typescript
export const translations = {
  en: {
    mySection: {
      title: "My Title",
      description: "My Description",
    },
  },
  pt: {
    mySection: {
      title: "Meu Título",
      description: "Minha Descrição",
    },
  },
  es: {
    mySection: {
      title: "Mi Título",
      description: "Mi Descripción",
    },
  },
}
```

2. Use in components:

```tsx
import { useLanguage } from "@/components/language-provider"

export function MyComponent() {
  const { t } = useLanguage()
  return <h1>{t.mySection.title}</h1>
}
```

---

## 🎨 Styling Guidelines

### Color Palette

```css
/* Primary (Purple) */
--primary: 147 51 234        /* #9333EA */
--primary-hover: 126 34 206   /* #7E22CE */

/* Secondary (Green) */
--secondary: 34 197 94       /* #22C55E */

/* Accent (Blue) */
--accent: 59 130 246         /* #3B82F6 */
```

### Component Patterns

**Button with forced styles:**

```tsx
<Button className="!bg-purple-600 hover:!bg-purple-700">
  Click Me
</Button>
```

**Dark mode support:**

```tsx
<div className="bg-white dark:bg-gray-800 text-gray-900 dark:text-white">
  Content
</div>
```

---

## 📊 State Management

### Zustand Store (V2)

```typescript
// store/v2-store.ts
interface V2Store {
  // User
  user: User | null
  setUser: (user: User) => void

  // Reviews
  reviews: Review[]
  setReviews: (reviews: Review[]) => void

  // Filters
  filters: ReviewFilters
  setFilters: (filters: ReviewFilters) => void
}
```

### TanStack Query (Server State)

```typescript
// hooks/use-reviews.ts
export function useReviews() {
  return useQuery({
    queryKey: ['reviews'],
    queryFn: () => api.reviews.list(),
  })
}
```

---

## 🔌 API Integration

### API Client

```typescript
// lib/api.ts
class PullwiseAPI {
  private baseURL: string

  async getReviews(): Promise<Review[]> {
    const response = await fetch(`${this.baseURL}/reviews`)
    return response.json()
  }
}

export const api = new PullwiseAPI(import.meta.env.VITE_API_URL)
```

### WebSocket Connection

```typescript
// hooks/use-websocket.ts
export function useWebSocket() {
  const [socket, setSocket] = useState<WebSocket | null>(null)

  useEffect(() => {
    const ws = new WebSocket(import.meta.env.VITE_WS_URL)
    setSocket(ws)

    ws.onmessage = (event) => {
      const data = JSON.parse(event.data)
      // Handle update
    }

    return () => ws.close()
  }, [])

  return socket
}
```

---

## 🧩 Component Architecture

### Example: Landing Page Component

```tsx
// components/landing/Hero.tsx
import { useLanguage } from "@/components/language-provider"
import { Button } from "@/components/ui/button"

export default function Hero() {
  const { t } = useLanguage()

  return (
    <section>
      <h1>{t.hero.headline}</h1>
      <p>{t.hero.subheadline}</p>
      <Button>{t.hero.ctaPrimary}</Button>
    </section>
  )
}
```

### Example: V2 Page with Data Fetching

```tsx
// pages/v2/DashboardPage.tsx
import { useReviews } from "@/hooks/use-reviews"

export function DashboardPage() {
  const { data: reviews, isLoading } = useReviews()

  if (isLoading) return <LoadingSpinner />

  return (
    <div>
      {reviews?.map(review => (
        <ReviewCard key={review.id} review={review} />
      ))}
    </div>
  )
}
```

---

## 🚀 Deployment

### Netlify

```bash
# Build
npm run build

# Deploy
netlify deploy --prod --dir=dist
```

Or connect the repo to Netlify for auto-deploys on push.

### Vercel

```bash
vercel --prod
```

### Docker

```bash
docker build -t pullwise-frontend .
docker run -p 3000:80 pullwise-frontend
```

---

## 🤝 Contributing

We appreciate contributions! Here's how to get started:

1. **Fork** the repository
2. **Create a branch**: `git checkout -b feature/amazing-feature`
3. **Make changes** following our code style
4. **Test**: `npm run lint && npm run build`
5. **Commit**: `git commit -m "Add amazing feature"`
6. **Push**: `git push origin feature/amazing-feature`
7. **Open Pull Request**

### Code Style

- Use **TypeScript** strict mode
- Follow **ESLint** rules
- Use **Prettier** for formatting
- Write **meaningful** commit messages
- Add **comments** for complex logic

### Areas We Need Help

- [ ] Mobile responsive improvements
- [ ] Accessibility enhancements
- [ ] Performance optimizations
- [ ] Additional language translations
- [ ] Unit tests coverage
- [ ] E2E tests with Playwright

---

## 📝 License

MIT License - see [LICENSE](../LICENSE) for details.

---

## 📞 Support

- **Issues**: [GitHub Issues](https://github.com/integralltech/pullwise-ai/issues)
- **Discord**: [Join Community](https://discord.gg/pullwise)
- **Email**: [hello@pullwise.ai](mailto:hello@pullwise.ai)
