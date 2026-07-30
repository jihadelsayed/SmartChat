import { createHash } from "node:crypto";
import { readFile } from "node:fs/promises";
import { AppError } from "../../shared/errors/app-error";

const signatures: Record<string, (bytes: Buffer) => boolean> = {
  "image/jpeg": (bytes) =>
    bytes.length >= 3 &&
    bytes[0] === 0xff &&
    bytes[1] === 0xd8 &&
    bytes[2] === 0xff,
  "image/png": (bytes) =>
    bytes.length >= 8 &&
    bytes.subarray(0, 8).equals(
      Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a])
    ),
  "image/webp": (bytes) =>
    bytes.length >= 12 &&
    bytes.subarray(0, 4).toString("ascii") === "RIFF" &&
    bytes.subarray(8, 12).toString("ascii") === "WEBP"
};

export interface ValidatedAttachmentFile {
  contentHash: string;
}

export async function validateAttachmentFile(
  filePath: string,
  mimeType: string
): Promise<ValidatedAttachmentFile> {
  const bytes = await readFile(filePath);
  const matches = signatures[mimeType]?.(bytes) === true;

  if (!matches) {
    throw new AppError(
      "The uploaded file does not match its declared image type.",
      415,
      "INVALID_ATTACHMENT_CONTENT",
      undefined,
      { retryable: false }
    );
  }

  return {
    contentHash: createHash("sha256").update(bytes).digest("hex")
  };
}
