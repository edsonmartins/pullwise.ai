import * as vscode from 'vscode';
import { Issue, SeverityLevel } from './types';

const SEVERITY_MAP: Record<string, vscode.DiagnosticSeverity> = {
  CRITICAL: vscode.DiagnosticSeverity.Error,
  HIGH: vscode.DiagnosticSeverity.Error,
  MEDIUM: vscode.DiagnosticSeverity.Warning,
  LOW: vscode.DiagnosticSeverity.Information,
  INFO: vscode.DiagnosticSeverity.Hint,
};

const SEVERITY_ORDER: Record<string, number> = {
  CRITICAL: 1,
  HIGH: 2,
  MEDIUM: 3,
  LOW: 4,
  INFO: 5,
};

export class DiagnosticsProvider implements vscode.Disposable {
  private diagnosticCollection: vscode.DiagnosticCollection;

  constructor() {
    this.diagnosticCollection =
      vscode.languages.createDiagnosticCollection('pullwise');
  }

  refresh(issues: Issue[]): void {
    this.diagnosticCollection.clear();

    // Apply severity filter from settings
    const config = vscode.workspace.getConfiguration('pullwise');
    const minSeverity = config.get<SeverityLevel>('severityFilter', 'low');
    const minOrder = SEVERITY_ORDER[minSeverity.toUpperCase()] || 4;

    const filteredIssues = issues.filter((issue) => {
      const order = SEVERITY_ORDER[issue.severity] || 5;
      return order <= minOrder;
    });

    // Group issues by file path
    const issuesByFile = new Map<string, Issue[]>();
    for (const issue of filteredIssues) {
      if (!issue.filePath) continue;

      const existing = issuesByFile.get(issue.filePath) || [];
      existing.push(issue);
      issuesByFile.set(issue.filePath, existing);
    }

    // Create diagnostics per file
    for (const [filePath, fileIssues] of issuesByFile) {
      const uri = this.resolveFileUri(filePath);
      if (!uri) continue;

      const diagnostics = fileIssues.map((issue) =>
        this.issueToDiagnostic(issue)
      );
      this.diagnosticCollection.set(uri, diagnostics);
    }
  }

  clear(): void {
    this.diagnosticCollection.clear();
  }

  dispose(): void {
    this.diagnosticCollection.dispose();
  }

  private issueToDiagnostic(issue: Issue): vscode.Diagnostic {
    const startLine = (issue.lineStart || 1) - 1;
    const endLine = (issue.lineEnd || issue.lineStart || 1) - 1;

    const range = new vscode.Range(startLine, 0, endLine, 999);
    const severity = SEVERITY_MAP[issue.severity] || vscode.DiagnosticSeverity.Information;

    let message = issue.title;
    if (issue.description) {
      message += `\n${issue.description}`;
    }
    if (issue.suggestion) {
      message += `\nSuggestion: ${issue.suggestion}`;
    }

    const diagnostic = new vscode.Diagnostic(range, message, severity);
    diagnostic.source = 'Pullwise';
    diagnostic.code = issue.ruleId || issue.type;

    return diagnostic;
  }

  private resolveFileUri(filePath: string): vscode.Uri | null {
    const workspaceFolders = vscode.workspace.workspaceFolders;
    if (!workspaceFolders || workspaceFolders.length === 0) return null;

    const workspaceRoot = workspaceFolders[0].uri;
    return vscode.Uri.joinPath(workspaceRoot, filePath);
  }
}
