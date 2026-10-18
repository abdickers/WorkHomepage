"use client";
import { useSyncExternalStore } from "react";
import { cleanConnectionPreferences, CONNECTION_PREFERENCES_KEY, type ConnectionId, type ConnectionPreferences } from "@/lib/connection-registry.mjs";
const CHANGE = "command-connections-change";
export function readConnectionPreferences(): ConnectionPreferences {
  try { return cleanConnectionPreferences(JSON.parse(localStorage.getItem(CONNECTION_PREFERENCES_KEY) || "{}")); }
  catch { return {}; }
}
export function connectionEnabled(id: string) { return readConnectionPreferences()[id as ConnectionId] !== false; }
export function saveConnectionPreferences(preferences: unknown) {
  localStorage.setItem(CONNECTION_PREFERENCES_KEY, JSON.stringify(cleanConnectionPreferences(preferences)));
  window.dispatchEvent(new Event(CHANGE));
}
export function setConnectionEnabled(id: ConnectionId, enabled: boolean) {
  saveConnectionPreferences({ ...readConnectionPreferences(), [id]: enabled });
}
function subscribe(onChange: () => void) {
  window.addEventListener(CHANGE, onChange);
  window.addEventListener("storage", onChange);
  return () => { window.removeEventListener(CHANGE, onChange); window.removeEventListener("storage", onChange); };
}
export function useConnectionEnabled(id: ConnectionId) {
  return useSyncExternalStore(subscribe, () => connectionEnabled(id), () => true);
}
