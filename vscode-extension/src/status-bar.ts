import * as vscode from 'vscode';
import { Review } from './types';

export class StatusBarManager implements vscode.Disposable {
  private statusBarItem: vscode.StatusBarItem;

  constructor() {
    this.statusBarItem = vscode.window.createStatusBarItem(
      vscode.StatusBarAlignment.Left,
      100
    );
    this.statusBarItem.command = 'pullwise.showIssues';
    this.statusBarItem.tooltip = 'Pullwise Code Review - Click to show issues';
    this.statusBarItem.text = '$(shield) Pullwise';
  }

  update(review: Review): void {
    if (!review.stats) {
      this.statusBarItem.text = `$(shield) Pullwise: ${review.status}`;
      return;
    }

    const { criticalCount, highCount, mediumCount, lowCount } = review.stats;
    const parts: string[] = [];

    if (criticalCount > 0) parts.push(`${criticalCount}C`);
    if (highCount > 0) parts.push(`${highCount}H`);
    if (mediumCount > 0) parts.push(`${mediumCount}M`);
    if (lowCount > 0) parts.push(`${lowCount}L`);

    const issueText = parts.length > 0 ? parts.join(' ') : 'No issues';

    if (criticalCount > 0) {
      this.statusBarItem.backgroundColor = new vscode.ThemeColor(
        'statusBarItem.errorBackground'
      );
    } else if (highCount > 0) {
      this.statusBarItem.backgroundColor = new vscode.ThemeColor(
        'statusBarItem.warningBackground'
      );
    } else {
      this.statusBarItem.backgroundColor = undefined;
    }

    this.statusBarItem.text = `$(shield) Pullwise: ${issueText}`;
    this.statusBarItem.tooltip = [
      `Pullwise Code Review`,
      `PR #${review.prNumber}: ${review.prTitle}`,
      `Status: ${review.status}`,
      `Critical: ${criticalCount} | High: ${highCount} | Medium: ${mediumCount} | Low: ${lowCount}`,
    ].join('\n');
  }

  show(): void {
    this.statusBarItem.show();
  }

  hide(): void {
    this.statusBarItem.hide();
  }

  dispose(): void {
    this.statusBarItem.dispose();
  }
}
