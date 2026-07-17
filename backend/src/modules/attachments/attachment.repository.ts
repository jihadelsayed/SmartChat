import { prisma } from "../../database/prisma";
import type { CreateAttachmentInput } from "./attachment.types";

export const attachmentRepository = {
  findMessageForUser(messageId: string, userId: string) {
    return prisma.message.findFirst({
      where: {
        id: messageId,
        conversation: {
          userId
        }
      },
      select: {
        id: true,
        conversationId: true
      }
    });
  },

  listByMessage(messageId: string) {
    return prisma.attachment.findMany({
      where: {
        messageId
      },
      orderBy: {
        createdAt: "asc"
      }
    });
  },

  create(input: CreateAttachmentInput) {
    return prisma.attachment.create({
      data: {
        messageId: input.messageId,
        fileName: input.fileName,
        mimeType: input.mimeType,
        fileUrl: input.fileUrl,
        sizeBytes: input.sizeBytes
      }
    });
  },

  findByIdForUser(attachmentId: string, userId: string) {
    return prisma.attachment.findFirst({
      where: {
        id: attachmentId,
        message: {
          conversation: {
            userId
          }
        }
      }
    });
  },

  deleteById(attachmentId: string) {
    return prisma.attachment.delete({
      where: {
        id: attachmentId
      }
    });
  }
};