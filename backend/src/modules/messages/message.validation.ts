import { z } from "zod";
import { MAX_MESSAGE_LENGTH } from "../../config/constants";
import { MAXIMUM_ATTACHMENTS_PER_MESSAGE } from "../../middleware/upload.middleware";

export const createMessageSchema = z.object({
  body: z.object({
    content: z.string().trim().max(MAX_MESSAGE_LENGTH).default(""),
    clientRequestId: z.string().uuid().optional(),
    attachmentIds: z.array(z.string().uuid())
      .max(MAXIMUM_ATTACHMENTS_PER_MESSAGE)
      .default([])
  }).refine(
    (body) => body.content.length > 0 || body.attachmentIds.length > 0,
    { message: "A message requires text or at least one attachment" }
  ),
  params: z.object({ conversationId: z.string().uuid() }),
  query: z.object({}),
  headers: z.object({
    "idempotency-key": z.string().uuid().optional()
  }).passthrough()
});
