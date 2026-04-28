import { test, expect } from '@playwright/test'

/**
 * Smoke E2E do BlastRadiusCard renderizado dentro do ReviewDetailPage.
 *
 * Mocka as APIs envolvidas para isolar a UI. Requer browsers instalados:
 *   npx playwright install chromium
 */
test.describe('BlastRadiusCard on review detail', () => {
  test.beforeEach(async ({ page }) => {
    // Bypass de auth: injeta token e usuário no localStorage antes de qualquer navegação
    await page.addInitScript(() => {
      localStorage.setItem('token', 'test-token')
    })

    // Mock /api/auth/me
    await page.route('**/api/auth/me', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: 1,
          email: 'test@example.com',
          username: 'tester',
          displayName: 'Test User',
          createdAt: '2026-01-01T00:00:00Z',
        }),
      })
    })

    await page.route('**/api/organizations', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: '[]' })
    })

    // Mock review
    await page.route('**/api/reviews/123', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: 123,
          pullRequestId: 1,
          status: 'COMPLETED',
          sastEnabled: true,
          llmEnabled: true,
          ragEnabled: false,
          createdAt: '2026-04-28T00:00:00Z',
          stats: { total: 0, critical: 0, high: 0, medium: 0, low: 0, info: 0 },
        }),
      })
    })

    // Mock issues
    await page.route('**/api/reviews/123/issues', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: '[]' })
    })

    // Mock blast-radius — núcleo do teste
    await page.route('**/api/reviews/123/blast-radius', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          impactedNodes: [
            {
              qualifiedName: 'com.x.AuthService',
              filePath: '/src/AuthService.java',
              depth: 1,
              propagatedConfidence: 1.0,
              riskScore: 0.85,
              riskBreakdown: { depth_inverse: 0.5, hotspot: 0.5, security: 1.0, test_gap: 1.0, confidence: 1.0 },
            },
            {
              qualifiedName: 'com.x.UserRepo',
              filePath: '/src/UserRepo.java',
              depth: 2,
              propagatedConfidence: 0.7,
              riskScore: 0.42,
              riskBreakdown: {},
            },
          ],
          impactedFiles: ['/src/AuthService.java', '/src/UserRepo.java'],
          truncated: false,
          seedCount: 1,
        }),
      })
    })
  })

  test('renderiza card com top impacted symbols', async ({ page }) => {
    await page.goto('/reviews/123')

    // Header do card
    await expect(page.getByText('Blast Radius', { exact: true })).toBeVisible({ timeout: 10000 })

    // Stats
    await expect(page.getByText(/seeds/)).toBeVisible()
    await expect(page.getByText(/impacted nodes/)).toBeVisible()

    // Top symbols listados
    await expect(page.getByText('com.x.AuthService')).toBeVisible()
    await expect(page.getByText('com.x.UserRepo')).toBeVisible()

    // Risk badge alto deve aparecer
    await expect(page.getByText('risk 0.85')).toBeVisible()
  })

  test('mostra mensagem vazia quando blast radius sem nós', async ({ page }) => {
    await page.unroute('**/api/reviews/123/blast-radius')
    await page.route('**/api/reviews/123/blast-radius', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          impactedNodes: [],
          impactedFiles: [],
          truncated: false,
          seedCount: 0,
        }),
      })
    })

    await page.goto('/reviews/123')
    await expect(page.getByText(/No downstream impact detected/i)).toBeVisible()
  })
})
