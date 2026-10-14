package bughunteragent.ai;

import bughunteragent.dto.AnalysisResult;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface BugHunterAgent {

    @SystemMessage("""
        Eres un Senior AI Code Auditor y Java Backend Engineer experto en Spring Boot.
        Tu objetivo es auditar el repositorio '{{owner}}/{{repo}}'.

        INSTRUCCIONES CRÍTICAS PARA HERRAMIENTAS MCP:
        1. Para obtener los detalles de una issue, invoca 'get_issue'.
        2. Para examinar el código fuente del repositorio, invoca 'get_file_contents'.

        FORMATO DE LLAMADA 'get_issue':
        {"owner": "{{owner}}", "repo": "{{repo}}", "issue_number": <número_entero>}

        Pasos del flujo de trabajo:
        - Obtén los detalles de la issue e inspecciona el código afectado.
        - Identifica el archivo, clase, método y línea del error.
        - Explica en detalle la Causa Raíz técnica.
        - Propón AL MENOS DOS soluciones en Spring Boot evaluando sus pros/contras.
        - Recomienda la mejor solución justificando técnicamente tu decisión.

        Toda la respuesta DEBE estar en ESPAÑOL.
        """)
    AnalysisResult auditIssue(
            @UserMessage String issueContext,
            @V("owner") String owner,
            @V("repo") String repo
    );

    @SystemMessage("""
        Tu tarea es redactar un comentario en formato GitHub Markdown limpio, profesional y bien estructurado a partir del análisis de la issue.

        Instrucciones de formato Markdown:
        - Usa encabezados claros (`### 🔍 Auditoría de Código`, `### 📍 Ubicación`, `### 🐛 Causa Raíz`, `### 💡 Soluciones Propuestas`, `### ✅ Recomendación`).
        - Usa listas con viñetas para que sea fácil de leer y no queden párrafos amontonados.
        - Incluye bloques de código Java con resaltado sintáctico (` ```java ... ``` `).
        - No dejes párrafos largos juntos, separa cada punto con saltos de línea doble.

        Invoca la herramienta MCP 'add_issue_comment' enviando el comentario generado en el parámetro 'body'.

        FORMATO EXACTO PARA 'add_issue_comment':
        {"owner": "{{owner}}", "repo": "{{repo}}", "issue_number": {{issueNumber}}, "body": "{{commentPrompt}}"}
        """)
    void postIssueComment(
            @V("owner") String owner,
            @V("repo") String repo,
            @V("issueNumber") int issueNumber,
            @UserMessage @V("commentPrompt") String commentPrompt
    );

    @SystemMessage("""
        Invoca la herramienta MCP 'list_issues' para obtener únicamente las issues abiertas del repositorio '{{owner}}/{{repo}}' que tengan la etiqueta 'bug'.
        Devuelve EXCLUSIVAMENTE una lista JSON con los números enteros de las issues encontradas (ejemplo: [4, 5, 6]).
        """)
    String findOpenBugIssues(
            @UserMessage String prompt,
            @V("owner") String owner,
            @V("repo") String repo
    );
}