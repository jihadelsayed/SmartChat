export interface AiProvider {
  generateReply(input: { message: string }): Promise<string>;
}
