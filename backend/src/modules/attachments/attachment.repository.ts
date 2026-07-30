import { prisma } from "../../database/prisma";
import { AttachmentStatus } from "../../generated/prisma/client";
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

  create(userId: string, input: CreateAttachmentInput) {
    return prisma.attachment.create({
      data: {
        userId,
        clientAttachmentId: input.clientAttachmentId,
        messageId: input.messageId,
        status: AttachmentStatus.UPLOADED,
        fileName: input.fileName,
        mimeType: input.mimeType,
        fileUrl: input.fileUrl,
        sizeBytes: input.sizeBytes,
        contentHash: input.contentHash
      }
    });
  },

  findByClientAttachmentId(userId: string, clientAttachmentId: string) {
    return prisma.attachment.findUnique({
      where: {
        userId_clientAttachmentId: {
          userId,
          clientAttachmentId
        }
      }
    });
  },

  findByIdForUser(attachmentId: string, userId: string) {
    return prisma.attachment.findFirst({
      where: {
        id: attachmentId,
        userId
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
