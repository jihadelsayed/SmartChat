import { unlink } from "node:fs/promises";
import path from "node:path";

const uploadsDirectory = path.resolve(process.cwd(), "uploads");

export function createUploadUrl(storedFileName: string): string {
  return `/uploads/${encodeURIComponent(storedFileName)}`;
}

export function resolveUploadPath(fileUrl: string): string {
  const storedFileName = decodeURIComponent(
    fileUrl.replace(/^\/uploads\//, "")
  );

  const resolvedPath = path.resolve(uploadsDirectory, storedFileName);
  const relativePath = path.relative(uploadsDirectory, resolvedPath);

  if (
    relativePath.startsWith("..") ||
    path.isAbsolute(relativePath)
  ) {
    throw new Error("Invalid attachment file path");
  }

  return resolvedPath;
}

export async function deleteUploadedFile(fileUrl: string): Promise<void> {
  const filePath = resolveUploadPath(fileUrl);

  try {
    await unlink(filePath);
  } catch (error: unknown) {
    if (
      error instanceof Error &&
      "code" in error &&
      error.code === "ENOENT"
    ) {
      return;
    }

    throw error;
  }
}