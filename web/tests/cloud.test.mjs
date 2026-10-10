// Tests du portage cloud (web/src/cloud.js) — AUCUN réseau : uniquement les fonctions
// pures (parsing, URLs, PKCE, mapAuthError) et les vecteurs calculés hors ligne.
// Vecteur SHA-256 indépendant : `printf 'abc' | sha256sum` →
// ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad, base64url =
// ungWv48Bz-pBQUDeXa4iI7ADYaOWF3qctBD_YfIAFa0. Second vecteur : RFC 7636 appendice B.
import {
  SUPABASE_URL,
  ANON_KEY,
  USER_AGENT,
  TIMEOUT_MS,
  CONNECT_TIMEOUT_MS,
  READ_TIMEOUT_MS,
  CloudError,
  NETWORK_ERROR,
  parseSession,
  mapAuthError,
  googleAuthorizeUrl,
  randomPkceVerifier,
  pkceChallenge,
  isSessionExpired,
  parsePullBody,
  parsePushBody,
} from '../src/cloud.js';

function assert(cond, msg) {
  if (!cond) throw new Error(msg);
}

function assertEq(actual, expected, label) {
  if (actual !== expected) {
    throw new Error(`${label}: attendu ${JSON.stringify(expected)}, obtenu ${JSON.stringify(actual)}`);
  }
}

// ---- constantes recopiées à l'identique de Cloud.kt ----
export function testConstantsExactes() {
  assertEq(SUPABASE_URL, 'https://api.webtvmedia.net', 'SUPABASE_URL');
  assertEq(
    ANON_KEY,
    'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJyb2xlIjoiYW5vbiIsImlzcyI6InN1cGFiYXNlIiwiaWF0IjoxNzc2ODY2NDUyLCJleHAiOjIwOTIyMjY0NTJ9.jOfn90sK6YeY6LRRuwzdiZpiO-s8pN4Ozr418B8iRXE',
    'ANON_KEY'
  );
  assertEq(CONNECT_TIMEOUT_MS, 12000, 'connect timeout Android');
  assertEq(READ_TIMEOUT_MS, 25000, 'read timeout Android');
  assertEq(TIMEOUT_MS, READ_TIMEOUT_MS, 'timeout total = read timeout');
  assert(USER_AGENT.startsWith('Mozilla/5.0') && USER_AGENT.includes('Liftbook/'), 'User-Agent Liftbook');
}

// ---- googleAuthorizeUrl : format, encodage, PAS de state ----
export function testGoogleAuthorizeUrl() {
  const url = googleAuthorizeUrl('verifier-quelconque', 'le-challenge', 'https://app.example.com/auth-callback');
  assertEq(
    url,
    'https://api.webtvmedia.net/auth/v1/authorize?provider=google' +
      '&redirect_to=https%3A%2F%2Fapp.example.com%2Fauth-callback' +
      '&flow_type=pkce&code_challenge=le-challenge&code_challenge_method=s256',
    'URL exacte'
  );
  assert(url.includes('provider=google'), 'provider=google');
  assert(url.includes('flow_type=pkce'), 'flow_type=pkce');
  assert(url.includes('code_challenge=le-challenge'), 'code_challenge porté');
  assert(url.includes('code_challenge_method=s256'), 'code_challenge_method=s256');
  // ⚠️ bug GoTrue v2.186 : JAMAIS de paramètre state (il écrase le flow-state → bad_oauth_state)
  assert(!/[?&]state=/.test(url), 'aucun paramètre state');
}

export function testGoogleAuthorizeUrlRedirectEncode() {
  // un redirect avec query string doit être intégralement encodé (pas de & ni ? parasites)
  const url = googleAuthorizeUrl('v', 'c', 'https://app.example.com/cb?next=/home&x=1');
  const redirectTo = new URL(url).searchParams.get('redirect_to');
  assertEq(redirectTo, 'https://app.example.com/cb?next=/home&x=1', 'redirect_to décodable intact');
  assert(!url.includes('next=/home&x=1&'), 'le & du redirect ne doit pas fuiter hors encodage');
  assert(url.includes('redirect_to=https%3A%2F%2Fapp.example.com%2Fcb%3Fnext%3D%2Fhome%26x%3D1'), 'encodage complet');
}

