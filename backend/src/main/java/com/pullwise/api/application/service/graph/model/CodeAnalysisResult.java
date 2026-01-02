package com.pullwise.api.application.service.graph.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Resultado da análise de código de um arquivo ou projeto.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeAnalysisResult {
    private String filePath;
    private String repositoryId;
    private LocalDateTime analyzedAt;

    // Estrutura do código
    private List<ClassInfo> classes;
    private int totalLines;
    private int linesOfCode;

    // Métricas de dependência
    private int fanIn;   // Quantas classes dependem desta
    private int fanOut;  // Quantas classes esta depende

    // Instabilidade (0 = estável, 1 = instável)
    private double instability;

    // Métricas de complexidade
    private int totalComplexity;
    private double averageComplexity;

    // Lista de todos os métodos
    private List<MethodInfo> allMethods;
}
