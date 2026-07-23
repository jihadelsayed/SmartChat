declare namespace NodeJS {
  interface ProcessEnv {
    NODE_ENV?: "development" | "test" | "production";
    PORT?: string;
    DATABASE_URL?: string;
    JWT_SECRET?: string;
    JWT_EXPIRES_IN?: string;
    CORS_ORIGIN?: string;
    AI_PROVIDER?: "mock" | "openai" | "gemini";
    OPENAI_API_KEY?: string;
    OPENAI_MODEL?: string;
    GEMINI_API_KEY?: string;
    GEMINI_MODEL?: string;
  }
}

export {};