// ---- PKCE ----
export async function testPkceChallengeVecteurs() {
  // vecteur 1 : SHA-256("abc") calculé via sha256sum + base64url (indépendant du code testé)
  assertEq(await pkceChallenge('abc'), 'ungWv48Bz-pBQUDeXa4iI7ADYaOWF3qctBD_YfIAFa0', 'SHA-256("abc") base64url');
  // vecteur 2 : RFC 7636 appendice B
  assertEq(
    await pkceChallenge('dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk'),
    'E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM',
    'vecteur RFC 7636'
  );
}

export function testRandomPkceVerifier() {
  const re = /^[A-Za-z0-9_-]+$/;
  const a = randomPkceVerifier();
  const b = randomPkceVerifier();
  // 48 octets aléatoires → 64 chars base64url SANS padding (jamais de + / =)
  assert(a.length >= 43 && a.length <= 64, `longueur ${a.length} hors bornes 43-64`);
  assert(re.test(a), 'charset base64url uniquement');
  assert(!a.includes('=') && !a.includes('+') && !a.includes('/'), 'pas de padding ni + ni /');
  assert(a !== b, 'deux tirages doivent différer');
}

// ---- expiration de session (marge 60 s par défaut, comme Cloud.refreshIfNeeded) ----
export function testIsSessionExpired() {
  const s = { expiresAt: 1_000_000 };
  assert(!isSessionExpired(s, 1_000_000 - 60_001), 'encore 60,001 s → non expiré');
  assert(isSessionExpired(s, 1_000_000 - 60_000), 'exactement 60 s → expiré (frontière incluse)');
  assert(isSessionExpired(s, 1_000_000 - 1), 'presque expiré');
  assert(!isSessionExpired(s, 1_000_000 - 30_000, 5_000), 'marge personnalisée 5 s');
  assert(isSessionExpired(s, 1_000_000 - 4_000, 5_000), 'marge personnalisée 5 s, sous la marge');
  assert(isSessionExpired(null, Date.now()), 'pas de session → expiré');
}

// ---- mapAuthError : messages FR EXACTS de Cloud.mapAuthError ----
export function testMapAuthError() {
  assertEq(mapAuthError(429, ''), 'Trop d\'essais — réessaie dans un instant.', '429');
  assertEq(mapAuthError(400, 'User already registered'), 'Cet email a déjà un compte. Connecte-toi.', 'already');
  assertEq(mapAuthError(400, '{"code":400,"msg":"already exists"}'), 'Cet email a déjà un compte. Connecte-toi.', 'already (json)');
  assertEq(mapAuthError(400, 'invalid_grant: Email not confirmed'), 'Email ou mot de passe incorrect.', 'invalid_grant');
  assertEq(mapAuthError(400, 'Invalid login credentials'), 'Email ou mot de passe incorrect.', 'invalid login');
  assertEq(mapAuthError(400, 'Password should be at least 6 characters.'), 'Mot de passe trop court (6 caractères minimum).', 'at least character');
  assertEq(mapAuthError(422, 'validation failed: email is invalid'), 'Adresse email invalide.', 'validation email');
  assertEq(mapAuthError(500, 'boom'), 'Serveur indisponible — réessaie plus tard.', '5xx');
  assertEq(mapAuthError(503, ''), 'Serveur indisponible — réessaie plus tard.', '503');
  assertEq(mapAuthError(418, 'teapot'), 'Erreur inattendue (418).', 'défaut');
  assertEq(mapAuthError(0, ''), 'Erreur inattendue (0).', 'défaut status 0');
  // ordre du when Kotlin : le corps est testé AVANT code >= 500
  assertEq(mapAuthError(500, 'User already registered'), 'Cet email a déjà un compte. Connecte-toi.', 'corps prioritaire sur 5xx');
}

// ---- parsing d'une réponse GoTrue (expires_in secondes → expiresAt ms) ----
export function testParseSessionReponseGotrue() {
  const reponse = {
    access_token: 'jwt-access',
    token_type: 'bearer',
    expires_in: 3600,
    refresh_token: 'jwt-refresh',
    user: { id: '9587ea84-48fb-440a-be18-a41189a26548', email: 'Me@Example.com' },
  };
  const s = parseSession(reponse, { nowMs: 1_000 });
  assertEq(s.access, 'jwt-access', 'access');
  assertEq(s.refresh, 'jwt-refresh', 'refresh');
  assertEq(s.userId, '9587ea84-48fb-440a-be18-a41189a26548', 'userId');
  assertEq(s.email, 'Me@Example.com', 'email depuis user.email');
  assertEq(s.expiresAt, 1_000 + 3600 * 1000, 'expires_in secondes → +3 600 000 ms');
  // expiresIn non entier / absent → défaut 3600 s (fallback Kotlin)
  assertEq(parseSession({ access_token: 'a' }, { nowMs: 0 }).expiresAt, 3_600_000, 'défaut expires_in=3600');
}

