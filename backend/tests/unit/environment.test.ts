import assert from "node:assert/strict";
import { test } from "node:test";
import "../setup";
import { environmentSchema } from "../../src/config/environment";

const validEnvironment = {
  NODE_ENV: "production",
  DATABASE_URL: "postgresql://database.example/smartchat",
  JWT_SECRET: "test-only-jwt-secret-with-more-than-32-characters",
  AI_PROVIDER: "openai",
  OPENAI_API_KEY: "test-only-openai-api-key",
  OPENAI_MODEL: "test-openai-model"
};

test("startup configuration requires the OpenAI API key", () => {
  const { OPENAI_API_KEY: _apiKey, ...withoutApiKey } =
    validEnvironment;

  assert.equal(
    environmentSchema.safeParse(withoutApiKey).success,
    false
  );
});

test("startup configuration requires the OpenAI model", () => {
  const { OPENAI_MODEL: _model, ...withoutModel } =
    validEnvironment;

  assert.equal(
    environmentSchema.safeParse(withoutModel).success,
    false
  );
});

test("startup configuration only permits the mock provider in tests", () => {
  assert.equal(
    environmentSchema.safeParse({
      ...validEnvironment,
      AI_PROVIDER: "mock"
    }).success,
    false
  );

  assert.equal(
    environmentSchema.safeParse({
      ...validEnvironment,
      NODE_ENV: "test",
      AI_PROVIDER: "mock"
    }).success,
    true
  );
});
