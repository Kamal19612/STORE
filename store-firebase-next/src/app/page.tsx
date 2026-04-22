import Link from "next/link";
import styles from "./page.module.css";

export default function Home() {
  return (
    <div className={styles.page}>
      <main className={styles.main}>
        <div className={styles.intro}>
          <h1>SUCRE STORE</h1>
          <p>
            Démo d’authentification Firebase (Web). Utilise la page Login pour
            te connecter (Email/Mot de passe ou Google).
          </p>
        </div>
        <div className={styles.ctas}>
          <Link className={styles.primary} href="/login">
            Ouvrir la page Login
          </Link>
          <Link className={styles.secondary} href="/dashboard">
            Aller au Dashboard
          </Link>
        </div>
      </main>
    </div>
  );
}
