import type { Request, Response } from "express";
import { AppError } from "../../shared/errors/app-error";
import { successResponse } from "../../shared/responses/api-response";
import {
  createUploadUrl,
  deleteUploadedFile
} from "../../shared/utils/file";
import { validateAttachmentFile } from "../attachments/attachment-file";
import { userService } from "./user.service";

function authenticatedUserId(request: Request): string {
  return request.authenticatedUser!.id;
}

export const userController = {
  async getProfile(request: Request, response: Response) {
    const user = await userService.getProfile(authenticatedUserId(request));
    response.json(successResponse(user));
  },

  async updateProfile(request: Request, response: Response) {
    const user = await userService.updateProfile(
      authenticatedUserId(request),
      request.body
    );

    response.json(successResponse(user));
  },

  async uploadProfileImage(request: Request, response: Response) {
    if (!request.file) {
      throw new AppError(
        "A profile image is required",
        400,
        "PROFILE_IMAGE_REQUIRED"
      );
    }

    const fileUrl = createUploadUrl(request.file.filename);

    try {
      await validateAttachmentFile(
        request.file.path,
        request.file.mimetype
      );

      const user = await userService.updateProfile(
        authenticatedUserId(request),
        {
          profileImageUrl: fileUrl
        }
      );

      response.status(200).json(successResponse(user));
    } catch (error: unknown) {
      try {
        await deleteUploadedFile(fileUrl);
      } catch (cleanupError: unknown) {
        console.error(
          "Failed to clean up rejected profile image",
          cleanupError
        );
      }

      throw error;
    }
  }
};