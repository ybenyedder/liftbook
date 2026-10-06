/** Account + cloud sync against the self-hosted Supabase (GoTrue) — port of Cloud.kt.
 *  One full snapshot (workouts, routines, settings, tombstones) per account;
 *  last-write-wins by client timestamp, union-merge on conflict. */
import { Platform } from "react-native";
import AsyncStorage from "@react-native-async-storage/async-storage";
import * as SecureStore from "expo-secure-store";
import * as WebBrowser from "expo-web-browser";
import * as Crypto from "expo-crypto";
import Constants from "expo-constants";
import { SyncPayload, defaultSettings } from "./models";
import { makeStore } from "./store";
import { Repo } from "./repo";
import { avatar } from "./avatar";

const BASE = "https://api.webtvmedia.net";
const ANON = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJyb2xlIjoiYW5vbiIsImlzcyI6InN1cGFiYXNlIiwiaWF0IjoxNzc2ODY2NDUyLCJleHAiOjIwOTIyMjY0NTJ9.jOfn90sK6YeY6LRRuwzdiZpiO-s8pN4Ozr418B8iRXE";
const APP_UA = "Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) Liftbook/1.0";

export interface CloudSession {
  email: string;
  userId: string;
  access: string;
  refresh: string;
  expiresAt: number;
}

interface CloudUi {
  session: CloudSession | null;
  skipped: boolean;
  busy: boolean;
  authError: string | null;
  syncStatus: string | null;
  googlePending: boolean;
}

const isWeb = Platform.OS === "web";

class CloudImpl {
  uiStore = makeStore<CloudUi>({ session: null, skipped: false, busy: false, authError: null, syncStatus: null, googlePending: false });
  get ui() {
    return this.uiStore.get();
  }
  get subscribe() {
    return this.uiStore.subscribe;
  }
  private setUi(patch: Partial<CloudUi>) {
    this.uiStore.set({ ...this.uiStore.get(), ...patch });
  }
  get session() {
    return this.uiStore.get().session;
  }
  private set session(s: CloudSession | null) {
    this.setUi({ session: s });
  }
  get skipped() {
    return this.uiStore.get().skipped;
  }
  set skipped(v: boolean) {
    this.setUi({ skipped: v });
  }
  get busy() {
    return this.uiStore.get().busy;
  }
  private set busy(v: boolean) {
    this.setUi({ busy: v });
  }
  get authError() {
    return this.uiStore.get().authError;
  }
  set authError(v: string | null) {
    this.setUi({ authError: v });
  }
  get syncStatus() {
    return this.uiStore.get().syncStatus;
  }
  set syncStatus(v: string | null) {
    this.setUi({ syncStatus: v });
  }
  get googlePending() {
    return this.uiStore.get().googlePending;
  }
  set googlePending(v: boolean) {
    this.setUi({ googlePending: v });
  }

  // ---- sync meta (persisted) ----
  private dirtyAt = 0;
  private pushedTs = 0;
  private lastSeenRemoteTs = 0;
  private lastAccount: string | null = null;
  private syncing = false;
  private delW: number[] = [];
  private delR: string[] = [];
  private syncTimer: ReturnType<typeof setTimeout> | null = null;
  private ready = false;

  async init() {
    if (this.ready) return;
    this.ready = true;
    try {
      const raw = await AsyncStorage.getItem("cloud_meta");
      if (raw) {
        const m = JSON.parse(raw);
        this.dirtyAt = m.dirtyAt ?? 0;
        this.pushedTs = m.pushedTs ?? 0;
        this.lastSeenRemoteTs = m.lastSeenRemoteTs ?? 0;
        this.lastAccount = m.lastAccount ?? null;
        this.skipped = !!m.skipped;
        this.delW = m.delW ?? [];
        this.delR = m.delR ?? [];
      }
    } catch {}
    // session at rest: iOS Keychain (SecureStore); localStorage on web
    try {
      const blob = isWeb ? localStorage.getItem("liftbook.session") : await SecureStore.getItemAsync("liftbook.session");
      if (blob) {
        const o = JSON.parse(blob);
        this.session = { email: o.email, userId: o.userId, access: o.access, refresh: o.refresh, expiresAt: o.expiresAt };
      }
    } catch {}
    this.uiStore.set({ ...this.uiStore.get() }); // notify after init
  }

