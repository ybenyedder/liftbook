# Liftbook iPhone (Expo / React Native)

Portage complet de l'app Android (Liftbook / clone Hevy) sur iPhone avec **Expo + React Native + TypeScript**.
Même serveur de compte (api.webtvmedia.net), même modèle de données, même design noir/bleu Hevy — les données
synchronisées entre l'iPhone et l'Android via le même compte (snapshot LWW `hevy_snapshots`).

## Lancer l'app sur ton iPhone (2 minutes, sans Mac)

1. L'app doit être servie par la machine de dev (celle-ci, IP LAN `192.168.1.194`) :
   ```bash
   cd /home/cvsbd/hevy/iphone
   npx expo start --port 8081
   ```
2. Sur l'iPhone : installe **Expo Go** (App Store, gratuit), connecte-toi au **même Wi-Fi**.
3. Ouvre Expo Go → « Enter URL manually » → `exp://192.168.1.194:8081`
   (ou scanne le QR code affiché par `expo start`).
4. Connecte-toi avec ton compte (`hsjsj@shjqja.com`) — tes séances et routines arrivent par sync.

> ⚠️ Si l'IP LAN de la machine change, la connexion Google en Expo Go casse (redirect fixe côté serveur).
> Corriger `ADDITIONAL_REDIRECT_URLS` dans `/home/volt/supabase/docker/.env` (nouvelle URL
> `exp://<IP>:8081/--/auth-callback`) puis `docker compose up -d auth && docker restart supabase-kong`.

## Publier sur l'App Store (plus tard)

Le même code se build en vraie app iOS sans Mac local, via EAS (build dans le cloud Expo) :
```bash
npm i -g eas-cli
eas login            # compte Expo gratuit
eas build -p ios     # nécessite un compte Apple Developer (99 $/an)
```
Le scheme `liftbook://` est déjà déclaré (app.json) et déjà autorisé côté serveur GoTrue.

## Développer / vérifier sur le web (sans iPhone)

```bash
python3 devweb/serve.py 8091     # page web branchée sur le bundler Metro (rechargement à chaud)
# puis ouvrir http://localhost:8091 (viewport mobile dans les devtools)
```
Deux flags pratiques (web uniquement, localStorage) : `liftbook_lang` = `fr`|`en` (forçage langue),
`liftbook_seed` = `1` (remplit l'app avec les données de démo au 1er lancement).

## Tests

```bash
bun test src/logic.test.ts   # 18 tests : recherche floue FR (dc/sdt/tirage poitrine/v-bar), import CSV
                              # Hevy/Strong/template, dates multi-langues (FR/EN/DE/ja), moteur PR,
                              # routinesFromWorkouts, seed déterministe, 609 exercices × noms FR × instructions
npx tsc --noEmit             # typecheck
```

## Structure (portage 1:1 des fichiers Kotlin)

| Android (Kotlin)            | iPhone (TS)                |
| --------------------------- | -------------------------- |
| `data/Models.kt`            | `src/models.ts`            |
| `data/Calc.kt`              | `src/calc.ts`              |
| `data/Data.kt` + `L10nData.kt` | `src/data/exdata.json` (extrait par `tools/extract_data.py`) + `src/l10ndata.ts` |
| `data/Repo.kt` (SQLite)     | `src/repo.ts` (AsyncStorage, mêmes mutateurs) |
| `data/Cloud.kt` (GoTrue/LWW)| `src/cloud.ts` (PKCE sans `state`, Keychain via SecureStore) |
| `RestNotifService` + `RestTimer` | `src/resttimer.ts` (expo-notifications) |
| `ui/App.kt` (Nav)           | `src/nav.ts` + `src/ui/AppRoot.tsx` |
| `ui/Comps.kt`               | `src/ui/comps.tsx` (+ drag & drop DragList) |
| `ui/Logger.kt`              | `src/ui/Logger.tsx` + `src/preval.ts` (badge PR) |
| autres `ui/*.kt`            | `src/ui/*.tsx` (un fichier par écran) |
| drawables `ill_*`/`d_ill_*` | `src/ui/figures_gen.ts` (SVG extraits par `tools/extract_figures.py`) |

Les 609 exercices (noms FR, alias de recherche, instructions par archétype, conseils) sont extraits
automatiquement du code Kotlin : re-lancer `python3 tools/extract_data.py` et `python3 tools/extract_figures.py`
après toute modification côté Android.

## Différences connues vs Android

- **Notifications repos (iOS)** : une notification « Repos terminé » est planifiée à l'échéance + une
  notification persistante affiche le décompte pendant que l'app est ouverte ; boutons −15/+15/Passer
  directement sur la notification (catégorie iOS). Le fond vert de la barre de repos in-app est identique.
- **Glisser-déposer** (routines, ordre des exercices) : appui long pour soulever, puis glisser.
- **Import Hevy** : sélectionner le **.csv** de l'export (si l'export est un .zip, le décompresser d'abord
  dans Fichiers — RN ne dézippe pas nativement). Format Strong/template acceptés comme sur Android.
- Pas de widget d'accueil (l'équivalent iOS serait un WidgetKit — non porté).
- Sur l'écran séance, le menu ∨ contient « Reprendre plus tard » (équivalent du bouton retour Android).
