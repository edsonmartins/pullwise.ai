package com.pullwise.api.application.service.graph.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Representação de um método analisado.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MethodInfo {
    private String name;
    private String returnType;
    private List<String> parameterTypes;
    private int startLine;
    private int endLine;
    private boolean isStatic;
    private boolean isPublic;
    private boolean isOverride;
    private List<String> calledMethods;  // Métodos que este método chama
    private List<String> calledClasses;  // Classes que este método usa
    private int complexity;              // Complexidade ciclomática
}
