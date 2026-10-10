/**
 * Couche cloud du port web — portage direct de `Cloud.kt` (Android) / `Cloud.swift` (iOS).
 * Module ES isomorphe (navigateur + Node ≥ 18) : `fetch`, `crypto.subtle`, `crypto.getRandomValues`.
 *
 * PURE HTTP : aucune orchestration d'état (session persistée, debounce de sync, tombstones…
 * vivent dans l'app). Ce module expose uniquement l'authentification GoTrue (Supabase
 * self-hosté), les endpoints de sync snapshot et le storage.
 *
 * Session = objet JS `{ email, userId, access, refresh, expiresAt }` (expiresAt en ms epoch).
 * Erreurs : `CloudError` (status, body) — status 0 = réseau impossible / timeout.
 */

// ================= constantes (recopiées À L'IDENTIQUE de Cloud.kt) =================

/** URL de base du Supabase self-hosté (Cloud.kt : `BASE`). */
export const SUPABASE_URL = 'https://api.webtvmedia.net';

/** Clé anon GoTrue (Cloud.kt : `ANON`) — clé publique, envoyée dans le header `apikey`. */
export const ANON_KEY =
  'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJyb2xlIjoiYW5vbiIsImlzcyI6InN1cGFiYXNlIiwiaWF0IjoxNzc2ODY2NDUyLCJleHAiOjIwOTIyMjY0NTJ9.jOfn90sK6YeY6LRRuwzdiZpiO-s8pN4Ozr418B8iRXE';

/** User-Agent de Cloud.kt (`BuildConfig.VERSION_NAME`). Les navigateurs refusent
 *  de le modifier (forbidden header) : il est posé dans un try/catch et ignoré sinon. */
export const USER_AGENT = 'Mozilla/5.0 (Linux; Android 14) Liftbook/1.53';

/** Timeouts Android (Cloud.kt : connectTimeout 12 s, readTimeout 25 s). Un AbortController
 *  ne sait découper que le total — on borne donc chaque requête au read timeout. */
export const CONNECT_TIMEOUT_MS = 12_000;
export const READ_TIMEOUT_MS = 25_000;
export const TIMEOUT_MS = READ_TIMEOUT_MS;

/** Message FR exact de Cloud.kt pour un échec réseau (hors ligne, DNS, timeout). */
export const NETWORK_ERROR = 'Connexion impossible — vérifie Internet.';

// ================= erreurs =================

/** Erreur HTTP/réseau de la couche cloud. `status` = code HTTP (0 = réseau/timeout),
 *  `body` = corps de réponse brut, `message` = texte FR affichable (mapAuthError). */
export class CloudError extends Error {
  constructor(status, body = '', message) {
    super(message ?? (status === 0 ? NETWORK_ERROR : `Erreur inattendue (${status}).`));
    this.name = 'CloudError';
    this.status = status;
    this.body = body;
  }
}

// ================= http maison (port de Cloud.http) =================

/**
 * Port de `Cloud.http` : apikey sur TOUTES les requêtes, Authorization Bearer si session,
 * Content-Type: application/json UNIQUEMENT quand il y a un corps (le storage-api Fastify
 * rejette en 400 les DELETE sans corps déclarés application/json), User-Agent en best-effort
 * (forbidden header côté navigateur → silencieusement ignoré), timeout via AbortController.
 *
 * @param {string} method
 * @param {string} path chemin relatif commençant par `/`
 * @param {{body?:string, bearer?:string, raw?:ArrayBuffer|Uint8Array|Blob,
 *          headers?:Record<string,string>, binary?:boolean, timeoutMs?:number}} [opts]
 *   - `body` : corps texte JSON ; `raw` : corps binaire (jpeg…) — Content-Type à fournir
 *     dans `headers` pour un binaire.
 *   - `binary: true` → la réponse est renvoyée en `bytes` (ArrayBuffer) au lieu de `text`.
 * @returns {Promise<{status:number, text?:string, bytes?:ArrayBuffer}>}
 * @throws {CloudError} status 0 si le réseau/le timeout échoue (comme une IOException Kotlin).
 */
