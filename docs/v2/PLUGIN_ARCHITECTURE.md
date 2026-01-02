# Arquitetura de Plugins - Pullwise.ai SaaS

## 📋 Visão Geral

A arquitetura de plugins permite que desenvolvedores estendam as capacidades do Pullwise.ai criando análises customizadas em **Java**, **TypeScript** ou **Python**. Esta documentação cobre:

1. **Conceitos Fundamentais** - Como os plugins funcionam
2. **Plugin API** - Interfaces e contratos
3. **Criando Plugins Java** - Via SPI (Service Provider Interface)
4. **Criando Plugins TypeScript** - Via Node.js subprocess
5. **Criando Plugins Python** - Via Jep ou subprocess
6. **Distribuição e Marketplace** - Como publicar plugins
7. **Exemplos Práticos** - Templates prontos para uso

---

## 🎯 Conceitos Fundamentais

### Por que Plugins?

Plugins permitem:
- ✅ **Extensibilidade** - Adicionar ferramentas específicas sem modificar o core
- ✅ **Comunidade** - Desenvolvedores contribuem com análises especializadas
- ✅ **Customização** - Empresas criam regras internas
- ✅ **Flexibilidade** - Suporte a múltiplas linguagens de plugin

### Tipos de Plugins

```yaml
Tipos Suportados:
  SAST:
    - Análise estática de código
    - Exemplo: Análise de SQL injection customizada
    
  LINTER:
    - Code style e formatação
    - Exemplo: Regras de naming específicas da empresa
    
  SECURITY:
    - Vulnerabilidades e segurança
    - Exemplo: Detecção de secrets customizados
    
  PERFORMANCE:
    - Issues de performance
    - Exemplo: Análise de queries N+1
    
  CUSTOM_LLM:
    - Análise com LLM customizado
    - Exemplo: Usar modelo fine-tuned interno
    
  INTEGRATION:
    - Integrações externas
    - Exemplo: Enviar métricas para Datadog
```

### Arquitetura de Execução

```
┌─────────────────────────────────────────────────────────┐
│                    Plugin Lifecycle                      │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  1. DISCOVERY                                           │
│     ├─ Java: Scan classpath via SPI                    │
│     ├─ TypeScript: Scan /plugins/typescript/*/         │
│     └─ Python: Scan /plugins/python/*/                 │
│                                                          │
│  2. INITIALIZATION                                      │
│     ├─ Load plugin metadata                            │
│     ├─ Validate configuration schema                   │
│     ├─ Setup plugin context                            │
│     └─ Check dependencies                              │
│                                                          │
│  3. EXECUTION                                           │
│     ├─ Filter by supported languages                   │
│     ├─ Pass AnalysisRequest                            │
│     ├─ Execute in appropriate runtime                  │
│     │   ├─ Java: Direct method call                    │
│     │   ├─ TypeScript: Node.js subprocess              │
│     │   └─ Python: Jep or subprocess                   │
│     └─ Collect AnalysisResult                          │
│                                                          │
│  4. AGGREGATION                                         │
│     ├─ Merge results from all plugins                  │
│     ├─ Deduplicate issues                              │
│     └─ Prioritize by confidence                        │
│                                                          │
│  5. CLEANUP                                             │
│     └─ Call plugin.shutdown()                          │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

---

## 📚 Plugin API

### Interface Base (Java)

```java
package com.integralltech.codereview.plugin.api;

public interface CodeReviewPlugin {
    // Identificação
    String getId();
    String getName();
    String getVersion();
    String getAuthor();
    String getDescription();
    
    // Capabilities
    Set<Language> getSupportedLanguages();
    PluginType getType();
    
    // Lifecycle
    void initialize(PluginContext context) throws PluginException;
    AnalysisResult analyze(AnalysisRequest request) throws PluginException;
    void shutdown();
    
