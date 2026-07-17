import { z } from "zod";

export const updateProfileSchema = z.object({
  body: z.object({
    displayName: z.string().trim().min(2).max(80).optional(),
    profileImageUrl: z.string().url().nullable().optional()
  }).refine((value) => Object.keys(value).length > 0, "At least one field is required"),
  params: z.object({}),
  query: z.object({})
});