  // ================= auth =================

  async signUp(email: string, password: string): Promise<string | null> {
    return this.authenticate("signup", email, password);
  }
  async signIn(email: string, password: string): Promise<string | null> {
    return this.authenticate("token?grant_type=password", email, password);
  }

  private async authenticate(kind: string, emailRaw: string, password: string): Promise<string | null> {
    const email = emailRaw.trim().toLowerCase();
    this.busy = true;
    this.authError = null;
    try {
      const [code, resp] = await this.http("POST", `/auth/v1/${kind}`, JSON.stringify({ email, password }));
      if (code < 200 || code > 299) {
        const err = this.mapAuthError(code, resp);
        this.authError = err;
        return err;
      }
      const obj = JSON.parse(resp);
      if (!obj.access_token) {
        const err = "Compte créé mais non confirmé — contacte l'administrateur.";
        this.authError = err;
        return err;
      }
      await this.applySession(
        email,
        obj.user?.id ?? "",
        obj.access_token,
        obj.refresh_token ?? "",
        obj.expires_in ?? 3600,
      );
      return null;
    } catch {
      const err = "Connexion impossible — vérifie Internet.";
      this.authError = err;
      return err;
    } finally {
      this.busy = false;
      this.googlePending = false;
    }
  }

  private async applySession(email: string, userId: string, token: string, refresh: string, expiresIn: number) {
    this.session = { email, userId, access: token, refresh, expiresAt: Date.now() + expiresIn * 1000 };
    await this.onLoggedIn(email);
    await this.persistSession();
    void this.syncNow();
  }

  private async onLoggedIn(email: string) {
    if (this.lastAccount != null && this.lastAccount !== email) {
      // Account switch: wipe this device's copy of the previous account's data.
      Repo.wipe(false);
      this.clearTombstones();
      this.dirtyAt = 0;
      this.pushedTs = 0;
      this.lastSeenRemoteTs = 0;
    } else if (this.lastAccount == null && (Repo.workouts.length > 0 || Repo.routines.length > 0)) {
      // First bind of an existing local profile → push local data (merged with remote).
      this.dirtyAt = Date.now();
    }
    this.lastAccount = email;
    this.skipped = false;
    await this.persistMeta();
  }

  signOut() {
    const s = this.session;
    if (s) void this.http("POST", "/auth/v1/logout", "{}", s.access).catch(() => {});
    if (isWeb) localStorage.removeItem("liftbook.session");
    else void SecureStore.deleteItemAsync("liftbook.session").catch(() => {});
    this.session = null;
    this.skipped = false;
    this.syncStatus = null;
  }

  // ================= Google sign-in (browser PKCE) =================

  private PKCE_TTL_MS = 10 * 60_000;

  /** The deep link Google redirects back to: exp:// in Expo Go, liftbook:// in a standalone build. */
  authRedirect(): string {
    const inGo = Constants.executionEnvironment === "storeClient";
    if (inGo) {
      const host = (Constants.expoConfig as any)?.hostUri ?? "192.168.1.194:8081";
      return `exp://${host}/--/auth-callback`;
    }
    return "liftbook://auth-callback";
  }

  /**
   * Opens the auth URL in the in-app browser (SFSafariViewController on iOS — honors the
   * deep-link redirect and returns to the app). NB: never add a `state` param — GoTrue v2.186
   * forwards it to Google where it overrides the flow-state UUID → bad_oauth_state.
   * CSRF is covered by the S256 code_verifier.
   */
  async startGoogleAuth(): Promise<boolean> {
    if (isWeb) {
      this.authError = "Connexion Google disponible sur iPhone (Expo Go) — pas sur le web.";
      return false;
    }
    const verifier = await this.generatePkceVerifier();
    await AsyncStorage.setItem("pkce_verifier", verifier);
    await AsyncStorage.setItem("pkce_ts", String(Date.now()));
    const challenge = await this.pkceChallenge(verifier);
    const url =
      `${BASE}/auth/v1/authorize?provider=google&redirect_to=` +
      encodeURIComponent(this.authRedirect()) +
      `&flow_type=pkce&code_challenge=${challenge}&code_challenge_method=s256`;
    this.googlePending = true;
    this.authError = null;
    try {
      const result = await WebBrowser.openAuthSessionAsync(url, this.authRedirect().split("/--/")[0]);
      if (result.type === "success" && result.url) {
        return this.handleAuthRedirect(result.url);
      }
      // dismissed without completing
      this.googlePending = false;
      return false;
    } catch {
      this.googlePending = false;
      this.authError = "Connexion impossible — vérifie Internet.";
      return false;
    }
  }

