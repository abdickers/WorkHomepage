import type { Metadata } from "next";
import "./globals.css";
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
      <body>{children}</body>
    </html>
  );
}