async function http(method, path, opts = {}) {
  const { body = null, bearer = null, raw = null, headers: extraHeaders = {}, binary = false, timeoutMs = TIMEOUT_MS } = opts;
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  try {
    const headers = new Headers({ apikey: ANON_KEY });
    // jamais de content-type sans corps (cf. Cloud.kt http()) ; un Content-Type explicite
    // dans extraHeaders (ex: image/jpeg pour un upload) prime sur le défaut JSON.
    const hasBody = body != null || raw != null;
    const extraCt = Object.keys(extraHeaders).find((k) => k.toLowerCase() === 'content-type');
    if (extraCt != null) headers.set(extraCt, extraHeaders[extraCt]);
    else if (hasBody) headers.set('Content-Type', 'application/json');
    // ⚠️ User-Agent est un forbidden header en navigateur : ne jamais laisser ça casser la requête.
    try { headers.set('User-Agent', USER_AGENT); } catch { /* ignoré silencieusement */ }
    if (bearer != null) headers.set('Authorization', `Bearer ${bearer}`);
    for (const [k, v] of Object.entries(extraHeaders)) headers.set(k, v);

    let res;
    try {
      res = await fetch(SUPABASE_URL + path, {
        method,
        headers,
        body: raw != null ? raw : body != null ? body : undefined,
        signal: controller.signal,
      });
    } catch (e) {
      // offline / DNS / timeout (AbortError) — l'équivalent Kotlin remonte en Exception.
      throw new CloudError(0, '', NETWORK_ERROR);
    }
    let buf;
    try {
      buf = await res.arrayBuffer();
    } catch (e) {
      // coupure en cours de corps : même statut réseau que le fetch lui-même (sinon une
      // coupure pendant un refresh token déconnectait l'utilisateur à tort)
      throw new CloudError(0, '', NETWORK_ERROR);
    }
    return binary
      ? { status: res.status, bytes: buf }
      : { status: res.status, text: new TextDecoder().decode(buf) };
  } finally {
    clearTimeout(timer);
  }
}

const ok = (status) => status >= 200 && status <= 299;

// ================= parsing GoTrue (port de Cloud.applySession / applySessionResponse) =================

/**
 * Parse une réponse JSON GoTrue en session. Fonction PURE (testable sans réseau).
 * Fallbacks recopiés de Cloud.kt : user.id ?? "", user.email ?? "", refresh_token ?? "",
 * expires_in ?? 3600 (SECONDES → expiresAt = nowMs + expires_in * 1000).
 *
 * @param {object|null} obj réponse GoTrue parsée
 * @param {{email?:string, nowMs?:number}} [opts] `email` = email saisi (flux mot de passe :
 *        Cloud.kt `authenticate` fait confiance à l'email trim+lowercase de la requête).
 * @returns {{email:string, userId:string, access:string, refresh:string, expiresAt:number}|null}
 *          null si pas d'access_token (compte créé mais non confirmé, réponse vide…).
 */
export function parseSession(obj, opts = {}) {
  const { email, nowMs = Date.now() } = opts;
  const token = obj && typeof obj.access_token === 'string' ? obj.access_token : null;
  if (token == null) return null;
  const user = obj.user && typeof obj.user === 'object' ? obj.user : {};
  const expiresIn = Number.isFinite(obj.expires_in) ? obj.expires_in : 3600;
  return {
    email: email !== undefined ? email : typeof user.email === 'string' ? user.email : '',
    userId: typeof user.id === 'string' ? user.id : '',
    access: token,
    refresh: typeof obj.refresh_token === 'string' ? obj.refresh_token : '',
    expiresAt: nowMs + expiresIn * 1000,
  };
}

// ================= auth =================

async function authenticate(kind, emailRaw, password) {
  const email = String(emailRaw).trim().toLowerCase(); // comme Cloud.kt authenticate()
  const body = JSON.stringify({ email, password });
  const res = await http('POST', `/auth/v1/${kind}`, { body });
  if (!ok(res.status)) throw new CloudError(res.status, res.text, mapAuthError(res.status, res.text));
  let obj = null;
  try { obj = JSON.parse(res.text); } catch { /* traité par parseSession → null */ }
  const session = parseSession(obj, { email });
  if (session == null) {
    // compte créé mais non confirmé (GoTrue sans autoconfirm renvoie 200 sans session)
    throw new CloudError(res.status, res.text, 'Compte créé mais non confirmé — contacte l\'administrateur.');
  }
  return session;
}

/** Connexion mot de passe → session. @throws {CloudError} (message FR via mapAuthError). */
export async function signIn(email, password) {
  return authenticate('token?grant_type=password', email, password);
}

