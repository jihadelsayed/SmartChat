import { prisma } from "../../database/prisma";
import { MessageSender } from "../../generated/prisma/client";

export const messageRepository = {
  listByConversation(conversationId: string) {
    return prisma.message.findMany({
      where: { conversationId },
      orderBy: { createdAt: "asc" },
      include: { attachments: true }
    });
  },

  createExchange(
    conversationId: string,
    userContent: string,
    assistantContent: string
  ) {
    return prisma.$transaction(async (transaction) => {
      const userMessage = await transaction.message.create({
        data: {
          conversationId,
          sender: MessageSender.USER,
          content: userContent
        },
        include: {
          attachments: true
        }
      });

      const assistantMessage = await transaction.message.create({
        data: {
          conversationId,
          sender: MessageSender.ASSISTANT,
          content: assistantContent
        },
        include: {
          attachments: true
        }
      });

      await transaction.conversation.update({
        where: { id: conversationId },
        data: { updatedAt: new Date() }
      });

      return {
        userMessage,
        assistantMessage
      };
    });
  }
};
