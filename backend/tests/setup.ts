import "dotenv/config";

const testDatabaseUrl =
  process.env.TEST_DATABASE_URL ??
  process.env.DATABASE_URL;

process.env.NODE_ENV = "test";
process.env.DATABASE_URL =
  testDatabaseUrl ??
  "postgresql://smartchat:smartchat@127.0.0.1:5433/smartchat?schema=public";
process.env.JWT_SECRET =
  "smartchat-test-only-jwt-secret-with-more-than-32-characters";
process.env.JWT_EXPIRES_IN = "15m";
process.env.CORS_ORIGIN = "*";
process.env.AI_PROVIDER = "mock";
process.env.OPENAI_API_KEY = "test-only-openai-api-key";
process.env.OPENAI_MODEL = "test-openai-model";
