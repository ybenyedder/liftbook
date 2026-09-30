# Liftbook (ex-Hevy Clone) — App Android native

Clone fonctionnel de l'app de workout tracking **Hevy**, en **Kotlin natif** (Jetpack Compose, Material 3), UI française complète (**609 exercices traduits** — 16 groupes dont Cardio —, noms alignés sur Hevy : « Tirage Poitrine », « Rowing », « Développé »…) + EN/ES/DE. Implémentation originale : aucun asset ni code de l'app officielle — design recréé par comparaison capture par capture avec les références officielles (Play Store FR) et les captures fournies par l'utilisateur. **Aucune donnée factice** : l'app démarre vide, tout est créé par l'utilisateur.

**Dernière version : v1.35** — https://github.com/ybenyedder/hevy-clone/releases

## Installation

Télécharge `hevy-clone-v1.28-release.apk` depuis la page Releases et installe-le (Android 8+, source inconnue autorisée). APK release R8 signé (~1,7 Mo). La langue suit automatiquement celle du téléphone (FR par défaut chez toi).

## Fonctions (toutes vérifiées par capture sur émulateur fr-FR ou par test unitaire)

### Fidélité Hevy (comparaison capture par capture)
- 3 onglets (Accueil / Entraînement / Profil), thème noir `#111113` + accent configurable, police Inter (OFL)
- **Accueil** : fil social (posts de séances, Temps/Volume/Records, chip du nombre de séries, « Voir N exercices en plus »)
- **Logger** : TERMINER en barre haute, **rangée live Durée / Volume / Séries**, grille SÉRIE|PRÉCÉDENTE|KG|RÉPS (précédent « 80kg × 8 »), coche à droite, **« Repos : 2min 0s » sur chaque carte** (toucher = picker 5 s type Hevy), **notes inline « Ajouter des notes ici… »**
- **Éditeur de routine** : barre Annuler | Créer une Routine | Enregistrer + champ « Titre de la routine » (captures Hevy)
- **Picker** : « Choisir des exercices » + OK bleu, illustrations anatomiques à gauche, sous-titre « Muscle · Matériel », chips muscles en français, bouton + bleu
- **Entraînement** : Démarrer vide / Reprendre la dernière / Nouvelle routine / Explorer / Mes routines (n) + tri
- **Détail de séance** : 4 stats (Temps/Volume/Records/Séries), cartes exercices, table alternée avec PRÉCÉDENTE
- **Historique** : sections Cette semaine ▾, date-box + plage horaire + volume, recherche floue + filtres de période, calendrier
- **Fiche exercice** : onglets **Résumé / Historique / Instructions** (Records + périodes 3m/6m/1a/Tout + 2 graphiques + progression vs record) / **instructions pas-à-pas numérotées** (57 archétypes de mouvement FR/EN, haltéro/plyo/cardio inclus) + conseils + muscle principal + matériel
- **Profil** : heatmap annuelle du volume, totaux, mois en cours, semaine glissante vs précédente, barres 6 mois, volume par groupe musculaire, historique

