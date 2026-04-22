"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { onAuthStateChanged } from "firebase/auth";
import type { Auth } from "firebase/auth";

import { getFirebase } from "@/lib/firebase";
import { signOut } from "@/lib/firebaseAuth";

export default function DashboardPage() {
  const router = useRouter();
  const [auth, setAuth] = useState<Auth | null>(null);
  const [email, setEmail] = useState<string>("");
  const [error, setError] = useState<string>("");

  useEffect(() => {
    try {
      const { auth: a } = getFirebase();
      setAuth(a);
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    }
  }, []);

  useEffect(() => {
    if (!auth) return;
    const unsub = onAuthStateChanged(auth, (user) => {
      if (!user) {
        router.replace("/login");
        return;
      }
      setEmail(user.email ?? "");
    });
    return () => unsub();
  }, [auth, router]);

  async function handleLogout() {
    await signOut();
    router.replace("/login");
  }

  return (
    <div style={{ flex: 1, padding: 24, fontFamily: "var(--font-geist-sans)" }}>
      <h1 style={{ fontSize: 22, marginBottom: 8 }}>Dashboard</h1>
      {error ? (
        <p style={{ color: "tomato", marginBottom: 12 }}>{error}</p>
      ) : null}
      <p style={{ opacity: 0.7, marginBottom: 16 }}>
        Connecté en tant que <b>{email || "—"}</b>
      </p>

      <button
        onClick={handleLogout}
        disabled={!auth}
        style={{
          height: 40,
          padding: "0 14px",
          borderRadius: 10,
          border: "1px solid rgba(0,0,0,0.12)",
          cursor: "pointer",
        }}
      >
        Se déconnecter
      </button>
    </div>
  );
}

