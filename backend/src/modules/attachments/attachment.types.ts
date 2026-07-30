export interface CreateAttachmentInput {
  messageId?: string;
  clientAttachmentId: string;
  fileName: string;
  mimeType: string;
  fileUrl: string;
  sizeBytes: number;
  contentHash: string;
}

export interface StoredAttachmentFile {
  originalName: string;
  storedFileName: string;
  mimeType: string;
  sizeBytes: number;
  fileUrl: string;
}

export interface AttachmentRouteParams {
  attachmentId: string;
}

export interface MessageAttachmentRouteParams {
  messageId: string;
}
