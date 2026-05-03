import { createHmac, timingSafeEqual } from "node:crypto";

export function verifyGitHubSignature(options: {
  payload: Buffer;
  signatureHeader: string | undefined;
  secret: string;
}): boolean {
  if (!options.secret || !options.signatureHeader?.startsWith("sha256=")) {
    return false;
  }

  const expected = `sha256=${createHmac("sha256", options.secret)
    .update(options.payload)
    .digest("hex")}`;

  const expectedBuffer = Buffer.from(expected);
  const actualBuffer = Buffer.from(options.signatureHeader);

  if (expectedBuffer.length !== actualBuffer.length) {
    return false;
  }

  return timingSafeEqual(expectedBuffer, actualBuffer);
}

export function isBotLogin(login: string | undefined): boolean {
  if (!login) return false;
  return login.endsWith("[bot]") || login.includes("bot");
}
