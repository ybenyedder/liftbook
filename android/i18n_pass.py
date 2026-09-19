#!/usr/bin/env python3
"""Localize UI literals: replace hardcoded strings with L10n.s(...) calls."""
import pathlib, sys

ROOT = pathlib.Path("app/src/main/java/com/hevyclone/app/ui")

# (file, old, new)
R = []

def add(f, old, new):
    R.append((f, old, new))

# ---- App.kt ----
add("App.kt", 'TabDef(Screen.HomeTab, "Accueil"', 'TabDef(Screen.HomeTab, L10n.s("Home", "Accueil", "Inicio", "Startseite")')
add("App.kt", 'TabDef(Screen.TrainingTab, "Entraînement"', 'TabDef(Screen.TrainingTab, L10n.s("Training", "Entraînement", "Entrenamiento", "Training")')
add("App.kt", 'TabDef(Screen.ProfileTab, "Profil"', 'TabDef(Screen.ProfileTab, L10n.s("Profile", "Profil", "Perfil", "Profil")')

# ---- Home.kt ----
add("Home.kt", 'Text("Accueil", fontSize = 27.sp', 'Text(L10n.s("Home", "Accueil", "Inicio", "Startseite"), fontSize = 27.sp')
add("Home.kt", 'toast(ctx, "Pas de nouvelles notifications")', 'toast(ctx, L10n.s("No new notifications", "Pas de nouvelles notifications", "Sin notificaciones nuevas", "Keine neuen Benachrichtigungen"))')
add("Home.kt", 'EmptyState("Aucune séance pour le moment.\\nVa dans Entraînement pour démarrer !")', 'EmptyState(L10n.s("No workouts yet.\\nHead to Training to get started!", "Aucune séance pour le moment.\\nVa dans Entraînement pour démarrer !", "Aún no hay entrenamientos.\\n¡Ve a Entrenamiento para empezar!", "Noch keine Workouts.\\nStarte unter Training!"))')
add("Home.kt", 'Text("Athlètes Recommandés"', 'Text(L10n.s("Suggested Athletes", "Athlètes Recommandés", "Atletas Recomendados", "Empfohlene Athleten")')
add("Home.kt", 'toast(ctx, "Invitation copiée !")', 'toast(ctx, L10n.s("Invite copied!", "Invitation copiée !", "¡Invitación copiada!", "Einladung kopiert!"))')
add("Home.kt", 'Text("Inviter un ami"', 'Text(L10n.s("Invite a friend", "Inviter un ami", "Invitar a un amigo", "Freund einladen")')
add("Home.kt", 'Text("Suivre"', 'Text(L10n.s("Follow", "Suivre", "Seguir", "Folgen")')
add("Home.kt", 'toast(ctx, "Demande envoyée à $name")', 'toast(ctx, L10n.s("Request sent to $name", "Demande envoyée à $name"))')
add("Home.kt", 'toast(ctx, "Options de la séance")', 'toast(ctx, L10n.s("Workout options", "Options de la séance"))')
add("Home.kt", 'toast(ctx, "Ajouté aux favoris")', 'toast(ctx, L10n.s("Added to favorites", "Ajouté aux favoris"))')
add("Home.kt", 'toast(ctx, "Les commentaires arrivent bientôt")', 'toast(ctx, L10n.s("Comments coming soon", "Les commentaires arrivent bientôt"))')
add("Home.kt", 'toast(ctx, "Partagé !")', 'toast(ctx, L10n.s("Shared!", "Partagé !", "¡Compartido!", "Geteilt!"))')
add("Home.kt", '"${ex.sets.size} série${if (ex.sets.size > 1) "s" else ""} ${ex.name}"', 'seriesLabel(ex.sets.size, exName(ex.name))')
add("Home.kt", '"Voir $hidden exercice${if (hidden > 1) "s" else ""} en plus"', 'seeMoreExercises(hidden)')
add("Home.kt", 'FeedStat("Temps"', 'FeedStat(L10n.s("Time", "Temps", "Tiempo", "Zeit")')
add("Home.kt", 'FeedStat("Volume"', 'FeedStat(L10n.s("Volume", "Volume", "Volumen", "Volumen")')
add("Home.kt", 'FeedStat("Records"', 'FeedStat(L10n.s("Records", "Records", "Récords", "Rekorde")')

