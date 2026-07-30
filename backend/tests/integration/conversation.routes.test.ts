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
  userId: string;
  title: string;
  createdAt: string;
  updatedAt: string;
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

test("conversation endpoints create, list, read, update, and delete", async () => {
  const authorizationHeaders = authenticatedHeaders(
    registeredUser.accessToken
  );

  const createResponse = await fetch(
    `${testServer.baseUrl}/api/v1/conversations`,
    {
      method: "POST",
      headers: {
        ...authorizationHeaders,
        "content-type": "application/json"
      },
      body: JSON.stringify({ title: "Test conversation" })
    }
  );
  const createBody = await readApiEnvelope<ConversationData>(createResponse);

  assert.equal(createResponse.status, 201);
  assert.ok(createBody.data);
  assert.equal(createBody.data.title, "Test conversation");

  const conversationId = createBody.data.id;
  const listResponse = await fetch(
    `${testServer.baseUrl}/api/v1/conversations`,
    { headers: authorizationHeaders }
  );
  const listBody = await readApiEnvelope<ConversationData[]>(listResponse);

  assert.equal(listResponse.status, 200);
  assert.ok(
    listBody.data?.some((conversation) => conversation.id === conversationId)
  );

  const detailResponse = await fetch(
    `${testServer.baseUrl}/api/v1/conversations/${conversationId}`,
    { headers: authorizationHeaders }
  );
  const detailBody = await readApiEnvelope<ConversationData>(detailResponse);

  assert.equal(detailResponse.status, 200);
  assert.equal(detailBody.data?.id, conversationId);

  const updateResponse = await fetch(
    `${testServer.baseUrl}/api/v1/conversations/${conversationId}`,
    {
      method: "PATCH",
      headers: {
        ...authorizationHeaders,
        "content-type": "application/json"
      },
      body: JSON.stringify({ title: "Updated conversation" })
    }
  );
  const updateBody = await readApiEnvelope<ConversationData>(updateResponse);

  assert.equal(updateResponse.status, 200);
  assert.equal(updateBody.data?.title, "Updated conversation");

  const deleteResponse = await fetch(
    `${testServer.baseUrl}/api/v1/conversations/${conversationId}`,
    {
      method: "DELETE",
      headers: authorizationHeaders
    }
  );

  assert.equal(deleteResponse.status, 204);
});

test("invalid conversation UUIDs return validation errors", async () => {
  const response = await fetch(
    `${testServer.baseUrl}/api/v1/conversations/not-a-uuid`,
    { headers: authenticatedHeaders(registeredUser.accessToken) }
  );
  const responseBody = await readApiEnvelope<never>(response);

  assert.equal(response.status, 400);
  assert.equal(responseBody.error?.code, "VALIDATION_ERROR");
});

test("users cannot list or open another user's conversations", async () => {
  const createResponse = await fetch(
    `${testServer.baseUrl}/api/v1/conversations`,
    {
      method: "POST",
      headers: {
        ...authenticatedHeaders(otherUser.accessToken),
        "content-type": "application/json"
      },
      body: JSON.stringify({ title: "Other user's conversation" })
    }
  );
  const createBody = await readApiEnvelope<ConversationData>(createResponse);
  assert.ok(createBody.data);

  const authorizationHeaders = authenticatedHeaders(
    registeredUser.accessToken
  );
  const listResponse = await fetch(
    `${testServer.baseUrl}/api/v1/conversations`,
    { headers: authorizationHeaders }
  );
  const listBody = await readApiEnvelope<ConversationData[]>(listResponse);

  assert.equal(listResponse.status, 200);
  assert.equal(
    listBody.data?.some(({ id }) => id === createBody.data?.id),
    false
  );

  const detailResponse = await fetch(
    `${testServer.baseUrl}/api/v1/conversations/${createBody.data.id}`,
    { headers: authorizationHeaders }
  );

  assert.equal(detailResponse.status, 404);
});
