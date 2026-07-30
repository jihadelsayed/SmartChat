import "dotenv/config";
import { z } from "zod";

export const environmentSchema = z
  .object({
    NODE_ENV: z.enum(["development", "test", "production"]).default("development"),
    PORT: z.coerce.number().int().positive().default(3000),
    DATABASE_URL: z.string().min(1),
    JWT_SECRET: z.string().min(32),
    JWT_EXPIRES_IN: z.string().default("7d"),
    CORS_ORIGIN: z.string().default("*"),
    AI_PROVIDER: z.enum(["mock", "openai", "gemini"]).default("openai"),
    OPENAI_API_KEY: z.string().trim().min(1),
    OPENAI_MODEL: z.string().trim().min(1),
    GEMINI_API_KEY: z.string().min(1).optional(),
    GEMINI_MODEL: z.string().min(1).default("gemini-2.5-flash")
  })
  .superRefine((configuration, context) => {
    if (
      configuration.AI_PROVIDER === "mock" &&
      configuration.NODE_ENV !== "test"
    ) {
      context.addIssue({
        code: "custom",
        path: ["AI_PROVIDER"],
        message: "The mock AI provider is only available during tests"
      });
    }
  });

const result = environmentSchema.safeParse(process.env);

if (!result.success) {
  console.error("Invalid environment configuration", result.error.flatten().fieldErrors);
  throw new Error("Invalid environment configuration");
}

export const environment = result.data;
