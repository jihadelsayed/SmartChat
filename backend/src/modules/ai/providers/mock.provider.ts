import type { AiProviderRequest } from "../ai.types";
import type { AiProvider } from "./ai-provider.interface";

export class MockAiProvider implements AiProvider {
  readonly supportsImages = true;

  async generateReply(input: AiProviderRequest): Promise<string> {
    return `Test assistant response for: "${input.message}"`;
  }
}
