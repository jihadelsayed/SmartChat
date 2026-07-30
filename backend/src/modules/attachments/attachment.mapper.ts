import type { Attachment } from "../../generated/prisma/client";

export interface AttachmentResponse {
  id: string;
  messageId: string | null;
  fileName: string;
  mimeType: string;
  fileUrl: string;
  sizeBytes: number;
  createdAt: string;
}

export function mapAttachmentToResponse(
  attachment: Attachment
): AttachmentResponse {
  return {
    id: attachment.id,
    messageId: attachment.messageId,
    fileName: attachment.fileName,
    mimeType: attachment.mimeType,
    fileUrl: attachment.fileUrl,
    sizeBytes: attachment.sizeBytes,
    createdAt: attachment.createdAt.toISOString()
  };
}

export function mapAttachmentsToResponse(
  attachments: Attachment[]
): AttachmentResponse[] {
  return attachments.map(mapAttachmentToResponse);
}
