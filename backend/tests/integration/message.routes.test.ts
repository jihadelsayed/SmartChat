import assert from "node:assert/strict";
import { randomUUID } from "node:crypto";
import { after, test } from "node:test";
import {
  AiProviderAuthenticationError,
  AiProviderError,
  AiQuotaExceededError,
  AiRateLimitedError,
  AiRequestTimeoutError
} from "../../src/modules/ai/ai.errors";
import { MockAiProvider } from "../../src/modules/ai/providers/mock.provider";
import { cleanupTestUsers } from "../helpers/test-database";
import {
  authenticatedHeaders,
  registerTestUser
} from "../helpers/test-user";
import {
  readApiEnvelope,
  startTestServer
} from "../helpers/test-server";

interface ConversationData {
  id: string;
}

interface MessageData {
  id: string;
  conversationId: string;
  sender: "USER" | "ASSISTANT";
  content: string;
}

interface SendMessageData {
  userMessage: MessageData;
  assistantMessage: MessageData;
}

const testServer = await startTestServer();
const registeredUser = await registerTestUser(testServer.baseUrl);
const otherUser = await registerTestUser(testServer.baseUrl);

async function createConversation(
  accessToken: string,
  title: string
): Promise<string> {
  const response = await fetch(
    `${testServer.baseUrl}/api/v1/conversations`,
    {
      method: "POST",
      headers: {
        ...authenticatedHeaders(accessToken),
        "content-type": "application/json"
      },
      body: JSON.stringify({ title })
    }
  );
  const responseBody =
    await readApiEnvelope<ConversationData>(response);

  assert.equal(response.status, 201);
  assert.ok(responseBody.data);
  return responseBody.data.id;
}

function sendMessage(
  conversationId: string,
  accessToken: string,
  content: string,
  options: {
    idempotencyKey?: string;
    clientRequestId?: string;
  } = {}
) {
  return fetch(
    `${testServer.baseUrl}/api/v1/conversations/${conversationId}/messages`,
    {
      method: "POST",
      headers: {
        ...authenticatedHeaders(accessToken),
        "content-type": "application/json",
        ...(options.idempotencyKey
          ? { "idempotency-key": options.idempotencyKey }
          : {})
      },
      body: JSON.stringify({
        content,
        ...(options.clientRequestId
          ? { clientRequestId: options.clientRequestId }
          : {})
      })
    }
  );
}

async function listMessages(
  conversationId: string,
  accessToken: string
): Promise<MessageData[]> {
  const response = await fetch(
    `${testServer.baseUrl}/api/v1/conversations/${conversationId}/messages`,
    { headers: authenticatedHeaders(accessToken) }
  );
  const responseBody =
    await readApiEnvelope<MessageData[]>(response);

  assert.equal(response.status, 200);
  return responseBody.data ?? [];
}

after(async () => {
  await cleanupTestUsers([
    registeredUser.credentials.email,
    otherUser.credentials.email
  ]);
  await testServer.stop();
});

test("conversation message endpoints send and list user and assistant messages", async () => {
  const authorizationHeaders = authenticatedHeaders(
    registeredUser.accessToken
  );
  const jsonHeaders = {
    ...authorizationHeaders,
    "content-type": "application/json"
  };

  const conversationResponse = await fetch(
    `${testServer.baseUrl}/api/v1/conversations`,
    {
      method: "POST",
      headers: jsonHeaders,
      body: JSON.stringify({ title: "Message route test" })
    }
  );
  const conversationBody =
    await readApiEnvelope<ConversationData>(conversationResponse);
  assert.ok(conversationBody.data);

  const messagesUrl = `${testServer.baseUrl}/api/v1/conversations/${conversationBody.data.id}/messages`;
  const sendResponse = await fetch(messagesUrl, {
    method: "POST",
    headers: jsonHeaders,
    body: JSON.stringify({ content: "Hello from the test suite" })
  });
  const sendBody = await readApiEnvelope<SendMessageData>(sendResponse);

  assert.equal(sendResponse.status, 201);
  assert.equal(sendBody.data?.userMessage.sender, "USER");
  assert.equal(sendBody.data?.assistantMessage.sender, "ASSISTANT");

  const listResponse = await fetch(messagesUrl, {
    headers: authorizationHeaders
  });
  const listBody = await readApiEnvelope<MessageData[]>(listResponse);

  assert.equal(listResponse.status, 200);
  assert.equal(listBody.data?.length, 2);
  assert.deepEqual(
    listBody.data?.map((message) => message.sender),
    ["USER", "ASSISTANT"]
  );
});

