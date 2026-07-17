import "dotenv/config";
import bcrypt from "bcrypt";
import { prisma } from "../src/database/prisma";

async function seed(): Promise<void> {
  const passwordHash = await bcrypt.hash("Password123!", 12);

  await prisma.user.upsert({
    where: { email: "demo@smartchat.local" },
    update: {},
    create: {
      email: "demo@smartchat.local",
      displayName: "Demo User",
      passwordHash
    }
  });
}

seed()
  .then(async () => prisma.$disconnect())
  .catch(async (error: unknown) => {
    console.error(error);
    await prisma.$disconnect();
    process.exit(1);
  });
