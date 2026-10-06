# Liftbook iOS — natif Swift / SwiftUI

Portage **100 % natif** de l'app Android (Liftbook / clone Hevy) pour iPhone :
SwiftUI + Swift Charts + UserNotifications + Keychain + ASWebAuthenticationSession.
**Aucune dépendance tierce** — aucun CocoaPod, aucun SPM externe, aucune WebView.

Le cœur logique (`LiftbookCore/`) est un package SwiftPM pur qui **compile et est testé
sur Linux** (17 tests : recherche floue FR, import CSV Hevy/Strong/template, dates
multi-langues, moteur PR, seed, SHA-256, round-trip de sync) — seuls les écrans SwiftUI
nécessitent Xcode.

## Ouvrir dans Xcode et lancer sur ton iPhone

1. Copier le dossier `ios-native/` sur un Mac (n'importe quel Mac d'un pote, boulot, lycée…).
2. Double-cliquer sur **`Liftbook.xcodeproj`** (Xcode 16+).
3. Brancher l'iPhone, sélectionner le target **Liftbook** → ton téléphone en haut.
4. Dans *Signing & Capabilities*, cocher *Automatically manage signing* et choisir ton
   **Apple ID personnel (gratuit)** dans Team.
5. **Run ▶** — l'app s'installe sur l'iPhone (signature personnelle valable 7 jours,
   rebrancher et re-run pour renouveler ; la synchro compte conserve toutes les données).

Se connecter avec le compte habituel : les séances/routines arrivent par la synchro
(même serveur que l'app Android, snapshot LWW `hevy_snapshots`).

### App Store (plus tard)
Il faudra l'abonnement Apple Developer (99 $/an) : *Product > Archive* puis upload.
Le scheme `liftbook://` et le bundle `net.webtvmedia.liftbook` sont déjà déclarés.

## Ce qui est porté (tout)

| Android (Kotlin)            | iOS (Swift)                          |
| --------------------------- | ------------------------------------ |
| `data/Models.kt`            | `LiftbookCore/Models.swift`          |
| `data/Calc.kt`              | `LiftbookCore/Calc.swift`            |
| `data/Data.kt`+`L10nData.kt`| `LiftbookCore/ExData.swift` (extrait du Kotlin par `../iphone/tools/extract_swift.py`) |
| recherche floue + archétypes| `LiftbookCore/Search.swift`          |
| `data/Cloud.kt` (GoTrue/LWW)| `LiftbookCore/Cloud.swift` (PKCE sans `state`, SHA-256 maison sans CryptoKit) |
| `data/Repo.kt` (SQLite)     | `Liftbook/AppState.swift` (UserDefaults JSON) + `Keychain` |
| `RestNotifService`/`RestTimer` | `Liftbook/RestTimer.swift` (UserNotifications + actions −15/+15/Passer) |
| `ui/App.kt` (Nav)           | `Liftbook/RootView.swift`            |
| `ui/Comps.kt`               | `Liftbook/Comps.swift`               |
| `ui/Logger.kt`              | `Liftbook/LoggerView.swift`          |
| autres `ui/*.kt`            | un `*View.swift` par écran           |
| drawables `ill_*`/`d_ill_*` | rendus natifs via parser SVG → `Path` SwiftUI (`SVGPath.swift`, données dans `Figures.swift`) |

Les 609 exercices (noms FR, alias « dc »/« sdt »/« tirage poitrine », instructions par
archétype, conseils) et les silhouettes musculaires sont **extraits automatiquement du
code Android** : `python3 ../iphone/tools/extract_swift.py` (nécessite d'avoir régénéré
`../iphone/src/data/exdata.json` via `extract_data.py`).

## Tester le cœur sans Mac (Linux)

```bash
cd LiftbookCore
swift test    # 17/17 (testé avec Swift 6.4 sur la machine de dev)
```

## Écrans (parité Android v1.42)

Connexion (email + Google via onglet Safari ASWebAuthenticationSession, deep link
`liftbook://auth-callback` déjà autorisé côté GoTrue) — fil d'accueil — Entraînement
(sélecteur Routines/Calendrier, démarrage vide, routines avec **réordonnancement natif
par glisser** des poignées, menus ⋯) — détail routine (graphiques Swift Charts
Volume/Réps/Durée) — séance (stats live TimelineView, PRÉCÉDENT, badge « Plus Gros
Poids », ligne record verte, barre de repos −15/+15/Passer, superset, notes,
remplacement, picker d'exercices avec recherche floue, minuteur par exercice) —
éditeur routine (titre sur filet, Enregistrer) — récap (records, « Routine modifiée ? »)
— exercices (Résumé/Historique/Instructions avec silhouette pulsante et étapes FR) —
historique (filtres, mini-calendrier, détail séance avec WEIGHT PR/1RM PR) — annulation
suppression 5 s — profil (heatmap annuelle Canvas, barres mensuelles, volume par muscle)
— réglages (photo de profil PhotosPicker + upload avatar, unités, minuteur, accents,
import Hevy/CSV via sélecteur de fichiers, export CSV, sauvegarde JSON, effacement).

## Différences assumées vs Android

- Réordonnancement : poignées de glisser natives des `List` (edit mode toujours actif)
  au lieu du drag custom — c'est le pattern iOS standard.
- Notifications : notification planifiée à la fin du repos (son) + notification persistante
  avec le décompte tant que l'app est ouverte ; boutons d'action sur la notification.
- Import Hevy : le sélecteur iOS ouvre le CSV directement (si l'export est un .zip,
  le décompresser d'abord dans Fichiers).
- Pas de widget d'écran d'accueil (équivalent iOS = WidgetKit, non porté).
- Le menu ∨ de la séance contient « Reprendre plus tard » (l'iPhone n'a pas de bouton
  retour système) ; Historique/Exercices ont un chevron retour.
