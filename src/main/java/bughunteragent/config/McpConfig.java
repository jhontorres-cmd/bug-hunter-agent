package bughunteragent.config;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Configuration
public class McpConfig {

    @Value("${github.token}")
    private String githubToken;

    @Value("${github.owner}")
    private String githubOwner;

    @Value("${github.repo}")
    private String githubRepo;

    @Bean
    public McpClient githubMcpClient() {
        String npxCommand = System.getProperty("os.name").toLowerCase().contains("win") ? "npx.cmd" : "npx";

        StdioMcpTransport transport = new StdioMcpTransport.Builder()
                .command(List.of(npxCommand, "-y", "@modelcontextprotocol/server-github"))
                .environment(Map.of(
                        "GITHUB_PERSONAL_ACCESS_TOKEN", githubToken,
                        "GITHUB_OWNER", githubOwner,
                        "GITHUB_REPO", githubRepo
                ))
                .build();

        return new DefaultMcpClient.Builder()
                .transport(transport)
                .initializationTimeout(Duration.ofSeconds(60))
                .toolExecutionTimeout(Duration.ofSeconds(60))
                .build();
    }

    @Bean
    public McpToolProvider githubToolProvider(McpClient githubMcpClient) {
        return McpToolProvider.builder()
                .mcpClients(List.of(githubMcpClient))
                .build();
    }
}
