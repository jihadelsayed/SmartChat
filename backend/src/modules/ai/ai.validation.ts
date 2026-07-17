import { z } from "zod";
import { MAX_MESSAGE_LENGTH } from "../../config/constants";

export const standaloneChatSchema = z.object({
  body: z.object({ message: z.string().trim().min(1).max(MAX_MESSAGE_LENGTH) }),
  params: z.object({}),
  query: z.object({})
});