/** Inscription → session. GoTrue peut répondre 2xx SANS session (email de confirmation) :
 *  géré comme Cloud.kt → CloudError « Compte créé mais non confirmé… ». */
export async function signUp(email, password) {
  return authenticate('signup', email, password);
}

/**
 * Rafraîchit la session (grant_type=refresh_token). Port de Cloud.refreshIfNeeded + Cloud.swift
 * refresh : si la réponse ne contient pas de refresh_token (vide), on GARDE l'ancien.
 * @throws {CloudError} — status 0 = réseau : l'orchestrateur garde la session et retentera
 *         plus tard (comportement Kotlin « keep session, retry later »).
 */
export async function refreshSession(refreshToken) {
  const body = JSON.stringify({ refresh_token: refreshToken });
  const res = await http('POST', '/auth/v1/token?grant_type=refresh_token', { body });
  if (!ok(res.status)) throw new CloudError(res.status, res.text, mapAuthError(res.status, res.text));
  let obj = null;
  try { obj = JSON.parse(res.text); } catch { /* traité par parseSession → null */ }
  const session = parseSession(obj);
  if (session == null) throw new CloudError(res.status, res.text, 'Session Google invalide.');
  if (!session.refresh) session.refresh = refreshToken; // garde l'ancien refresh si réponse vide
  return session;
}

/** Déconnexion (révocation server-side). Best-effort comme Cloud.signOut : ne jette JAMAIS. */
export async function logout(session) {
  if (session == null) return;
  try {
    await http('POST', '/auth/v1/logout', { body: '{}', bearer: session.access });
  } catch { /* best effort — Cloud.kt enroule dans runCatching */ }
}

/** Messages FR EXACTS de Cloud.mapAuthError (ordre des when inclus : le corps est testé
 *  AVANT code >= 500). `body` est passé en minuscules avant comparaison. */
export function mapAuthError(status, body) {
  const b = String(body ?? '').toLowerCase();
  if (status === 429) return 'Trop d\'essais — réessaie dans un instant.';
  if (b.includes('already') || b.includes('registered')) return 'Cet email a déjà un compte. Connecte-toi.';
  if (b.includes('invalid_grant') || b.includes('invalid login')) return 'Email ou mot de passe incorrect.';
  if (b.includes('at least') && b.includes('character')) return 'Mot de passe trop court (8 caractères minimum).';
  if (b.includes('validation') && b.includes('email')) return 'Adresse email invalide.';
  if (status >= 500) return 'Serveur indisponible — réessaie plus tard.';
  return `Erreur inattendue (${status}).`;
}

// ================= Google OAuth (PKCE) =================

/**
 * URL d'autorisation Google. ⚠️ JAMAIS de paramètre `state` : GoTrue v2.186 le forwarde à
 * Google où il écrase l'UUID de flow de GoTrue, et le /callback rejette alors la réponse
 * (bad_oauth_state). Le CSRF est couvert par le code_verifier S256 (cf. Cloud.kt).
 *
 * @param {string} verifier code_verifier PKCE (non utilisé dans l'URL — porté par l'appelant)
 * @param {string} challenge pkceChallenge(verifier)
 * @param {string} redirectTo URL complète de rappel (ex: `${location.origin}/auth-callback`)
 */
export function googleAuthorizeUrl(verifier, challenge, redirectTo) {
  return (
    `${SUPABASE_URL}/auth/v1/authorize?provider=google` +
    `&redirect_to=${encodeURIComponent(redirectTo)}` +
    `&flow_type=pkce&code_challenge=${challenge}&code_challenge_method=s256`
  );
}

/**
 * Échange le code d'autorisation contre une session (POST /auth/v1/token?grant_type=pkce).
 * ⚠️ Le champ s'appelle `auth_code` (PAS `code`) — recopié de Cloud.kt handleAuthRedirect.
 * @throws {CloudError} (réseau → « Connexion impossible… », HTTP → « Connexion Google échouée (N) »)
 */
export async function exchangePkce(code, verifier) {
  const body = JSON.stringify({ auth_code: code, code_verifier: verifier });
  const res = await http('POST', '/auth/v1/token?grant_type=pkce', { body });
  if (!ok(res.status)) throw new CloudError(res.status, res.text, `Connexion Google échouée (${res.status}).`);
  let obj = null;
  try { obj = JSON.parse(res.text); } catch { /* traité par parseSession → null */ }
  const session = parseSession(obj);
  if (session == null) throw new CloudError(res.status, res.text, 'Session Google invalide.');
  return session;
}

