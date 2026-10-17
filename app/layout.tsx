import type { Metadata } from "next";
import "./globals.css";
import "./realm-presentation.css";
import "./study-presentation.css";
export const metadata: Metadata = {
  title: "The Command Center · Adam",
  description:
    "Your daily council for work, priorities, and everything in between.",
  icons: { icon: "/favicon.svg" },
};
export default function RootLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en">
      <head>
        {/* The dashboard owns both themes; extension recoloring can replace its artwork. */}
        <meta name="darkreader-lock" />
      </head>
      <body>{children}</body>
    </html>
  );
}
