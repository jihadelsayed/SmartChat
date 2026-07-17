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

interface AiChatData {
  reply: string;
}

const testServer = await startTestServer();
const registeredUser = await registerTestUser(testServer.baseUrl);

after(async () => {
  await cleanupTestUsers([registeredUser.credentials.email]);
  await testServer.stop();
});

test("POST /api/v1/ai/chat returns a mock provider reply", async () => {
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
  assert.match(responseBody.data?.reply ?? "", /SmartChat received/);
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
