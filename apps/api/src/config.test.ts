import { afterEach, describe, expect, it } from "vitest";
import { loadConfig } from "./config.js";

const originalEnv = { ...process.env };

afterEach(() => {
  process.env = { ...originalEnv };
});

describe("loadConfig", () => {
  it("defaults to a localhost-only API with no browser CORS origins", () => {
    delete process.env.HOST;
    delete process.env.PORT;
    delete process.env.ALLOWED_ORIGINS;

    expect(loadConfig()).toEqual({
      host: "127.0.0.1",
      port: 4000,
      allowedOrigins: []
    });
  });

  it("parses explicit host, port, and comma-separated CORS origins", () => {
    process.env.HOST = "0.0.0.0";
    process.env.PORT = "4100";
    process.env.ALLOWED_ORIGINS = "https://app.example.com, http://localhost:3000";

    expect(loadConfig()).toEqual({
      host: "0.0.0.0",
      port: 4100,
      allowedOrigins: ["https://app.example.com", "http://localhost:3000"]
    });
  });

  it("rejects invalid ports", () => {
    process.env.PORT = "99999";

    expect(() => loadConfig()).toThrow("Invalid PORT value");
  });
});
