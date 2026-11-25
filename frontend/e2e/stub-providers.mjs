// Stub Google (OIDC) and GitHub (OAuth 2.0) providers for Playwright E2E runs.
// Matches backend/src/main/resources/application-e2e.yml. No external network and no credentials.
// Run: node e2e/stub-providers.mjs   (port 9999, override with STUB_PORT)
import { createServer } from 'node:http'
import { generateKeyPairSync, createSign, randomBytes } from 'node:crypto'

const PORT = Number(process.env.STUB_PORT ?? 9999)
const BASE = `http://localhost:${PORT}`
const GOOGLE_CLIENT_ID = 'e2e-google-client'
const KID = 'e2e-key'

const GOOGLE_USER = {
  sub: 'e2e-google-sub-1',
  name: 'E2E Google User',
  email: 'e2e.google@example.com',
  email_verified: true,
  picture: 'https://example.com/google.png',
}
const GITHUB_USER = {
  id: 4242,
  login: 'e2e-octocat',
  name: 'E2E GitHub User',
  email: null,
  avatar_url: 'https://example.com/github.png',
}

const { privateKey, publicKey } = generateKeyPairSync('rsa', { modulusLength: 2048 })
const jwk = { ...publicKey.export({ format: 'jwk' }), kid: KID, alg: 'RS256', use: 'sig' }

/** authorization code -> { nonce } issued by /authorize, consumed by /token. */
const codes = new Map()

const b64url = (input) => Buffer.from(input).toString('base64url')

function signIdToken(claims) {
  const header = b64url(JSON.stringify({ alg: 'RS256', typ: 'JWT', kid: KID }))
  const payload = b64url(JSON.stringify(claims))
  const signer = createSign('RSA-SHA256')
  signer.update(`${header}.${payload}`)
  return `${header}.${payload}.${signer.sign(privateKey).toString('base64url')}`
}

function json(res, status, body) {
  res.writeHead(status, { 'Content-Type': 'application/json' })
  res.end(JSON.stringify(body))
}

function redirect(res, location) {
  res.writeHead(302, { Location: location })
  res.end()
}

async function readForm(req) {
  let raw = ''
  for await (const chunk of req) raw += chunk
  return new URLSearchParams(raw)
}

const server = createServer(async (req, res) => {
  const url = new URL(req.url, BASE)
  const path = url.pathname

  // Google (OIDC)
  if (req.method === 'GET' && path === '/.well-known/openid-configuration') {
    return json(res, 200, {
      issuer: BASE,
      authorization_endpoint: `${BASE}/authorize`,
      token_endpoint: `${BASE}/token`,
      userinfo_endpoint: `${BASE}/userinfo`,
      jwks_uri: `${BASE}/jwks`,
      response_types_supported: ['code'],
      subject_types_supported: ['public'],
      id_token_signing_alg_values_supported: ['RS256'],
    })
  }
  if (req.method === 'GET' && path === '/jwks') return json(res, 200, { keys: [jwk] })
  if (req.method === 'GET' && path === '/authorize') {
    const code = randomBytes(16).toString('hex')
    codes.set(code, { nonce: url.searchParams.get('nonce') })
    const target = new URL(url.searchParams.get('redirect_uri'))
    target.searchParams.set('code', code)
    target.searchParams.set('state', url.searchParams.get('state') ?? '')
    return redirect(res, target.toString())
  }
  if (req.method === 'POST' && path === '/token') {
    const form = await readForm(req)
    const issued = codes.get(form.get('code'))
    if (!issued) return json(res, 400, { error: 'invalid_grant' })
    codes.delete(form.get('code'))
    const now = Math.floor(Date.now() / 1000)
    const idToken = signIdToken({
      iss: BASE,
      sub: GOOGLE_USER.sub,
      aud: GOOGLE_CLIENT_ID,
      iat: now,
      exp: now + 300,
      nonce: issued.nonce,
      name: GOOGLE_USER.name,
      email: GOOGLE_USER.email,
      email_verified: GOOGLE_USER.email_verified,
      picture: GOOGLE_USER.picture,
    })
    return json(res, 200, {
      access_token: 'e2e-google-access',
      token_type: 'Bearer',
      expires_in: 3600,
      id_token: idToken,
    })
  }
  if (req.method === 'GET' && path === '/userinfo') {
    return json(res, 200, GOOGLE_USER)
  }

  // GitHub (OAuth 2.0)
  if (req.method === 'GET' && path === '/github/authorize') {
    const code = randomBytes(16).toString('hex')
    const target = new URL(url.searchParams.get('redirect_uri'))
    target.searchParams.set('code', code)
    target.searchParams.set('state', url.searchParams.get('state') ?? '')
    return redirect(res, target.toString())
  }
  if (req.method === 'POST' && path === '/github/token') {
    return json(res, 200, {
      access_token: 'e2e-github-access',
      token_type: 'bearer',
      scope: 'read:user',
    })
  }
  if (req.method === 'GET' && path === '/github/user') {
    return json(res, 200, GITHUB_USER)
  }

  json(res, 404, { error: 'not_found', path })
})

server.listen(PORT, () => {
  console.log(`E2E OAuth stub listening on ${BASE}`)
})