test("empty message content returns a validation error", async () => {
  const authorizationHeaders = authenticatedHeaders(
    registeredUser.accessToken
  );
  const conversationResponse = await fetch(
    `${testServer.baseUrl}/api/v1/conversations`,
    {
      method: "POST",
      headers: {
        ...authorizationHeaders,
        "content-type": "application/json"
      },
      body: JSON.stringify({})
    }
  );
  const conversationBody =
    await readApiEnvelope<ConversationData>(conversationResponse);
  assert.ok(conversationBody.data);

  const response = await fetch(
    `${testServer.baseUrl}/api/v1/conversations/${conversationBody.data.id}/messages`,
    {
      method: "POST",
      headers: {
        ...authorizationHeaders,
        "content-type": "application/json"
      },
      body: JSON.stringify({ content: "   " })
    }
  );
  const responseBody = await readApiEnvelope<never>(response);

  assert.equal(response.status, 400);
  assert.equal(responseBody.error?.code, "VALIDATION_ERROR");
});

test("message history remains chronological across exchanges", async () => {
  const authorizationHeaders = authenticatedHeaders(
    registeredUser.accessToken
  );
  const jsonHeaders = {
    ...authorizationHeaders,
    "content-type": "application/json"
  };
  const conversationResponse = await fetch(
    `${testServer.baseUrl}/api/v1/conversations`,
    {
      method: "POST",
      headers: jsonHeaders,
      body: JSON.stringify({ title: "Chronological history" })
    }
  );
  const conversationBody =
    await readApiEnvelope<ConversationData>(conversationResponse);
  assert.ok(conversationBody.data);
  const messagesUrl = `${testServer.baseUrl}/api/v1/conversations/${conversationBody.data.id}/messages`;

  for (const content of ["First question", "Second question"]) {
    const response = await fetch(messagesUrl, {
      method: "POST",
      headers: jsonHeaders,
      body: JSON.stringify({ content })
    });
    assert.equal(response.status, 201);
  }

  const historyResponse = await fetch(messagesUrl, {
    headers: authorizationHeaders
  });
  const historyBody =
    await readApiEnvelope<MessageData[]>(historyResponse);

  assert.equal(historyResponse.status, 200);
  assert.deepEqual(
    historyBody.data?.map(({ sender, content }) => ({ sender, content })),
    [
      { sender: "USER", content: "First question" },
      {
        sender: "ASSISTANT",
        content: "Test assistant response for: \"First question\""
      },
      { sender: "USER", content: "Second question" },
      {
        sender: "ASSISTANT",
        content: "Test assistant response for: \"Second question\""
      }
    ]
  );
});

test("users cannot load or send messages in another user's conversation", async () => {
  const conversationResponse = await fetch(
    `${testServer.baseUrl}/api/v1/conversations`,
    {
      method: "POST",
      headers: {
        ...authenticatedHeaders(otherUser.accessToken),
        "content-type": "application/json"
      },
      body: JSON.stringify({ title: "Private message history" })
    }
  );
  const conversationBody =
    await readApiEnvelope<ConversationData>(conversationResponse);
  assert.ok(conversationBody.data);
  const messagesUrl = `${testServer.baseUrl}/api/v1/conversations/${conversationBody.data.id}/messages`;
  const authorizationHeaders = authenticatedHeaders(
    registeredUser.accessToken
  );

  const listResponse = await fetch(messagesUrl, {
    headers: authorizationHeaders
  });
  assert.equal(listResponse.status, 404);

  const sendResponse = await fetch(messagesUrl, {
    method: "POST",
    headers: {
      ...authorizationHeaders,
      "content-type": "application/json"
    },
    body: JSON.stringify({ content: "Unauthorized" })
  });
  assert.equal(sendResponse.status, 404);

  const ownerHistoryResponse = await fetch(messagesUrl, {
    headers: authenticatedHeaders(otherUser.accessToken)
  });
  const ownerHistoryBody =
    await readApiEnvelope<MessageData[]>(ownerHistoryResponse);
  assert.deepEqual(ownerHistoryBody.data, []);
});

