import assert from "node:assert/strict";
import { after, test } from "node:test";
import { cleanupTestUsers } from "../helpers/test-database";
import {
  authenticatedHeaders,
  registerTestUser
} from "../helpers/test-user";
import {
  readApiEnvelope,
  startTestServer
} from "../helpers/test-server";
import { AiProviderError } from "../../src/modules/ai/ai.errors";
import type {
  AiContextMessage,
  AiProviderRequest
} from "../../src/modules/ai/ai.types";
import { MockAiProvider } from "../../src/modules/ai/providers/mock.provider";

interface AiChatData {
  reply: string;
  userMessage: AiMessageData;
  assistantMessage: AiMessageData;
}

interface AiMessageData {
  id: string;
  conversationId: string;
  role: "USER" | "ASSISTANT";
  content: string;
  createdAt: string;
}

interface ConversationData {
  id: string;
}

interface ConversationSummaryData extends ConversationData {
  _count: {
    messages: number;
  };
}

interface PersistedMessageData {
  id: string;
  conversationId: string;
  sender: "USER" | "ASSISTANT";
  content: string;
}

const testServer = await startTestServer();
const registeredUser = await registerTestUser(testServer.baseUrl);
const otherUser = await registerTestUser(testServer.baseUrl);

after(async () => {
  await cleanupTestUsers([
    registeredUser.credentials.email,
    otherUser.credentials.email
  ]);
  await testServer.stop();
});

test("POST /api/v1/ai/chat creates a conversation and persists both messages", async () => {
  const response = await fetch(`${testServer.baseUrl}/api/v1/ai/chat`, {
    method: "POST",
    headers: {
      ...authenticatedHeaders(registeredUser.accessToken),
      "content-type": "application/json"
    },
    body: JSON.stringify({ message: "Hello AI" })
  });
  const responseBody = await readApiEnvelope<AiChatData>(response);

  assert.equal(response.status, 200);
  assert.equal(responseBody.success, true);
  assert.match(responseBody.data?.reply ?? "", /Test assistant response/);
  assert.ok(responseBody.data);
  assert.equal(responseBody.data.userMessage.role, "USER");
  assert.equal(responseBody.data.userMessage.content, "Hello AI");
  assert.equal(responseBody.data.assistantMessage.role, "ASSISTANT");
  assert.equal(
    responseBody.data.userMessage.conversationId,
    responseBody.data.assistantMessage.conversationId
  );
  assert.ok(Date.parse(responseBody.data.userMessage.createdAt));
  assert.ok(Date.parse(responseBody.data.assistantMessage.createdAt));

  const messagesResponse = await fetch(
    `${testServer.baseUrl}/api/v1/conversations/${responseBody.data.userMessage.conversationId}/messages`,
    { headers: authenticatedHeaders(registeredUser.accessToken) }
  );
  const messagesBody =
    await readApiEnvelope<PersistedMessageData[]>(messagesResponse);

  assert.equal(messagesResponse.status, 200);
  assert.deepEqual(
    messagesBody.data?.map(({ id, conversationId, sender, content }) => ({
      id,
      conversationId,
      sender,
      content
    })),
    [
      {
        id: responseBody.data.userMessage.id,
        conversationId: responseBody.data.userMessage.conversationId,
        sender: responseBody.data.userMessage.role,
        content: responseBody.data.userMessage.content
      },
      {
        id: responseBody.data.assistantMessage.id,
        conversationId: responseBody.data.assistantMessage.conversationId,
        sender: responseBody.data.assistantMessage.role,
        content: responseBody.data.assistantMessage.content
      }
    ]
  );
});

test("POST /api/v1/ai/chat persists into an owned requested conversation", async () => {
  const jsonHeaders = {
    ...authenticatedHeaders(registeredUser.accessToken),
    "content-type": "application/json"
  };
  const conversationResponse = await fetch(
    `${testServer.baseUrl}/api/v1/conversations`,
    {
      method: "POST",
      headers: jsonHeaders,
      body: JSON.stringify({ title: "Existing AI conversation" })
    }
  );
  const conversationBody =
    await readApiEnvelope<ConversationData>(conversationResponse);
  assert.ok(conversationBody.data);

  const response = await fetch(`${testServer.baseUrl}/api/v1/ai/chat`, {
    method: "POST",
    headers: jsonHeaders,
    body: JSON.stringify({
      message: "Continue this conversation",
      conversationId: conversationBody.data.id
    })
  });
  const responseBody = await readApiEnvelope<AiChatData>(response);

  assert.equal(response.status, 200);
  assert.equal(
    responseBody.data?.userMessage.conversationId,
    conversationBody.data.id
  );
  assert.equal(
    responseBody.data?.assistantMessage.conversationId,
    conversationBody.data.id
  );

  const messagesResponse = await fetch(
    `${testServer.baseUrl}/api/v1/conversations/${conversationBody.data.id}/messages`,
    { headers: authenticatedHeaders(registeredUser.accessToken) }
  );
  const messagesBody =
    await readApiEnvelope<PersistedMessageData[]>(messagesResponse);

  assert.equal(messagesResponse.status, 200);
  assert.deepEqual(
    messagesBody.data?.map(({ sender, content }) => ({
      sender,
      content
    })),
    [
      {
        sender: "USER",
        content: "Continue this conversation"
      },
      {
        sender: "ASSISTANT",
        content:
          "Test assistant response for: \"Continue this conversation\""
      }
    ]
  );
});

