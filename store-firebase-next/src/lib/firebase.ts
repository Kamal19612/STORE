import { FirebaseApp, getApp, getApps, initializeApp } from "firebase/app";
import { Auth, getAuth, GoogleAuthProvider } from "firebase/auth";
import { Firestore, getFirestore } from "firebase/firestore";
import { FirebaseStorage, getStorage } from "firebase/storage";

type FirebaseServices = {
  app: FirebaseApp;
  auth: Auth;
  db: Firestore;
  storage: FirebaseStorage;
  googleProvider: GoogleAuthProvider;
};

function requiredEnv(name: string): string {
  const v = process.env[name];
  if (!v || v.trim().length === 0) {
    throw new Error(
      `[firebase] Missing environment variable: ${name}. ` +
        `Create .env.local and set NEXT_PUBLIC_FIREBASE_* values.`,
    );
  }
  return v;
}

function getFirebaseConfig() {
  return {
    apiKey: requiredEnv("NEXT_PUBLIC_FIREBASE_API_KEY"),
    authDomain: requiredEnv("NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN"),
    projectId: requiredEnv("NEXT_PUBLIC_FIREBASE_PROJECT_ID"),
    storageBucket: requiredEnv("NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET"),
    messagingSenderId: requiredEnv("NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID"),
    appId: requiredEnv("NEXT_PUBLIC_FIREBASE_APP_ID"),
  };
}

/**
 * Point d'entrée unique (singleton) pour Firebase.
 * - Initialise l'app une seule fois (getApps/getApp)
 * - Expose Auth + Firestore + Storage + Google provider
 */
export function getFirebase(): FirebaseServices {
  const app = getApps().length ? getApp() : initializeApp(getFirebaseConfig());

  // Services
  const auth = getAuth(app);
  const db = getFirestore(app);
  const storage = getStorage(app);

  // Providers
  const googleProvider = new GoogleAuthProvider();

  return { app, auth, db, storage, googleProvider };
}

