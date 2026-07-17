import { NotFoundError } from "../../shared/errors/not-found-error";
import { deleteUploadedFile } from "../../shared/utils/file";
import { attachmentRepository } from "./attachment.repository";
import type { CreateAttachmentInput } from "./attachment.types";

export const attachmentService = {
  async listByMessage(messageId: string, userId: string) {
    const message = await attachmentRepository.findMessageForUser(
      messageId,
      userId
    );

    if (!message) {
      throw new NotFoundError("Message");
    }

    return attachmentRepository.listByMessage(messageId);
  },

  async create(userId: string, input: CreateAttachmentInput) {
    const message = await attachmentRepository.findMessageForUser(
      input.messageId,
      userId
    );

    if (!message) {
      throw new NotFoundError("Message");
    }

    return attachmentRepository.create(input);
  },

  async getById(attachmentId: string, userId: string) {
    const attachment = await attachmentRepository.findByIdForUser(
      attachmentId,
      userId
    );

    if (!attachment) {
      throw new NotFoundError("Attachment");
    }

    return attachment;
  },

  async remove(attachmentId: string, userId: string) {
    const attachment = await attachmentRepository.findByIdForUser(
      attachmentId,
      userId
    );

    if (!attachment) {
      throw new NotFoundError("Attachment");
    }

    await attachmentRepository.deleteById(attachment.id);

    try {
      await deleteUploadedFile(attachment.fileUrl);
    } catch (error: unknown) {
      console.error(
        `Failed to delete attachment file ${attachment.fileUrl}`,
        error
      );
    }

    return attachment;
  }
};