# ---- Routines.kt (Training) ----
add("Routines.kt", 'Text("Entraînement", fontSize = 26.sp', 'Text(L10n.s("Training", "Entraînement", "Entrenamiento", "Training"), fontSize = 26.sp')
add("Routines.kt", 'toast(ctx, "Synchronisé")', 'toast(ctx, L10n.s("Synced", "Synchronisé"))')
add("Routines.kt", 'Text("Démarrer un Entraînement Vide", fontSize = 16.sp', 'Text(L10n.s("Start an Empty Workout", "Démarrer un Entraînement Vide", "Iniciar un Entrenamiento Vacío", "Leeres Workout starten"), fontSize = 16.sp')
add("Routines.kt", 'Text("Routines", fontSize = 20.sp', 'Text(L10n.s("Routines", "Routines", "Rutinas", "Routinen"), fontSize = 20.sp')
add("Routines.kt", 'Text("Nouv. Routine"', 'Text(L10n.s("New Routine", "Nouv. Routine", "Nueva Rutina", "Neue Routine")')
add("Routines.kt", 'Text("Explorer"', 'Text(L10n.s("Explore", "Explorer", "Explorar", "Entdecken")')
add("Routines.kt", '"Mes routines (${routines.size})"', 'L10n.s("My routines (%1$d)", "Mes routines (%1$d)", "Mis rutinas (%1$d)", "Meine Routinen (%1$d)").format(routines.size)')
add("Routines.kt", '"Commencer la Routine", color = MaterialTheme.colorScheme.onPrimary, fontSize = 15.sp', 'L10n.s("Start Routine", "Commencer la Routine", "Comenzar la Rutina", "Routine starten"), color = MaterialTheme.colorScheme.onPrimary, fontSize = 15.sp')
add("Routines.kt", 'toast(ctx, "Termine d\'abord la séance en cours")', 'toast(ctx, L10n.s("Finish the current workout first", "Termine d\'abord la séance en cours"))')
add("Routines.kt", 'toast(ctx, "Options de la routine")', 'toast(ctx, L10n.s("Routine options", "Options de la routine"))')
add("Routines.kt", 'EmptyState("Aucune routine.\\nTouche « Nouv. Routine » pour en créer une.")', 'EmptyState(L10n.s("No routines.\\nTap “New Routine” to create one.", "Aucune routine.\\nTouche « Nouv. Routine » pour en créer une."))')
add("Routines.kt", 'Text("Routine", fontWeight = FontWeight.SemiBold', 'Text(L10n.s("Routine", "Routine", "Rutina", "Routine"), fontWeight = FontWeight.SemiBold')
add("Routines.kt", 'toast(ctx, "Partagé !")', 'toast(ctx, L10n.s("Shared!", "Partagé !"))')
add("Routines.kt", 'toast(ctx, "Options de la routine") } ) { Icon(Icons.Rounded.MoreHoriz, null) }', 'toast(ctx, L10n.s("Routine options", "Options de la routine")) } ) { Icon(Icons.Rounded.MoreHoriz, null) }')
add("Routines.kt", 'r.exercises.joinToString(", ") { it.name }', 'r.exercises.joinToString(", ") { exName(it.name) }')
add("Routines.kt", '"Créée par ${Repo.settings.handle}"', 'L10n.s("Created by %1$s", "Créée par %1$s").format(Repo.settings.handle)')
add("Routines.kt", 'Text("Commencer la Routine", color = MaterialTheme.colorScheme.onPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)', 'Text(L10n.s("Start Routine", "Commencer la Routine", "Comenzar la Rutina", "Routine starten"), color = MaterialTheme.colorScheme.onPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)')
add("Routines.kt", '"3 derniers mois ˅"', 'L10n.s("3 months ˅", "3 derniers mois ˅")')
add("Routines.kt", 'listOf("Volume", "Réps", "Durée")', 'listOf(L10n.s("Volume", "Volume"), L10n.s("Reps", "Réps"), L10n.s("Duration", "Durée"))')
add("Routines.kt", 'Text("Exercices", fontSize = 18.sp', 'Text(L10n.s("Exercises", "Exercices", "Ejercicios", "Übungen"), fontSize = 18.sp')
add("Routines.kt", 'Text(\n                        "Modifier la Routine",', 'Text(\n                        L10n.s("Edit Routine", "Modifier la Routine"),')
add("Routines.kt", '"Minuteur de Repos: ${if (m > 0) "${m}min " else ""}${s}s"', 'L10n.s("Rest Timer: %1$s", "Minuteur de Repos: %1$s").format("${if (m > 0) "${m}min " else ""}${s}s")')
add("Routines.kt", 'Text("SÉRIE"', 'Text(L10n.s("SET", "SÉRIE")')
add("Routines.kt", 'Text("RÉPS"', 'Text(L10n.s("REPS", "RÉPS")')
add("Routines.kt", 'Nav.push(Screen.ExerciseDetail(ex.name)) }', 'Nav.push(Screen.ExerciseDetail(ex.name)) }')  # name display handled below
add("Routines.kt", 'Text(\n                            ex.name,\n                            color = MaterialTheme.colorScheme.primary, fontSize = 18.sp', 'Text(\n                            exName(ex.name),\n                            color = MaterialTheme.colorScheme.primary, fontSize = 18.sp')
add("Routines.kt", 'Text(if (s.kg != null) Calc.fmtKg(s.kg, unit) else "—"', 'Text(if (s.kg != null) Calc.fmtKg(s.kg, unit) else "—"')

