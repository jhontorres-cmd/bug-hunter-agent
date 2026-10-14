package bughunteragent.dto;

import java.util.List;

public record AnalysisResult(
        int issueNumber,
        String issueTitle,
        String issueUrl,
        String locationInfo,
        String rootCause,
        List<SolutionOption> solutions,
        String recommendation
) {}