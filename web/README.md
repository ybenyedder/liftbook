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
Publié sur GitHub Pages par `.github/workflows/pages.yml` (dossier `web/` servi à la racine du
site). OAuth Google : `redirect_to = <origine>/auth-callback` (URL déclarée dans
`ADDITIONAL_REDIRECT_URLS` du GoTrue du serveur).

## Écarts assumés (plateforme web)
- Pas de widget d'accueil (exclusivité Android), pas de notification chronomètre de séance persistante.
- Notifications de fin de repos : Notification API du navigateur (permission au premier timer, comme Android).
- Session stockée en localStorage (pas de KeyStore navigateur) — déconnexion conseillée sur un ordinateur partagé.
- Google Sign-In passe par le navigateur (PKCE), pas de Credential Manager.