    // Metadata
    PluginMetadata getMetadata();
}
```

### Data Transfer Objects

```java
// Request
public class AnalysisRequest {
    private String diff;                    // Git diff completo
    private List<String> changedFiles;      // Arquivos modificados
    private Repository repository;          // Info do repositório
    private PullRequest pullRequest;        // Contexto do PR
    private Map<String, Object> config;     // Configuração do plugin
    
    // Helper methods
    public String getFileContent(String path);
    public List<String> getFilesOfType(Language language);
}

// Result
public class AnalysisResult {
    private List<Issue> issues;             // Issues encontrados
    private Map<String, Object> metadata;   // Metadados adicionais
    private Duration executionTime;         // Tempo de execução
    private boolean success;                // Se executou com sucesso
    private String errorMessage;            // Mensagem de erro se falhou
}

// Issue
public class Issue {
    private String id;
    private IssueType type;                 // BUG, SECURITY, STYLE, etc
    private Severity severity;              // CRITICAL, HIGH, MEDIUM, LOW
    private String title;
    private String description;
    private String filePath;
    private Integer lineStart;
    private Integer lineEnd;
    private String code;                    // Código identificador
    private String suggestedFix;            // Fix sugerido (opcional)
    private double confidence;              // 0.0 - 1.0
}
```

---

## ☕ Criando Plugins Java

### 1. Estrutura do Projeto

```
my-custom-plugin/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/company/plugins/
│   │   │       ├── MyCustomPlugin.java
│   │   │       └── analyzers/
│   │   │           └── CustomAnalyzer.java
│   │   └── resources/
│   │       └── META-INF/
│   │           └── services/
│   │               └── com.integralltech.codereview.plugin.api.CodeReviewPlugin
│   └── test/
│       └── java/
└── README.md
```

### 2. Maven Dependencies

```xml
<project>
    <modelVersion>4.0.0</modelVersion>
    
    <groupId>com.company</groupId>
    <artifactId>my-custom-plugin</artifactId>
    <version>1.0.0</version>
    
    <dependencies>
        <!-- Plugin API -->
        <dependency>
            <groupId>com.integralltech</groupId>
            <artifactId>codereview-plugin-api</artifactId>
            <version>2.0.0</version>
            <scope>provided</scope>
        </dependency>
        
        <!-- Ferramentas de análise (opcional) -->
        <dependency>
            <groupId>com.github.javaparser</groupId>
            <artifactId>javaparser-core</artifactId>
            <version>3.25.7</version>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <version>3.5.1</version>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals>
                            <goal>shade</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

### 3. Implementação do Plugin

