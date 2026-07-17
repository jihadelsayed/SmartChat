import {
  runAttachmentCleanup,
  type AttachmentCleanupResult
} from "./attachment-cleanup.job";

export interface CleanupJobResult {
  attachmentCleanup: AttachmentCleanupResult;
}

export async function runCleanupJob(): Promise<CleanupJobResult> {
  const attachmentCleanup = await runAttachmentCleanup();

  return {
    attachmentCleanup
  };
}
