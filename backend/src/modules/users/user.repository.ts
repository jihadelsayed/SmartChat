import { prisma } from "../../database/prisma";

export const userRepository = {
  findById(userId: string) {
    return prisma.user.findUnique({ where: { id: userId } });
  },

  findByEmail(email: string) {
    return prisma.user.findUnique({ where: { email } });
  },

  updateProfile(userId: string, data: { displayName?: string; profileImageUrl?: string | null }) {
    return prisma.user.update({ where: { id: userId }, data });
  }
};
