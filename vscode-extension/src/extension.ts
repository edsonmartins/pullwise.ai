import * as vscode from 'vscode';
import { AuthManager } from './auth-manager';
import { ApiClient } from './api-client';
import { DiagnosticsProvider } from './diagnostics';
import { StatusBarManager } from './status-bar';
import { registerCommands } from './commands';

let diagnosticsProvider: DiagnosticsProvider;
let statusBarManager: StatusBarManager;

export function activate(context: vscode.ExtensionContext) {
  const authManager = new AuthManager(context);
  const apiClient = new ApiClient(authManager);
  diagnosticsProvider = new DiagnosticsProvider();
  statusBarManager = new StatusBarManager();

  // Register all commands
  registerCommands(context, authManager, apiClient, diagnosticsProvider, statusBarManager);

  // Add disposables
  context.subscriptions.push(diagnosticsProvider);
  context.subscriptions.push(statusBarManager);

  // Auto-refresh on file save if enabled
  const config = vscode.workspace.getConfiguration('pullwise');
  if (config.get<boolean>('autoRefresh', false)) {
    context.subscriptions.push(
      vscode.workspace.onDidSaveTextDocument(async () => {
        const projectId = config.get<string>('projectId', '');
        if (projectId && (await authManager.isAuthenticated())) {
          try {
            const review = await apiClient.getLatestReview(projectId);
            if (review && review.status === 'completed') {
              const issues = await apiClient.getReviewIssues(review.id);
              diagnosticsProvider.refresh(issues);
              statusBarManager.update(review);
            }
          } catch {
            // Silent fail on auto-refresh
          }
        }
      })
    );
  }

  // Show status bar on activation
  statusBarManager.show();

  vscode.window.showInformationMessage('Pullwise Code Review activated');
}

export function deactivate() {
  if (diagnosticsProvider) {
    diagnosticsProvider.dispose();
  }
  if (statusBarManager) {
    statusBarManager.dispose();
  }
}
