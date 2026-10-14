# Bug Hunter AI Agent (MCP)

Agente de Inteligencia Artificial desarrollado en **Java 17**, **Spring Boot 3.x** y **LangChain4j**, diseñado para automatizar la auditoría de código, el análisis de causa raíz y la interacción en repositorios de GitHub mediante el estándar **Model Context Protocol (MCP)**.

## Arquitectura de la Solución

La solución implementa un desacoplamiento estricto entre la fase de razonamiento (consultas de lectura con LLM + MCP) y la fase de mutaciones/notificaciones (comentarios en GitHub y correos SMTP).


<img width="2752" height="1536" alt="Arquitectura Bug Hunter Agent" src="https://github.com/user-attachments/assets/c42518fd-8670-49cd-a02d-265231bbf1aa" />


### Componentes Principales

1. **`BugHunterAgent.java`**: Interfaz declarativa de `@AiService` que define los contratos de consulta, generación de comentarios en Markdown y descubrimiento dinámico vía MCP.

2. **`McpConfig.java`**: Configuración del cliente MCP Stdio inicializando el servidor oficial `@modelcontextprotocol/server-github` mediante `npx`.

3. **`IssueScannerService.java`**: Orquestador principal que ejecuta la auditoría paso a paso, aplicando aislamiento de fallos (`try/catch` independiente por issue) y lista de respaldo dinámico.

4. **`EmailReportService.java`**: Servicio de formateo y envío de correo SMTP en formato HTML enriquecido con los hallazgos consolidados.

## Tecnologías Utilizadas

* **Lenguaje:** Java 17

* **Framework:** Spring Boot 4.1.1

* **Orquestador de IA:** LangChain4j (`langchain4j-core`, `langchain4j-mcp`)

* **Modelo LLM:** `GLM-5.3-Flash` (vía OpenAiChatModel con timeout de 3 minutos)

* **Protocolo de Herramientas:** Model Context Protocol (MCP Stdio)

* **Servidor MCP:** `@modelcontextprotocol/server-github` (Node.js/npx)

* **Envío de Correos:** Spring Boot Starter Mail (SMTP HTML)

* **Testing:** JUnit 5, Mockito & Spring Boot Test

## Gestión de Secretos y Variables de Entorno

Ningún secreto, token o contraseña se encuentra hardcodeado en el repositorio. Las credenciales se gestionan a través de variables de entorno mapeadas en `src/main/resources/application.properties`:

| Variable | Descripción | Valor de Ejemplo | 
| ----- | ----- | ----- | 
| `OPENAI_API_KEY` | API Key del modelo LLM | `sk-...` | 
| `OPENAI_BASE_URL` | Endpoint base del proxy API | `https://api.clients.globant.com/v1` | 
| `GITHUB_PERSONAL_ACCESS_TOKEN` | Token Classic de GitHub (`scope: repo`) | `ghp_...` | 
| `SPRING_MAIL_USERNAME` | Correo emisor SMTP | `tu-correo@gmail.com` | 
| `SPRING_MAIL_PASSWORD` | App Password SMTP | `abcd efgh ijkl mnop` | 

## Instrucciones de Ejecución

### Prerrequisitos

* **Java 17** o superior

* **Maven 3.8+**

* **Node.js** (con `npx` habilitado)

### 1. Clonar el repositorio

```
git clone https://github.com/jhontorres-cmd/bug-hunter-agent.git
cd bug-hunter-agent

```

### 2. Configurar variables de entorno (PowerShell)

```
$env:OPENAI_API_KEY="tu_openai_key"
$env:GITHUB_PERSONAL_ACCESS_TOKEN="ghp_tu_github_token"
$env:SPRING_MAIL_USERNAME="tu_correo@gmail.com"
$env:SPRING_MAIL_PASSWORD="tu_app_password"

```

### 3. Compilar y Ejecutar la Aplicación

```
.\mvnw.cmd clean compile
.\mvnw.cmd spring-boot:run

```

## Pruebas Automatizadas

El proyecto cuenta con una suite de pruebas unitarias aisladas mediante **Mockito**, que valida la orquestación del agente y la resiliencia sin consumir cuotas de API reales ni requerir conectividad a red.

### Ejecutar Pruebas

Para ejecutar las pruebas en la consola:

```
.\mvnw.cmd test

```

### Escenarios Validados en `IssueScannerServiceTest`:

1. **Flujo Exitoso Completo**: Descubrimiento dinámico de issues, análisis de causa raíz, publicación de comentario y consolidación en correo.

2. **Resiliencia ante fallos de GitHub**: Garantiza que si la publicación del comentario en GitHub falla (por ejemplo, error HTTP 403 / Rate Limit), el agente captura la excepción y asegura el envío del correo consolidado.

3. **Mecanismo de Respaldo (Fallback)**: Valida que si el descubrimiento dinámico por MCP presenta un timeout, el agente recurre automáticamente a la lista predefinida `[4, 5, 6]`.

## Decisiones Técnicas

* **Uso de GitHub Personal Access Token Classic (`repo`)**: Permite interactuar mediante el servidor MCP con repositorios privados y públicos sin bloqueos de permisos.

* **Doble Formateo Adaptativo de Presentación (Markdown para GitHub vs. HTML para Correo).**

* **Aislamiento de Fallos y Tolerancia a Errores**: Cada issue se procesa dentro de un bloque `try/catch` independiente. Un error en una issue o en la publicación de un comentario no detiene el análisis del resto de las issues ni interrumpe la notificación por correo.

* **Separación entre Razonamiento y Efectos Secundarios**: La auditoría (`auditIssue`) genera el DTO estructurado en memoria, mientras que la publicación en GitHub (`postIssueComment`) se ejecuta en una fase desacoplada.

* **Aislamiento de Entorno de Tests**: Las pruebas usan `application-test.properties` bajo el perfil `test`.