```java
package com.company.plugins;

import com.integralltech.codereview.plugin.api.*;
import java.util.*;

public class MyCustomPlugin implements CodeReviewPlugin {
    
    private PluginContext context;
    
    @Override
    public String getId() {
        return "my-custom-plugin";
    }
    
    @Override
    public String getName() {
        return "My Custom Plugin";
    }
    
    @Override
    public String getVersion() {
        return "1.0.0";
    }
    
    @Override
    public String getAuthor() {
        return "Your Company";
    }
    
    @Override
    public String getDescription() {
        return "Detects custom business logic issues specific to our company";
    }
    
    @Override
    public Set<Language> getSupportedLanguages() {
        return Set.of(Language.JAVA, Language.KOTLIN);
    }
    
    @Override
    public PluginType getType() {
        return PluginType.CUSTOM;
    }
    
    @Override
    public void initialize(PluginContext context) throws PluginException {
        this.context = context;
        
        // Carregar configuração
        Map<String, Object> config = context.getConfiguration();
        
        // Inicializar recursos
        // Ex: conectar com banco de dados, carregar regras, etc
    }
    
    @Override
    public AnalysisResult analyze(AnalysisRequest request) throws PluginException {
        List<Issue> issues = new ArrayList<>();
        
        try {
            // Filtrar apenas arquivos Java
            List<String> javaFiles = request.getFilesOfType(Language.JAVA);
            
            for (String filePath : javaFiles) {
                String content = request.getFileContent(filePath);
                
                // Sua lógica de análise aqui
                issues.addAll(analyzeFile(filePath, content));
            }
            
            return AnalysisResult.builder()
                .issues(issues)
                .success(true)
                .metadata(Map.of(
                    "filesAnalyzed", javaFiles.size(),
                    "rulesApplied", getRuleCount()
                ))
                .build();
                
        } catch (Exception e) {
            return AnalysisResult.builder()
                .success(false)
                .errorMessage(e.getMessage())
                .build();
        }
    }
    
    private List<Issue> analyzeFile(String filePath, String content) {
        List<Issue> issues = new ArrayList<>();
        
        // Exemplo: Detectar uso de Date ao invés de LocalDateTime
        if (content.contains("import java.util.Date")) {
            issues.add(Issue.builder()
                .type(IssueType.CODE_SMELL)
                .severity(Severity.MEDIUM)
                .title("Avoid using java.util.Date")
                .description("Use java.time.LocalDateTime instead of legacy Date class")
                .filePath(filePath)
                .lineStart(findLineNumber(content, "import java.util.Date"))
                .confidence(0.95)
                .suggestedFix("import java.time.LocalDateTime;")
                .build());
        }
        
        // Exemplo: Detectar logger não-SLF4J
        if (content.contains("System.out.println")) {
            issues.add(Issue.builder()
                .type(IssueType.CODE_SMELL)
                .severity(Severity.LOW)
                .title("Use proper logging framework")
                .description("Replace System.out with SLF4J logger")
                .filePath(filePath)
                .confidence(0.90)
                .build());
        }
        
        return issues;
    }
    
    @Override
    public void shutdown() {
        // Cleanup de recursos
    }
    
    @Override
    public PluginMetadata getMetadata() {
        return PluginMetadata.builder()
            .configSchema(getConfigurationSchema())
            .website("https://github.com/company/my-custom-plugin")
            .license("MIT")
            .tags(List.of("java", "best-practices", "company-specific"))
            .build();
    }
    
    private Map<String, Object> getConfigurationSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "strictMode", Map.of(
                    "type", "boolean",
                    "default", false,
                    "description", "Enable strict checking mode"
                ),
                "excludedPackages", Map.of(
                    "type", "array",
                    "items", Map.of("type", "string"),
                    "default", List.of(),
                    "description", "Packages to exclude from analysis"
                )
            )
        );
    }
}
```

### 4. Registrar Plugin (SPI)

Criar arquivo: `src/main/resources/META-INF/services/com.integralltech.codereview.plugin.api.CodeReviewPlugin`

```
com.company.plugins.MyCustomPlugin
```

### 5. Build e Deploy

```bash
# Build
mvn clean package

# O JAR gerado estará em target/my-custom-plugin-1.0.0.jar

# Deploy (copiar para diretório de plugins)
cp target/my-custom-plugin-1.0.0.jar /opt/codereview/plugins/java/
```

---

## 🟦 Criando Plugins TypeScript

### 1. Estrutura do Projeto

```
my-typescript-plugin/
├── package.json
├── tsconfig.json
├── src/
│   ├── index.ts
│   ├── analyzers/
│   │   └── customAnalyzer.ts
│   └── types/
│       └── index.ts
├── dist/
└── README.md
```

### 2. Package.json

```json
{
  "name": "@company/my-typescript-plugin",
  "version": "1.0.0",
  "description": "Custom TypeScript plugin for Pullwise.ai",
  "displayName": "My TypeScript Plugin",
  "main": "dist/index.js",
  "author": "Your Company",
  "license": "MIT",
  
  "codereview-plugin": {
    "type": "LINTER",
    "supportedLanguages": ["javascript", "typescript", "jsx", "tsx"],
    "version": "2.0.0"
  },
  
  "scripts": {
    "build": "tsc",
    "dev": "tsc --watch"
  },
  
  "dependencies": {
    "@babel/parser": "^7.23.5",
    "@babel/traverse": "^7.23.5"
  },
  
  "devDependencies": {
    "@types/node": "^20.10.5",
    "typescript": "^5.3.3"
  }
}
```

### 3. TypeScript Types

