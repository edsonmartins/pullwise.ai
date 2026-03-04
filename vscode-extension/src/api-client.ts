import axios, { AxiosInstance } from 'axios';
import * as vscode from 'vscode';
import { AuthManager } from './auth-manager';
import { Review, Issue, Project } from './types';

export class ApiClient {
  private client: AxiosInstance;
  private authManager: AuthManager;

  constructor(authManager: AuthManager) {
    this.authManager = authManager;

    const config = vscode.workspace.getConfiguration('pullwise');
    const baseURL = config.get<string>('apiUrl', 'http://localhost:8080/api');

    this.client = axios.create({
      baseURL,
      timeout: 30000,
      headers: { 'Content-Type': 'application/json' },
    });

    this.client.interceptors.request.use(async (reqConfig) => {
      const token = await this.authManager.getToken();
      if (token) {
        reqConfig.headers.Authorization = `Bearer ${token}`;
      }
      return reqConfig;
    });
  }

  async login(email: string, password: string): Promise<string> {
    const response = await this.client.post('/auth/login', { email, password });
    const token = response.data.token;
    await this.authManager.setToken(token);
    return token;
  }

  async getProjects(): Promise<Project[]> {
    const response = await this.client.get('/projects');
    return response.data.content || response.data;
  }

  async getReviews(projectId: string): Promise<Review[]> {
    const response = await this.client.get('/reviews', {
      params: { projectId },
    });
    return response.data.content || response.data;
  }

  async getLatestReview(projectId: string): Promise<Review | null> {
    const reviews = await this.getReviews(projectId);
    if (reviews.length === 0) return null;

    // Sort by createdAt descending
    reviews.sort(
      (a, b) =>
        new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
    );
    return reviews[0];
  }

  async getReviewIssues(reviewId: string): Promise<Issue[]> {
    const response = await this.client.get(`/reviews/${reviewId}/issues`);
    return response.data;
  }

  async triggerReview(
    projectId: string,
    prNumber: number
  ): Promise<Review> {
    const response = await this.client.post('/reviews', {
      pullRequestId: projectId,
      sastEnabled: true,
      llmEnabled: true,
      ragEnabled: false,
    });
    return response.data;
  }

  async markFalsePositive(issueId: string): Promise<void> {
    await this.client.post(`/reviews/issues/${issueId}/false-positive`);
  }

  async acknowledgeIssue(issueId: string): Promise<void> {
    await this.client.post(`/reviews/issues/${issueId}/acknowledge`);
  }
}
