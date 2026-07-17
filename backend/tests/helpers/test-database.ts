import "../setup";

export async function cleanupTestUsers(emails: string[]): Promise<void> {
  if (emails.length === 0) {
    return;
  }

  const { prisma } = await import("../../src/database/prisma");

  await prisma.user.deleteMany({
    where: {
      email: {
        in: emails
      }
    }
  });
}
