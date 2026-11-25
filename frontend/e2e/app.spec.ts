import { expect, test, type Page } from '@playwright/test'

// Unique names per run: the e2e database is shared across runs.
const RUN = Date.now().toString(36)
const PROJECT = `E2E Home ${RUN}`
const TASK = `Pay bills ${RUN}`

function isoDate(daysFromToday: number): string {
  const d = new Date()
  d.setDate(d.getDate() + daysFromToday)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

async function signIn(page: Page, provider: 'Google' | 'GitHub') {
  await page.goto('/login')
  await page.getByRole('link', { name: `Continue with ${provider}` }).click()
  await expect(page.getByRole('heading', { name: 'All tasks' })).toBeVisible()
}

test('Google sign-in: create project and task, search, overdue notification, logout', async ({
  page,
}) => {
  await signIn(page, 'Google')

  // Session survives a reload.
  await page.reload()
  await expect(page.getByRole('heading', { name: 'All tasks' })).toBeVisible()

  // Create a list (project).
  const sidebar = page.getByRole('navigation', { name: 'Projects' })
  await sidebar.getByLabel('New project').fill(PROJECT)
  await sidebar.getByRole('button', { name: 'Add project' }).click()
  await page.getByRole('link', { name: new RegExp(PROJECT) }).click()
  await expect(page).toHaveURL(/\/projects\//)

  // Create an overdue task in that list.
  await page.getByRole('button', { name: 'New task' }).click()
  const dialog = page.getByRole('dialog')
  await dialog.getByLabel('Title', { exact: true }).fill(TASK)
  await dialog.getByLabel('Due date', { exact: true }).fill(isoDate(-1))
  await dialog.getByRole('button', { name: 'Create task' }).click()
  await expect(dialog).toBeHidden()
  const tasks = page.getByRole('list', { name: 'Tasks' })
  await expect(tasks.getByText(TASK)).toBeVisible()

  // Notifications: the scheduler (2 s interval in the e2e profile) reports the overdue task.
  await expect(page.getByRole('button', { name: /need attention/ })).toBeVisible({
    timeout: 20_000,
  })

  // Search filter: matching term shows the task, non-matching term shows the empty state.
  const search = page.getByLabel('Search', { exact: true })
  await search.fill(TASK.slice(0, 7))
  await expect(tasks.getByText(TASK)).toBeVisible()
  await search.fill(`nothing-${RUN}`)
  await expect(page.getByText('No tasks match your filters')).toBeVisible()
  await page.getByRole('button', { name: 'Reset', exact: true }).click()
  await expect(tasks.getByText(TASK)).toBeVisible()

  // Logout ends the session.
  const logoutResponse = page.waitForResponse(
    (res) => res.request().method() === 'POST' && res.url().endsWith('/api/v1/logout'),
  )
  await page.getByRole('button', { name: 'Log out' }).click()
  expect((await logoutResponse).status()).toBe(204)
  await expect(page).toHaveURL(/\/login$/)
  await page.goto('/')
  await expect(page).toHaveURL(/\/login$/)
})

test('GitHub sign-in is a separate account: Google-created lists are not visible', async ({
  page,
}) => {
  await signIn(page, 'GitHub')
  await expect(page.getByRole('navigation', { name: 'Projects' })).toBeVisible()
  await expect(page.getByRole('link', { name: new RegExp(PROJECT) })).toHaveCount(0)
})
