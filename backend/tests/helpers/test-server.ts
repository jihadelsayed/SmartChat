import assert from "node:assert/strict";
import type { AddressInfo } from "node:net";
import { once } from "node:events";
import "../setup";

export interface ApiEnvelope<T> {
  success: boolean;
  data?: T;
  error?: {
    code: string;
    message: string;
    details?: unknown;
    retryable?: boolean;
    requestId?: string;
  };
}

export interface TestServer {
  baseUrl: string;
  stop: () => Promise<void>;
}

export async function readApiEnvelope<T>(
  response: Response
): Promise<ApiEnvelope<T>> {
  return (await response.json()) as ApiEnvelope<T>;
}

export async function startTestServer(): Promise<TestServer> {
  const [{ app }, { prisma }] = await Promise.all([
    import("../../src/app"),
    import("../../src/database/prisma")
  ]);

  const server = app.listen(0, "127.0.0.1");
  await once(server, "listening");

  const address = server.address();
  assert.ok(address && typeof address !== "string");

  return {
    baseUrl: `http://127.0.0.1:${(address as AddressInfo).port}`,
    async stop(): Promise<void> {
      await new Promise<void>((resolve, reject) => {
        server.close((error) => {
          if (error) {
            reject(error);
            return;
          }

          resolve();
        });
      });

      await prisma.$disconnect();
    }
  };
}
