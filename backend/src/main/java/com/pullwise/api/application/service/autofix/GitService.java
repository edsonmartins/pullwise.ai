package com.pullwise.api.application.service.autofix;

import com.pullwise.api.domain.model.Project;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Serviço para operações Git.
 *
 * <p>Responsável por:
 * <ul>
 *   <li>Clone de repositórios</li>
 *   <li>Criação de branches</li>
 *   <li>Aplicação de patches</li>
 *   <li>Commit de mudanças</li>
 *   <li>Push de branches</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GitService {

    @Value("${autofix.work-dir:/tmp/pullwise/work}")
    private String workDir;

    @Value("${autofix.git-timeout-seconds:300}")
    private int gitTimeoutSeconds;

    private static final String GIT_BINARY = "git";

    /**
     * Clone um repositório Git.
     *
     * @param project Projeto com URL do repositório
     * @return Caminho local do repositório clonado
     */
    public GitCloneResult cloneRepository(Project project) {
        String repoId = project.getId().toString();
        Path targetPath = Path.of(workDir, repoId);

        try {
            // Remove diretório existente se houver
            if (Files.exists(targetPath)) {
                deleteDirectory(targetPath);
            }

            // Cria diretório pai
            Files.createDirectories(targetPath.getParent());

            // Executa clone
            List<String> command = List.of(
                    GIT_BINARY, "clone",
                    "--depth", "1",
                    project.getRepositoryUrl(),
                    targetPath.toString()
            );

            ProcessResult result = executeCommand(command, workDir);

            if (result.exitCode() != 0) {
                throw new GitException("Failed to clone repository: " + result.stderr());
            }

            log.info("Cloned repository {} to {}", project.getRepositoryUrl(), targetPath);

            return new GitCloneResult(
                    targetPath.toString(),
                    extractCurrentBranch(targetPath),
                    extractCurrentCommit(targetPath)
            );

        } catch (IOException e) {
            throw new GitException("Failed to clone repository", e);
        }
    }

    /**
     * Cria um novo branch a partir da base especificada.
     *
     * @param repoPath    Caminho do repositório local
     * @param branchName  Nome do novo branch
     * @param startPoint  Ponto de partida (branch, tag, commit)
     */
    public void createBranch(Path repoPath, String branchName, String startPoint) {
        List<String> command = List.of(
                GIT_BINARY, "checkout", "-b",
                branchName,
                startPoint
        );

        ProcessResult result = executeCommand(command, repoPath.toString());

        if (result.exitCode() != 0) {
            throw new GitException("Failed to create branch " + branchName + ": " + result.stderr());
        }

        log.info("Created branch {} from {} in {}", branchName, startPoint, repoPath);
    }

    /**
     * Aplica um patch/unified diff ao repositório.
     *
     * @param repoPath Caminho do repositório local
     * @param patch    Conteúdo do patch (formato unified diff)
     * @param fileName Nome do arquivo (para logging)
     * @return true se o patch foi aplicado com sucesso
     */
    public GitApplyResult applyPatch(Path repoPath, String patch, String fileName) {
        try {
            // Cria arquivo temporário com o patch
            Path patchFile = Files.createTempFile("patch-", ".diff");
            Files.writeString(patchFile, patch);

            try {
                // Tenta aplicar com --3way para merge automático
                List<String> command = List.of(
                        GIT_BINARY, "apply",
                        "--3way",
                        "--whitespace=fix",
                        patchFile.toString()
                );

                ProcessResult result = executeCommand(command, repoPath.toString());

                boolean success = result.exitCode() == 0;
                boolean hasConflicts = !success && result.stderr().contains("conflict");

                return new GitApplyResult(
                        success,
                        hasConflicts,
                        success ? "Patch applied successfully" : result.stderr(),
                        extractModifiedFiles(repoPath)
                );

            } finally {
                Files.deleteIfExists(patchFile);
            }

        } catch (IOException e) {
            throw new GitException("Failed to apply patch", e);
        }
    }

    /**
     * Aplica mudanças diretamente em um arquivo.
     *
     * @param repoPath  Caminho do repositório
     * @param filePath  Caminho do arquivo (relativo à raiz)
     * @param newContent Novo conteúdo
     */
    public void writeFile(Path repoPath, String filePath, String newContent) {
        try {
            Path targetFile = repoPath.resolve(filePath);

            // Cria diretórios pais se necessário
            Files.createDirectories(targetFile.getParent());

            // Escreve novo conteúdo
            Files.writeString(targetFile, newContent);

            log.debug("Wrote {} bytes to {}", newContent.length(), filePath);

        } catch (IOException e) {
            throw new GitException("Failed to write file " + filePath, e);
        }
    }

    /**
     * Lê o conteúdo de um arquivo.
     *
     * @param repoPath Caminho do repositório
     * @param filePath Caminho do arquivo
     * @return Conteúdo do arquivo
     */
    public String readFile(Path repoPath, String filePath) {
        try {
            Path targetFile = repoPath.resolve(filePath);
            return Files.readString(targetFile);
        } catch (IOException e) {
            throw new GitException("Failed to read file " + filePath, e);
        }
    }

    /**
     * Commita mudanças com mensagem.
     *
     * @param repoPath Caminho do repositório
     * @param message  Mensagem de commit
     * @return Hash do commit criado
     */
    public String commit(Path repoPath, String message) {
        // Stage todas as mudanças
        List<String> addCommand = List.of(GIT_BINARY, "add", "-A");
        ProcessResult addResult = executeCommand(addCommand, repoPath.toString());

        if (addResult.exitCode() != 0) {
            throw new GitException("Failed to stage changes: " + addResult.stderr());
        }

        // Commit
        List<String> commitCommand = List.of(
                GIT_BINARY, "commit",
                "-m", message
        );

        ProcessResult commitResult = executeCommand(commitCommand, repoPath.toString());

        if (commitResult.exitCode() != 0) {
            throw new GitException("Failed to commit: " + commitResult.stderr());
        }

        // Extrai hash do commit
        String commitHash = extractCurrentCommit(repoPath);

        log.info("Committed changes in {}: {}", repoPath, commitHash);

        return commitHash;
    }

    /**
     * Faz push de um branch para o remoto.
     *
     * @param repoPath   Caminho do repositório
     * @param branchName Nome do branch
     * @param remote     Remoto (default: origin)
     */
    public void push(Path repoPath, String branchName, String remote) {
        List<String> command = List.of(
                GIT_BINARY, "push",
                "-u",
                remote,
                branchName
        );

        ProcessResult result = executeCommand(command, repoPath.toString());

        if (result.exitCode() != 0) {
            throw new GitException("Failed to push branch " + branchName + ": " + result.stderr());
        }

        log.info("Pushed branch {} to {}", branchName, remote);
    }

    /**
     * Faz push usando credenciais personalizadas.
     *
     * @param repoPath   Caminho do repositório
     * @param branchName Nome do branch
     * @param remoteUrl  URL com credenciais embutidas
     */
    public void pushWithAuth(Path repoPath, String branchName, String remoteUrl) {
        // Configura URL temporária
        List<String> urlCommand = List.of(
                GIT_BINARY, "remote",
                "set-url", "origin",
                remoteUrl
        );

        executeCommand(urlCommand, repoPath.toString());

        // Faz push
        push(repoPath, branchName, "origin");
    }

    /**
     * Remove um branch local.
     *
     * @param repoPath   Caminho do repositório
     * @param branchName Nome do branch
     */
    public void deleteBranch(Path repoPath, String branchName) {
        // Primeiro faz checkout de outro branch
        List<String> checkoutCommand = List.of(
                GIT_BINARY, "checkout", "-"
        );

        executeCommand(checkoutCommand, repoPath.toString());

        // Deleta o branch
        List<String> deleteCommand = List.of(
                GIT_BINARY, "branch", "-D",
                branchName
        );

        ProcessResult result = executeCommand(deleteCommand, repoPath.toString());

        if (result.exitCode() != 0) {
            log.warn("Failed to delete branch {}: {}", branchName, result.stderr());
        } else {
            log.info("Deleted branch {}", branchName);
        }
    }

    /**
     * Obtém o diff de um arquivo.
     *
     * @param repoPath Caminho do repositório
     * @param filePath Caminho do arquivo
     * @return Diff em formato unificado
     */
    public String getFileDiff(Path repoPath, String filePath) {
        List<String> command = List.of(
                GIT_BINARY, "diff",
                filePath
        );

        ProcessResult result = executeCommand(command, repoPath.toString());

        if (result.exitCode() != 0 && !result.stderr().isBlank()) {
            throw new GitException("Failed to get diff: " + result.stderr());
        }

        return result.stdout();
    }

    /**
     * Verifica se há mudanças no repositório.
     *
     * @param repoPath Caminho do repositório
     * @return true se há mudanças
     */
    public boolean hasChanges(Path repoPath) {
        List<String> command = List.of(
                GIT_BINARY, "status",
                "--porcelain"
        );

        ProcessResult result = executeCommand(command, repoPath.toString());

        return !result.stdout().isBlank();
    }

    /**
     * Lista arquivos modificados.
     *
     * @param repoPath Caminho do repositório
     * @return Lista de arquivos modificados
     */
    public List<String> getModifiedFiles(Path repoPath) {
        List<String> command = List.of(
                GIT_BINARY, "diff",
                "--name-only",
                "HEAD"
        );

        ProcessResult result = executeCommand(command, repoPath.toString());

        if (result.exitCode() != 0) {
            return List.of();
        }

        String output = result.stdout().trim();
        if (output.isBlank()) {
            return List.of();
        }

        return List.of(output.split("\n"));
    }

    /**
     * Limpa um repositório clonado.
     *
     * @param repoPath Caminho do repositório
     */
    public void cleanup(Path repoPath) {
        try {
            if (Files.exists(repoPath)) {
                deleteDirectory(repoPath);
                log.info("Cleaned up {}", repoPath);
            }
        } catch (IOException e) {
            log.warn("Failed to cleanup {}", repoPath, e);
        }
    }

    // ========== Private Methods ==========

    /**
     * Executa um comando no shell.
     */
    private ProcessResult executeCommand(List<String> command, String workingDir) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(Path.of(workingDir).toFile());

            Process process = pb.start();

            // Lê stdout e stderr em paralelo
            StreamGobbler stdoutGobbler = new StreamGobbler(process.getInputStream());
            StreamGobbler stderrGobbler = new StreamGobbler(process.getErrorStream());

            new Thread(stdoutGobbler).start();
            new Thread(stderrGobbler).start();

            // Aguarda com timeout
            boolean finished = process.waitFor(gitTimeoutSeconds, java.util.concurrent.TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                throw new GitException("Command timed out after " + gitTimeoutSeconds + "s");
            }

            int exitCode = process.exitValue();

            return new ProcessResult(
                    exitCode,
                    stdoutGobbler.getOutput(),
                    stderrGobbler.getOutput()
            );

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GitException("Command interrupted", e);
        } catch (IOException e) {
            throw new GitException("Failed to execute command", e);
        }
    }

    /**
     * Remove diretório recursivamente.
     */
    private void deleteDirectory(Path path) throws IOException {
        Files.walk(path)
                .sorted((a, b) -> b.compareTo(a)) // Reverse order
                .forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (IOException e) {
                        log.warn("Failed to delete {}", p, e);
                    }
                });
    }

    /**
     * Extrai o branch atual.
     */
    private String extractCurrentBranch(Path repoPath) {
        List<String> command = List.of(
                GIT_BINARY, "rev-parse",
                "--abbrev-ref", "HEAD"
        );

        ProcessResult result = executeCommand(command, repoPath.toString());

        if (result.exitCode() != 0) {
            return "unknown";
        }

        return result.stdout().trim();
    }

    /**
     * Extrai o hash do commit atual.
     */
    private String extractCurrentCommit(Path repoPath) {
        List<String> command = List.of(
                GIT_BINARY, "rev-parse",
                "HEAD"
        );

        ProcessResult result = executeCommand(command, repoPath.toString());

        if (result.exitCode() != 0) {
            return "unknown";
        }

        return result.stdout().trim();
    }

    /**
     * Extrai lista de arquivos modificados.
     */
    private List<String> extractModifiedFiles(Path repoPath) {
        return getModifiedFiles(repoPath);
    }

    // ========== Inner Classes ==========

    /**
     * Consumidor de stream de processo.
     */
    private static class StreamGobbler implements Runnable {
        private final java.io.InputStream inputStream;
        private final StringBuilder output = new StringBuilder();

        public StreamGobbler(java.io.InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public void run() {
            try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            } catch (IOException e) {
                // Ignore
            }
        }

        public String getOutput() {
            return output.toString();
        }
    }

    // ========== DTOs ==========

    public record GitCloneResult(
            String localPath,
            String branch,
            String commitHash
    ) {}

    public record GitApplyResult(
            boolean success,
            boolean hasConflicts,
            String message,
            List<String> modifiedFiles
    ) {}

    public record ProcessResult(
            int exitCode,
            String stdout,
            String stderr
    ) {}

    /**
     * Exceção para erros de operações Git.
     */
    public static class GitException extends RuntimeException {
        public GitException(String message) {
            super(message);
        }

        public GitException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