```typescript
// src/types/index.ts

export interface AnalysisRequest {
  diff: string;
  changedFiles: string[];
  repository: Repository;
  pullRequest: PullRequest;
  configuration: Record<string, any>;
}

export interface AnalysisResult {
  issues: Issue[];
  metadata?: Record<string, any>;
  executionTime?: number;
  success: boolean;
  errorMessage?: string;
}

export interface Issue {
  id?: string;
  type: IssueType;
  severity: Severity;
  title: string;
  description: string;
  filePath: string;
  lineStart?: number;
  lineEnd?: number;
  columnStart?: number;
  code?: string;
  suggestedFix?: string;
  confidence: number;
}

export enum IssueType {
  BUG = 'BUG',
  SECURITY = 'SECURITY',
  PERFORMANCE = 'PERFORMANCE',
  CODE_SMELL = 'CODE_SMELL',
  STYLE = 'STYLE',
}

export enum Severity {
  CRITICAL = 'CRITICAL',
  HIGH = 'HIGH',
  MEDIUM = 'MEDIUM',
  LOW = 'LOW',
  INFO = 'INFO',
}

export interface Repository {
  id: string;
  name: string;
  path: string;
}

export interface PullRequest {
  id: string;
  number: number;
  title: string;
  description: string;
}
```

### 4. Implementação do Plugin

```typescript
// src/index.ts
import * as fs from 'fs';
import * as path from 'path';
import { parse } from '@babel/parser';
import traverse from '@babel/traverse';
import { AnalysisRequest, AnalysisResult, Issue, IssueType, Severity } from './types';

class MyTypeScriptPlugin {
  private config: Record<string, any> = {};

  async initialize(config: Record<string, any>): Promise<void> {
    this.config = config;
    // Inicialização adicional se necessário
  }

  async analyze(request: AnalysisRequest): Promise<AnalysisResult> {
    const startTime = Date.now();
    const issues: Issue[] = [];

    try {
      // Filtrar arquivos JS/TS
      const jsFiles = request.changedFiles.filter(file => 
        /\.(js|jsx|ts|tsx)$/.test(file)
      );

      for (const filePath of jsFiles) {
        const fullPath = path.join(request.repository.path, filePath);
        const content = fs.readFileSync(fullPath, 'utf-8');
        
        // Analisar arquivo
        const fileIssues = this.analyzeFile(filePath, content);
        issues.push(...fileIssues);
      }

      return {
        issues,
        success: true,
        executionTime: Date.now() - startTime,
        metadata: {
          filesAnalyzed: jsFiles.length,
          pluginVersion: '1.0.0',
        },
      };
    } catch (error) {
      return {
        issues: [],
        success: false,
        errorMessage: error instanceof Error ? error.message : 'Unknown error',
        executionTime: Date.now() - startTime,
      };
    }
  }

  private analyzeFile(filePath: string, content: string): Issue[] {
    const issues: Issue[] = [];

    try {
      // Parse código com Babel
      const ast = parse(content, {
        sourceType: 'module',
        plugins: ['jsx', 'typescript'],
      });

      // Atravessar AST
      traverse(ast, {
        // Detectar console.log
        CallExpression: (path) => {
          const callee = path.node.callee;
          if (
            callee.type === 'MemberExpression' &&
            callee.object.type === 'Identifier' &&
            callee.object.name === 'console'
          ) {
            issues.push({
              type: IssueType.CODE_SMELL,
              severity: Severity.LOW,
              title: 'Avoid console statements in production code',
              description: `Found console.${callee.property.name}() - use proper logging`,
              filePath,
              lineStart: path.node.loc?.start.line,
              lineEnd: path.node.loc?.end.line,
              confidence: 0.95,
            });
          }
        },

        // Detectar componentes React sem PropTypes
        FunctionDeclaration: (path) => {
          const name = path.node.id?.name;
          if (name && this.looksLikeReactComponent(name)) {
            // Verificar se tem PropTypes ou TypeScript types
            const hasTypes = this.hasTypeAnnotations(path);
            if (!hasTypes && !this.hasPropTypes(path)) {
              issues.push({
                type: IssueType.CODE_SMELL,
                severity: Severity.MEDIUM,
                title: 'React component missing prop types',
                description: `Component ${name} should have PropTypes or TypeScript interface`,
                filePath,
                lineStart: path.node.loc?.start.line,
                confidence: 0.85,
              });
            }
          }
        },
      });
    } catch (error) {
      console.error(`Failed to parse ${filePath}:`, error);
    }

    return issues;
  }

  private looksLikeReactComponent(name: string): boolean {
    // Heurística: componentes React começam com maiúscula
    return /^[A-Z]/.test(name);
  }

  private hasTypeAnnotations(path: any): boolean {
    // Verificar se função tem type annotations do TypeScript
    return path.node.params.some((param: any) => param.typeAnnotation);
  }

  private hasPropTypes(path: any): boolean {
    // Verificar se tem PropTypes definido
    // Esta é uma simplificação, implementação completa seria mais robusta
    const parent = path.parent;
    return parent && parent.type === 'Program';
  }

  shutdown(): void {
    // Cleanup se necessário
  }
}

// Entry point para execução via Node.js
async function main() {
  const plugin = new MyTypeScriptPlugin();

  // Ler request do stdin
  let inputData = '';
  process.stdin.on('data', (chunk) => {
    inputData += chunk;
  });

  process.stdin.on('end', async () => {
    try {
      const request: AnalysisRequest = JSON.parse(inputData);
      
      // Executar análise
      const result = await plugin.analyze(request);
      
      // Retornar resultado via stdout
      console.log(JSON.stringify(result, null, 2));
      
      process.exit(result.success ? 0 : 1);
    } catch (error) {
      console.error(JSON.stringify({
        success: false,
        errorMessage: error instanceof Error ? error.message : 'Failed to process request',
        issues: [],
      }));
      process.exit(1);
    }
  });
}

// Executar se for chamado diretamente
if (require.main === module) {
  main();
}

export default MyTypeScriptPlugin;
```