test("POST /api/v1/ai/chat sends only the 20 most recent context messages", async (testContext) => {
  const jsonHeaders = {
    ...authenticatedHeaders(registeredUser.accessToken),
    "content-type": "application/json"
  };
  const conversationResponse = await fetch(
    `${testServer.baseUrl}/api/v1/conversations`,
    {
      method: "POST",
      headers: jsonHeaders,
      body: JSON.stringify({ title: "AI context limit" })
    }
  );
  const conversationBody =
    await readApiEnvelope<ConversationData>(conversationResponse);
  assert.ok(conversationBody.data);

  for (let index = 1; index <= 10; index += 1) {
    const seedResponse: Response = await fetch(
      `${testServer.baseUrl}/api/v1/ai/chat`,
      {
        method: "POST",
        headers: jsonHeaders,
        body: JSON.stringify({
          message: `History ${index}`,
          conversationId: conversationBody.data.id
        })
      }
    );

    assert.equal(seedResponse.status, 200);
  }

  let capturedMessages: AiContextMessage[] = [];
  testContext.mock.method(
    MockAiProvider.prototype,
    "generateReply",
    async (input: AiProviderRequest) => {
      capturedMessages = input.messages;
      return "Context-limited response";
    }
  );

  const response = await fetch(`${testServer.baseUrl}/api/v1/ai/chat`, {
    method: "POST",
    headers: jsonHeaders,
    body: JSON.stringify({
      message: "Current message",
      conversationId: conversationBody.data.id
    })
  });

  assert.equal(response.status, 200);
  assert.equal(capturedMessages.length, 20);
  assert.equal(capturedMessages[0]?.role, "assistant");
  assert.match(capturedMessages[0]?.content ?? "", /History 1/);
  assert.deepEqual(capturedMessages.at(-1), {
    role: "user",
    content: "Current message"
  });
});

test("POST /api/v1/ai/chat rejects another user's conversation without writing messages", async () => {
  const otherUserHeaders = {
    ...authenticatedHeaders(otherUser.accessToken),
    "content-type": "application/json"
  };
  const conversationResponse = await fetch(
    `${testServer.baseUrl}/api/v1/conversations`,
    {
      method: "POST",
      headers: otherUserHeaders,
      body: JSON.stringify({ title: "Private conversation" })
    }
  );
  const conversationBody =
    await readApiEnvelope<ConversationData>(conversationResponse);
  assert.ok(conversationBody.data);

  const response = await fetch(`${testServer.baseUrl}/api/v1/ai/chat`, {
    method: "POST",
    headers: {
      ...authenticatedHeaders(registeredUser.accessToken),
      "content-type": "application/json"
    },
    body: JSON.stringify({
      message: "Unauthorized message",
      conversationId: conversationBody.data.id
    })
  });

  assert.equal(response.status, 404);

  const messagesResponse = await fetch(
    `${testServer.baseUrl}/api/v1/conversations/${conversationBody.data.id}/messages`,
    { headers: authenticatedHeaders(otherUser.accessToken) }
  );
  const messagesBody =
    await readApiEnvelope<PersistedMessageData[]>(messagesResponse);

  assert.equal(messagesResponse.status, 200);
  assert.deepEqual(messagesBody.data, []);
});

