import { z } from "zod";

const optionalBodySchema = z.object({}).passthrough().optional();

const emptyQuerySchema = z.object({}).passthrough();

export const messageAttachmentSchema = z.object({
  body: optionalBodySchema,
  params: z.object({
    messageId: z.string().uuid()
  }),
  query: emptyQuerySchema
});

export const attachmentIdSchema = z.object({
  body: optionalBodySchema,
  params: z.object({
    attachmentId: z.string().uuid()
  }),
  query: emptyQuerySchema
});