### 5. Build e Deploy

```bash
# Install dependencies
npm install

# Build
npm run build

# Deploy (copiar para diretório de plugins)
cp -r . /opt/codereview/plugins/typescript/my-typescript-plugin/
```

---

## 🐍 Criando Plugins Python

### 1. Estrutura do Projeto

```
my-python-plugin/
├── plugin.yaml
├── plugin.py
├── requirements.txt
├── analyzers/
│   └── custom_analyzer.py
└── README.md
```

### 2. Plugin Metadata (plugin.yaml)

```yaml
id: my-python-plugin
name: My Python Plugin
version: 1.0.0
author: Your Company
description: Custom Python plugin for advanced code analysis
license: MIT

plugin:
  type: SECURITY
  supportedLanguages:
    - python
    - java
  
configuration:
  type: object
  properties:
    max_complexity:
      type: integer
      default: 10
      description: Maximum allowed cyclomatic complexity
    
    check_sql_injection:
      type: boolean
      default: true
      description: Enable SQL injection detection
```

### 3. Implementação do Plugin

```python
# plugin.py
import json
import sys
from typing import List, Dict, Any
from dataclasses import dataclass, asdict
from enum import Enum
import ast
import re

class IssueType(Enum):
    BUG = "BUG"
    SECURITY = "SECURITY"
    PERFORMANCE = "PERFORMANCE"
    CODE_SMELL = "CODE_SMELL"
    STYLE = "STYLE"

class Severity(Enum):
    CRITICAL = "CRITICAL"
    HIGH = "HIGH"
    MEDIUM = "MEDIUM"
    LOW = "LOW"
    INFO = "INFO"

@dataclass
class Issue:
    type: str
    severity: str
    title: str
    description: str
    file_path: str
    line_start: int = None
    line_end: int = None
    code: str = None
    suggested_fix: str = None
    confidence: float = 0.0

@dataclass
class AnalysisResult:
    issues: List[Dict]
    success: bool
    metadata: Dict[str, Any] = None
    execution_time: float = None
    error_message: str = None

class MyPythonPlugin:
    def __init__(self):
        self.config = {}
    
    def initialize(self, config: Dict[str, Any]):
        """Inicializar plugin com configuração"""
        self.config = config
    
    def analyze(self, request: Dict[str, Any]) -> AnalysisResult:
        """Executar análise de código"""
        import time
        start_time = time.time()
        
        issues = []
        
        try:
            changed_files = request.get('changedFiles', [])
            repository_path = request.get('repository', {}).get('path', '')
            
            # Filtrar apenas arquivos Python
            python_files = [f for f in changed_files if f.endswith('.py')]
            
            for file_path in python_files:
                full_path = f"{repository_path}/{file_path}"
                
                try:
                    with open(full_path, 'r') as f:
                        content = f.read()
                    
                    file_issues = self.analyze_file(file_path, content)
                    issues.extend(file_issues)
                    
                except Exception as e:
                    print(f"Warning: Failed to analyze {file_path}: {e}", file=sys.stderr)
            
            execution_time = time.time() - start_time
            
            return AnalysisResult(
                issues=[asdict(issue) for issue in issues],
                success=True,
                execution_time=execution_time,
                metadata={
                    'files_analyzed': len(python_files),
                    'plugin_version': '1.0.0'
                }
            )
            
        except Exception as e:
            return AnalysisResult(
                issues=[],
                success=False,
                error_message=str(e),
                execution_time=time.time() - start_time
            )
    
    def analyze_file(self, file_path: str, content: str) -> List[Issue]:
        """Analisar arquivo Python"""
        issues = []
        
        try:
            # Parse Python AST
            tree = ast.parse(content)
            
            # Análise 1: Detectar SQL injection potencial
            if self.config.get('check_sql_injection', True):
                issues.extend(self.detect_sql_injection(file_path, content, tree))
            
            # Análise 2: Verificar complexidade
            max_complexity = self.config.get('max_complexity', 10)
            issues.extend(self.check_complexity(file_path, tree, max_complexity))
            
            # Análise 3: Detectar hardcoded secrets
            issues.extend(self.detect_hardcoded_secrets(file_path, content))
            
        except SyntaxError as e:
            # Arquivo com erro de sintaxe
            issues.append(Issue(
                type=IssueType.BUG.value,
                severity=Severity.HIGH.value,
                title="Syntax error in Python file",
                description=str(e),
                file_path=file_path,
                line_start=e.lineno,
                confidence=1.0
            ))
        
        return issues
    
    def detect_sql_injection(self, file_path: str, content: str, tree: ast.AST) -> List[Issue]:
        """Detectar potencial SQL injection"""
        issues = []
        
        for node in ast.walk(tree):
            # Detectar string formatting em queries SQL
            if isinstance(node, ast.Call):
                # Procurar por .format() ou f-strings em strings SQL
                if self.looks_like_sql_query(node):
                    issues.append(Issue(
                        type=IssueType.SECURITY.value,
                        severity=Severity.HIGH.value,
                        title="Potential SQL Injection vulnerability",
                        description="SQL query uses string formatting. Use parameterized queries instead.",
                        file_path=file_path,
                        line_start=node.lineno,
                        suggested_fix="Use cursor.execute('SELECT * FROM users WHERE id = %s', (user_id,))",
                        confidence=0.85
                    ))
        
        return issues
    
    def check_complexity(self, file_path: str, tree: ast.AST, max_complexity: int) -> List[Issue]:
        """Verificar complexidade ciclomática"""
        issues = []
        
        for node in ast.walk(tree):
            if isinstance(node, ast.FunctionDef):
                complexity = self.calculate_complexity(node)
                
                if complexity > max_complexity:
                    issues.append(Issue(
                        type=IssueType.CODE_SMELL.value,
                        severity=Severity.MEDIUM.value,
                        title=f"Function too complex: {node.name}",
                        description=f"Cyclomatic complexity is {complexity}, max allowed is {max_complexity}",
                        file_path=file_path,
                        line_start=node.lineno,
                        confidence=1.0
                    ))
        
        return issues
    
    def detect_hardcoded_secrets(self, file_path: str, content: str) -> List[Issue]:
        """Detectar secrets hardcoded"""
        issues = []
        
        # Regex patterns para detectar secrets
        patterns = {
            'AWS Key': r'AKIA[0-9A-Z]{16}',
            'API Key': r'api[_-]?key["\']?\s*[:=]\s*["\'][a-zA-Z0-9]{32,}["\']',
            'Password': r'password["\']?\s*[:=]\s*["\'][^"\']+["\']',
        }
        
        lines = content.split('\n')
        
        for secret_type, pattern in patterns.items():
            for i, line in enumerate(lines, 1):
                if re.search(pattern, line, re.IGNORECASE):
                    issues.append(Issue(
                        type=IssueType.SECURITY.value,
                        severity=Severity.CRITICAL.value,
                        title=f"Hardcoded {secret_type} detected",
                        description=f"Found hardcoded {secret_type}. Use environment variables instead.",
                        file_path=file_path,
                        line_start=i,
                        confidence=0.90
                    ))
        
        return issues
    
    def looks_like_sql_query(self, node: ast.Call) -> bool:
        """Heurística para detectar queries SQL"""
        # Simplificação - implementação real seria mais sofisticada
        if hasattr(node, 'args'):
            for arg in node.args:
                if isinstance(arg, ast.Constant) and isinstance(arg.value, str):
                    sql_keywords = ['SELECT', 'INSERT', 'UPDATE', 'DELETE', 'FROM', 'WHERE']
                    if any(keyword in arg.value.upper() for keyword in sql_keywords):
                        return True
        return False
    
    def calculate_complexity(self, node: ast.FunctionDef) -> int:
        """Calcular complexidade ciclomática"""
        complexity = 1  # Base complexity
        
        for child in ast.walk(node):
            # Incrementar para cada decisão
            if isinstance(child, (ast.If, ast.While, ast.For, ast.ExceptHandler)):
                complexity += 1
            elif isinstance(child, ast.BoolOp):
                complexity += len(child.values) - 1
        
        return complexity
    
    def shutdown(self):
        """Cleanup de recursos"""
        pass

# Entry point
def main():
    plugin = MyPythonPlugin()
    
    # Ler request do stdin
    input_data = sys.stdin.read()
    request = json.loads(input_data)
    
    # Inicializar com configuração
    config = request.get('configuration', {})
    plugin.initialize(config)
    
    # Executar análise
    result = plugin.analyze(request)
    
    # Retornar resultado
    print(json.dumps(asdict(result), indent=2))
    
    sys.exit(0 if result.success else 1)

if __name__ == '__main__':
    main()
```

