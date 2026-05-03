export interface AppConfig {
  port: number;
  githubWebhookSecret: string;
}

export function loadConfig(): AppConfig {
  return {
    port: Number.parseInt(process.env.PORT ?? "4000", 10),
    githubWebhookSecret: process.env.GITHUB_WEBHOOK_SECRET ?? ""
  };
}
