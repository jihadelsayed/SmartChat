import { readdir, stat, unlink } from "node:fs/promises";
import path from "node:path";
import { prisma } from "../database/prisma";

const uploadsDirectory = path.resolve(process.cwd(), "uploads");
const cleanupGracePeriodMs = 60 * 60 * 1000;

export interface AttachmentCleanupResult {
  scannedFiles: number;
  deletedFiles: number;
}

function extractStoredFileName(fileUrl: string): string | null {
  if (!fileUrl.startsWith("/uploads/")) {
    return null;
  }

  try {
    return decodeURIComponent(fileUrl.slice("/uploads/".length));
  } catch {
    return null;
  }
}

function isMissingFileError(error: unknown): boolean {
  return error instanceof Error && "code" in error && error.code === "ENOENT";
}

export async function runAttachmentCleanup(): Promise<AttachmentCleanupResult> {
  const attachments = await prisma.attachment.findMany({
    select: {
      fileUrl: true
    }
  });

  const referencedFileNames = new Set(
    attachments
      .map((attachment) => extractStoredFileName(attachment.fileUrl))
      .filter((fileName): fileName is string => fileName !== null)
  );

  let storedFileNames: string[];

  try {
    storedFileNames = await readdir(uploadsDirectory);
  } catch (error: unknown) {
    if (isMissingFileError(error)) {
      return {
        scannedFiles: 0,
        deletedFiles: 0
      };
    }

    throw error;
  }

  let deletedFiles = 0;
  const cleanupCutoff = Date.now() - cleanupGracePeriodMs;

  for (const storedFileName of storedFileNames) {
    if (
      storedFileName === ".gitkeep" ||
      referencedFileNames.has(storedFileName)
    ) {
      continue;
    }

    const filePath = path.join(uploadsDirectory, storedFileName);

    try {
      const fileStats = await stat(filePath);

      if (!fileStats.isFile() || fileStats.mtimeMs > cleanupCutoff) {
        continue;
      }

      await unlink(filePath);
      deletedFiles += 1;
    } catch (error: unknown) {
      if (!isMissingFileError(error)) {
        throw error;
      }
    }
  }

  return {
    scannedFiles: storedFileNames.length,
    deletedFiles
  };
}