### 4. Requirements

```txt
# requirements.txt
# Nenhuma dependência externa necessária para este exemplo básico
# mas você pode adicionar:
# bandit>=1.7.5  # Para análise de segurança
# radon>=6.0.1   # Para métricas de complexidade
# pylint>=3.0.0  # Para análise estática
```

### 5. Deploy

```bash
# Deploy (copiar para diretório de plugins)
cp -r . /opt/codereview/plugins/python/my-python-plugin/

# Instalar dependências (se houver)
pip install -r requirements.txt --target /opt/codereview/plugins/python/my-python-plugin/
```

---

## 📦 Distribuição e Marketplace

### 1. Preparar para Publicação

Cada plugin deve incluir:

```
my-plugin/
├── README.md           # Documentação completa
├── LICENSE             # Licença (MIT, Apache, etc)
├── CHANGELOG.md        # Histórico de versões
├── examples/           # Exemplos de uso
│   └── sample-config.yaml
└── tests/             # Testes unitários
    └── test_plugin.py
```

### 2. Metadata para Marketplace

```yaml
# marketplace.yaml
plugin:
  id: my-awesome-plugin
  name: My Awesome Plugin
  tagline: "Detect custom patterns in your code"
  description: |
    This plugin provides advanced detection of company-specific
    code patterns and best practices.
  
  author:
    name: Your Company
    email: plugins@company.com
    website: https://company.com
  
  version: 1.0.0
  license: MIT
  
  repository:
    type: github
    url: https://github.com/company/my-awesome-plugin
  
  categories:
    - code-quality
    - best-practices
    - company-specific
  
  pricing:
    model: free  # ou: paid, freemium
    price: 0     # em USD/mês
  
  compatibility:
    minVersion: 2.0.0
    maxVersion: 3.0.0
  
  screenshots:
    - url: https://example.com/screenshot1.png
      caption: "Detection of custom patterns"
    - url: https://example.com/screenshot2.png
      caption: "Configuration interface"
```

