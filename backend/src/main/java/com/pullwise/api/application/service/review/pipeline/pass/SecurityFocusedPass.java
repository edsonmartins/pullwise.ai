package com.pullwise.api.application.service.review.pipeline.pass;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pullwise.api.application.service.integration.GitHubService;
import com.pullwise.api.application.service.llm.router.MultiModelLLMRouter;
import com.pullwise.api.domain.model.Issue;
import com.pullwise.api.domain.model.PullRequest;
import com.pullwise.api.domain.model.Review;
import com.pullwise.api.domain.enums.*;
import com.pullwise.api.application.service.review.pipeline.MultiPassReviewOrchestrator.PassResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Passada 3: Security-Focused Analysis
 *
 * <p>Análise especializada em segurança, usando Claude 3.5 Sonnet
 * (modelo com melhor performance em segurança).
 *
 * <p>Focus em OWASP Top 10:
 * <ol>
 *   <li>Broken Access Control</li>
 *   <li>Cryptographic Failures</li>
 *   <li>Injection (SQL, NoSQL, OS, LDAP)</li>
 *   <li>Insecure Design</li>
 *   <li>Security Misconfiguration</li>
 *   <li>Supply Chain Vulnerabilities</li>
 *   <li>Authentication Failures</li>
 *   <li>Software and Data Integrity Failures</li>
 *   <li>Logging and Monitoring Failures</li>
 *   <li>Server-Side Request Forgery (SSRF)</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityFocusedPass {

    private final MultiModelLLMRouter llmRouter;
    private final ObjectMapper objectMapper;

    /**
     * Executa a análise focada em segurança.
     *
     * @param pullRequest  O PR a ser analisado
     * @param review       O review associado
     * @param sastResult   Resultados do SAST
     * @param llmResult    Resultados da análise LLM primária
     * @return PassResult com issues de segurança
     */
    public PassResult execute(PullRequest pullRequest, Review review,
                             PassResult sastResult, PassResult llmResult,
                             List<GitHubService.FileDiff> diffs) {
        long startTime = System.currentTimeMillis();

        String repoIdentifier = pullRequest.getProject() != null
                ? pullRequest.getProject().getName()
                : "unknown";
        log.debug("Starting Security-focused analysis for PR {}/{}", repoIdentifier, pullRequest.getPrNumber());

        List<Issue> issues = new ArrayList<>();

        try {
            // Coletar issues de segurança já encontrados
            List<Issue> existingSecurityIssues = collectSecurityIssues(sastResult, llmResult);

            // Encontrar novas vulnerabilidades via LLM
            List<Issue> newSecurityIssues = findSecurityVulnerabilities(pullRequest, review, existingSecurityIssues, diffs);
            issues.addAll(newSecurityIssues);

            log.debug("Security pass completed: {} security issues found", issues.size());

        } catch (Exception e) {
            log.warn("Security analysis encountered errors", e);
        }

        PassResult result = new PassResult();
        result.setPassName("Security-Focused Analysis");
        result.setSuccess(true);
        result.setIssues(issues);
        result.setDurationMs(System.currentTimeMillis() - startTime);

        // Metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("modelUsed", "claude-3.5-sonnet (recommended)");
        metadata.put("owaspBreakdown", issues.size());

        result.setMetadata(metadata);

        return result;
    }

    /**
     * Coleta issues de segurança das passadas anteriores.
     */
    private List<Issue> collectSecurityIssues(PassResult sastResult, PassResult llmResult) {
        List<Issue> securityIssues = new ArrayList<>();

        if (sastResult != null && sastResult.getIssues() != null) {
            securityIssues.addAll(sastResult.getIssues().stream()
                    .filter(i -> i.getType() == IssueType.SECURITY)
                    .toList());
        }

        if (llmResult != null && llmResult.getIssues() != null) {
            securityIssues.addAll(llmResult.getIssues().stream()
                    .filter(i -> i.getType() == IssueType.SECURITY)
                    .toList());
        }

        return securityIssues;
    }

    /**
     * Busca vulnerabilidades de segurança usando LLM especializado.
     */
    private List<Issue> findSecurityVulnerabilities(PullRequest pullRequest, Review review,
                                                    List<Issue> existingIssues,
                                                    List<GitHubService.FileDiff> diffs) {
        List<Issue> issues = new ArrayList<>();

        try {
            // Construir prompt focado em segurança
            String systemPrompt = buildSecurityPrompt();
            String userPrompt = buildSecurityAnalysisPrompt(pullRequest, existingIssues, diffs);

            // Executar com modelo especializado em segurança
            var response = llmRouter.execute(
                    ReviewTaskType.SECURITY_ANALYSIS,
                    systemPrompt,
                    userPrompt
            );

            // Parse resposta
            issues = parseSecurityResponse(response.content(), pullRequest, review);

        } catch (Exception e) {
            log.warn("Security LLM analysis failed: {}", e.getMessage());
        }

        return issues;
    }

    /**
     * Constrói prompt especializado em segurança.
     */
    private String buildSecurityPrompt() {
        return """
            You are a cybersecurity expert specializing in code security review.
            Analyze the provided code for security vulnerabilities following OWASP Top 10.

            Focus on:
            1. **Injection**: SQL, NoSQL, OS command, LDAP injection
            2. **Broken Authentication**: Session management, password handling
            3. **XSS**: Cross-site scripting vulnerabilities
            4. **CSRF**: Cross-site request forgery
            5. **SSRF**: Server-side request forgery
            6. **Insecure Deserialization**
            7. **Cryptographic Issues**: Weak algorithms, hardcoded secrets
            8. **Access Control**: Authorization bypasses
            9. **Security Misconfiguration**: Debug modes, verbose errors
            10. **Supply Chain**: Vulnerable dependencies

            Format your response as JSON:
            ```json
            {
              "issues": [
                {
                  "title": "SQL Injection vulnerability",
                  "description": "Detailed explanation of the vulnerability and exploit scenario",
                  "severity": "CRITICAL|HIGH|MEDIUM|LOW",
                  "owasp": "A03:2021-Injection",
                  "line": 123,
                  "recommendation": "How to fix"
                }
              ]
            }
            ```
            """;
    }

    /**
     * Constrói prompt de análise de segurança.
     */
    private String buildSecurityAnalysisPrompt(PullRequest pullRequest, List<Issue> existingIssues,
                                               List<GitHubService.FileDiff> diffs) {
        StringBuilder sb = new StringBuilder();
        sb.append("Perform a security review of this pull request.\n\n");

        String repoIdentifier = pullRequest.getProject() != null
                ? pullRequest.getProject().getName()
                : "unknown";
        sb.append("**Repository**: ").append(repoIdentifier).append("\n");
        sb.append("**PR Title**: ").append(pullRequest.getTitle()).append("\n");

        if (!existingIssues.isEmpty()) {
            sb.append("\n**Already identified security issues**:\n");
            for (Issue issue : existingIssues) {
                sb.append("- ").append(issue.getTitle())
                        .append(" (").append(issue.getSeverity()).append(")\n");
            }
        }

        sb.append("\n**Changed files**:\n");
        for (GitHubService.FileDiff diff : diffs) {
            if (diff.patch() != null && !diff.patch().isBlank()) {
                sb.append("\n**File**: ").append(diff.filename()).append("\n");
                sb.append("```diff\n");
                sb.append(diff.patch());
                sb.append("\n```\n");
            }
        }

        return sb.toString();
    }

    /**
     * Parse resposta LLM de segurança.
     */
    private List<Issue> parseSecurityResponse(String response, PullRequest pullRequest, Review review) {
        List<Issue> issues = new ArrayList<>();

        try {
            String jsonBlock = extractJsonBlock(response);
            if (jsonBlock == null || jsonBlock.length() < 10) {
                return issues;
            }

            // Tentar parse JSON estruturado
            SecurityIssueResponse parsed = null;
            try {
                parsed = objectMapper.readValue(jsonBlock, SecurityIssueResponse.class);
            } catch (Exception e) {
                try {
                    SecurityIssue[] issueArray = objectMapper.readValue(jsonBlock, SecurityIssue[].class);
                    parsed = new SecurityIssueResponse(List.of(issueArray));
                } catch (Exception e2) {
                    log.debug("JSON parse failed for security response, falling back to keyword matching: {}", e2.getMessage());
                }
            }

            if (parsed != null && parsed.issues() != null && !parsed.issues().isEmpty()) {
                for (SecurityIssue secIssue : parsed.issues()) {
                    Severity severity = parseSeverity(secIssue.severity());
                    String filePath = secIssue.file() != null ? secIssue.file() : pullRequest.getTargetBranch();

                    issues.add(Issue.builder()
                            .review(review)
                            .type(IssueType.SECURITY)
                            .severity(severity)
                            .title(secIssue.title() != null ? secIssue.title() : "Security Issue")
                            .description(buildSecurityDescription(secIssue))
                            .filePath(filePath)
                            .lineStart(secIssue.line() != null ? secIssue.line() : 1)
                            .lineEnd(secIssue.line() != null ? secIssue.line() : 1)
                            .ruleId(secIssue.owasp() != null ? secIssue.owasp() : "SECURITY")
                            .suggestion(secIssue.recommendation())
                            .source(IssueSource.LLM)
                            .createdAt(LocalDateTime.now())
                            .build());
                }
            } else {
                // Fallback: keyword matching para respostas não-JSON
                issues.addAll(keywordFallbackParse(response, pullRequest, review));
            }

        } catch (Exception e) {
            log.debug("Failed to parse security response: {}", e.getMessage());
        }

        return issues;
    }

    private String buildSecurityDescription(SecurityIssue issue) {
        StringBuilder sb = new StringBuilder();
        if (issue.description() != null) {
            sb.append(issue.description());
        }
        if (issue.owasp() != null) {
            sb.append("\n\n**OWASP**: ").append(issue.owasp());
        }
        return sb.toString();
    }

    private Severity parseSeverity(String severity) {
        if (severity == null) return Severity.HIGH;
        try {
            return Severity.valueOf(severity.toUpperCase());
        } catch (Exception e) {
            return Severity.HIGH;
        }
    }

    /**
     * Fallback: detecção por keywords quando parse JSON falha.
     */
    private List<Issue> keywordFallbackParse(String response, PullRequest pullRequest, Review review) {
        List<Issue> issues = new ArrayList<>();
        String lower = response.toLowerCase();

        if (lower.contains("sql injection")) {
            issues.add(createSecurityIssue("SQL Injection Vulnerability",
                    "User input appears to be directly concatenated into SQL query without proper parameterization.",
                    Severity.CRITICAL, "A03:2021-Injection", pullRequest, review));
        }
        if (lower.contains("xss") || lower.contains("cross-site scripting")) {
            issues.add(createSecurityIssue("Cross-Site Scripting (XSS)",
                    "User input may be rendered without proper sanitization.",
                    Severity.HIGH, "A03:2021-XSS", pullRequest, review));
        }
        if (lower.contains("hardcoded") && lower.contains("secret")) {
            issues.add(createSecurityIssue("Hardcoded Secrets",
                    "Secrets or credentials appear to be hardcoded in source code.",
                    Severity.CRITICAL, "A07:2021-Identification", pullRequest, review));
        }

        return issues;
    }

    // Records for JSON deserialization
    private record SecurityIssueResponse(List<SecurityIssue> issues) {}
    private record SecurityIssue(String title, String description, String severity,
                                 String owasp, Integer line, String recommendation, String file) {}

    /**
     * Cria um issue de segurança.
     */
    private Issue createSecurityIssue(String title, String description, Severity severity,
                                     String owaspCategory, PullRequest pullRequest, Review review) {
        return Issue.builder()
                .review(review)
                .type(IssueType.SECURITY)
                .severity(severity)
                .title(title)
                .description(description)
                .filePath(pullRequest.getTargetBranch())
                .lineStart(1)
                .lineEnd(1)
                .ruleId(owaspCategory)
                .source(IssueSource.LLM)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Extrai categoria OWASP.
     */
    private String extractCategory(String owaspCategory) {
        if (owaspCategory == null) return "security";
        if (owaspCategory.contains("Injection")) return "injection";
        if (owaspCategory.contains("XSS")) return "xss";
        if (owaspCategory.contains("Auth")) return "authentication";
        return "security";
    }

    /**
     * Extrai bloco JSON de resposta.
     */
    private String extractJsonBlock(String response) {
        int jsonStart = response.indexOf("```json");
        if (jsonStart == -1) {
            jsonStart = response.indexOf("```");
        }
        if (jsonStart == -1) return null;

        int contentStart = response.indexOf("\n", jsonStart) + 1;
        int jsonEnd = response.indexOf("```", contentStart);

        if (jsonEnd == -1) return response.substring(contentStart);
        return response.substring(contentStart, jsonEnd).trim();
    }
}
