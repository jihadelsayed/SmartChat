import OpenAI from "openai";
import {
  AI_MAXIMUM_RETRIES,
  AI_REQUEST_TIMEOUT_MS
} from "../ai.constants";
import {
  AiConfigurationError,
  AiEmptyResponseError,
  AiImageInputUnsupportedError
} from "../ai.errors";
import type { AiProviderRequest } from "../ai.types";
import type { AiProvider } from "./ai-provider.interface";
import { mapOpenAiError } from "./openai-error.mapper";

const SMARTCHAT_INSTRUCTIONS =
  "You are SmartChat, a concise and helpful AI assistant.";

type OpenAiClient = Pick<OpenAI, "responses">;

export class OpenAiProvider implements AiProvider {
  readonly supportsImages = true;
  private readonly client: OpenAiClient;

  constructor(
    private readonly apiKey: string,
    private readonly model: string,
    client?: OpenAiClient
  ) {
    if (!apiKey.trim() || !model.trim()) {
      throw new AiConfigurationError();
    }

    this.client =
      client ??
      new OpenAI({
        apiKey,
        timeout: AI_REQUEST_TIMEOUT_MS,
        maxRetries: AI_MAXIMUM_RETRIES
      });
  }

  async generateReply(input: AiProviderRequest): Promise<string> {
    try {
      const responseInput = input.images?.length
        ? input.messages.map((message, index) => {
            if (index !== input.messages.length - 1) {
              return message;
            }
            return {
              role: "user" as const,
              content: [
                {
                  type: "input_text" as const,
                  text: message.content || "Please analyze the attached image."
                },
                ...input.images!.map((image) => ({
                  type: "input_image" as const,
                  image_url: `data:${image.mimeType};base64,${image.dataBase64}`,
                  detail: "auto" as const
                }))
              ]
            };
          })
        : input.messages;
      const response = await this.client.responses.create({
        model: this.model,
        instructions: input.systemPrompt ?? SMARTCHAT_INSTRUCTIONS,
        input: responseInput
      });
      const generatedText = response.output_text.trim();

      if (!generatedText) {
        throw new AiEmptyResponseError();
      }

      return generatedText;
    } catch (error: unknown) {
      if (error instanceof AiEmptyResponseError) {
        throw error;
      }

      const mappedError = mapOpenAiError(error, this.apiKey);
      if (
        input.images?.length &&
        mappedError.code === "AI_PROVIDER_REQUEST_FAILED"
      ) {
        throw new AiImageInputUnsupportedError();
      }
      throw mappedError;
    }
  }
}