/** 48 octets aléatoires en base64url sans padding (port de Cloud.generatePkceVerifier). */
export function randomPkceVerifier() {
  const bytes = crypto.getRandomValues(new Uint8Array(48));
  return base64url(bytes);
}

/** SHA-256 du verifier, base64url sans padding (port de Cloud.pkceChallenge). */
export async function pkceChallenge(verifier) {
  const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(verifier));
  return base64url(new Uint8Array(digest));
}

/** true s'il faut rafraîchir avant la prochaine requête (port de Cloud.refreshIfNeeded :
 *  marge de 60 s par défaut). `session` absent → true. */
export function isSessionExpired(session, nowMs = Date.now(), marginMs = 60_000) {
  if (session == null) return true;
  return session.expiresAt - nowMs <= marginMs;
}

// ================= sync snapshot =================

/** Parse la réponse du GET snapshot (fonction PURE, testable sans réseau). */
export function parsePullBody(text) {
  let arr = null;
  try { const v = JSON.parse(text); if (Array.isArray(v)) arr = v; } catch { /* → vide */ }
  if (arr == null || arr.length === 0) return { payload: null, clientTs: 0 };
  const row = arr[0] && typeof arr[0] === 'object' ? arr[0] : {};
  const n = Number(row.client_ts);
  const ts = Number.isFinite(n) ? n : 0;
  // Cloud.kt : `row["data"] as? JsonObject ?: return null to ts` — non-objet → payload null, ts conservé
  const payload = row.data && typeof row.data === 'object' && !Array.isArray(row.data) ? row.data : null;
  return { payload, clientTs: ts };
}

/**
 * GET la ligne snapshot du compte (PostgREST). 0 ligne → `{payload: null, clientTs: 0}`.
 * @returns {Promise<{payload:object|null, clientTs:number}>}
 * @throws {CloudError} (401 = unauthorized, comme Cloud.pull)
 */
export async function pullSnapshot(session) {
  const res = await http('GET', '/rest/v1/hevy_snapshots?select=data,client_ts', { bearer: session.access });
  if (res.status === 401) throw new CloudError(401, res.text, 'unauthorized');
  if (!ok(res.status)) throw new CloudError(res.status, res.text, `pull ${res.status}`);
  return parsePullBody(res.text);
}

/**
 * Parse la réponse du RPC push (fonction PURE) : nombre stocké côté serveur, sinon NULL.
 * ⚠️ Plus de repli sur le ts émis : marquer un push « réussi » sans accusé réel du serveur
 * effaçait dirtyAt alors que rien n'était stocké — les saisies disparaissaient au pull suivant.
 */
export function parsePushBody(text) {
  try {
    const v = JSON.parse(text);
    if (typeof v === 'number' && Number.isFinite(v)) return v;
    if (typeof v === 'string' && v.trim() !== '' && Number.isFinite(Number(v))) return Number(v);
  } catch { /* corps vide/non JSON → null */ }
  return null;
}

/**
 * POST le snapshot complet avec garde LWW (RPC hevy_push_snapshot).
 * @returns {Promise<number>} le ts maintenant stocké côté serveur (rejet si réponse inexploitable).
 * @throws {CloudError} (401 = unauthorized, comme Cloud.push)
 */
export async function pushSnapshot(session, payload, clientTs) {
  const body = JSON.stringify({ p_data: payload, p_client_ts: clientTs });
  const res = await http('POST', '/rest/v1/rpc/hevy_push_snapshot', { body, bearer: session.access });
  if (res.status === 401) throw new CloudError(401, res.text, 'unauthorized');
  if (!ok(res.status)) throw new CloudError(res.status, res.text, `push ${res.status}`);
  const stored = parsePushBody(res.text);
  if (stored == null) throw new CloudError(res.status, res.text, 'push réponse invalide');
  return stored;
}

// ================= storage =================

/**
 * Upload l'avatar JPEG (bucket public `avatars`, x-upsert). Comme Cloud.uploadAvatar :
 * tout échec (réseau inclus) → null. Succès → URL publique.
 * @returns {Promise<string|null>}
 */
