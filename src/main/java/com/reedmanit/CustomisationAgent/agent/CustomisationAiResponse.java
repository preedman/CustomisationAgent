package com.reedmanit.CustomisationAgent.agent;

import java.util.List;

public record CustomisationAiResponse(
        String summary,
        List<String> stepsCompleted,
        String status
) {

    public String toHumanReadableString() {
        StringBuilder sb = new StringBuilder();
        if (status != null && !status.isBlank()) {
            sb.append("Status: ").append(status).append("\n");
        }
        if (summary != null && !summary.isBlank()) {
            sb.append("Summary: ").append(summary).append("\n");
        }
        if (stepsCompleted != null && !stepsCompleted.isEmpty()) {
            sb.append("Steps:\n- ").append(String.join("\n- ", stepsCompleted));
        }
        return sb.toString().trim();
    }
}
