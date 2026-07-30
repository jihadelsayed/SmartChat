import { readdir, stat, unlink } from "node:fs/promises";
import path from "node:path";
import { prisma } from "../database/prisma";

const uploadsDirectory = path.resolve(process.cwd(), "uploads");
const cleanupGracePeriodMs = 7 * 24 * 60 * 60 * 1000;

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
      id: true,
      messageId: true,
      createdAt: true,
      fileUrl: true
    }
  });
  const cleanupCutoff = Date.now() - cleanupGracePeriodMs;
  const abandoned = attachments.filter(
    (attachment) =>
      attachment.messageId === null &&
      attachment.createdAt.getTime() <= cleanupCutoff
  );
  for (const attachment of abandoned) {
    await prisma.attachment.delete({ where: { id: attachment.id } });
    try {
      await unlink(resolveAttachmentPath(attachment.fileUrl));
    } catch (error: unknown) {
      if (!isMissingFileError(error)) throw error;
    }
  }
  const abandonedIds = new Set(abandoned.map(({ id }) => id));

  const referencedFileNames = new Set(
    attachments
      .filter((attachment) => !abandonedIds.has(attachment.id))
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

function resolveAttachmentPath(fileUrl: string): string {
  const storedFileName = extractStoredFileName(fileUrl);
  if (!storedFileName) {
    throw new Error("Invalid attachment file path");
  }
  const resolved = path.resolve(uploadsDirectory, storedFileName);
  const relative = path.relative(uploadsDirectory, resolved);
  if (relative.startsWith("..") || path.isAbsolute(relative)) {
    throw new Error("Invalid attachment file path");
  }
  return resolved;
}
