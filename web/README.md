# Liftbook web — le port navigateur de l'app Android

La même application que l'APK (catalogue 609 exercices, recherche floue FR, séances/routines,
cardio MIN/KM, calculateur de disques, photos de progression, sync cloud), en **JavaScript ES
modules pur — zéro dépendance, zéro build**. Le design (palette `ui/Theme.kt`, Inter,
Material Symbols) et les comportements sont portés écran par écran depuis le Kotlin.

## Lancer en local
```bash
cd web && python3 -m http.server 8090   # → http://localhost:8090
```
Ouvrir `dev/seed.html` une fois pour remplir des données de démo (49 séances, 4 routines),
puis revenir à `index.html`. `localStorage` garde tout hors ligne ; les photos vivent dans
IndexedDB.

## Tests (port des assertions de LogicTest.kt)
```bash
node web/tests/run.mjs
```

## Architecture
- `src/app.js` — shell : porte auth, onglets, barre undo, toasts, sync au boot/visibilité, callback OAuth.
- `src/router.js` — pile d'écrans (port de `Nav` App.kt) + bouton retour navigateur + restauration après rechargement.
- `src/store.js` — port de `Repo.kt` + orchestration sync `Cloud.kt` (LWW, tombstones, photos ≤8/sync, LRU 12).
- `src/calc.js` / `src/search.js` — ports de `Calc.kt` / `L10n+L10nData.kt` (e1rm, streak, CSV Hevy/Strong multi-langues, recherche floue FR accents+typos).
- `src/cloud.js` — GoTrue (email/mot de passe + Google PKCE), snapshots, avatars, bucket progress.
- `src/ui.js` — composants Comps.kt (LineChart SVG animé, BodyMap, dialogues, bottom-sheets, menus ⋮).
- `src/data.js` / `src/figures.js` — **générés** depuis le Kotlin (`python3 web/extract_web.py`).
- `src/restimer.js` — minuteur de repos global (persistance, −15/+15/Passer, chime `rest_done.wav`, notification web).
- `src/screens/*.js` — un module par écran, enregistrements via `nav.registerScreen`.

## Comptes / sync
Même compte Supabase que les apps Android/iOS (`api.webtvmedia.net`) : une séance saisie sur
le téléphone apparaît dans le navigateur après « Synchroniser maintenant » (ou au retour
d'onglet), et réciproquement — payload v4 identique, tombstones comprises.

## Déploiement
- **URL principale (Cloudflare, HTTPS)** : **https://liftbook.webtvmedia.net** — tunnel Cloudflare
  du serveur volthost (règle ingress `liftbook.webtvmedia.net → http://localhost:8913` dans
  `~/webtvmedia-site/tunnel/tunnel.yml`, CNAME routé vers le tunnel `bd29b796-…`).
- **GitHub Pages** (miroir) : `.github/workflows/pages.yml` → https://ybenyedder.github.io/liftbook/.
- **Serveur volthost** : `./deploy-volthost.sh` — rsync vers `volt@192.168.1.87:/srv/liftbook-web/`,
  servi par le systemd `liftbook-web.service` sur le port 8913 (LAN : http://192.168.1.87:8913,
  Tailscale : http://100.107.129.74:8913). No-store : un rsync suffit, aucun redémarrage.
  Le docroot et le script serveur vivent hors `/home` (durcissement systemd `ProtectHome=yes`,
  le service ne peut pas lire le reste du home volthost) ; le serveur envoie les headers de
  sécurité (CSP, XFO, nosniff, Referrer-Policy, HSTS) et n'a ni directory listing ni bannière
  de version. ⚠️ le déploiement exclut `dev/`, `deploy-volthost.sh` et `README.md` (ne pas
  les re-ajouter au rsync : seed.html écrase le localStorage du visiteur).
- OAuth Google : `redirect_to = <origine>/auth-callback` — les 3 origines (Cloudflare, Pages,
  localhost + LAN 8913) sont déclarées dans `ADDITIONAL_REDIRECT_URLS` du GoTrue.
  ⚠️ après TOUTE modification du `.env` Supabase : `docker compose up -d auth` (recréer) —
  un simple `restart` ne relit PAS les variables (piège qui a cassé le login Google web
  du 06/10 au 10/10/2026).

## Écarts assumés (plateforme web)
- Pas de widget d'accueil (exclusivité Android), pas de notification chronomètre de séance persistante.
- Notifications de fin de repos : Notification API du navigateur (permission au premier timer, comme Android).
- Session stockée en localStorage (pas de KeyStore navigateur) — CSP stricte en contrôle
  compensatoire ; déconnexion conseillée sur un ordinateur partagé.
- Google Sign-In passe par le navigateur (PKCE), pas de Credential Manager.
