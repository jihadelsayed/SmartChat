import { prisma } from "../../database/prisma";

export const authRepository = {
  findByEmail(email: string) {
    return prisma.user.findUnique({ where: { email } });
  },

  createUser(data: { email: string; passwordHash: string; displayName: string }) {
    return prisma.user.create({ data });
  }
};
