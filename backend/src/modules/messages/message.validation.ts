import { z } from "zod";
import { MAX_MESSAGE_LENGTH } from "../../config/constants";

export const createMessageSchema = z.object({
  body: z.object({
    content: z.string().trim().min(1).max(MAX_MESSAGE_LENGTH)
  }),
  params: z.object({ conversationId: z.string().uuid() }),
  query: z.object({})
});
