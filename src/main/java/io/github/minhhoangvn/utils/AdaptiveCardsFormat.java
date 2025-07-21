package io.github.minhhoangvn.utils;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.ce.posttask.PostProjectAnalysisTask.ProjectAnalysis;
import org.sonar.api.ce.posttask.QualityGate;

import java.util.Map;
import java.util.stream.Collectors;

public class AdaptiveCardsFormat {

    private AdaptiveCardsFormat() {
        // Utility class
    }

    public static String createMessageCardJSONPayload(ProjectAnalysis analysis, String projectUrl) {
        return createMessageCardJSONPayload(analysis, projectUrl, Constants.DEFAULT_WEBHOOK_MESSAGE_AVATAR);
    }

    public static String createMessageCardJSONPayload(ProjectAnalysis analysis, String projectUrl, String imageUrl) {
        if (analysis == null) {
            throw new IllegalArgumentException("ProjectAnalysis cannot be null");
        }

        // Use default image URL if provided imageUrl is null or empty
        String finalImageUrl = StringUtils.isEmpty(imageUrl) ? Constants.DEFAULT_WEBHOOK_MESSAGE_AVATAR : imageUrl;
        
        return createAdaptiveCardTemplate(analysis, projectUrl, finalImageUrl);
    }

    private static String createAdaptiveCardTemplate(ProjectAnalysis analysis, String projectUrl, String imageUrl) {
        String projectName = analysis.getProject().getName();
        String status = getAnalysisStatus(analysis);
        String qualityGate = getQualityGateInfo(analysis);
        String newViolations = getMetricValue(analysis, "new_violations");
        String newCoverage = getMetricValue(analysis, "new_coverage");
        String newDuplicatedLinesDensity = getMetricValue(analysis, "new_duplicated_lines_density");
        String newSecurityHotspotsReviewed = getMetricValue(analysis, "new_security_hotspots_reviewed");

        return String.format(
            "{\n" +
            "    \"type\": \"AdaptiveCard\",\n" +
            "    \"$schema\": \"http://adaptivecards.io/schemas/adaptive-card.json\",\n" +
            "    \"version\": \"1.5\",\n" +
            "    \"body\": [\n" +
            "        {\n" +
            "            \"type\": \"TextBlock\",\n" +
            "            \"size\": \"Medium\",\n" +
            "            \"weight\": \"Bolder\",\n" +
            "            \"text\": \"SonarQube Analysis Result\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"type\": \"ColumnSet\",\n" +
            "            \"columns\": [\n" +
            "                {\n" +
            "                    \"type\": \"Column\",\n" +
            "                    \"items\": [\n" +
            "                        {\n" +
            "                            \"type\": \"Image\",\n" +
            "                            \"style\": \"Person\",\n" +
            "                            \"url\": \"%s\",\n" +
            "                            \"altText\": \"Supermicro IT2 DevOps Team\",\n" +
            "                            \"size\": \"Small\"\n" +
            "                        }\n" +
            "                    ],\n" +
            "                    \"width\": \"auto\"\n" +
            "                },\n" +
            "                {\n" +
            "                    \"type\": \"Column\",\n" +
            "                    \"items\": [\n" +
            "                        {\n" +
            "                            \"type\": \"TextBlock\",\n" +
            "                            \"weight\": \"Bolder\",\n" +
            "                            \"text\": \"Supermicro IT2 DevOps Team\",\n" +
            "                            \"wrap\": true\n" +
            "                        }\n" +
            "                    ],\n" +
            "                    \"width\": \"stretch\"\n" +
            "                }\n" +
            "            ]\n" +
            "        },\n" +
            "        {\n" +
            "            \"type\": \"TextBlock\",\n" +
            "            \"text\": \"%s SonarQube Analysis Result\",\n" +
            "            \"wrap\": true,\n" +
            "            \"weight\": \"Bolder\",\n" +
            "            \"color\": \"Accent\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"type\": \"FactSet\",\n" +
            "            \"facts\": [\n" +
            "                {\n" +
            "                    \"title\": \"Status\",\n" +
            "                    \"value\": \"%s\"\n" +
            "                },\n" +
            "                {\n" +
            "                    \"title\": \"Quality Gate\",\n" +
            "                    \"value\": \"%s\"\n" +
            "                },\n" +
            "                {\n" +
            "                    \"title\": \"New Violations\",\n" +
            "                    \"value\": \"%s\"\n" +
            "                },\n" +
            "                {\n" +
            "                    \"title\": \"New Coverage\",\n" +
            "                    \"value\": \"%s\"\n" +
            "                },\n" +
            "                {\n" +
            "                    \"title\": \"New Duplicated Lines Density\",\n" +
            "                    \"value\": \"%s\"\n" +
            "                },\n" +
            "                {\n" +
            "                    \"title\": \"New Security Hotspots Reviewed\",\n" +
            "                    \"value\": \"%s\"\n" +
            "                }\n" +
            "            ]\n" +
            "        }\n" +
            "    ],\n" +
            "    \"actions\": [\n" +
            "        {\n" +
            "            \"type\": \"Action.OpenUrl\",\n" +
            "            \"title\": \"View Analysis\",\n" +
            "            \"url\": \"%s\"\n" +
            "        }\n" +
            "    ]\n" +
            "}",
            imageUrl,           // %s - Image URL in the Image element
            projectName,        // %s - Project name in the TextBlock 
            status,             // %s - Status value in FactSet
            qualityGate,        // %s - Quality Gate value in FactSet
            newViolations,      // %s - New Violations value in FactSet
            newCoverage,        // %s - New Coverage value in FactSet
            newDuplicatedLinesDensity,      // %s - New Duplicated Lines Density value in FactSet
            newSecurityHotspotsReviewed,    // %s - New Security Hotspots Reviewed value in FactSet
            projectUrl          // %s - Project URL in Action.OpenUrl
        );
    }

    private static String getQualityGateInfo(ProjectAnalysis analysis) {
        if (analysis.getQualityGate() == null) {
            return "N/A";
        }
        
        QualityGate qualityGate = analysis.getQualityGate();
        String name = qualityGate.getName() != null ? qualityGate.getName() : "Unknown";
        String status = qualityGate.getStatus() != null ? qualityGate.getStatus().toString() : "UNKNOWN";
        
        return String.format("%s (%s)", name, status);
    }

    private static String getAnalysisStatus(ProjectAnalysis analysis) {
        if (analysis.getCeTask() == null || analysis.getCeTask().getStatus() == null) {
            return "UNKNOWN";
        }
        return analysis.getCeTask().getStatus().toString();
    }

    private static String getMetricValue(ProjectAnalysis analysis, String metricKey) {
        if (analysis.getQualityGate() == null || analysis.getQualityGate().getConditions() == null) {
            return "N/A";
        }

        Map<String, String> metrics = analysis.getQualityGate().getConditions().stream()
                .filter(condition -> condition.getMetricKey() != null)
                .collect(Collectors.toMap(
                        QualityGate.Condition::getMetricKey,
                        condition -> condition.getValue() != null ? condition.getValue() : "N/A",
                        (existing, replacement) -> existing
                ));

        return metrics.getOrDefault(metricKey, "N/A");
    }
}
