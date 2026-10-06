/** Login / signup gate — port of Auth.kt (Liftbook wordmark, Google button, skip). */
import React, { useState } from "react";
import { KeyboardAvoidingView, Platform, Pressable, ScrollView, View } from "react-native";
import { C, accentColor } from "../theme";
import { Cloud } from "../cloud";
import { useStore } from "../store";
import { s } from "../l10n";
import { GoogleG, Row, Spinner, TF, Txt } from "./comps";
import { MIcon } from "./icons";

export default function AuthScreen() {
  const [mode, setMode] = useState(0); // 0 = sign in, 1 = create account
  const [email, setEmail] = useState("");
  const [pw, setPw] = useState("");
  const [showPw, setShowPw] = useState(false);
  const ui = useStore(Cloud.uiStore);
  const accent = accentColor("blue");

  const submit = () => {
    const mail = email.trim();
    if (!mail.includes("@") || mail.length < 5) {
      Cloud.authError = s("Enter a valid email address", "Entre une adresse email valide");
      return;
    }
    if (pw.length < 6) {
      Cloud.authError = s("Password must be 6+ characters", "Mot de passe de 6 caractères minimum");
      return;
    }
    Cloud.authError = null;
    void (mode === 0 ? Cloud.signIn(mail, pw) : Cloud.signUp(mail, pw));
  };

  const canSubmit = email.trim() !== "" && pw !== "" && !ui.busy;

  return (
    <KeyboardAvoidingView behavior={Platform.OS === "ios" ? "padding" : undefined} style={{ flex: 1, backgroundColor: C.Bg }}>
      <ScrollView contentContainerStyle={{ paddingHorizontal: 24, paddingTop: 46, paddingBottom: 30 }} keyboardShouldPersistTaps="handled">
        <Row>
          <View style={{ width: 58, height: 58, borderRadius: 16, backgroundColor: accent, alignItems: "center", justifyContent: "center" }}>
            <MIcon name="fitness-center" size={30} color="#fff" />
          </View>
          <View style={{ width: 14 }} />
          <View>
            <Txt weight={800} size={24}>
              Liftbook
            </Txt>
            <Txt size={12.5} weight={500} color={C.Mut}>
              {s("Workout tracker", "Carnet de musculation")}
            </Txt>
          </View>
        </Row>
        <View style={{ height: 34 }} />
        <Txt weight={800} size={26}>
          {mode === 0 ? s("Welcome back", "Content de te revoir") : s("Create an account", "Créer un compte")}
        </Txt>
        <View style={{ height: 8 }} />
        <Txt size={14} color={C.Mut} style={{ lineHeight: 20 }}>
          {s(
            "Your workouts and routines, synced on all your devices.",
            "Tes séances et tes routines, synchronisées sur tous tes appareils.",
          )}
        </Txt>
        <View style={{ height: 26 }} />
        <TF
          value={email}
          onChange={(v) => {
            setEmail(v);
            Cloud.authError = null;
          }}
          placeholder={s("Email", "Email")}
          keyboardType="email-address"
          returnKey="next"
          height={56}
          weight={500}
        />
        <View style={{ height: 12 }} />
        <TF
          value={pw}
          onChange={(v) => {
            setPw(v);
            Cloud.authError = null;
          }}
          placeholder={s("Password", "Mot de passe")}
          password
          showPw={showPw}
          onTogglePw={() => setShowPw(!showPw)}
          onSubmit={submit}
          height={56}
          weight={500}
        />
        {ui.authError ? (
          <Txt size={13.5} color={C.Red} style={{ marginTop: 10 }}>
            {ui.authError}
          </Txt>
        ) : null}
        <View style={{ height: 20 }} />
        <Pressable
          onPress={submit}
          disabled={!canSubmit}
          style={{
            height: 52,
            borderRadius: 12,
            backgroundColor: canSubmit ? accent : C.Card,
            alignItems: "center",
            justifyContent: "center",
            flexDirection: "row",
          }}
        >
          {ui.busy ? (
            <Spinner color={C.Mut} />
          ) : (
            <Txt weight={700} size={16} color={canSubmit ? C.AccText : C.Mut}>
              {mode === 0 ? s("Sign in", "Se connecter") : s("Create account", "Créer un compte")}
            </Txt>
          )}
        </Pressable>
        <View style={{ height: 18 }} />
        <Row>
          <View style={{ flex: 1, height: 1, backgroundColor: C.Line }} />
          <Txt size={13} color={C.Mut} style={{ marginHorizontal: 14 }}>
            {s("or", "ou")}
          </Txt>
          <View style={{ flex: 1, height: 1, backgroundColor: C.Line }} />
        </Row>
        <View style={{ height: 18 }} />
        <Pressable
          onPress={() => void Cloud.startGoogleAuth()}
          disabled={ui.busy}
          style={{ height: 52, borderRadius: 12, backgroundColor: "#FFFFFF", alignItems: "center", justifyContent: "center", flexDirection: "row" }}
        >
          <GoogleG size={22} />
          <View style={{ width: 12 }} />
          <Txt weight={600} size={15.5} color="#1F1F1F">
            {s("Continue with Google", "Continuer avec Google")}
          </Txt>
        </Pressable>
        {ui.googlePending ? (
          <Row style={{ marginTop: 12, justifyContent: "center" }}>
            <Spinner size={16} color={accent} />
            <View style={{ width: 10 }} />
            <Txt size={13.5} color={C.Mut}>
              {s("Finishing Google sign-in…", "Connexion Google en cours…")}
            </Txt>
          </Row>
        ) : null}
        <View style={{ height: 22 }} />
        <Row style={{ justifyContent: "center" }}>
          <Txt size={14} color={C.Mut}>
            {mode === 0 ? s("No account yet? ", "Pas encore de compte ? ") : s("Already have an account? ", "Déjà un compte ? ")}
          </Txt>
          <Pressable
            onPress={() => {
              setMode(1 - mode);
              Cloud.authError = null;
            }}
          >
            <Txt weight={600} size={14} color={accent}>
              {mode === 0 ? s("Sign up", "S'inscrire") : s("Sign in", "Se connecter")}
            </Txt>
          </Pressable>
        </Row>
        <View style={{ height: 40 }} />
        <Pressable
          onPress={() => {
            Cloud.skipped = true;
            void Cloud.persistSkip();
          }}
        >
          <Txt size={14} weight={500} color={C.Mut} style={{ textAlign: "center" }}>
            {s("Continue without an account", "Continuer sans compte")}
          </Txt>
        </Pressable>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}
