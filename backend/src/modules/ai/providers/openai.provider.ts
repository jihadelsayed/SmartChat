import { AI_REQUEST_TIMEOUT_MS } from "../ai.constants";
import type { AiProvider } from "./ai-provider.interface";

interface OpenAiTextContent {
  type?: string;
  text?: string;
}

interface OpenAiOutputItem {
  type?: string;
  content?: OpenAiTextContent[];
}

interface OpenAiErrorResponse {
  error?: {
    message?: string;
  };
}

interface OpenAiResponse extends OpenAiErrorResponse {
  output_text?: string;
  output?: OpenAiOutputItem[];
}

function extractOutputText(response: OpenAiResponse): string | null {
  if (
    typeof response.output_text === "string" &&
    response.output_text.trim()
  ) {
    return response.output_text.trim();
  }

  for (const outputItem of response.output ?? []) {
    for (const contentItem of outputItem.content ?? []) {
      if (
        contentItem.type === "output_text" &&
        typeof contentItem.text === "string" &&
        contentItem.text.trim()
      ) {
        return contentItem.text.trim();
      }
    }
  }

  return null;
}

export class OpenAiProvider implements AiProvider {
  private readonly apiKey: string | undefined;
  private readonly model: string;

  constructor(
    apiKey = process.env.OPENAI_API_KEY,
    model = process.env.OPENAI_MODEL ?? "gpt-5.6"
  ) {
    this.apiKey = apiKey;
    this.model = model;
  }

  async generateReply(input: { message: string }): Promise<string> {
    if (!this.apiKey) {
      throw new Error(
        "OPENAI_API_KEY is not configured. Use the mock provider or add the key to backend/.env."
      );
    }

    const abortController = new AbortController();

    const timeout = setTimeout(() => {
      abortController.abort();
    }, AI_REQUEST_TIMEOUT_MS);

    try {
      const response = await fetch(
        "https://api.openai.com/v1/responses",
        {
          method: "POST",
          headers: {
            Authorization: `Bearer ${this.apiKey}`,
            "Content-Type": "application/json"
          },
          body: JSON.stringify({
            model: this.model,
            instructions:
              "You are SmartChat, a concise and helpful AI assistant.",
            input: input.message
          }),
          signal: abortController.signal
        }
      );

      const responseBody =
        (await response.json()) as OpenAiResponse;

      if (!response.ok) {
        throw new Error(
          responseBody.error?.message ??
            `OpenAI request failed with status ${response.status}`
        );
      }

      const generatedText = extractOutputText(responseBody);

      if (!generatedText) {
        throw new Error(
          "OpenAI returned a response without generated text"
        );
      }

      return generatedText;
    } catch (error: unknown) {
      if (
        error instanceof Error &&
        error.name === "AbortError"
      ) {
        throw new Error(
          `OpenAI request timed out after ${AI_REQUEST_TIMEOUT_MS} ms`
        );
      }

      throw error;
    } finally {
      clearTimeout(timeout);
    }
  }
}