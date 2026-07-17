declare namespace NodeJS {
  interface ProcessEnv {
    NODE_ENV?: "development" | "test" | "production";
    PORT?: string;
    DATABASE_URL?: string;
    JWT_SECRET?: string;
    JWT_EXPIRES_IN?: string;
    CORS_ORIGIN?: string;
    AI_PROVIDER?: "mock" | "openai";
    OPENAI_API_KEY?: string;
    OPENAI_MODEL?: string;
  }
}

export {};
