package bughunteragent.service;

import bughunteragent.dto.AnalysisResult;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmailReportService {

    private final JavaMailSender mailSender;

    public EmailReportService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendAuditReport(String recipient, List<AnalysisResult> results) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(recipient);
        helper.setSubject("[Bug Hunter] Informe de análisis - i-agent-code-auditor-test");
        helper.setText(buildHtmlContent(results), true);

        mailSender.send(message);
    }

    private String buildHtmlContent(List<AnalysisResult> results) {
        StringBuilder html = new StringBuilder();
        html.append("<div style='font-family: Arial, sans-serif;'>");
        html.append("<h2>🤖 Informe de Auditoría Automática de Código (MCP Agent)</h2>");

        for (AnalysisResult r : results) {
            html.append("<div style='border: 1px solid #ccc; padding: 15px; margin-bottom: 20px; border-radius: 5px;'>");
            html.append("<h3 style='color: #d9534f;'>Issue #").append(r.issueNumber()).append(": ").append(r.issueTitle()).append("</h3>");
            html.append("<p><b>URL:</b> <a href='").append(r.issueUrl()).append("'>").append(r.issueUrl()).append("</a></p>");
            html.append("<p><b>📍 Ubicación:</b> <code>").append(r.locationInfo()).append("</code></p>");
            html.append("<p><b>🔍 Causa Raíz:</b> ").append(r.rootCause()).append("</p>");

            html.append("<h4>💡 Opciones de Solución:</h4>");
            for (var sol : r.solutions()) {
                html.append("<div style='background: #f9f9f9; padding: 10px; margin-bottom: 10px;'>");
                html.append("<p><b>Opción:</b> ").append(sol.description()).append("</p>");
                html.append("<pre style='background: #272822; color: #f8f8f2; padding: 10px; border-radius: 4px;'><code>")
                        .append(sol.codeSnippet()).append("</code></pre>");
                html.append("<p><b>Pros:</b> ").append(sol.pros()).append("<br/><b>Contras:</b> ").append(sol.contras()).append("</p>");
                html.append("</div>");
            }

            html.append("<p style='color: #5cb85c;'><b>✅ Recomendación:</b> ").append(r.recommendation()).append("</p>");
            html.append("</div>");
        }

        html.append("</div>");
        return html.toString();
    }
}