# ---- Logger.kt ----
add("Logger.kt", '"Ajouter un Exercice"', 'L10n.s("Add Exercise", "Ajouter un Exercice", "Añadir Ejercicio", "Übung hinzufügen")')
add("Logger.kt", 'if (isWorkout) "Terminer" else "Enregistrer"', 'if (isWorkout) L10n.s("Finish", "Terminer") else L10n.s("Save", "Enregistrer")')
add("Logger.kt", '"+  Ajouter une Série"', 'L10n.s("+ Add Set", "+  Ajouter une Série")')
add("Logger.kt", 'Text("REPOS"', 'Text(L10n.s("REST", "REPOS")')
add("Logger.kt", '"Copier la série"', 'L10n.s("Copy set", "Copier la série")')
add("Logger.kt", '"Supprimer la série"', 'L10n.s("Delete set", "Supprimer la série")')
add("Logger.kt", '"Choisir un Exercice"', 'L10n.s("Select Exercise", "Choisir un Exercice")')
add("Logger.kt", '"Rechercher des exercices"', 'L10n.s("Search exercises", "Rechercher des exercices")')
add("Logger.kt", '"Aucun exercice trouvé."', 'L10n.s("No exercises found.", "Aucun exercice trouvé.")')
add("Logger.kt", '"Résumé de la séance"', 'L10n.s("Workout Summary", "Résumé de la séance")')
add("Logger.kt", 'GhostButton("Supprimer", onClick = onDiscard', 'GhostButton(L10n.s("Discard", "Supprimer"), onClick = onDiscard')
add("Logger.kt", 'PrimaryButton("Save", onClick = onSave', 'PrimaryButton(L10n.s("Save", "Enregistrer"), onClick = onSave')
add("Logger.kt", '"Supprimer la séance ?"', 'L10n.s("Discard workout?", "Supprimer la séance ?")')
add("Logger.kt", '"Ignorer les modifications ?"', 'L10n.s("Discard changes?", "Ignorer les modifications ?")')
add("Logger.kt", '"Aucune série terminée"', 'L10n.s("No sets completed", "Aucune série terminée")')
add("Logger.kt", 'Text("Continuer") }', 'Text(L10n.s("Keep editing", "Continuer")) }')
add("Logger.kt", '{ Text("Annuler") }', '{ Text(L10n.s("Cancel", "Annuler")) }')
add("Logger.kt", '{ Text("Supprimer", color', '{ Text(L10n.s("Discard", "Supprimer"), color')
add("Logger.kt", 'if (isWorkout) "Supprimer la séance" else "Ignorer les modifications"', 'if (isWorkout) L10n.s("Discard workout", "Supprimer la séance") else L10n.s("Discard changes", "Ignorer les modifications")')
add("Logger.kt", 'toast(ctx, "Supprimée")', 'toast(ctx, L10n.s("Discarded", "Supprimée"))')
add("Logger.kt", 'toast(ctx, "Séance enregistrée")', 'toast(ctx, L10n.s("Workout saved", "Séance enregistrée"))')
add("Logger.kt", 'toast(ctx, "Routine enregistrée")', 'toast(ctx, L10n.s("Routine saved", "Routine enregistrée"))')
add("Logger.kt", 'toast(ctx, "Donne un nom à ta routine")', 'toast(ctx, L10n.s("Name your routine first", "Donne un nom à ta routine"))')
add("Logger.kt", 'toast(ctx, "Ajoute au moins un exercice")', 'toast(ctx, L10n.s("Add at least one exercise", "Ajoute au moins un exercice"))')
add("Logger.kt", '"Durée"', 'L10n.s("Duration", "Durée")')
add("Logger.kt", '"Séries"', 'L10n.s("Sets", "Séries")')
add("Logger.kt", '"Réps"', 'L10n.s("Reps", "Réps")')
add("Logger.kt", '"Aucun nouveau record cette séance"', 'L10n.s("No new records this session", "Aucun nouveau record cette séance")')
add("Logger.kt", 'Text("SÉRIE"', 'Text(L10n.s("SET", "SÉRIE")')
add("Logger.kt", 'Text("PRÉCÉDENTE"', 'Text(L10n.s("PREVIOUS", "PRÉCÉDENTE")')
add("Logger.kt", 'Text("RÉPS"', 'Text(L10n.s("REPS", "RÉPS")')

files = sorted(set(f for (f, _, _) in R))
content = {f: (ROOT / f).read_text() for f in files}
missed = []
for (f, old, new) in R:
    if old in content[f]:
        content[f] = content[f].replace(old, new, 1)
    else:
        missed.append((f, old[:60]))
for f in files:
    (ROOT / f).write_text(content[f])
print("applied:", len(R) - len(missed), "missed:", len(missed))
for f, o in missed:
    print(" MISS", f, "|", o)
