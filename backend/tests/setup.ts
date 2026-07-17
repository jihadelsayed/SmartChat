process.env.NODE_ENV = "test";
process.env.DATABASE_URL =
  "postgresql://smartchat:smartchat@127.0.0.1:5433/smartchat?schema=public";
process.env.JWT_SECRET =
  "smartchat-test-only-jwt-secret-with-more-than-32-characters";
process.env.JWT_EXPIRES_IN = "15m";
process.env.CORS_ORIGIN = "*";
process.env.AI_PROVIDER = "mock";
