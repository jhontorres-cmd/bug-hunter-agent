package bughunteragent.service;

import bughunteragent.ai.BugHunterAgent;
import bughunteragent.dto.AnalysisResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class IssueScannerService implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(IssueScannerService.class);

    private final BugHunterAgent bugHunterAgent;
    private final EmailReportService emailReportService;

    @Value("${github.owner}")
    private String githubOwner;

    @Value("${github.repo}")
    private String githubRepo;

    @Value("${spring.mail.username}")
    private String reportRecipient;

    public IssueScannerService(BugHunterAgent bugHunterAgent, EmailReportService emailReportService) {
        this.bugHunterAgent = bugHunterAgent;
        this.emailReportService = emailReportService;
    }

    @Override
    public void run(String... args) {
        log.info("🚀 Iniciando Agente Bug Hunter (MCP)...");

        // 1. Descubrimiento dinámico de issues vía MCP
        List<Integer> targetIssues = discoverIssuesDynamically();

        if (targetIssues.isEmpty()) {
            log.warn("⚠️ No se encontraron issues vía MCP. Usando lista de respaldo [4, 5, 6].");
            targetIssues = List.of(4, 5, 6);
        }

        List<AnalysisResult> results = new ArrayList<>();

        // 2. Procesamiento: Fase de Análisis + Fase de Comentario Desacoplada
        for (int issueNumber : targetIssues) {
            log.info("🔎 [Fase 1] Analizando Issue #{} en {}/{}...", issueNumber, githubOwner, githubRepo);

            String issueContext = String.format(
                    "Ejecuta get_issue para auditar la issue número %d en el repositorio '%s/%s'.",
                    issueNumber, githubOwner, githubRepo
            );

            try {
                // Paso A: Análisis puro del código y generación de causa raíz/soluciones
                AnalysisResult result = bugHunterAgent.auditIssue(issueContext, githubOwner, githubRepo);

                if (result != null) {
                    results.add(result);
                    log.info("✅ [Fase 1] Análisis de Issue #{} completado con éxito.", issueNumber);

                    // Paso B: Publicación de comentario en GitHub (desacoplada)
                    publishCommentSafely(issueNumber, result);
                }
            } catch (Exception e) {
                log.error("❌ Error durante el análisis de la Issue #{}: {}", issueNumber, e.getMessage(), e);
            }
        }

        // 3. Envío del reporte consolidado por correo
        if (!results.isEmpty()) {
            log.info("📧 Enviando reporte de auditoría consolidado a {}...", reportRecipient);
            try {
                emailReportService.sendAuditReport(reportRecipient, results);
                log.info("🎉 ¡Proceso de auditoría, publicación en GitHub y envío de correo finalizado con éxito!");
            } catch (Exception e) {
                log.error("❌ Error al enviar el correo del reporte: {}", e.getMessage(), e);
            }
        }
    }

    private void publishCommentSafely(int issueNumber, AnalysisResult result) {
        try {
            log.info("💬 [Fase 2] Generando y publicando comentario formateado en Issue #{}...", issueNumber);

            // Convertir la lista de objetos SolutionOption a un String formateado
            String solutionsText = result.solutions() != null ?
                    result.solutions().stream()
                            .map(sol -> String.format("- **%s**:\n  - Pros: %s\n  - Contras: %s\n  - Código:\n```java\n%s\n```",
                                    sol.description(),
                                    String.join(", ", sol.pros()),
                                    String.join(", ", sol.contras()),
                                    sol.codeSnippet()))
                            .reduce("", (a, b) -> a + "\n\n" + b)
                    : "No hay opciones registradas";

            String commentPrompt = String.format("""
            Genera un comentario profesional de auditoría en Markdown para la Issue #%d usando estos datos:

            Ubicación: %s
            Causa Raíz: %s
            Opciones de Solución:
            %s

            Recomendación: %s

            Recuerda usar encabezados claros (###), viñetas organizadas y saltos de línea para que quede muy ordenado e ilustrativo.
            """,
                    issueNumber,
                    result.locationInfo(),
                    result.rootCause(),
                    solutionsText,
                    result.recommendation()
            );

            bugHunterAgent.postIssueComment(githubOwner, githubRepo, issueNumber, commentPrompt);
            log.info("✅ [Fase 2] Comentario enriquecido publicado con éxito en la Issue #{}.", issueNumber);

        } catch (Exception e) {
            log.warn("⚠️ No se pudo publicar el comentario en la Issue #{}: {}", issueNumber, e.getMessage());
        }
    }

    private List<Integer> discoverIssuesDynamically() {
        List<Integer> issueNumbers = new ArrayList<>();
        try {
            log.info("🔍 Descubriendo issues abiertamente vía MCP 'list_issues' en {}/{}...", githubOwner, githubRepo);
            String response = bugHunterAgent.findOpenBugIssues("Lista las issues abiertas con la etiqueta 'bug'", githubOwner, githubRepo);

            Pattern pattern = Pattern.compile("(?:#|issue\\s*|number\\s*)?(\\d{1,3})\\b", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(response);
            while (matcher.find()) {
                int num = Integer.parseInt(matcher.group(1));
                if (num < 100 && !issueNumbers.contains(num)) {
                    issueNumbers.add(num);
                }
            }
            log.info("📋 Issues descubiertas dinámicamente: {}", issueNumbers);
        } catch (Exception e) {
            log.error("⚠️ Fallo en el descubrimiento dinámico de issues: {}", e.getMessage());
        }
        return issueNumbers;
    }
}