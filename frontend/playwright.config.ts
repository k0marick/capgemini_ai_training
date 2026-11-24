import { defineConfig, devices } from '@playwright/test'

// E2E stack: OAuth stub (9999) -> Spring Boot "e2e" profile (8080) -> Vite dev server (5173).
// Requires a local MySQL 8.0 with schema todo_manager_e2e (or E2E_MYSQL_DB) and the TEST_MYSQL_* credentials.
const isCI = !!process.env.CI
const E2E_BACKEND_PORT = 8081

export default defineConfig({
  testDir: './e2e',
  testMatch: '**/*.spec.ts',
  fullyParallel: false,
  workers: 1,
  retries: isCI ? 1 : 0,
  reporter: isCI ? [['list'], ['html', { open: 'never' }]] : 'list',
  use: {
    baseURL: 'http://localhost:5173',
    trace: 'retain-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: [
    {
      command: 'node e2e/stub-providers.mjs',
      url: 'http://localhost:9999/.well-known/openid-configuration',
      reuseExistingServer: !isCI,
    },
    {
      // Own port and never reused: a backend already on 8080 may run another profile (real OAuth).
      // sh runs the Unix wrapper on Windows (Git Bash) and Linux alike.
      command: `sh ./mvnw -q spring-boot:run -Dspring-boot.run.profiles=e2e`,
      cwd: '../backend',
      url: `http://localhost:${E2E_BACKEND_PORT}/actuator/health`,
      env: { SERVER_PORT: String(E2E_BACKEND_PORT) },
      timeout: 300_000,
      reuseExistingServer: false,
    },
    {
      command: 'npm run dev',
      url: 'http://localhost:5173',
      env: { BACKEND_URL: `http://localhost:${E2E_BACKEND_PORT}` },
      reuseExistingServer: false,
    },
  ],
})
