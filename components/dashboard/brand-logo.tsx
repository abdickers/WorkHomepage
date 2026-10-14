const files: Record<string, string> = {
  Dotdigital: "dotdigital.png",
  Avalara: "avalara.svg",
  Cybersource: "cybersource.svg",
  Netlify: "netlify.svg",
  NetSuite: "netsuite.svg",
  Celigo: "celigo.svg",
  Gmail: "gmail.png",
  Calendar: "calendar.png",
  "Google Calendar": "calendar.png",
  GitHub: "github.svg",
  Slack: "slack.svg",
  Drive: "drive.png",
  ChatGPT: "openai.svg",
  BigCommerce: "bigcommerce.svg",
  Jira: "jira.svg",
  Confluence: "confluence.svg",
  Vercel: "vercel.svg",
  Zoom: "zoom.svg",
  Cloudflare: "cloudflare.svg",
  Claude: "claude.svg",
  Gemini: "gemini.svg",
};
export function BrandLogo({
  name,
  className = "",
}: {
  name: string;
  className?: string;
}) {
  return (
    <img
      className={`brand-logo ${className} ${["GitHub", "ChatGPT", "Vercel", "BigCommerce", "NetSuite"].includes(name) ? "mono-logo" : ""}`}
      src={`/logos/${files[name] || "chatgpt.svg"}`}
      alt={`${name} logo`}
      width={32}
      height={32}
    />
  );
}
