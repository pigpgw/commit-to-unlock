export interface AppConfig {
  host: string;
  port: number;
  allowedOrigins: string[];
}

export function loadConfig(): AppConfig {
  return {
    host: process.env.HOST?.trim() || "127.0.0.1",
    port: parsePort(process.env.PORT),
    allowedOrigins: parseAllowedOrigins(process.env.ALLOWED_ORIGINS)
  };
}

function parsePort(value: string | undefined): number {
  const parsed = Number.parseInt(value ?? "4000", 10);
  if (!Number.isInteger(parsed) || parsed < 1 || parsed > 65535) {
    throw new Error(`Invalid PORT value: ${value}`);
  }
  return parsed;
}

function parseAllowedOrigins(value: string | undefined): string[] {
  if (!value) return [];

  return value
    .split(",")
    .map((origin) => origin.trim())
    .filter((origin) => origin.length > 0);
}