  /** Deep-link entry: <redirect>?code=... → exchange for a session (single-use verifier, 10 min TTL). */
  async handleAuthRedirect(urlString: string): Promise<boolean> {
    try {
      const url = new URL(urlString);
      if (url.host !== "auth-callback" && !urlString.includes("auth-callback")) return false;
      this.googlePending = false;
      const err = url.searchParams.get("error_description") ?? url.searchParams.get("error");
      if (err) {
        this.authError = err.slice(0, 120);
        await this.clearPkce();
        return true;
      }
      const verifier = await AsyncStorage.getItem("pkce_verifier");
      const issuedAt = parseInt((await AsyncStorage.getItem("pkce_ts")) ?? "0", 10);
      await this.clearPkce();
      if (!verifier || Date.now() - issuedAt > this.PKCE_TTL_MS) {
        this.authError = "Session de connexion expirée — réessaie.";
        return true;
      }
      const code = url.searchParams.get("code");
      if (!code) {
        this.authError = "Connexion Google incomplète — réessaie.";
        return true;
      }
      this.busy = true;
      this.authError = null;
      try {
        const body = JSON.stringify({ auth_code: code, code_verifier: verifier });
        const [httpCode, resp] = await this.http("POST", "/auth/v1/token?grant_type=pkce", body);
        if (httpCode < 200 || httpCode > 299) {
          this.authError = `Connexion Google échouée (${httpCode})`;
          return true;
        }
        const obj = JSON.parse(resp);
        if (!obj.access_token) {
          this.authError = "Session Google invalide.";
          return true;
        }
        await this.applySession(
          obj.user?.email ?? "",
          obj.user?.id ?? "",
          obj.access_token,
          obj.refresh_token ?? "",
          obj.expires_in ?? 3600,
        );
        return true;
      } catch {
        this.authError = "Connexion impossible — vérifie Internet.";
        return true;
      } finally {
        this.busy = false;
      }
    } catch {
      return false;
    }
  }

  /** App foregrounded again without completing the flow → drop the pending spinner. */
  async clearGooglePendingIfStale() {
    if (this.googlePending && !this.session && !this.busy) {
      const issuedAt = parseInt((await AsyncStorage.getItem("pkce_ts")) ?? "0", 10);
      if (!issuedAt || Date.now() - issuedAt > 20_000) this.googlePending = false;
    }
  }

  private async clearPkce() {
    await AsyncStorage.multiRemove(["pkce_verifier", "pkce_ts"]);
  }

  private async generatePkceVerifier(): Promise<string> {
    const bytes = Crypto.getRandomBytes(48);
    return base64Url(bytes);
  }

  private async pkceChallenge(verifier: string): Promise<string> {
    const digest = await Crypto.digestStringAsync(Crypto.CryptoDigestAlgorithm.SHA256, verifier, { encoding: Crypto.CryptoEncoding.BASE64 });
    return digest.replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
  }

  // ================= avatar storage =================

  /** Uploads the local avatar JPEG (base64); returns the public URL on success. */
  async uploadAvatar(base64Jpeg: string): Promise<string | null> {
    const s = this.session;
    if (!s) return null;
    try {
      let body: FormData | Blob;
      if (isWeb) {
        const bin = atob(base64Jpeg);
        const arr = new Uint8Array(bin.length);
        for (let i = 0; i < bin.length; i++) arr[i] = bin.charCodeAt(i);
        body = new Blob([arr], { type: "image/jpeg" });
      } else {
        const uri = await avatar.writeBase64(base64Jpeg);
        const form = new FormData();
        form.append("file", { uri, name: "avatar.jpg", type: "image/jpeg" } as any);
        body = form;
      }
      const res = await fetch(`${BASE}/storage/v1/object/avatars/${s.userId}.jpg`, {
        method: "POST",
        headers: {
          apikey: ANON,
          Authorization: `Bearer ${s.access}`,
          "Content-Type": isWeb ? "image/jpeg" : undefined as any,
          "x-upsert": "true",
        },
        body: body as any,
      });
      if (!res.ok) return null;
      return `${BASE}/storage/v1/object/public/avatars/${s.userId}.jpg`;
    } catch {
      return null;
    }
  }