### 3. Publicar no Marketplace

```bash
# CLI para publicar (fictício, ajustar conforme implementação real)
codereview-cli plugin publish \
  --manifest marketplace.yaml \
  --package my-plugin-1.0.0.zip \
  --token YOUR_API_TOKEN
```

---

## 🎯 Templates Prontos

### Template Java - FindBugs-like Plugin

```java
// Detectar patterns similares ao FindBugs
public class FindBugsStylePlugin implements CodeReviewPlugin {
    
    @Override
    public AnalysisResult analyze(AnalysisRequest request) {
        List<Issue> issues = new ArrayList<>();
        
        for (String file : request.getFilesOfType(Language.JAVA)) {
            CompilationUnit cu = javaParser.parse(request.getFileContent(file));
            
            // Detectar equals() sem hashCode()
            issues.addAll(detectEqualsWithoutHashCode(cu, file));
            
            // Detectar comparação de String com ==
            issues.addAll(detectStringComparison(cu, file));
            
            // Detectar streams não fechados
            issues.addAll(detectUnclosedStreams(cu, file));
        }
        
        return AnalysisResult.builder().issues(issues).success(true).build();
    }
}
```

### Template TypeScript - React Best Practices

```typescript
// Detectar anti-patterns no React
class ReactBestPracticesPlugin {
  analyzeFile(filePath: string, content: string): Issue[] {
    const issues: Issue[] = [];
    const ast = parse(content, { plugins: ['jsx', 'typescript'] });
    
    traverse(ast, {
      // Detectar useState sem initializer
      CallExpression: (path) => {
        if (this.isUseStateWithoutInit(path)) {
          issues.push(/* ... */);
        }
      },
      
      // Detectar componentes sem memo quando apropriado
      FunctionDeclaration: (path) => {
        if (this.shouldUseMemo(path)) {
          issues.push(/* ... */);
        }
      },
    });
    
    return issues;
  }
}
```

