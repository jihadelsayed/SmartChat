import { randomUUID } from "node:crypto";
import { mkdirSync } from "node:fs";
import path from "node:path";
import multer from "multer";
import { AppError } from "../shared/errors/app-error";

const uploadDirectory = path.resolve(process.cwd(), "uploads");

mkdirSync(uploadDirectory, {
  recursive: true
});

const storage = multer.diskStorage({
  destination: (_request, _file, callback) => {
    callback(null, uploadDirectory);
  },

  filename: (_request, file, callback) => {
    const originalExtension = path
      .extname(file.originalname)
      .toLowerCase();

    const storedFileName = `${randomUUID()}${originalExtension}`;

    callback(null, storedFileName);
  }
});

export const MAXIMUM_ATTACHMENT_SIZE_BYTES = 10 * 1024 * 1024;

export const ALLOWED_ATTACHMENT_MIME_TYPES = new Set([
  "image/jpeg",
  "image/png",
  "image/webp",
  "image/gif"
]);

const attachmentUpload = multer({
  storage,

  limits: {
    files: 1,
    fileSize: MAXIMUM_ATTACHMENT_SIZE_BYTES
  },

  fileFilter: (_request, file, callback) => {
    if (!ALLOWED_ATTACHMENT_MIME_TYPES.has(file.mimetype)) {
      callback(
        new AppError(
          "Unsupported attachment type. Use JPEG, PNG, WebP, or GIF",
          415,
          "UNSUPPORTED_ATTACHMENT_TYPE"
        )
      );
      return;
    }

    callback(null, true);
  }
});

export const uploadAttachment = attachmentUpload.single("file");