  /** Another device changed the profile photo → download it for local display. */
  private async maybeFetchAvatar(previousUrl: string) {
    const url = Repo.settings.avatarUrl;
    if (!url || url === previousUrl) return;
    try {
      if (isWeb) {
        const blob = await (await fetch(url)).blob();
        const reader = new FileReader();
        reader.onload = () => {
          if (typeof reader.result === "string") {
            localStorage.setItem("liftbook.avatar", reader.result);
            avatar.bump();
          }
        };
        reader.readAsDataURL(blob);
      } else {
        await avatar.download(url);
      }
    } catch {}
  }

  private async refreshIfNeeded() {
    const s = this.session;
    if (!s) return;
    if (s.expiresAt - Date.now() > 60_000) return;
    try {
      const body = JSON.stringify({ refresh_token: s.refresh });
      const [code, resp] = await this.http("POST", "/auth/v1/token?grant_type=refresh_token", body);
      if (code < 200 || code > 299) {
        this.signOut();
        return;
      }
      const obj = JSON.parse(resp);
      s.access = obj.access_token ?? s.access;
      s.refresh = obj.refresh_token ?? s.refresh;
      s.expiresAt = Date.now() + (obj.expires_in ?? 3600) * 1000;
      await this.persistSession();
    } catch {
      /* keep session, retry later */
    }
  }

  private mapAuthError(code: number, body: string): string {
    const b = body.toLowerCase();
    if (code === 429) return "Trop d'essais — réessaie dans un instant.";
    if (b.includes("already") || b.includes("registered")) return "Cet email a déjà un compte. Connecte-toi.";
    if (b.includes("invalid_grant") || b.includes("invalid login")) return "Email ou mot de passe incorrect.";
    if (b.includes("at least") && b.includes("character")) return "Mot de passe trop court (6 caractères minimum).";
    if (b.includes("validation") && b.includes("email")) return "Adresse email invalide.";
    if (code >= 500) return "Serveur indisponible — réessaie plus tard.";
    return `Erreur inattendue (${code}).`;
  }

  // ================= sync =================

  markDirty() {
    this.dirtyAt = Date.now();
    void this.persistMeta();
    this.requestSync();
  }

  currentTombW(): number[] {
    return [...this.delW];
  }
  currentTombR(): string[] {
    return [...this.delR];
  }

  async tombstoneWorkout(startedAt: number) {
    if (!this.delW.includes(startedAt)) this.delW.push(startedAt);
    if (this.delW.length > 400) this.delW.shift();
    await this.persistMeta();
  }

  async tombstoneRoutine(name: string) {
    if (!this.delR.includes(name)) this.delR.push(name);
    if (this.delR.length > 400) this.delR.shift();
    await this.persistMeta();
  }

  private async clearTombstones() {
    this.delW = [];
    this.delR = [];
    await this.persistMeta();
  }
  private async adoptTombstones(p: SyncPayload) {
    this.delW = [...p.delW];
    this.delR = [...p.delR];
    await this.persistMeta();
  }
  private async mergeTombstones(p: SyncPayload) {
    for (const w of p.delW) if (!this.delW.includes(w)) this.delW.push(w);
    for (const r of p.delR) if (!this.delR.includes(r)) this.delR.push(r);
    await this.persistMeta();
  }

  requestSync(debounceMs = 3000) {
    if (!this.session) return;
    if (this.syncTimer) clearTimeout(this.syncTimer);
    this.syncTimer = setTimeout(() => void this.syncNow(), debounceMs);
  }

