import type { Request, Response } from "express";
import { AppError } from "../../shared/errors/app-error";
import { successResponse } from "../../shared/responses/api-response";
import {
  createUploadUrl,
  deleteUploadedFile
} from "../../shared/utils/file";
import {
  mapAttachmentToResponse,
  mapAttachmentsToResponse
} from "./attachment.mapper";
import { attachmentService } from "./attachment.service";

export const attachmentController = {
  async listByMessage(request: Request, response: Response) {
    const attachments = await attachmentService.listByMessage(
      String(request.params.messageId),
      request.authenticatedUser!.id
    );

    response
      .status(200)
      .json(successResponse(mapAttachmentsToResponse(attachments)));
  },

  async upload(request: Request, response: Response) {
    if (!request.file) {
      throw new AppError(
        "An attachment file is required",
        400,
        "ATTACHMENT_FILE_REQUIRED"
      );
    }

    const fileUrl = createUploadUrl(request.file.filename);

    try {
      const attachment = await attachmentService.create(
        request.authenticatedUser!.id,
        {
          messageId: String(request.params.messageId),
          fileName: request.file.originalname,
          mimeType: request.file.mimetype,
          fileUrl,
          sizeBytes: request.file.size
        }
      );

      response
        .status(201)
        .json(successResponse(mapAttachmentToResponse(attachment)));
    } catch (error: unknown) {
      try {
        await deleteUploadedFile(fileUrl);
      } catch (cleanupError: unknown) {
        console.error(
          `Failed to clean up uploaded file ${fileUrl}`,
          cleanupError
        );
      }

      throw error;
    }
  },

  async getById(request: Request, response: Response) {
    const attachment = await attachmentService.getById(
      String(request.params.attachmentId),
      request.authenticatedUser!.id
    );

    response
      .status(200)
      .json(successResponse(mapAttachmentToResponse(attachment)));
  },

  async remove(request: Request, response: Response) {
    const attachment = await attachmentService.remove(
      String(request.params.attachmentId),
      request.authenticatedUser!.id
    );

    response
      .status(200)
      .json(successResponse(mapAttachmentToResponse(attachment)));
  }
};