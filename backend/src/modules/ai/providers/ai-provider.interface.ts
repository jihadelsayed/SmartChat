import type { AiProviderRequest } from "../ai.types";

export interface AiProvider {
  readonly supportsImages?: boolean;
  generateReply(input: AiProviderRequest): Promise<string>;
}