  async syncNow() {
    if (!this.session || this.syncing) return;
    this.syncing = true;
    try {
      await this.refreshIfNeeded();
      const [remote, remoteTs] = await this.pull();
      const dirty = this.dirtyAt > this.pushedTs;
      if (!dirty) {
        if (remote && remoteTs > this.lastSeenRemoteTs) {
          const oldAvatar = Repo.settings.avatarUrl;
          Repo.replaceAll(remote);
          await this.adoptTombstones(remote);
          this.lastSeenRemoteTs = remoteTs;
          this.pushedTs = remoteTs;
          this.dirtyAt = remoteTs;
          await this.persistMeta();
          void this.maybeFetchAvatar(oldAvatar);
          this.syncStatus = this.stamp("Synchronisé");
        }
      } else {
        if (remote && remoteTs > this.lastSeenRemoteTs) {
          Repo.mergeRemote(remote);
          await this.mergeTombstones(remote);
          this.lastSeenRemoteTs = remoteTs;
        }
        const ts = Math.max(Date.now(), this.lastSeenRemoteTs + 1);
        const payload = Repo.snapshot();
        const stored = await this.push(payload, ts);
        this.pushedTs = ts;
        this.dirtyAt = ts;
        this.lastSeenRemoteTs = stored;
        await this.persistMeta();
        this.syncStatus = this.stamp("Synchronisé");
      }
    } catch (e: any) {
      this.syncStatus = this.stamp(`Sync échouée (${String(e?.message ?? "réseau").slice(0, 60)})`);
    } finally {
      this.syncing = false;
      this.uiStore.set({ ...this.uiStore.get() });
    }
  }

  private stamp(s: string): string {
    return s + " · " + new Date().toLocaleTimeString("fr-FR", { hour: "2-digit", minute: "2-digit" });
  }

  /** GET own snapshot row; returns [payload?, client_ts]. */
  private async pull(): Promise<[SyncPayload | null, number]> {
    const s = this.session!;
    const [code, resp] = await this.http("GET", "/rest/v1/hevy_snapshots?select=data,client_ts", null, s.access);
    if (code === 401) throw new Error("unauthorized");
    if (code < 200 || code > 299) throw new Error(`pull ${code}`);
    const arr = JSON.parse(resp);
    if (!arr.length) return [null, 0];
    const row = arr[0];
    const ts = row.client_ts ?? 0;
    const data = row.data;
    if (!data) return [null, ts];
    return [normalizePayload(data), ts];
  }

  /** Push full snapshot with LWW guard; returns the ts now stored server-side. */
  private async push(payload: SyncPayload, ts: number): Promise<number> {
    const s = this.session!;
    const body = JSON.stringify({ p_data: payload, p_client_ts: ts });
    const [code, resp] = await this.http("POST", "/rest/v1/rpc/hevy_push_snapshot", body, s.access);
    if (code === 401) throw new Error("unauthorized");
    if (code < 200 || code > 299) throw new Error(`push ${code}`);
    const n = Number(JSON.parse(resp));
    return Number.isFinite(n) ? n : ts;
  }

  // ================= persistence =================

  private async persistSession() {
    const s = this.session;
    if (!s) return;
    const blob = JSON.stringify(s);
    try {
      if (isWeb) localStorage.setItem("liftbook.session", blob);
      else await SecureStore.setItemAsync("liftbook.session", blob);
    } catch {}
  }

  private async persistMeta() {
    try {
      await AsyncStorage.setItem(
        "cloud_meta",
        JSON.stringify({
          dirtyAt: this.dirtyAt,
          pushedTs: this.pushedTs,
          lastSeenRemoteTs: this.lastSeenRemoteTs,
          lastAccount: this.lastAccount,
          skipped: this.skipped,
          delW: this.delW,
          delR: this.delR,
        }),
      );
    } catch {}
  }

  async persistSkip() {
    await this.persistMeta();
  }

  // ================= http =================

  private async http(method: string, path: string, body: string | null, bearer?: string): Promise<[number, string]> {
    const headers: Record<string, string> = { apikey: ANON };
    if (body != null) headers["Content-Type"] = "application/json";
    if (!isWeb) headers["User-Agent"] = APP_UA;
    if (bearer) headers.Authorization = `Bearer ${bearer}`;
    const res = await fetch(BASE + path, { method, headers, body: body ?? undefined });
    const resp = await res.text();
    return [res.status, resp];
  }
}

function base64Url(bytes: Uint8Array): string {
  let bin = "";
  for (const b of bytes) bin += String.fromCharCode(b);
  return btoa(bin).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

/** Defensive payload normalizer (missing fields from older snapshots). */
export function normalizePayload(p: any): SyncPayload {
  return {
    workouts: p.workouts ?? [],
    routines: p.routines ?? [],
    settings: p.settings ?? defaultSettings(),
    delW: p.delW ?? [],
    delR: p.delR ?? [],
    v: p.v ?? 1,
  };
}

export const Cloud = new CloudImpl();
