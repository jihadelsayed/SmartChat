import assert from "node:assert/strict";
import { test } from "node:test";
import OpenAI, {
  APIConnectionError,
  APIConnectionTimeoutError,
  APIError
} from "openai";
import {
  AiConfigurationError,
  AiEmptyResponseError,
  AiImageInputUnsupportedError,
  AiProviderError,
  AiProviderAuthenticationError,
  AiProviderRequestFailedError,
  AiProviderUnavailableError,
  AiQuotaExceededError,
  AiRateLimitedError,
  AiRequestTimeoutError
} from "../../src/modules/ai/ai.errors";
import type { AiProviderRequest } from "../../src/modules/ai/ai.types";
import { OpenAiProvider } from "../../src/modules/ai/providers/openai.provider";

type OpenAiClient = Pick<OpenAI, "responses">;

const providerInput: AiProviderRequest = {
  message: "Current question",
  messages: [
    { role: "user", content: "Earlier question" },
    { role: "assistant", content: "Earlier answer" },
    { role: "user", content: "Current question" }
  ]
};

function mockClient(
  create: (request: unknown) => Promise<unknown>
): OpenAiClient {
  return {
    responses: {
      create
    }
  } as unknown as OpenAiClient;
}

function openAiApiError(
  status: number,
  code: string,
  headers: Record<string, string> = {}
): APIError {
  return APIError.generate(
    status,
    {
      error: {
        code,
        type: "provider_error",
        message: `Provider failure for ${code}`
      }
    },
    undefined,
    new Headers({
      "x-request-id": "openai-request-id",
      ...headers
    })
  );
}

test("OpenAI provider sends conversation context through the Responses API", async () => {
  let capturedRequest: unknown;
  const provider = new OpenAiProvider(
    "test-api-key",
    "test-model",
    mockClient(async (request) => {
      capturedRequest = request;
      return { output_text: "  Context-aware reply  " };
    })
  );

  const result = await provider.generateReply(providerInput);

  assert.equal(result, "Context-aware reply");
  assert.deepEqual(capturedRequest, {
    model: "test-model",
    instructions:
      "You are SmartChat, a concise and helpful AI assistant.",
    input: providerInput.messages
  });
});

test("OpenAI provider sends private image bytes as image input", async () => {
  let capturedRequest: any;
  const provider = new OpenAiProvider(
    "test-api-key",
    "test-model",
    mockClient(async (request) => {
      capturedRequest = request;
      return { output_text: "Image reply" };
    })
  );

  await provider.generateReply({
    ...providerInput,
    images: [{
      mimeType: "image/png",
      dataBase64: "iVBORw0KGgo="
    }]
  });

  const currentMessage = capturedRequest.input.at(-1);
  assert.equal(currentMessage.content[0].type, "input_text");
  assert.equal(currentMessage.content[1].type, "input_image");
  assert.match(currentMessage.content[1].image_url, /^data:image\/png;base64,/);
});

test("image rejection returns a sanitized non-retryable capability error", async () => {
  const provider = new OpenAiProvider(
    "test-api-key",
    "text-only-model",
    mockClient(async () => {
      throw openAiApiError(400, "invalid_request_error");
    })
  );

  await assert.rejects(
    provider.generateReply({
      ...providerInput,
      images: [{ mimeType: "image/png", dataBase64: "abc" }]
    }),
    AiImageInputUnsupportedError
  );
});

test("OpenAI provider rejects missing configuration without exposing a key", () => {
  assert.throws(
    () => new OpenAiProvider("", "test-model"),
    AiConfigurationError
  );
});

test("OpenAI provider converts empty responses to a safe application error", async () => {
  const provider = new OpenAiProvider(
    "test-api-key",
    "test-model",
    mockClient(async () => ({ output_text: "   " }))
  );

  await assert.rejects(
    provider.generateReply(providerInput),
    AiEmptyResponseError
  );
});

test("OpenAI provider converts timeouts to a safe application error", async () => {
  const provider = new OpenAiProvider(
    "test-api-key",
    "test-model",
    mockClient(async () => {
      throw new APIConnectionTimeoutError({
        message: "internal timeout detail"
      });
    })
  );

  await assert.rejects(
    provider.generateReply(providerInput),
    AiRequestTimeoutError
  );
});