test("POST /api/v1/ai/chat keeps a new conversation with its USER message when the provider fails", async (testContext) => {
  const authorizationHeaders = authenticatedHeaders(
    registeredUser.accessToken
  );
  const beforeResponse = await fetch(
    `${testServer.baseUrl}/api/v1/conversations`,
    { headers: authorizationHeaders }
  );
  const beforeBody =
    await readApiEnvelope<ConversationSummaryData[]>(beforeResponse);
  const existingConversationIds = new Set(
    beforeBody.data?.map((conversation) => conversation.id)
  );

  testContext.mock.method(
    MockAiProvider.prototype,
    "generateReply",
    async () => {
      throw new AiProviderError();
    }
  );

  const response = await fetch(`${testServer.baseUrl}/api/v1/ai/chat`, {
    method: "POST",
    headers: {
      ...authorizationHeaders,
      "content-type": "application/json"
    },
    body: JSON.stringify({
      message: "First request will fail"
    })
  });
  const responseBody = await readApiEnvelope<never>(response);

  assert.equal(response.status, 502);
  assert.equal(responseBody.error?.code, "AI_PROVIDER_ERROR");
  assert.equal(
    responseBody.error?.message,
    "The AI service is temporarily unavailable. Please try again."
  );

  const afterResponse = await fetch(
    `${testServer.baseUrl}/api/v1/conversations`,
    { headers: authorizationHeaders }
  );
  const afterBody =
    await readApiEnvelope<ConversationSummaryData[]>(afterResponse);
  const createdConversations =
    afterBody.data?.filter(
      (conversation) => !existingConversationIds.has(conversation.id)
    ) ?? [];

  assert.equal(createdConversations.length, 1);
  assert.equal(createdConversations[0]?._count.messages, 1);

  const messagesResponse = await fetch(
    `${testServer.baseUrl}/api/v1/conversations/${createdConversations[0]?.id}/messages`,
    { headers: authorizationHeaders }
  );
  const messagesBody =
    await readApiEnvelope<PersistedMessageData[]>(messagesResponse);

  assert.equal(messagesResponse.status, 200);
  assert.deepEqual(
    messagesBody.data?.map(({ sender, content }) => ({
      sender,
      content
    })),
    [
      {
        sender: "USER",
        content: "First request will fail"
      }
    ]
  );
});

test("POST /api/v1/ai/chat returns a safe error when the AI provider fails", async (testContext) => {
  const jsonHeaders = {
    ...authenticatedHeaders(registeredUser.accessToken),
    "content-type": "application/json"
  };
  const conversationResponse = await fetch(
    `${testServer.baseUrl}/api/v1/conversations`,
    {
      method: "POST",
      headers: jsonHeaders,
      body: JSON.stringify({ title: "Provider failure" })
    }
  );
  const conversationBody =
    await readApiEnvelope<ConversationData>(conversationResponse);
  assert.ok(conversationBody.data);

  testContext.mock.method(
    MockAiProvider.prototype,
    "generateReply",
    async () => {
      throw new AiProviderError();
    }
  );

  const response = await fetch(`${testServer.baseUrl}/api/v1/ai/chat`, {
    method: "POST",
    headers: jsonHeaders,
    body: JSON.stringify({
      message: "This request will fail",
      conversationId: conversationBody.data.id
    })
  });
  const responseBody = await readApiEnvelope<never>(response);

  assert.equal(response.status, 502);
  assert.equal(responseBody.error?.code, "AI_PROVIDER_ERROR");
  assert.equal(
    responseBody.error?.message,
    "The AI service is temporarily unavailable. Please try again."
  );

  const messagesResponse = await fetch(
    `${testServer.baseUrl}/api/v1/conversations/${conversationBody.data.id}/messages`,
    { headers: authenticatedHeaders(registeredUser.accessToken) }
  );
  const messagesBody =
    await readApiEnvelope<PersistedMessageData[]>(messagesResponse);

  assert.equal(messagesResponse.status, 200);
  assert.deepEqual(
    messagesBody.data?.map(({ sender, content }) => ({
      sender,
      content
    })),
    [
      {
        sender: "USER",
        content: "This request will fail"
      }
    ]
  );
});

test("POST /api/v1/ai/chat requires authentication", async () => {
  const response = await fetch(`${testServer.baseUrl}/api/v1/ai/chat`, {
    method: "POST",
    headers: {
      "content-type": "application/json"
    },
    body: JSON.stringify({ message: "Hello AI" })
  });

  assert.equal(response.status, 401);
});

test("POST /api/v1/ai/chat rejects invalid access tokens", async () => {
  const response = await fetch(`${testServer.baseUrl}/api/v1/ai/chat`, {
    method: "POST",
    headers: {
      authorization: "Bearer invalid-token",
      "content-type": "application/json"
    },
    body: JSON.stringify({ message: "Hello AI" })
  });
  const responseBody = await readApiEnvelope<never>(response);

  assert.equal(response.status, 401);
  assert.equal(responseBody.error?.code, "AUTHENTICATION_REQUIRED");
});
