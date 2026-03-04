import * as vscode from 'vscode';
import { AuthManager } from './auth-manager';
import { ApiClient } from './api-client';
import { DiagnosticsProvider } from './diagnostics';
import { StatusBarManager } from './status-bar';

export function registerCommands(
  context: vscode.ExtensionContext,
  authManager: AuthManager,
  apiClient: ApiClient,
  diagnosticsProvider: DiagnosticsProvider,
  statusBarManager: StatusBarManager
): void {
  context.subscriptions.push(
    vscode.commands.registerCommand('pullwise.login', () =>
      loginCommand(authManager, apiClient)
    ),
    vscode.commands.registerCommand('pullwise.logout', () =>
      logoutCommand(authManager, statusBarManager, diagnosticsProvider)
    ),
    vscode.commands.registerCommand('pullwise.triggerReview', () =>
      triggerReviewCommand(apiClient, diagnosticsProvider, statusBarManager)
    ),
    vscode.commands.registerCommand('pullwise.showIssues', () =>
      showIssuesCommand(apiClient, diagnosticsProvider, statusBarManager)
    ),
    vscode.commands.registerCommand('pullwise.installHooks', () =>
      installHooksCommand()
    ),
    vscode.commands.registerCommand('pullwise.clearDiagnostics', () => {
      diagnosticsProvider.clear();
      vscode.window.showInformationMessage('Pullwise diagnostics cleared');
    })
  );
}

async function loginCommand(
  authManager: AuthManager,
  apiClient: ApiClient
): Promise<void> {
  const email = await vscode.window.showInputBox({
    prompt: 'Enter your Pullwise email',
    placeHolder: 'user@example.com',
    ignoreFocusOut: true,
  });

  if (!email) return;

  const password = await vscode.window.showInputBox({
    prompt: 'Enter your Pullwise password',
    password: true,
    ignoreFocusOut: true,
  });

  if (!password) return;

  try {
    await apiClient.login(email, password);
    vscode.window.showInformationMessage('Successfully logged in to Pullwise');
  } catch (error: any) {
    vscode.window.showErrorMessage(
      `Login failed: ${error.response?.data?.message || error.message}`
    );
  }
}

async function logoutCommand(
  authManager: AuthManager,
  statusBarManager: StatusBarManager,
  diagnosticsProvider: DiagnosticsProvider
): Promise<void> {
  await authManager.clearToken();
  diagnosticsProvider.clear();
  statusBarManager.show();
  vscode.window.showInformationMessage('Logged out of Pullwise');
}

async function triggerReviewCommand(
  apiClient: ApiClient,
  diagnosticsProvider: DiagnosticsProvider,
  statusBarManager: StatusBarManager
): Promise<void> {
  const config = vscode.workspace.getConfiguration('pullwise');
  const projectId = config.get<string>('projectId', '');

  if (!projectId) {
    vscode.window.showWarningMessage(
      'Please set pullwise.projectId in settings first'
    );
    return;
  }

  // Try to detect PR number from current branch
  const prNumber = await detectPrNumber();
  if (!prNumber) {
    vscode.window.showWarningMessage(
      'Could not detect PR number from current branch. Make sure you have a PR open.'
    );
    return;
  }

  await vscode.window.withProgress(
    {
      location: vscode.ProgressLocation.Notification,
      title: 'Pullwise: Triggering review...',
      cancellable: false,
    },
    async (progress) => {
      try {
        const review = await apiClient.triggerReview(projectId, prNumber);
        progress.report({ message: 'Review started, fetching results...' });

        // Poll for completion (max 5 minutes)
        const maxAttempts = 60;
        for (let i = 0; i < maxAttempts; i++) {
          await sleep(5000);

          const reviews = await apiClient.getReviews(projectId);
          const current = reviews.find((r) => r.id === review.id);
          if (!current) continue;

          if (current.status === 'completed') {
            const issues = await apiClient.getReviewIssues(current.id);
            diagnosticsProvider.refresh(issues);
            statusBarManager.update(current);
            vscode.window.showInformationMessage(
              `Pullwise review completed: ${issues.length} issues found`
            );
            return;
          }

          if (current.status === 'failed') {
            vscode.window.showErrorMessage('Pullwise review failed');
            return;
          }

          progress.report({
            message: `Review in progress... (${i + 1}/${maxAttempts})`,
          });
        }

        vscode.window.showWarningMessage('Review timed out. Check later with "Show Issues".');
      } catch (error: any) {
        vscode.window.showErrorMessage(
          `Failed to trigger review: ${error.response?.data?.message || error.message}`
        );
      }
    }
  );
}

async function showIssuesCommand(
  apiClient: ApiClient,
  diagnosticsProvider: DiagnosticsProvider,
  statusBarManager: StatusBarManager
): Promise<void> {
  const config = vscode.workspace.getConfiguration('pullwise');
  const projectId = config.get<string>('projectId', '');

  if (!projectId) {
    vscode.window.showWarningMessage(
      'Please set pullwise.projectId in settings first'
    );
    return;
  }

  try {
    const review = await apiClient.getLatestReview(projectId);
    if (!review) {
      vscode.window.showInformationMessage('No reviews found for this project');
      return;
    }

    if (review.status !== 'completed') {
      vscode.window.showInformationMessage(
        `Latest review is ${review.status}. Waiting for completion.`
      );
      return;
    }

    const issues = await apiClient.getReviewIssues(review.id);
    diagnosticsProvider.refresh(issues);
    statusBarManager.update(review);

    vscode.window.showInformationMessage(
      `Pullwise: ${issues.length} issues loaded from review of PR #${review.prNumber}`
    );

    // Focus the Problems panel
    vscode.commands.executeCommand('workbench.actions.view.problems');
  } catch (error: any) {
    vscode.window.showErrorMessage(
      `Failed to fetch issues: ${error.response?.data?.message || error.message}`
    );
  }
}

async function installHooksCommand(): Promise<void> {
  const terminal = vscode.window.createTerminal('Pullwise');
  terminal.show();
  terminal.sendText('pullwise hooks install');
}

async function detectPrNumber(): Promise<number | null> {
  try {
    const gitExtension = vscode.extensions.getExtension('vscode.git');
    if (!gitExtension) return null;

    const git = gitExtension.exports.getAPI(1);
    if (!git || git.repositories.length === 0) return null;

    const repo = git.repositories[0];
    const branch = repo.state.HEAD?.name;

    if (!branch) return null;

    // Common patterns: feature/123, fix/PR-123, 123-feature-name
    const match = branch.match(/(\d+)/);
    return match ? parseInt(match[1], 10) : null;
  } catch {
    return null;
  }
}

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}
