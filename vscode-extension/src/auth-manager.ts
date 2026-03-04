import * as vscode from 'vscode';

const TOKEN_KEY = 'pullwise.token';

export class AuthManager {
  private secretStorage: vscode.SecretStorage;

  constructor(context: vscode.ExtensionContext) {
    this.secretStorage = context.secrets;
  }

  async getToken(): Promise<string | undefined> {
    return this.secretStorage.get(TOKEN_KEY);
  }

  async setToken(token: string): Promise<void> {
    await this.secretStorage.store(TOKEN_KEY, token);
  }

  async clearToken(): Promise<void> {
    await this.secretStorage.delete(TOKEN_KEY);
  }

  async isAuthenticated(): Promise<boolean> {
    const token = await this.getToken();
    return token !== undefined && token.length > 0;
  }
}
