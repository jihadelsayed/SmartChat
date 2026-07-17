import { z } from "zod";

const emptyParamsSchema = z.object({});

const emptyQuerySchema = z.object({}).passthrough();

const conversationParamsSchema = z.object({
  conversationId: z.string().uuid()
});

export const createConversationSchema = z.object({
  body: z.object({
    title: z.string().trim().min(1).max(120).optional()
  }),
  params: emptyParamsSchema,
  query: emptyQuerySchema
});

export const conversationIdSchema = z.object({
  body: z.object({}).passthrough().optional(),
  params: conversationParamsSchema,
  query: emptyQuerySchema
});

export const updateConversationSchema = z.object({
  body: z.object({
    title: z.string().trim().min(1).max(120)
  }),
  params: conversationParamsSchema,
  query: emptyQuerySchema
});