### Template Python - Security Scanner

```python
# Scanner de segurança focado
class SecurityScannerPlugin:
    def analyze_file(self, file_path, content):
        issues = []
        
        # Detectar imports perigosos
        issues.extend(self.check_dangerous_imports(content))
        
        # Detectar eval/exec
        issues.extend(self.check_eval_exec(content))
        
        # Detectar deserialização insegura (pickle)
        issues.extend(self.check_unsafe_deserialization(content))
        
        return issues
```

---

## 🚀 Próximos Passos

1. **Criar seu primeiro plugin** usando um dos templates
2. **Testar localmente** com o CLI de desenvolvimento
3. **Publicar no marketplace interno** da sua organização
4. **Compartilhar com a comunidade** (se open source)

---

## 📚 Recursos Adicionais

- **Documentação da API**: https://docs.pullwise.ai/plugins/api
- **Exemplos oficiais**: https://github.com/pullwise-ai/plugin-examples
- **Discord da comunidade**: https://discord.gg/pullwise-ai
- **Tutorial em vídeo**: https://youtube.com/pullwise-ai-plugins

---

Este documento fornece a base completa para criar plugins nas três linguagens suportadas. A arquitetura flexível permite que desenvolvedores estendam o Pullwise.ai de acordo com suas necessidades específicas.
