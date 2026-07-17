import { app } from "./app";
import { environment } from "./config/environment";
import { prisma } from "./database/prisma";
import {
  runCleanupJob,
  type CleanupJobResult
} from "./jobs/cleanup.job";

const cleanupIntervalMs = 60 * 60 * 1000;

async function startServer(): Promise<void> {
  await prisma.$connect();

  const server = app.listen(environment.PORT, () => {
    console.log(
      `SmartChat backend is running at http://localhost:${environment.PORT}`
    );
  });

  const cleanupTimer = setInterval(() => {
    void runCleanupJob()
      .then((cleanupResult: CleanupJobResult) => {
        console.log("Cleanup job completed", cleanupResult);
      })
      .catch((error: unknown) => {
        console.error("Cleanup job failed", error);
      });
  }, cleanupIntervalMs);

  cleanupTimer.unref();

  async function shutdown(signal: string): Promise<void> {
    console.log(`${signal} received. Shutting down SmartChat backend.`);

    clearInterval(cleanupTimer);

    server.close(async () => {
      await prisma.$disconnect();
      process.exit(0);
    });

    setTimeout(() => {
      console.error("Forced shutdown after timeout");
      process.exit(1);
    }, 10_000).unref();
  }

  process.once("SIGINT", () => {
    void shutdown("SIGINT");
  });

  process.once("SIGTERM", () => {
    void shutdown("SIGTERM");
  });
}

void startServer().catch(async (error: unknown) => {
  console.error("Failed to start SmartChat backend", error);
  await prisma.$disconnect();
  process.exit(1);
});