test("an idempotent replay returns the original exchange without another provider call", async (testContext) => {
  const conversationId = await createConversation(
    registeredUser.accessToken,
    "Idempotent replay"
  );
  const idempotencyKey = randomUUID();
  let providerCalls = 0;
  testContext.mock.method(
    MockAiProvider.prototype,
    "generateReply",
    async () => {
      providerCalls += 1;
      return "Idempotent assistant response";
    }
  );

  const firstResponse = await sendMessage(
    conversationId,
    registeredUser.accessToken,
    "One request",
    { idempotencyKey }
  );
  const firstBody =
    await readApiEnvelope<SendMessageData>(firstResponse);
  const replayResponse = await sendMessage(
    conversationId,
    registeredUser.accessToken,
    "One request",
    { clientRequestId: idempotencyKey }
  );
  const replayBody =
    await readApiEnvelope<SendMessageData>(replayResponse);

  assert.equal(firstResponse.status, 201);
  assert.equal(replayResponse.status, 201);
  assert.deepEqual(replayBody.data, firstBody.data);
  assert.equal(providerCalls, 1);

  const messages = await listMessages(
    conversationId,
    registeredUser.accessToken
  );
  assert.deepEqual(
    messages.map(({ sender, content }) => ({ sender, content })),
    [
      { sender: "USER", content: "One request" },
      {
        sender: "ASSISTANT",
        content: "Idempotent assistant response"
      }
    ]
  );
});

test("reusing an idempotency key with different content returns 409", async (testContext) => {
  const conversationId = await createConversation(
    registeredUser.accessToken,
    "Idempotency payload conflict"
  );
  const idempotencyKey = randomUUID();
  let providerCalls = 0;
  testContext.mock.method(
    MockAiProvider.prototype,
    "generateReply",
    async () => {
      providerCalls += 1;
      return "Original response";
    }
  );

  const firstResponse = await sendMessage(
    conversationId,
    registeredUser.accessToken,
    "Original content",
    { idempotencyKey }
  );
  const conflictResponse = await sendMessage(
    conversationId,
    registeredUser.accessToken,
    "Different content",
    { idempotencyKey }
  );
  const conflictBody =
    await readApiEnvelope<never>(conflictResponse);

  assert.equal(firstResponse.status, 201);
  assert.equal(conflictResponse.status, 409);
  assert.equal(
    conflictBody.error?.code,
    "IDEMPOTENCY_KEY_CONFLICT"
  );
  assert.equal(conflictBody.error?.retryable, false);
  assert.equal(providerCalls, 1);
});

