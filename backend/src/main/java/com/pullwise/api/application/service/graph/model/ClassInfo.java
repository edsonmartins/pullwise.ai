package com.pullwise.api.application.service.graph.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Representação de uma classe analisada.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassInfo {
    private String qualifiedName;
    private String simpleName;
    private String packageName;
    private boolean isInterface;
    private boolean isEnum;
    private boolean isAbstract;
    private String superClass;
    private List<String> implementedInterfaces;
    private List<String> dependencies;     // Classes que esta classe depende
    private List<String> dependents;       // Classes que dependem desta
    private List<MethodInfo> methods;
    private List<String> fields;
    private int startLine;
    private int endLine;
}
