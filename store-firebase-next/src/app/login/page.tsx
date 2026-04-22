"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import type { FirebaseError } from "firebase/app";
import { onAuthStateChanged } from "firebase/auth";
import type { Auth } from "firebase/auth";

import styles from "./page.module.css";
import {
  signInWithEmailPassword,
  signInWithGoogle,
  signUpWithEmailPassword,
} from "@/lib/firebaseAuth";
import { getFirebase } from "@/lib/firebase";

function friendlyFirebaseError(err: unknown): string {
  const e = err as Partial<FirebaseError> & { message?: string };
  const code = typeof e?.code === "string" ? e.code : "";

  switch (code) {
    case "auth/invalid-email":
      return "Email invalide.";
    case "auth/missing-password":
      return "Mot de passe requis.";
    case "auth/wrong-password":
      return "Email ou mot de passe incorrect.";
    case "auth/user-not-found":
      return "Aucun utilisateur trouvé avec cet email.";
    case "auth/email-already-in-use":
      return "Cet email est déjà utilisé.";
    case "auth/weak-password":
      return "Mot de passe trop faible (6 caractères minimum).";
    case "auth/popup-closed-by-user":
      return "Connexion Google annulée.";
    case "auth/operation-not-allowed":
      return "Méthode d'authentification non activée dans Firebase Console.";
    default:
      return e?.message || "Erreur inconnue. Vérifie ta configuration Firebase.";
  }
}

export default function LoginPage() {
  const router = useRouter();
  const [auth, setAuth] = useState<Auth | null>(null);

  const [mode, setMode] = useState<"signin" | "signup">("signin");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    try {
      const { auth: a } = getFirebase();
      setAuth(a);
    } catch (e) {
      setError(friendlyFirebaseError(e));
    }
  }, []);

  useEffect(() => {
    if (!auth) return;
    const unsub = onAuthStateChanged(auth, (user) => {
      if (user) router.replace("/dashboard");
    });
    return () => unsub();
  }, [auth, router]);

  async function handleEmailAuth(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      if (mode === "signup") {
        await signUpWithEmailPassword(email.trim(), password);
      } else {
        await signInWithEmailPassword(email.trim(), password);
      }
      router.replace("/dashboard");
    } catch (err) {
      setError(friendlyFirebaseError(err));
    } finally {
      setLoading(false);
    }
  }

  async function handleGoogle() {
    setError(null);
    setLoading(true);
    try {
      await signInWithGoogle();
      router.replace("/dashboard");
    } catch (err) {
      setError(friendlyFirebaseError(err));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className={styles.page}>
      <div className={styles.card}>
        <div className={styles.title}>
          {mode === "signin" ? "Connexion" : "Créer un compte"}
        </div>
        <div className={styles.subtitle}>
          Authentification Firebase (Email/Mot de passe + Google).
        </div>

        {error ? <div className={styles.error}>{error}</div> : null}

        <form className={styles.form} onSubmit={handleEmailAuth}>
          <div className={styles.field}>
            <label className={styles.label} htmlFor="email">
              Email
            </label>
            <input
              id="email"
              className={styles.input}
              type="email"
              autoComplete="email"
              inputMode="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="ex: admin@sucrestore.com"
              required
              disabled={loading || !auth}
            />
          </div>

          <div className={styles.field}>
            <label className={styles.label} htmlFor="password">
              Mot de passe
            </label>
            <input
              id="password"
              className={styles.input}
              type="password"
              autoComplete={
                mode === "signup" ? "new-password" : "current-password"
              }
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="6 caractères minimum"
              required
              disabled={loading || !auth}
            />
          </div>

          <div className={styles.row}>
            <button
              className={styles.btn}
              type="submit"
              disabled={loading || !auth}
            >
              {loading
                ? "Chargement…"
                : mode === "signin"
                  ? "Se connecter"
                  : "Créer le compte"}
            </button>
            <button
              className={`${styles.btn} ${styles.btnSecondary}`}
              type="button"
              disabled={loading || !auth}
              onClick={() =>
                setMode((m) => (m === "signin" ? "signup" : "signin"))
              }
            >
              {mode === "signin" ? "Créer un compte" : "J'ai déjà un compte"}
            </button>
          </div>
        </form>

        <div className={styles.divider}>OU</div>

        <button
          className={styles.btn}
          onClick={handleGoogle}
          disabled={loading || !auth}
        >
          Continuer avec Google
        </button>

        <div className={styles.note}>
          Pense à activer <b>Email/Password</b> et <b>Google</b> dans Firebase
          Console → Authentication → Sign-in method, et à ajouter ton domaine
          (ex: <code>localhost</code>) dans Authorized domains.
        </div>
      </div>
    </div>
  );
}

