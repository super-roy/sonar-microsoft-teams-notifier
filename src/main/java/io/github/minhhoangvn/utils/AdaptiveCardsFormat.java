package io.github.minhhoangvn.utils;

import lombok.NonNull;
import org.sonar.api.ce.posttask.PostProjectAnalysisTask.ProjectAnalysis;
import org.sonar.api.ce.posttask.QualityGate.Condition;

import java.util.Collection;
import java.util.Objects;

public class AdaptiveCardsFormat {

    public static String createMessageCardJSONPayload(
            @NonNull ProjectAnalysis analysis, String projectUrl) {
        return createAdaptiveCardTemplate(analysis, projectUrl);
    }

    private static String createAdaptiveCardTemplate(ProjectAnalysis analysis, String projectUrl) {
        String projectName = analysis.getProject().getName();
        String status = analysis.getCeTask().getStatus().name();
        String qualityGate = getQualityGateInfo(analysis);
        String newViolations = getMetricValue(analysis, "new_violations", "0");
        String newCoverage = getMetricValue(analysis, "new_coverage", "N/A");
        String newDuplicatedLinesDensity = getMetricValue(analysis, "new_duplicated_lines_density", "N/A");
        String newSecurityHotspotsReviewed = getMetricValue(analysis, "new_security_hotspots_reviewed", "N/A");

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
            "                            \"url\": \"https://upload.wikimedia.org/wikipedia/commons/thumb/1/1d/Super_Micro_Computer_Logo.svg/330px-Super_Micro_Computer_Logo.svg.png\",\n" +
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
            projectName, status, qualityGate, newViolations, newCoverage, 
            newDuplicatedLinesDensity, newSecurityHotspotsReviewed, projectUrl
        );
    }

    private static String getQualityGateInfo(ProjectAnalysis analysis) {
        if (analysis.getQualityGate() == null) {
            return "N/A";
        }
        String status = analysis.getQualityGate().getStatus().name();
        String name = analysis.getQualityGate().getName();
        return name + " (" + status + ")";
    }

    private static String getMetricValue(ProjectAnalysis analysis, String metricKey, String defaultValue) {
        if (analysis.getQualityGate() == null || analysis.getQualityGate().getConditions() == null) {
            return defaultValue;
        }
        
        Collection<Condition> conditions = analysis.getQualityGate().getConditions();
        return conditions.stream()
                .filter(condition -> condition.getMetricKey().equals(metricKey))
                .findFirst()
                .map(condition -> {
                    String value = condition.getValue();
                    return (value == null || "NO_VALUE".equals(condition.getStatus().name())) ? defaultValue : value;
                })
                .orElse(defaultValue);
    }
}