export function testParseSessionFallbacks() {
  // fallbacks recopiés de Cloud.kt : user.id ?? "", user.email ?? "", refresh_token ?? ""
  const s = parseSession({ access_token: 'tok' }, { nowMs: 10_000 });
  assertEq(s.userId, '', 'userId fallback');
  assertEq(s.email, '', 'email fallback');
  assertEq(s.refresh, '', 'refresh fallback');
  assertEq(s.expiresAt, 10_000 + 3_600_000, 'expires_in fallback');
  // le flux mot de passe fait foi pour l'email (Cloud.kt authenticate : trim + lowercase de la saisie)
  assertEq(
    parseSession({ access_token: 't', user: { id: 'u', email: 'other@x.io' } }, { email: 'me@example.com', nowMs: 0 }).email,
    'me@example.com',
    'email de la requête prioritaire'
  );
  // pas d'access_token → null (compte créé mais non confirmé / réponse vide)
  assert(parseSession({ user: { id: 'u' } }) === null, 'sans access_token → null');
  assert(parseSession(null) === null, 'réponse null → null');
  assert(parseSession('nope') === null, 'réponse non objet → null');
}

// ---- parsing pull/push (logique de pull()/push() sans réseau) ----
export function testParsePullBody() {
  assertEq(JSON.stringify(parsePullBody('[]')), '{"payload":null,"clientTs":0}', '0 ligne → null/0');
  assertEq(JSON.stringify(parsePullBody('[{"data":null,"client_ts":42}]')), '{"payload":null,"clientTs":42}', 'data null → payload null, ts conservé');
  const r = parsePullBody('[{"data":{"workouts":[1,2],"v":3},"client_ts":1759000000123}]');
  assertEq(r.clientTs, 1759000000123, 'client_ts');
  assertEq(JSON.stringify(r.payload), '{"workouts":[1,2],"v":3}', 'payload = objet data brut');
  assertEq(parsePullBody('[{"client_ts":9}]').clientTs, 9, 'ligne sans data');
  assertEq(JSON.stringify(parsePullBody('[{"data":"chaine","client_ts":1}]')), '{"payload":null,"clientTs":1}', 'data non-objet → payload null');
  assertEq(JSON.stringify(parsePullBody('')), '{"payload":null,"clientTs":0}', 'corps vide');
}

export function testParsePushBody() {
  assertEq(parsePushBody('123', 7), 123, 'nombre nu');
  assertEq(parsePushBody('123.5', 7), 123.5, 'nombre décimal');
  assertEq(parsePushBody('"456"', 7), 456, 'nombre en chaîne (jsonPrimitive.long Kotlin)');
  assertEq(parsePushBody('', 99), 99, 'corps vide → ts émis');
  assertEq(parsePushBody('null', 99), 99, 'null JSON → ts émis');
  assertEq(parsePushBody('"abc"', 99), 99, 'non numérique → ts émis');
  let threw = false;
  try { parsePushBody('{', 5); } catch { threw = true; }
  assert(!threw, 'JSON invalide ne doit pas jeter');
  assertEq(parsePushBody('{', 5), 5, 'JSON invalide → ts émis (getOrDefault Kotlin)');
}

// ---- CloudError : forme et messages par défaut ----
export function testCloudErrorDefaults() {
  const net = new CloudError(0, '');
  assertEq(net.message, NETWORK_ERROR, 'message réseau par défaut');
  assertEq(net.message, 'Connexion impossible — vérifie Internet.', 'texte FR exact');
  assertEq(new CloudError(503, 'oops').message, 'Erreur inattendue (503).', 'message défaut non-réseau');
  assert(new CloudError(400, 'b') instanceof Error, 'CloudError est une Error');
  assertEq(new CloudError(400, 'corps').body, 'corps', 'body conservé');
  assertEq(new CloudError(400).status, 400, 'status conservé');
  assertEq(new CloudError(400, 'x', 'Email ou mot de passe incorrect.').message, 'Email ou mot de passe incorrect.', 'message explicite');
}
