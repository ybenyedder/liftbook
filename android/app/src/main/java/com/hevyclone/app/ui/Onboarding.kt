package com.hevyclone.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hevyclone.app.data.Repo

/**
 * Premier lancement uniquement (Repo.needsOnboarding) : titre + accroche, trois puces,
 * gros CTA « START » qui crée un brouillon vide et ouvre le Logger, et un lien discret
 * « Passer ». Les deux posent le flag onboarding via le mécanisme des réglages.
 */
@Composable
fun OnboardingScreen() {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.weight(0.6f))
        // ---- Titre + accroche ----
        Box(
            Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.FitnessCenter, contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(34.dp),
            )
        }
        Spacer(Modifier.height(22.dp))
        Text(
            "Liftbook",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 38.sp, fontWeight = FontWeight.ExtraBold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            L10n.s("Your training journal — simple, fast, offline.", "Ton journal d'entraînement — simple, rapide, hors-ligne."),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 15.5.sp, lineHeight = 22.sp,
        )
        Spacer(Modifier.height(30.dp))
        // ---- Trois puces ----
        OnboardBullet(L10n.s("Create any workout in seconds", "Créer une séance en quelques secondes"))
        OnboardBullet(L10n.s("Track your volume and records", "Suivre ton volume et tes records"))
        OnboardBullet(L10n.s("Optional cloud sync on your account", "Synchronisation cloud optionnelle via ton compte"))
        Spacer(Modifier.weight(0.7f))
        // ---- GROS CTA : crée un brouillon vide et ouvre le Logger ----
        PrimaryButton(
            "START",
            onClick = {
                // Même effet que le tap widget (pendingStartEmpty) : on en prend le relais.
                Nav.pendingStartEmpty = false
                Repo.setOnboarded()
                if (Repo.draft == null) Repo.startWorkout(null)
                if (Nav.current != Screen.Logger) Nav.push(Screen.Logger)
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        // ---- Lien discret : pose juste le flag ----
        Text(
            L10n.s("Skip", "Passer"),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickableNoRipple { Repo.setOnboarded() }
                .padding(vertical = 14.dp),
        )
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun OnboardBullet(text: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        )
        Spacer(Modifier.width(14.dp))
        Text(text, color = MaterialTheme.colorScheme.onBackground, fontSize = 15.5.sp, lineHeight = 22.sp)
    }
}