test("a concurrent processing duplicate returns AI_REQUEST_IN_PROGRESS and calls the provider once", async (testContext) => {
  const conversationId = await createConversation(
    registeredUser.accessToken,
    "Concurrent idempotency"
  );
  const idempotencyKey = randomUUID();
  let providerCalls = 0;
  let releaseProvider!: () => void;
  let markProviderStarted!: () => void;
  const providerStarted = new Promise<void>((resolve) => {
    markProviderStarted = resolve;
  });
  const providerRelease = new Promise<void>((resolve) => {
    releaseProvider = resolve;
  });
  testContext.mock.method(
    MockAiProvider.prototype,
    "generateReply",
    async () => {
      providerCalls += 1;
      markProviderStarted();
      await providerRelease;
      return "Only response";
    }
  );

  const firstRequest = sendMessage(
    conversationId,
    registeredUser.accessToken,
    "Concurrent content",
    { idempotencyKey }
  );
  await providerStarted;

  try {
    const duplicateResponse = await sendMessage(
      conversationId,
      registeredUser.accessToken,
      "Concurrent content",
      { idempotencyKey }
    );
    const duplicateBody =
      await readApiEnvelope<never>(duplicateResponse);

    assert.equal(duplicateResponse.status, 409);
    assert.equal(
      duplicateBody.error?.code,
      "AI_REQUEST_IN_PROGRESS"
    );
    assert.equal(duplicateBody.error?.retryable, true);
    assert.equal(providerCalls, 1);
  } finally {
    releaseProvider();
  }

  const firstResponse = await firstRequest;
  assert.equal(firstResponse.status, 201);
  assert.equal(providerCalls, 1);
  assert.equal(
    (
      await listMessages(
        conversationId,
        registeredUser.accessToken
      )
    ).length,
    2
  );
});

test("a failed idempotent request retains one USER message and replays its sanitized error", async (testContext) => {
  const conversationId = await createConversation(
    registeredUser.accessToken,
    "Failed idempotent request"
  );
  const idempotencyKey = randomUUID();
  let providerCalls = 0;
  testContext.mock.method(
    MockAiProvider.prototype,
    "generateReply",
    async () => {
      providerCalls += 1;
      throw new AiProviderError();
    }
  );

  const firstResponse = await sendMessage(
    conversationId,
    registeredUser.accessToken,
    "Persist failed request",
    { idempotencyKey }
  );
  const firstBody = await readApiEnvelope<never>(firstResponse);
  const replayResponse = await sendMessage(
    conversationId,
    registeredUser.accessToken,
    "Persist failed request",
    { idempotencyKey }
  );
  const replayBody = await readApiEnvelope<never>(replayResponse);

  assert.equal(firstResponse.status, 502);
  assert.equal(replayResponse.status, 502);
  assert.equal(firstBody.error?.code, "AI_PROVIDER_ERROR");
  assert.equal(replayBody.error?.code, firstBody.error?.code);
  assert.equal(
    replayBody.error?.message,
    firstBody.error?.message
  );
  assert.equal(providerCalls, 1);

  const messages = await listMessages(
    conversationId,
    registeredUser.accessToken
  );
  assert.deepEqual(
    messages.map(({ sender, content }) => ({ sender, content })),
    [
      {
        sender: "USER",
        content: "Persist failed request"
      }
    ]
  );
});

test("different idempotency keys create separate exchanges", async () => {
  const conversationId = await createConversation(
    registeredUser.accessToken,
    "Different keys"
  );

  for (const idempotencyKey of [randomUUID(), randomUUID()]) {
    const response = await sendMessage(
      conversationId,
      registeredUser.accessToken,
      "Intentional duplicate content",
      { idempotencyKey }
    );
    assert.equal(response.status, 201);
  }

  assert.equal(
    (
      await listMessages(
        conversationId,
        registeredUser.accessToken
      )
    ).length,
    4
  );
});

test("the same idempotency key can be used in different conversations", async () => {
  const idempotencyKey = randomUUID();
  const conversationIds = await Promise.all([
    createConversation(
      registeredUser.accessToken,
      "Scoped key one"
    ),
    createConversation(
      registeredUser.accessToken,
      "Scoped key two"
    )
  ]);

  for (const conversationId of conversationIds) {
    const response = await sendMessage(
      conversationId,
      registeredUser.accessToken,
      "Scoped content",
      { idempotencyKey }
    );
    assert.equal(response.status, 201);
  }

  for (const conversationId of conversationIds) {
    assert.equal(
      (
        await listMessages(
          conversationId,
          registeredUser.accessToken
        )
      ).length,
      2
    );
  }
});

