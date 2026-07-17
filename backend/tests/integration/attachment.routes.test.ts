import assert from "node:assert/strict";
import { randomUUID } from "node:crypto";
import {
  access,
  readdir,
  utimes,
  writeFile
} from "node:fs/promises";
import path from "node:path";
import { after, test } from "node:test";
import { runAttachmentCleanup } from "../../src/jobs/attachment-cleanup.job";
import { MAXIMUM_ATTACHMENT_SIZE_BYTES } from "../../src/middleware/upload.middleware";
import {
  deleteUploadedFile,
  resolveUploadPath
} from "../../src/shared/utils/file";
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
}

interface SendMessageData {
  userMessage: MessageData;
}

interface AttachmentData {
  id: string;
  messageId: string;
  fileName: string;
  mimeType: string;
  fileUrl: string;
  sizeBytes: number;
}

const testServer = await startTestServer();
const registeredUser = await registerTestUser(testServer.baseUrl);
let uploadedFileUrl: string | undefined;

after(async () => {
  if (uploadedFileUrl) {
    await deleteUploadedFile(uploadedFileUrl);
  }

  await cleanupTestUsers([registeredUser.credentials.email]);
  await testServer.stop();
});

async function createMessageForAttachment(): Promise<MessageData> {
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
      body: JSON.stringify({ title: "Attachment route test" })
    }
  );
  const conversationBody =
    await readApiEnvelope<ConversationData>(conversationResponse);
  assert.ok(conversationBody.data);

  const messageResponse = await fetch(
    `${testServer.baseUrl}/api/v1/conversations/${conversationBody.data.id}/messages`,
    {
      method: "POST",
      headers: jsonHeaders,
      body: JSON.stringify({ content: "Message with an attachment" })
    }
  );
  const messageBody = await readApiEnvelope<SendMessageData>(messageResponse);
  assert.ok(messageBody.data);

  return messageBody.data.userMessage;
}

function createUploadForm(
  mimeType: string,
  fileName: string,
  contents = new Uint8Array([137, 80, 78, 71])
): FormData {
  const form = new FormData();
  form.append(
    "file",
    new Blob([contents], { type: mimeType }),
    fileName
  );
  return form;
}

test("attachment endpoints upload, list, read, and physically delete a file", async () => {
  assert.equal(MAXIMUM_ATTACHMENT_SIZE_BYTES, 10 * 1024 * 1024);

  const message = await createMessageForAttachment();
  const authorizationHeaders = authenticatedHeaders(
    registeredUser.accessToken
  );
  const messageAttachmentsUrl =
    `${testServer.baseUrl}/api/v1/messages/${message.id}/attachments`;

  const uploadResponse = await fetch(messageAttachmentsUrl, {
    method: "POST",
    headers: authorizationHeaders,
    body: createUploadForm("image/png", "attachment-test.png")
  });
  const uploadBody = await readApiEnvelope<AttachmentData>(uploadResponse);

  assert.equal(uploadResponse.status, 201);
  assert.ok(uploadBody.data);
  assert.equal(uploadBody.data.messageId, message.id);
  assert.equal(uploadBody.data.mimeType, "image/png");

  uploadedFileUrl = uploadBody.data.fileUrl;
  const uploadedFilePath = resolveUploadPath(uploadedFileUrl);
  await access(uploadedFilePath);

  const listResponse = await fetch(messageAttachmentsUrl, {
    headers: authorizationHeaders
  });
  const listBody = await readApiEnvelope<AttachmentData[]>(listResponse);

  assert.equal(listResponse.status, 200);
  assert.equal(listBody.data?.length, 1);
  assert.equal(listBody.data?.[0]?.id, uploadBody.data.id);

  const attachmentUrl =
    `${testServer.baseUrl}/api/v1/attachments/${uploadBody.data.id}`;
  const getResponse = await fetch(attachmentUrl, {
    headers: authorizationHeaders
  });
  const getBody = await readApiEnvelope<AttachmentData>(getResponse);

  assert.equal(getResponse.status, 200);
  assert.equal(getBody.data?.id, uploadBody.data.id);

  const deleteResponse = await fetch(attachmentUrl, {
    method: "DELETE",
    headers: authorizationHeaders
  });

  assert.equal(deleteResponse.status, 200);
  await assert.rejects(access(uploadedFilePath));
  uploadedFileUrl = undefined;
});

test("unsupported attachment MIME types return a client error", async () => {
  const message = await createMessageForAttachment();
  const response = await fetch(
    `${testServer.baseUrl}/api/v1/messages/${message.id}/attachments`,
    {
      method: "POST",
      headers: authenticatedHeaders(registeredUser.accessToken),
      body: createUploadForm("text/plain", "attachment-test.txt")
    }
  );
  const responseBody = await readApiEnvelope<never>(response);

  assert.equal(response.status, 415);
  assert.equal(responseBody.error?.code, "UNSUPPORTED_ATTACHMENT_TYPE");
});

test("attachments larger than 10 MB are rejected and removed", async () => {
  const uploadsDirectory = path.dirname(
    resolveUploadPath("/uploads/directory-probe")
  );
  const filesBeforeUpload = (await readdir(uploadsDirectory)).sort();

  const response = await fetch(
    `${testServer.baseUrl}/api/v1/messages/${randomUUID()}/attachments`,
    {
      method: "POST",
      headers: authenticatedHeaders(registeredUser.accessToken),
      body: createUploadForm(
        "image/png",
        "oversized.png",
        new Uint8Array(MAXIMUM_ATTACHMENT_SIZE_BYTES + 1)
      )
    }
  );
  const responseBody = await readApiEnvelope<never>(response);

  assert.equal(response.status, 413);
  assert.equal(responseBody.error?.code, "ATTACHMENT_TOO_LARGE");
  assert.deepEqual((await readdir(uploadsDirectory)).sort(), filesBeforeUpload);
});

test("a failed attachment database write removes the uploaded file", async () => {
  const uploadsDirectory = path.dirname(
    resolveUploadPath("/uploads/directory-probe")
  );
  const filesBeforeUpload = (await readdir(uploadsDirectory)).sort();

  const response = await fetch(
    `${testServer.baseUrl}/api/v1/messages/${randomUUID()}/attachments`,
    {
      method: "POST",
      headers: authenticatedHeaders(registeredUser.accessToken),
      body: createUploadForm("image/png", "failed-write.png")
    }
  );

  assert.equal(response.status, 404);
  assert.deepEqual((await readdir(uploadsDirectory)).sort(), filesBeforeUpload);
});

test("the orphan cleanup job removes unreferenced files after its grace period", async () => {
  const orphanFileUrl = `/uploads/orphan-${randomUUID()}.png`;
  const orphanFilePath = resolveUploadPath(orphanFileUrl);
  const oldTimestamp = new Date(Date.now() - 2 * 60 * 60 * 1000);

  await writeFile(orphanFilePath, new Uint8Array([137, 80, 78, 71]));
  await utimes(orphanFilePath, oldTimestamp, oldTimestamp);

  const cleanupResult = await runAttachmentCleanup();

  assert.ok(cleanupResult.deletedFiles >= 1);
  await assert.rejects(access(orphanFilePath));
});