export async function uploadAvatar(session, jpegBytes) {
  if (session == null) return null;
  const name = encodeURIComponent(session.userId);
  try {
    const res = await http('POST', `/storage/v1/object/avatars/${name}.jpg`, {
      bearer: session.access,
      raw: jpegBytes,
      headers: { 'Content-Type': 'image/jpeg', 'x-upsert': 'true' },
    });
    if (!ok(res.status)) return null;
    return `${SUPABASE_URL}/storage/v1/object/public/avatars/${name}.jpg`;
  } catch { return null; }
}

/** Chemin de l'objet storage d'une photo de progression (port de Cloud.photoObjectPath). */
export function photoObjectPath(session, photoId) {
  return `${session.userId}/ph_${photoId}.jpg`;
}

/**
 * Upload une photo de progression dans le bucket privé `progress` (bearer requis).
 * Comme Cloud.uploadProgressPhoto : échec → false (null en Kotlin), succès → true.
 */
export async function uploadProgressPhoto(session, photoId, jpegBytes) {
  if (session == null) return false;
  try {
    const res = await http('POST', `/storage/v1/object/progress/${photoObjectPath(session, photoId)}`, {
      bearer: session.access,
      raw: jpegBytes,
      headers: { 'Content-Type': 'image/jpeg', 'x-upsert': 'true' },
    });
    return ok(res.status);
  } catch { return false; }
}

/**
 * Télécharge une photo de progression (bucket privé, bearer requis).
 * Comme Cloud.downloadProgressPhoto : échec réseau / non-2xx → null.
 * ⚠️ Prend l'ID NUMÉRIQUE de la photo (le chemin est reconstruit) — passer p.remote
 * (déjà « {uid}/ph_{id}.jpg ») provoquait une double encapsulation d'URL → 404 permanent.
 * @returns {Promise<ArrayBuffer|null>}
 */
export async function downloadProgressPhoto(session, photoId) {
  if (session == null) return null;
  try {
    const res = await http('GET', `/storage/v1/object/progress/${photoObjectPath(session, photoId)}`, {
      bearer: session.access,
      binary: true,
    });
    return ok(res.status) ? res.bytes : null;
  } catch { return null; }
}

/**
 * Télécharge le blob d'une URL publique d'avatar (port de Cloud.downloadAvatar).
 * L'URL est VALIDÉE : elle doit pointer vers notre bucket avatars — settings.avatarUrl venant
 * du snapshot cloud, une URL arbitraire y ferait de chaque rendu un beacon de tracking.
 * @returns {Promise<Blob|null>}
 */
export async function downloadUrlBlob(url) {
  const prefix = `${SUPABASE_URL}/storage/v1/object/public/avatars/`;
  if (typeof url !== 'string' || !url.startsWith(prefix) || url.length > 400 || !/^[\w.-]+\.jpg$/.test(url.slice(prefix.length))) return null;
  try {
    const res = await http('GET', url.slice(SUPABASE_URL.length), { binary: true });
    return ok(res.status) ? new Blob([res.bytes], { type: 'image/jpeg' }) : null;
  } catch { return null; }
}

/**
 * DELETE un objet storage ( chemin COMPLET bucket inclus, ex: `progress/<uid>/ph_12.jpg`).
 * ⚠️ JAMAIS de header Content-Type sur ce DELETE sans corps : le storage-api Fastify
 * répond 400 sinon (piège documenté dans Cloud.kt http()).
 * Comme Cloud.flushPendingPhotoDeletes : 404 (déjà supprimé) ou 403 « not found » = succès.
 * @returns {Promise<boolean>}
 */
export async function deleteStorageObject(session, path) {
  if (session == null || !path) return false;
  try {
    const res = await http('DELETE', `/storage/v1/object/${path}`, { bearer: session.access });
    if (ok(res.status) || res.status === 404) return true; // 404 = déjà parti
    if (res.status === 403 && /not found/i.test(res.text)) return true;
    return false;
  } catch { return false; }
}

// ================= helpers base64url =================

/** bytes → base64url SANS padding (équivalent android Base64.URL_SAFE|NO_PADDING|NO_WRAP). */
function base64url(bytes) {
  let bin = '';
  for (let i = 0; i < bytes.length; i++) bin += String.fromCharCode(bytes[i]);
  return btoa(bin).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}
