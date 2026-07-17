import assert from "node:assert/strict";
import { after, test } from "node:test";
import {
  readApiEnvelope,
  startTestServer
} from "../helpers/test-server";

interface HealthData {
  status: string;
  service: string;
}

const testServer = await startTestServer();

after(async () => {
  await testServer.stop();
});

test("GET /api/health returns the service status", async () => {
  const response = await fetch(`${testServer.baseUrl}/api/health`);
  const responseBody = await readApiEnvelope<HealthData>(response);

  assert.equal(response.status, 200);
  assert.equal(responseBody.success, true);
  assert.equal(responseBody.data?.status, "ok");
  assert.equal(responseBody.data?.service, "smartchat-backend");
});

test("unknown routes return a structured 404 response", async () => {
  const response = await fetch(`${testServer.baseUrl}/api/unknown`);
  const responseBody = await readApiEnvelope<never>(response);

  assert.equal(response.status, 404);
  assert.equal(responseBody.error?.code, "ROUTE_NOT_FOUND");
});
