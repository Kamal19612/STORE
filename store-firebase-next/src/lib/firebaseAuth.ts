import {
  UserCredential,
  createUserWithEmailAndPassword,
  signInWithEmailAndPassword,
  signInWithPopup,
  signOut as fbSignOut,
} from "firebase/auth";

import { getFirebase } from "./firebase";

export async function signUpWithEmailPassword(
  email: string,
  password: string,
): Promise<UserCredential> {
  const { auth } = getFirebase();
  return await createUserWithEmailAndPassword(auth, email, password);
}

export async function signInWithEmailPassword(
  email: string,
  password: string,
): Promise<UserCredential> {
  const { auth } = getFirebase();
  return await signInWithEmailAndPassword(auth, email, password);
}

export async function signInWithGoogle(): Promise<UserCredential> {
  const { auth, googleProvider } = getFirebase();
  return await signInWithPopup(auth, googleProvider);
}

export async function signOut(): Promise<void> {
  const { auth } = getFirebase();
  await fbSignOut(auth);
}

