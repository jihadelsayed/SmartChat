import { prisma } from "../../database/prisma";

export const conversationRepository = {
  listForUser(userId: string) {
    return prisma.conversation.findMany({
      where: { userId },
      orderBy: { updatedAt: "desc" },
      include: { _count: { select: { messages: true } } }
    });
  },

  findOwned(conversationId: string, userId: string) {
    return prisma.conversation.findFirst({
      where: { id: conversationId, userId },
      include: {
        messages: {
          orderBy: { createdAt: "asc" },
          include: { attachments: true }
        }
      }
    });
  },

  create(userId: string, title: string) {
    return prisma.conversation.create({ data: { userId, title } });
  },

  update(conversationId: string, userId: string, title: string) {
    return prisma.conversation.updateMany({
      where: { id: conversationId, userId },
      data: { title }
    });
  },

  delete(conversationId: string, userId: string) {
    return prisma.conversation.deleteMany({ where: { id: conversationId, userId } });
  }
};
