import { z } from "zod";

const password = z.string()
  .min(8)
  .max(128)
  .regex(/[A-Z]/, "Password must contain an uppercase letter")
  .regex(/[a-z]/, "Password must contain a lowercase letter")
  .regex(/[0-9]/, "Password must contain a number");

export const registerSchema = z.object({
  body: z.object({
    email: z.string().trim().toLowerCase().email(),
    password,
    displayName: z.string().trim().min(2).max(80)
  }),
  params: z.object({}),
  query: z.object({})
});

export const loginSchema = z.object({
  body: z.object({
    email: z.string().trim().toLowerCase().email(),
    password: z.string().min(1)
  }),
  params: z.object({}),
  query: z.object({})
});
