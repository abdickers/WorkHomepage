import type { SVGProps } from "react";

// Supplied 24px interface artwork, inlined so it inherits each control's ink.
const shapes = {
  "arrow-right": <><path d="M3 12h18m-7-7 7 7-7 7"/></>,
  "beacon": <><path d="M6 21h12l-1-10H7L6 21Zm1-10V8h3v3m4 0V8h3m-6-5c-4 4 4 5 2 1 3 3 0 5-1 4"/></>,
  "calendar": <><rect x="3" y="5" width="18" height="16" rx="2"/><path d="M7 3v5m10-5v5M3 11h18m-14 4h3m4 0h3m-10 3h3"/></>,
  "check": <><path d="m4 12 5 5L20 6"/></>,
  "chevron-down": <><path d="m5 9 7 7 7-7"/></>,
  "chevron-right": <><path d="m9 5 7 7-7 7"/></>,
  "close": <><path d="m6 6 12 12M6 18 18 6"/></>,
  "coins": <><ellipse cx="9" cy="7" rx="6" ry="3"/><path d="M3 7v5c0 4 12 4 12 0V7M3 12v5c0 4 12 4 12 0v-5m3-3c5 0 5 6 0 6m0-1c5 0 5 6 0 6"/></>,
  "council": <><circle cx="12" cy="10" r="7"/><path d="m12 6 1 3 3 1-3 1-1 3-1-3-3-1 3-1 1-3ZM7 17l-2 4h14l-2-4"/></>,
  "external": <><path d="M13 4h7v7m0-7L10 14m0-9H4v15h15v-6"/></>,
  "focus": <><path d="M7 3h10M7 21h10M8 3v4l8 10v4M16 3v4L8 17v4m2-3h4"/></>,
  "important": <><path d="m12 3 3 6 6 3-6 3-3 6-3-6-6-3 6-3 3-6Z"/></>,
  "mail": <><rect x="3" y="5" width="18" height="14" rx="2"/><path d="m3 6 9 7 9-7"/></>,
  "notes": <><path d="M11 4H5v16h14v-7M9 15l1-5 9-8 3 3-9 9-4 1Z"/></>,
  "overview": <><rect x="4" y="4" width="6" height="6" rx="1"/><rect x="14" y="4" width="6" height="6" rx="1"/><rect x="4" y="14" width="6" height="6" rx="1"/><path d="M17 13v8m-4-4h8"/></>,
  "pause": <><path d="M8 4v16M16 4v16"/></>,
  "play": <><path d="m8 4 12 8-12 8V4Z"/></>,
  "plus": <><path d="M12 4v16M4 12h16"/></>,
  "quests": <><path d="m4 6 2 2 3-4m3 2h8M4 13l2 2 3-4m3 2h8M4 20l2 2 3-4m3 2h8"/></>,
  "raven": <><path d="m3 17 6-8 5 2 3-5 4 3-3 2-2 7H8l-3 3m7-3v3m-5 0h9M9 9l-3 7 8-2"/></>,
  "refresh": <><path d="M20 9a8 8 0 0 0-14-4L3 8m0-5v5h5M4 15a8 8 0 0 0 14 4l3-3m0 5v-5h-5"/></>,
  "search": <><circle cx="10" cy="10" r="6"/><path d="m15 15 6 6"/></>,
  "settings": <><path d="m9 3-1 3-3 1-2 4 2 2v4l4 3 3-1 3 1 4-3v-4l2-2-2-4-3-1-1-3H9Z"/><circle cx="12" cy="12" r="3"/></>,
  "shield": <><path d="m12 3 8 3v6c0 5-8 9-8 9S4 17 4 12V6l8-3Z"/></>,
  "waiting": <><circle cx="12" cy="12" r="9"/><path d="M12 6v6l4 2"/></>,
  "warning": <><path d="m12 3 10 18H2L12 3Zm0 6v5m0 3v1"/></>,
} as const;
type IconProps = Omit<SVGProps<SVGSVGElement>, "name"> & { size?: number };
export function RealmIcon({ name, size = 20, ...props }: IconProps & { name: keyof typeof shapes }) {
  return <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" focusable="false" pointerEvents="none" {...props}>{shapes[name]}</svg>;
}
const icon = (name: keyof typeof shapes) => function ControlIcon(props: IconProps) { return <RealmIcon name={name} {...props} />; };
export const LayoutDashboard = icon("overview");
export const ListChecks = icon("quests");
export const NotebookPen = icon("notes");
export const RadioTower = icon("beacon");
export const Settings = icon("settings");
export const Search = icon("search");
export const ArrowUpRight = icon("external");
export const Plus = icon("plus");
export const ArrowRight = icon("arrow-right");
export const Clock3 = icon("waiting");
export const CalendarDays = icon("calendar");
export const ChevronRight = icon("chevron-right");
export const Check = icon("check");
export const X = icon("close");
export const Flame = icon("important");
export const Play = icon("play");
export const Pause = icon("pause");
export const RefreshCw = icon("refresh");
export const Shield = icon("shield");
export const Bird = icon("raven");
export const Swords = icon("quests");
export const Coins = icon("coins");
export const Mail = icon("mail");
export const ShieldAlert = icon("warning");
