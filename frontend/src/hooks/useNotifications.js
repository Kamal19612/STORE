import { useEffect, useRef, useCallback } from "react";
import { toast } from "react-toastify";
import useAuthStore from "../store/authStore";
import { sendBrowserNotification } from "./useBrowserNotifications";

// ─── Audio ────────────────────────────────────────────────────────────────────

let sharedAudioCtx = null;

function getAudioContext() {
  if (!sharedAudioCtx || sharedAudioCtx.state === "closed") {
    const AC = window.AudioContext || window.webkitAudioContext;
    if (!AC) return null;
    sharedAudioCtx = new AC();
  }
  return sharedAudioCtx;
}

function unlockAudio() {
  const ctx = getAudioContext();
  if (ctx && ctx.state === "suspended") ctx.resume();
}
if (typeof window !== "undefined") {
  ["click", "touchstart", "keydown"].forEach((evt) =>
    window.addEventListener(evt, unlockAudio, { once: false, passive: true })
  );
}

async function playNotificationSound(type) {
  try {
    const ctx = getAudioContext();
    if (!ctx) return;
    if (ctx.state === "suspended") await ctx.resume();

    const beep = (freq, startTime, duration, gainValue = 0.4) => {
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.type = "sine";
      osc.frequency.setValueAtTime(freq, startTime);
      gain.gain.setValueAtTime(0, startTime);
      gain.gain.linearRampToValueAtTime(gainValue, startTime + 0.01);
      gain.gain.linearRampToValueAtTime(0, startTime + duration);
      osc.start(startTime);
      osc.stop(startTime + duration + 0.05);
    };

    if (type === "order") {
      beep(880, ctx.currentTime + 0.0, 0.12);
      beep(880, ctx.currentTime + 0.18, 0.12);
      beep(880, ctx.currentTime + 0.36, 0.20);
    } else {
      beep(660, ctx.currentTime + 0.0, 0.15);
      beep(880, ctx.currentTime + 0.22, 0.25, 0.5);
    }
  } catch (_) {}
}

// ─── Hook principal SSE ───────────────────────────────────────────────────────

/**
 * Ouvre UNE SEULE connexion SSE par rôle dans le Layout.
 * Diffuse les événements via window.dispatchEvent(CustomEvent) pour
 * que les pages enfants puissent s'abonner sans créer une 2ème connexion.
 *
 * @param {"admin" | "delivery"} role
 */
export function useNotifications(role) {
  const { token } = useAuthStore();
  const esRef = useRef(null);
  const reconnectTimerRef = useRef(null);

  const connect = useCallback(() => {
    if (!token) return;

    // Fermer une éventuelle connexion précédente avant d'en ouvrir une nouvelle
    if (esRef.current) {
      esRef.current.close();
      esRef.current = null;
    }
    if (reconnectTimerRef.current) {
      clearTimeout(reconnectTimerRef.current);
      reconnectTimerRef.current = null;
    }

    const baseUrl = import.meta.env.VITE_API_URL || "/api";
    const endpoint = role === "delivery" ? "delivery" : "admin";
    const url = `${baseUrl}/notifications/stream/${endpoint}?token=${encodeURIComponent(token)}`;

    const es = new EventSource(url);
    esRef.current = es;

    es.addEventListener("new_order", (e) => {
      try {
        const data = JSON.parse(e.data);
        const typeLabel =
          data.deliveryType === "EXPRESS" ? "⚡ Express" :
          data.deliveryType === "PROGRAMMER" ? "🕐 Programmée" : "";

        playNotificationSound("order");

        toast.info(
          `🛒 Nouvelle commande #${data.orderNumber}\n${data.customerName}${typeLabel ? " · " + typeLabel : ""}`,
          { autoClose: 10000, style: { whiteSpace: "pre-line" } }
        );

        // Notification système uniquement si l'onglet est en arrière-plan
        // (évite le doublon avec le Web Push qui arrive aussi du backend)
        if (document.hidden) {
          sendBrowserNotification("🛒 Nouvelle commande", {
            body: `#${data.orderNumber} — ${data.customerName}${typeLabel ? "\n" + typeLabel : ""}`,
            tag: `order-${data.orderNumber}`,
            requireInteraction: true,
          });
        }

        window.dispatchEvent(new CustomEvent("sse:new_order", { detail: data }));
      } catch (_) {}
    });

    es.addEventListener("new_delivery", (e) => {
      try {
        const data = JSON.parse(e.data);
        const typeLabel =
          data.deliveryType === "EXPRESS" ? "⚡ Express" :
          data.deliveryType === "PROGRAMMER" ? "🕐 Programmée" : "";

        playNotificationSound("delivery");

        toast.success(
          `🚚 Nouvelle livraison disponible !\n#${data.orderNumber}${typeLabel ? " · " + typeLabel : ""}`,
          { autoClose: 12000, style: { whiteSpace: "pre-line" } }
        );

        // Notification système uniquement si l'onglet est en arrière-plan
        if (document.hidden) {
          sendBrowserNotification("🚚 Nouvelle livraison disponible", {
            body: `Commande #${data.orderNumber}${typeLabel ? " · " + typeLabel : ""}`,
            tag: `delivery-${data.orderNumber}`,
            requireInteraction: true,
          });
        }

        window.dispatchEvent(new CustomEvent("sse:new_delivery", { detail: data }));
      } catch (_) {}
    });

    es.addEventListener("connected", () => {
      console.info(`[SSE] Connecté au flux ${role}`);
    });

    es.onerror = () => {
      es.close();
      esRef.current = null;
      // Reconnexion automatique après 5s
      reconnectTimerRef.current = setTimeout(connect, 5000);
    };
  }, [token, role]);

  useEffect(() => {
    connect();
    return () => {
      esRef.current?.close();
      esRef.current = null;
      if (reconnectTimerRef.current) clearTimeout(reconnectTimerRef.current);
    };
  }, [connect]);
}

// ─── Hook secondaire pour les pages enfants ───────────────────────────────────

/**
 * Permet à une page enfant de s'abonner aux événements SSE
 * sans créer de nouvelle connexion.
 *
 * @param {"new_order" | "new_delivery"} eventType
 * @param {Function} callback  - appelé avec `detail` de l'événement
 */
export function useSseEvent(eventType, callback) {
  const stableCallback = useRef(callback);
  useEffect(() => { stableCallback.current = callback; }, [callback]);

  useEffect(() => {
    const handler = (e) => stableCallback.current(e.detail);
    window.addEventListener(`sse:${eventType}`, handler);
    return () => window.removeEventListener(`sse:${eventType}`, handler);
  }, [eventType]);
}
