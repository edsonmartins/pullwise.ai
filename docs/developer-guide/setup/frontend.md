# Frontend Setup

Set up your development environment for Pullwise frontend development.

## Prerequisites

### Required Software

| Software | Version | Install |
|----------|---------|---------|
| **Node.js** | 20+ | [Nodejs.org](https://nodejs.org/) |
| **npm** | 10+ | Included with Node |
| **Git** | Latest | [Git-scm.com](https://git-scm.com/) |

### Recommended Tools

| Tool | Purpose |
|------|---------|
| **VS Code** | IDE (recommended) |
| **Chrome DevTools** | Debugging |
| **React DevTools** | React debugging |

## Installation

### Install Node.js

```bash
# Using nvm (recommended)
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash

# Install Node.js 20
nvm install 20
nvm use 20

# Verify
node --version
npm --version
```

### macOS

```bash
# Using Homebrew
brew install node@20

# Link
brew link node@20
```

### Ubuntu/Debian

```bash
# Using NodeSource
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install -y nodejs
```

### Windows

Download and install from [nodejs.org](https://nodejs.org/).

## Clone Repository

```bash
# Clone the repository
git clone https://github.com/integralltech/pullwise-ai.git
cd pullwise-ai/frontend

# Verify directory structure
ls -la
```

## Install Dependencies

```bash
# Install dependencies
npm install

# This installs:
# - React 18
# - TypeScript 5
# - Vite 5
# - Mantine UI
# - TanStack Query
# - And all other dependencies
```

### Install Time

Initial install may take 2-5 minutes depending on your connection.

## Environment Variables

### Create .env File

Create `.env` in the `frontend/` directory:

```bash
# API Configuration
VITE_API_URL=http://localhost:8080/api
VITE_WS_URL=ws://localhost:8080/ws

# Feature Flags
VITE_ENABLE_ANALYTICS=true
VITE_ENABLE_CODE_GRAPH=false

# Development
VITE_DEV_TOOLS=true
```

### .env.example Reference

```bash
cp .env.example .env
# Edit .env with your values
```

## IDE Configuration

### VS Code

1. **Install VS Code**: [code.visualstudio.com](https://code.visualstudio.com/)

2. **Recommended Extensions**:

```bash
# Install from command line
code --install-extension dbaeumer.vscode-eslint
code --install-extension esbenp.prettier-vscode
code --install-extension ms-python.vscode-python
code --install-extension bradlc.vscode-tailwindcss
code --install-extension usernamehw.errorlens
code --install-extension eamodio.gitlens
```

3. **Workspace Settings** (`.vscode/settings.json`):

```json
{
  "typescript.tsdk": "node_modules/typescript/lib",
  "typescript.enablePromptUseWorkspaceTsdk": true,
  "editor.formatOnSave": true,
  "editor.defaultFormatter": "esbenp.prettier-vscode",
  "editor.codeActionsOnSave": {
    "source.fixAll.eslint": "explicit"
  },
  "[typescript]": {
    "editor.defaultFormatter": "esbenp.prettier-vscode"
  },
  "[typescriptreact]": {
    "editor.defaultFormatter": "esbenp.prettier-vscode"
  }
}
```

### WebStorm

1. **Preferences** → **Languages & Frameworks** → **TypeScript**
2. Set **TypeScript language version** to **5.0**
3. Enable **ESLint** and **Prettier**
4. Configure **Run Configuration** for npm scripts

## Run Development Server

### Start Dev Server

```bash
# Start development server
npm run dev

# Access at http://localhost:3000
```

### Available Scripts

```bash
npm run dev          # Start dev server
npm run build        # Build for production
npm run preview      # Preview production build
npm run lint         # ESLint check
npm run lint:fix     # ESLint auto-fix
npm run type-check   # TypeScript check
```

## Hot Module Replacement

Vite provides HMR for fast development:

- Changes to React components refresh automatically
- State is preserved during HMR
- CSS updates apply without full refresh

## Backend Connection

### Proxy Configuration

Vite proxies API requests to backend:

```typescript
// vite.config.ts
export default defineConfig({
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/ws': {
        target: 'ws://localhost:8080',
        ws: true,
      },
    },
  },
})
```

### Without Backend

Test UI without backend:

```bash
# Start with mock data
VITE_WITH_MOCKS=true npm run dev
```

## Build for Production

### Production Build

```bash
# Build optimized bundle
npm run build

# Output in dist/ directory
ls -lh dist/
```

### Test Production Build

```bash
# Preview production build
npm run preview

# Access at http://localhost:4173
```

### Build Size

Typical build sizes:

```yaml
dist/
├── assets/
│   ├── index-[hash].js      ~200 KB (gzip: ~60 KB)
│   ├── index-[hash].css     ~50 KB (gzip: ~15 KB)
│   └── vendor-[hash].js     ~300 KB (gzip: ~90 KB)
└── index.html               ~2 KB
```

## Testing

### Unit Tests

```bash
# Run tests
npm run test

# Watch mode
npm run test:watch

# Coverage
npm run test:coverage
```

### E2E Tests (if configured)

```bash
# Install Playwright
npm install -D @playwright/test

# Run E2E tests
npm run test:e2e
```

## Troubleshooting

### Port Already in Use

```bash
# Find process
lsof -i :3000

# Kill process
kill -9 <PID>

# Or use different port
npm run dev -- --port 3001
```

### Module Not Found

```bash
# Clear node_modules and reinstall
rm -rf node_modules package-lock.json
npm install
```

### Type Errors

```bash
# Regenerate TypeScript types
npm run type-check

# Clear cache
rm -rf node_modules/.vite
```

### HMR Not Working

```bash
# Clear Vite cache
rm -rf node_modules/.vite

# Restart dev server
npm run dev
```

## Development Tips

### 1. Use TypeScript Strict Mode

```json
// tsconfig.json
{
  "compilerOptions": {
    "strict": true,
    "noUncheckedIndexedAccess": true
  }
}
```

### 2. Enable ESLint

```bash
# Fix all auto-fixable issues
npm run lint:fix
```

### 3. Use React DevTools

```bash
# Install React DevTools browser extension
# https://react.dev/learn/react-developer-tools
```

### 4. Profile Performance

```bash
# Use React Profiler to identify slow components
# Record, interact, stop, analyze
```

## Next Steps

- [Backend Setup](/docs/developer-guide/setup/backend) - Backend environment
- [IDE Configuration](/docs/developer-guide/setup/ide) - IDE tips
- [Frontend Architecture](/docs/developer-guide/architecture/frontend-architecture) - Frontend architecture