test("one user cannot retrieve another user's idempotency result", async (testContext) => {
  const conversationId = await createConversation(
    registeredUser.accessToken,
    "Private idempotency result"
  );
  const idempotencyKey = randomUUID();
  let providerCalls = 0;
  testContext.mock.method(
    MockAiProvider.prototype,
    "generateReply",
    async () => {
      providerCalls += 1;
      return "Private response";
    }
  );

  const ownerResponse = await sendMessage(
    conversationId,
    registeredUser.accessToken,
    "Private request",
    { idempotencyKey }
  );
  const otherUserResponse = await sendMessage(
    conversationId,
    otherUser.accessToken,
    "Private request",
    { idempotencyKey }
  );

  assert.equal(ownerResponse.status, 201);
  assert.equal(otherUserResponse.status, 404);
  assert.equal(providerCalls, 1);
});

test("requests without an idempotency key retain non-idempotent behavior", async (testContext) => {
  const conversationId = await createConversation(
    registeredUser.accessToken,
    "No idempotency key"
  );
  let providerCalls = 0;
  testContext.mock.method(
    MockAiProvider.prototype,
    "generateReply",
    async () => {
      providerCalls += 1;
      return `Response ${providerCalls}`;
    }
  );

  for (let request = 0; request < 2; request += 1) {
    const response = await sendMessage(
      conversationId,
      registeredUser.accessToken,
      "Same unkeyed content"
    );
    assert.equal(response.status, 201);
  }

  assert.equal(providerCalls, 2);
  assert.equal(
    (
      await listMessages(
        conversationId,
        registeredUser.accessToken
      )
    ).length,
    4
  );
});

const providerErrorMappings = [
  {
    name: "insufficient quota",
    createError: () => new AiQuotaExceededError(),
    status: 503,
    code: "AI_QUOTA_EXCEEDED",
    retryable: false
  },
  {
    name: "rate limiting",
    createError: () => new AiRateLimitedError("11"),
    status: 429,
    code: "AI_RATE_LIMITED",
    retryable: true,
    retryAfter: "11"
  },
  {
    name: "provider timeout",
    createError: () => new AiRequestTimeoutError(),
    status: 504,
    code: "AI_PROVIDER_TIMEOUT",
    retryable: true
  },
  {
    name: "provider authentication",
    createError: () => new AiProviderAuthenticationError(),
    status: 503,
    code: "AI_PROVIDER_AUTH_ERROR",
    retryable: false
  }
] as const;

for (const mapping of providerErrorMappings) {
  test(`${mapping.name} returns a sanitized retry-aware API error`, async (testContext) => {
    const conversationId = await createConversation(
      registeredUser.accessToken,
      `Provider error: ${mapping.name}`
    );
    testContext.mock.method(
      MockAiProvider.prototype,
      "generateReply",
      async () => {
        throw mapping.createError();
      }
    );

    const response = await sendMessage(
      conversationId,
      registeredUser.accessToken,
      "Provider error request"
    );
    const responseBody = await readApiEnvelope<never>(response);

    assert.equal(response.status, mapping.status);
    assert.equal(responseBody.error?.code, mapping.code);
    assert.equal(
      responseBody.error?.retryable,
      mapping.retryable
    );
    assert.equal(
      responseBody.error?.requestId,
      response.headers.get("x-request-id")
    );
    assert.doesNotMatch(
      JSON.stringify(responseBody),
      /api[-_ ]?key|credential|stack/i
    );

    if ("retryAfter" in mapping) {
      assert.equal(
        response.headers.get("retry-after"),
        mapping.retryAfter
      );
    }
  });
}

test("invalid conversation IDs on message routes return validation errors", async () => {
  const response = await fetch(
    `${testServer.baseUrl}/api/v1/conversations/not-a-uuid/messages`,
    { headers: authenticatedHeaders(registeredUser.accessToken) }
  );
  const responseBody = await readApiEnvelope<never>(response);

  assert.equal(response.status, 400);
  assert.equal(responseBody.error?.code, "VALIDATION_ERROR");
});
