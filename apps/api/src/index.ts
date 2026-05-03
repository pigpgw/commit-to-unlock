import cors from "@fastify/cors";
import Fastify from "fastify";
import { loadConfig } from "./config.js";
import { registerHealthRoutes } from "./routes/health.js";

const config = loadConfig();

const app = Fastify({
  logger: true,
  bodyLimit: 5 * 1024 * 1024
});

await app.register(cors, {
  origin: config.allowedOrigins.length > 0 ? config.allowedOrigins : false
});

app.addContentTypeParser(
  "application/json",
  { parseAs: "buffer" },
  (request, body, done) => {
    const rawBody = Buffer.isBuffer(body) ? body : Buffer.from(body);
    (request as typeof request & { rawBody: Buffer }).rawBody = rawBody;

    try {
      done(null, JSON.parse(rawBody.toString("utf8")) as unknown);
    } catch (error) {
      done(error as Error, undefined);
    }
  }
);

await registerHealthRoutes(app);

await app.listen({ port: config.port, host: config.host });
