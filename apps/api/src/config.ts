export interface AppConfig {
  port: number;
}

export function loadConfig(): AppConfig {
  return {
    port: Number.parseInt(process.env.PORT ?? "4000", 10)
  };
}
