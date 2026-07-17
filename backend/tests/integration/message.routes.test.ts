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

after(async () => {
  await cleanupTestUsers([registeredUser.credentials.email]);
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