test("OpenAI provider maps insufficient quota separately from rate limiting", async () => {
  const provider = new OpenAiProvider(
    "test-api-key",
    "test-model",
    mockClient(async () => {
      throw openAiApiError(
        429,
        "insufficient_quota",
        { "retry-after": "30" }
      );
    })
  );

  await assert.rejects(
    provider.generateReply(providerInput),
    (error: unknown) => {
      assert.ok(error instanceof AiQuotaExceededError);
      assert.equal(error.statusCode, 503);
      assert.equal(error.code, "AI_QUOTA_EXCEEDED");
      assert.equal(error.retryable, false);
      assert.equal(error.retryAfter, "30");
      return true;
    }
  );
});

test("OpenAI provider maps actual rate limits and preserves Retry-After", async () => {
  const provider = new OpenAiProvider(
    "test-api-key",
    "test-model",
    mockClient(async () => {
      throw openAiApiError(
        429,
        "rate_limit_exceeded",
        { "retry-after": "7" }
      );
    })
  );

  await assert.rejects(
    provider.generateReply(providerInput),
    (error: unknown) => {
      assert.ok(error instanceof AiRateLimitedError);
      assert.equal(error.statusCode, 429);
      assert.equal(error.code, "AI_RATE_LIMITED");
      assert.equal(error.retryable, true);
      assert.equal(error.retryAfter, "7");
      return true;
    }
  );
});

test("OpenAI provider maps connection failures as retryable unavailability", async () => {
  const provider = new OpenAiProvider(
    "test-api-key",
    "test-model",
    mockClient(async () => {
      throw new APIConnectionError({
        message: "upstream connection failed"
      });
    })
  );

  await assert.rejects(
    provider.generateReply(providerInput),
    (error: unknown) => {
      assert.ok(error instanceof AiProviderUnavailableError);
      assert.equal(error.statusCode, 503);
      assert.equal(error.code, "AI_PROVIDER_UNAVAILABLE");
      assert.equal(error.retryable, true);
      return true;
    }
  );
});

test("OpenAI provider maps invalid model requests as non-retryable", async () => {
  const provider = new OpenAiProvider(
    "test-api-key",
    "test-model",
    mockClient(async () => {
      throw openAiApiError(404, "model_not_found");
    })
  );

  await assert.rejects(
    provider.generateReply(providerInput),
    (error: unknown) => {
      assert.ok(error instanceof AiProviderRequestFailedError);
      assert.equal(error.statusCode, 502);
      assert.equal(error.code, "AI_PROVIDER_REQUEST_FAILED");
      assert.equal(error.retryable, false);
      return true;
    }
  );
});

test("OpenAI provider maps authentication failures without logging or returning credentials", async (testContext) => {
  const apiKey = "test-secret-provider-credential";
  let loggedOutput = "";
  testContext.mock.method(
    console,
    "error",
    (...values: unknown[]) => {
      loggedOutput += values.join(" ");
    }
  );
  const provider = new OpenAiProvider(
    apiKey,
    "test-model",
    mockClient(async () => {
      throw APIError.generate(
        401,
        {
          error: {
            code: "invalid_api_key",
            type: "authentication_error",
            message: `Invalid credential ${apiKey}`
          }
        },
        undefined,
        new Headers({
          "x-request-id": "authentication-request-id"
        })
      );
    })
  );

  await assert.rejects(
    provider.generateReply(providerInput),
    (error: unknown) => {
      assert.ok(error instanceof AiProviderAuthenticationError);
      assert.equal(error.statusCode, 503);
      assert.equal(error.code, "AI_PROVIDER_AUTH_ERROR");
      assert.equal(error.retryable, false);
      assert.doesNotMatch(error.message, /credential/i);
      return true;
    }
  );
  assert.match(loggedOutput, /authentication-request-id/);
  assert.match(loggedOutput, /invalid_api_key/);
  assert.doesNotMatch(loggedOutput, new RegExp(apiKey));
});

test("OpenAI provider hides provider error internals", async () => {
  const provider = new OpenAiProvider(
    "test-api-key",
    "test-model",
    mockClient(async () => {
      throw new Error("private upstream failure detail");
    })
  );

  await assert.rejects(
    provider.generateReply(providerInput),
    (error: unknown) => {
      assert.ok(error instanceof AiProviderError);
      assert.doesNotMatch(error.message, /private upstream/i);
      return true;
    }
  );
});
