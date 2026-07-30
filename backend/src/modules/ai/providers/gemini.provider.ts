import { AI_REQUEST_TIMEOUT_MS } from "../ai.constants";
import type { AiProviderRequest } from "../ai.types";
import type { AiProvider } from "./ai-provider.interface";

interface GeminiResponse {
  candidates?: Array<{
    content?: {
      parts?: Array<{
        text?: string;
      }>;
    };
  }>;
  error?: {
    message?: string;
  };
}

export class GeminiProvider implements AiProvider {
  private readonly apiKey: string | undefined;
  private readonly model: string;

  constructor(
    apiKey = process.env.GEMINI_API_KEY,
    model = process.env.GEMINI_MODEL ?? "gemini-2.5-flash"
  ) {
    this.apiKey = apiKey;
    this.model = model;
  }

  async generateReply(input: AiProviderRequest): Promise<string> {
    if (!this.apiKey) {
      throw new Error(
        "GEMINI_API_KEY is not configured. Add it to backend/.env or switch back to the mock provider."
      );
    }

    const abortController = new AbortController();
    const timeout = setTimeout(() => abortController.abort(), AI_REQUEST_TIMEOUT_MS);

    try {
      const response = await fetch(
        `https://generativelanguage.googleapis.com/v1beta/models/${this.model}:generateContent?key=${this.apiKey}`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json"
          },
          body: JSON.stringify({
            contents: [
              {
                parts: [{ text: input.message }]
              }
            ],
            generationConfig: {
              temperature: 0.7
            }
          }),
          signal: abortController.signal
        }
      );

      const responseBody = (await response.json()) as GeminiResponse;

      if (!response.ok) {
        throw new Error(responseBody.error?.message ?? `Gemini request failed with status ${response.status}`);
      }

      const generatedText = responseBody.candidates?.[0]?.content?.parts?.[0]?.text?.trim();
      if (!generatedText) {
        throw new Error("Gemini returned a response without generated text");
      }

      return generatedText;
    } catch (error: unknown) {
      if (error instanceof Error && error.name === "AbortError") {
        throw new Error(`Gemini request timed out after ${AI_REQUEST_TIMEOUT_MS} ms`);
      }
      throw error;
    } finally {
      clearTimeout(timeout);
    }
  }
}