### Entraînement
- Supersets (menu ⋯, rail bleu + label), **drag-and-drop** des exercices (appui long)
- Minuteurs : repos global, **par exercice**, **notification système façon Hevy : fond vrai noir, barre de progression, boutons − 15s / + 15s / Passer directement dans la notification** (service foreground, persistant au redémarrage) ; chronomètre de séance en notification noire permanente
- Brouillon de séance **persistant** (survit à la fermeture/kill de l'app) ; reprise de la dernière séance ou **de n'importe quelle séance passée** (menu ⋯ du détail)
- Notes de séance et notes d'exercice (dialogues), états PR (Epley) avec badges WEIGHT/1RM PR

### Recherche
- Moteur **tolérant aux fautes** (Levenshtein sur le nom uniquement), insensible aux accents, multi-mots
- **Remplacement d'exercice en pleine séance** (menu carte, les séries restent)
- Matche noms FR + EN + **alias de salle** (« dc », « sdt », « barre au front »…) + **synonymes de muscles** (dos → dorsaux/lombaires/trapèzes, abdos, jambes → quadriceps/ischio/mollets, bras, poitrine, mollets, fessiers) + **matériel FR** (poulie, haltères, barre, machine, poids du corps)
- Mots vides ignorés (en/de/la/prise…) + racinement léger (rowing→row) : « rowing poulie assis prise en v » → Tirage Horizontal (Poulie, V-Bar)
- « tirage poitrine » → Tirage Poitrine (Machine) ; « developer coucher » → Développé Couché (Barre) (tests unitaires)
- Tolérance de fautes limitée au nom pour éviter les faux positifs (« goblet » ≠ « mollets »)

### Données & système
- SQLite locale + persistance JSON du brouillon
- **Import du compte Hevy** : Profil → Réglages → « Importer depuis Hevy » — export officiel Hevy (.csv ou .zip) au format exact (title/start_time/exercise_title/weight_kg…, dates « 18 sept. 2026, 17:31 » localisées EN/FR/DE/IT/ES/PT/JA), noms FR remappés, lbs→kg, supersets, CSV multi-lignes quoté, sans doublons (testé unitairement ET en conditions réelles). **Hevy n exportant pas les templates, les routines sont reconstruites automatiquement depuis les noms de séances** (dernière occurrence de chaque programme)
- **Sauvegarde/restauration complète en fichier JSON** (roundtrip testé)
- **Export CSV** des séances + **import CSV** (parseur groupant par date+heure, remap FR→EN, testé)
- Partage réel d'une séance (share sheet Android avec résumé complet)
- Widget « Démarrer une séance » sur l'écran d'accueil (deep-link testé)
- Thèmes d'accent (Bleu/Teal/Violet/Orange), multi-fenêtre vérifié, mode paysage vérifié, transitions animées, retours haptiques

## Architecture

```
android/
├── app/src/main/java/com/hevyclone/app/
│   ├── MainActivity.kt          single Activity, edge-to-edge, deep-link widget
│   ├── QuickWidgetProvider.kt   widget RemoteViews
│   ├── data/
│   │   ├── Models.kt            @Serializable : Workout, Routine, Draft, BackupData…
│   │   ├── Data.kt              609 exercices (16 groupes musculaires dont Cardio), cues, hints matériel, instructions (57 archétypes)
│   │   ├── L10nData.kt          traductions FR + alias + synonymes + moteur de recherche flou (Levenshtein)
│   │   ├── Calc.kt              PUR : 1RM Epley, volume, streak, PR cache, CSV, seed
│   │   └── Repo.kt              SQLite + mémoire + draft persistant + backup
│   └── ui/
│       ├── Theme.kt             colorScheme + accents + Inter
│       ├── Comps.kt             cartes, chips, LineChart Canvas
│       ├── App.kt               navigation à pile + Crossfade + bottom bar
│       ├── Home.kt, Routines.kt, History.kt, Exercises.kt, Profile.kt, Logger.kt
│       └── Util.kt              partages (séance, CSV, JSON), haptique
└── app/src/test/…/LogicTest.kt  12 tests unitaires (1RM, PRs, streak, recherche floue, CSV, backup)
```

## Reconstruire / tester

```bash
cd android
JAVA_HOME=/usr/lib/jvm/java-17-openjdk ~/gradle/gradle-8.7/bin/gradle assembleDebug      # debug
JAVA_HOME=/usr/lib/jvm/java-17-openjdk ~/gradle/gradle-8.7/bin/gradle assembleRelease    # release R8 signée
JAVA_HOME=/usr/lib/jvm/java-17-openjdk ~/gradle/gradle-8.7/bin/gradle testDebugUnitTest  # 12 tests
```

## Historique des versions

v1.0 natif → v1.2 fluidité → v1.4 zéro donnée factice → v1.7–v1.9 fidélité capture par capture → v1.10 supersets/drag → v1.11 profil/partage/CSV → v1.12 filtres/records → v1.13 notes/reprise/minuteur persistant → v1.14 widget/tri → v1.15 brouillon persistant/Explorer/paysage → v1.16 refaire séance/minuteur par exercice → v1.17 graphiques mensuels/chrono notification/import CSV → v1.18 accents/progression/multi-fenêtre → v1.19 sauvegarde JSON/semaine glissante → v1.20 undo suppression/duplication → v1.21 routine depuis séance → v1.22 finition Hevy : bibliothèque 140→259 exercices (noms Hevy FR), recherche élargie (synonymes + alias + matériel FR, « tirage poitrine » OK), instructions pas-à-pas par exercice, onglets Résumé/Historique/Instructions, stats live Durée/Volume/Séries, « Repos : Xmin Ys » + picker 5 s, notes inline, éditeur de routine Annuler/Créer/Enregistrer → **v1.23 bibliothèque 609 exercices : haltérophilie, kettlebell, anneaux, élastiques, plyo, groupe Cardio, 57 archétypes d'instructions, fix Nordic/Glute Ham**. Détail complet dans l'historique git et les notes de release.

---
Projet personnel à but éducatif — non affilié à Hevy. Police Inter et illustrations vectorielles originales sous licences libres.
