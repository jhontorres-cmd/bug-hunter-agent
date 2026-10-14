package bughunteragent.service;

import bughunteragent.ai.BugHunterAgent;
import bughunteragent.dto.AnalysisResult;
import bughunteragent.dto.SolutionOption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IssueScannerServiceTest {

    private static final String TEST_OWNER = "test-owner";
    private static final String TEST_REPO = "test-repo";
    private static final String TEST_RECIPIENT = "test@example.com";

    @Mock
    private BugHunterAgent bugHunterAgent;

    @Mock
    private EmailReportService emailReportService;

    @InjectMocks
    private IssueScannerService issueScannerService;

    @BeforeEach
    void setUp() {
        // Se inyectan manualmente los valores dummy a los campos @Value de Spring
        ReflectionTestUtils.setField(issueScannerService, "githubOwner", TEST_OWNER);
        ReflectionTestUtils.setField(issueScannerService, "githubRepo", TEST_REPO);
        ReflectionTestUtils.setField(issueScannerService, "reportRecipient", TEST_RECIPIENT);
    }

    @Test
    @DisplayName("Debe ejecutar el flujo exitoso completo: descubrimiento, análisis, comentario y envío de correo")
    void testFullWorkflowSuccess() throws Exception {
        // Mock del descubrimiento dinámico
        when(bugHunterAgent.findOpenBugIssues(anyString(), eq(TEST_OWNER), eq(TEST_REPO)))
                .thenReturn("[4, 5]");

        // Mock del resultado del análisis de auditoría
        SolutionOption optionMock = new SolutionOption(
                "Solución defensiva",
                "if(user != null) return user.getFullName();",
                "Simple",
                "No resuelve el problema de fondo"
        );
        AnalysisResult resultMock = new AnalysisResult(
                4,
                "Error fetching user",
                "https://github.com/test-owner/test-repo/issues/4",
                "UserService.java línea 24",
                "NullPointerException al obtener el nombre",
                List.of(optionMock),
                "Aplicar la solución recomendada"
        );

        when(bugHunterAgent.auditIssue(anyString(), eq(TEST_OWNER), eq(TEST_REPO)))
                .thenReturn(resultMock);

        // Ejecuta servicio
        issueScannerService.run();

        // Verificaciones
        verify(bugHunterAgent, times(1)).findOpenBugIssues(anyString(), eq(TEST_OWNER), eq(TEST_REPO));
        verify(bugHunterAgent, times(2)).auditIssue(anyString(), eq(TEST_OWNER), eq(TEST_REPO));
        verify(bugHunterAgent, times(2)).postIssueComment(eq(TEST_OWNER), eq(TEST_REPO), anyInt(), anyString());
        verify(emailReportService, times(1)).sendAuditReport(eq(TEST_RECIPIENT), anyList());
    }

    @Test
    @DisplayName("Debe continuar el flujo y enviar el correo aunque la publicación del comentario en GitHub falle")
    void testWorkflowContinuesWhenGithubCommentFails() throws Exception {
        // Mock de descubrimiento de issue
        when(bugHunterAgent.findOpenBugIssues(anyString(), eq(TEST_OWNER), eq(TEST_REPO)))
                .thenReturn("[4]");

        SolutionOption optionMock = new SolutionOption("Solución", "code()", "Pro", "Con");
        AnalysisResult resultMock = new AnalysisResult(
                4, "Issue Title", "http://github.com/test/4", "Location", "Root Cause", List.of(optionMock), "Recommendation"
        );

        when(bugHunterAgent.auditIssue(anyString(), eq(TEST_OWNER), eq(TEST_REPO)))
                .thenReturn(resultMock);

        // Simula excepción en el comentario en GitHub
        doThrow(new RuntimeException("GitHub API Error 403"))
                .when(bugHunterAgent).postIssueComment(eq(TEST_OWNER), eq(TEST_REPO), anyInt(), anyString());

        issueScannerService.run();

        // Verificación: El correo DEBE enviarse obligatoriamente a pesar del error en el comentario
        verify(emailReportService, times(1)).sendAuditReport(eq(TEST_RECIPIENT), anyList());
    }

    @Test
    @DisplayName("Debe usar la lista de respaldo si el descubrimiento dinámico falla o arroja excepción")
    void testFallbackIssuesWhenDiscoveryFails() throws Exception {
        // Simula fallo en el descubrimiento dinámico
        when(bugHunterAgent.findOpenBugIssues(anyString(), eq(TEST_OWNER), eq(TEST_REPO)))
                .thenThrow(new RuntimeException("Timeout al conectar con MCP"));

        // Ejecutar servicio
        issueScannerService.run();

        // Verificación: Debe intentar auditar las 3 issues de la lista de respaldo [4, 5, 6]
        verify(bugHunterAgent, times(3)).auditIssue(anyString(), eq(TEST_OWNER), eq(TEST_REPO));
    }
}