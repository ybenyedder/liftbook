# Hevy Clone — App Android native

Clone fonctionnel de l'app de workout tracking **Hevy**, en **Kotlin natif** (Jetpack Compose, Material 3). Implémentation originale : aucun asset ni code de l'app officielle n'est copié — design recréé d'après les captures fournies (thème noir + accent bleu `#028CFD`, police Inter OFL), 3 onglets (Accueil / Entraînement / Profil), UI française complète (noms d'exercices traduits) + EN/ES/DE.

**v1.4** : **aucune donnée factice** (l'app démarre vide, tout est créé par l'utilisateur) + **optimisation de la fluidité** (build release R8 de 1,7 Mo, modèles de vue immuables, clés de recomposition, stats mises en cache) + illustrations anatomiques animées (play/pause) dans chaque fiche d'exercice.

## Livrable

**`hevy-clone-v1.0-debug.apk`** (à la racine) — APK debug signé automatiquement, installable sur tout appareil Android 8.0+ (minSdk 26).

```bash
adb install hevy-clone-v1.0-debug.apk
```

## Reconstruire

```bash
cd android
JAVA_HOME=/usr/lib/jvm/java-17-openjdk ~/gradle/gradle-8.7/bin/gradle assembleDebug
# APK : android/app/build/outputs/apk/debug/app-debug.apk
```

Tests unitaires (logique pure, sans émulateur) :

```bash
JAVA_HOME=/usr/lib/jvm/java-17-openjdk ~/gradle/gradle-8.7/bin/gradle testDebugUnitTest
```

## Fonctions

- **Bottom nav 5 onglets** : Home, History, Routines, Exercises, Profile
- **Home** : avatar, streak 🔥 (jours consécutifs), stats de la semaine (séances, volume, reps, PRs), bouton jaune « Start an Empty Workout », routines en scroll horizontal, activité récente
- **Logger de séance** : grille `PREVIOUS | SET | KG | REPS` + case à cocher jaune, **timer de repos auto** (−10 s / +15 s / ignorer, 90 s par défaut), copier/supprimer une série (menu au clic sur le numéro), + Add Set, notes par exercice, résumé de fin (durée, volume, séries, reps, nouveaux records) avant sauvegarde
- **Sélecteur d'exercices** : bottom sheet avec recherche + 15 filtres musculaires, 122 exercices embarqués
- **Routines** : Push/Pull/Legs/Upper pré-remplies, création/édition (même éditeur, sans case à cocher), démarrage en 1 tap (pré-remplit le logger), « last performed », suppression
- **History** : calendrier mensuel avec pastilles jaunes sur les jours d'entraînement + jour du jour cerclé, liste groupée (This Week / Last Week / mois), fiche séance avec badges **WEIGHT PR / 1RM PR** par série, bannière trophée des records, suppression
- **Exercises** : recherche + filtres, fiche détaillée (matériel, cues de coaching, records, courbe de 1RM estimé Epley, historique des sessions), « Add to Current Workout » si une séance est en cours
- **Profile** : totaux (séances, volume, streak, PRs), **Weekly Volume** (line chart Canvas maison avec dégradé), **Muscle Split** (barres par groupe musculaire), records récents
- **Réglages** (roue crantée) : unités **kg/lb** (conversion partout, y compris les champs de saisie), timer 60/90/120/180 s, **thème sombre/clair**, recharger la démo, tout effacer
- **Données de démo** : 45 séances sur 12 semaines (Push/Pull/Legs progressifs, PRs rétro-calculés), 4 routines — générées au premier lancement
- **Persistance** : SQLite locale (workouts/routines en blobs JSON via kotlinx.serialization, table settings), écrite à chaque mutation

## Architecture

```
android/
├── app/src/main/java/com/hevyclone/app/
│   ├── MainActivity.kt          single Activity, edge-to-edge, thème
│   ├── data/
│   │   ├── Models.kt            @Serializable : Workout, Routine, SetEntry…
│   │   ├── Data.kt              122 exercices, cues, hints matériel
│   │   ├── Calc.kt              PUR : 1RM Epley, volume, streak, PR cache,
│   │   │                        semaine/muscles, seed démo (unit-testable)
│   │   └── Repo.kt              SQLite + mémoire + mutations (rev → recompose)
│   └── ui/
│       ├── Theme.kt             colorScheme dark/light (jaune #FFDB5C)
│       ├── Comps.kt             cartes, chips, métriques, LineChart Canvas
│       ├── App.kt               pile de navigation + bottom bar
│       ├── Home.kt, History.kt, Routines.kt, Exercises.kt, Profile.kt
│       └── Logger.kt            éditeur séance/routine, timer, picker, résumé
└── app/src/test/java/com/hevyclone/app/LogicTest.kt   8 tests JUnit
```

- **Navigation** : pile maison (`mutableStateListOf`) + `BackHandler` — pas de lib de navigation
- **Rest timer** : objet singleton avec `mutableStateOf` + ticker `produceState` (250 ms)
- **Recomposition** : compteur `Repo.rev` incrémenté à chaque mutation de données

## Testé

- `assembleDebug` : BUILD SUCCESSFUL
- `testDebugUnitTest` : 8/8 (1RM, volume/sets/reps, conversion kg↔lb, PR cache + flags chrono, streak, cohérence du seed, stats hebdo)
- **Émulateur Android 14 (Pixel 6 AVD)** : parcours complet — 5 onglets, calendrier, détail séance avec badges PR, création d'une séance (picker → recherche → saisie 80 kg × 8 → coche → timer REST 1:28 → Finish → résumé exact (640 kg, 1 série, « no new records » car 80 < 82.5) → Save) → incréments vérifiés sur l'accueil (+640 kg, +8 reps, +1 séance), filtres musculaires, fiche exercice, profil + graphiques
