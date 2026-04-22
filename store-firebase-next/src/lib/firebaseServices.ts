import { getFirebase } from "./firebase";

/**
 * Exports pratiques pour importer `db`, `auth`, `storage` sans répéter getFirebase().
 * Attention : reste un singleton derrière.
 */
export const firebase = getFirebase();
export const firebaseApp = firebase.app;
export const auth = firebase.auth;
export const db = firebase.db;
export const storage = firebase.storage;

