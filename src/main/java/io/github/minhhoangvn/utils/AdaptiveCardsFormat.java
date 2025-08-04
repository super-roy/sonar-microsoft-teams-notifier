package io.github.minhhoangvn.utils;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.ce.posttask.PostProjectAnalysisTask.ProjectAnalysis;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import java.util.Map;
import java.util.stream.Collectors;

public class AdaptiveCardsFormat {

    private static final Logger LOGGER = Loggers.get(AdaptiveCardsFormat.class);
    private static Configuration configuration;

    private AdaptiveCardsFormat() {
        // Utility class
    }

    /**
     * Set the SonarQube configuration for accessing properties at runtime.
     */
    public static void setConfiguration(Configuration config) {
        configuration = config;
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
        String teamName = getTeamNameFromConfig();
        
        return createAdaptiveCardTemplate(analysis, projectUrl, finalImageUrl, teamName);
    }

    public static String getTeamNameFromConfig() {
        if (configuration != null) {
            String teamName = configuration.get(Constants.WEBHOOK_TEAM_NAME).orElse(Constants.DEFAULT_WEBHOOK_TEAM_NAME);
            LOGGER.info("AdaptiveCardsFormat.getTeamNameFromConfig() - configuration not null");
            LOGGER.info("Constants.WEBHOOK_TEAM_NAME = {}", Constants.WEBHOOK_TEAM_NAME);
            LOGGER.info("Constants.DEFAULT_WEBHOOK_TEAM_NAME = '{}'", Constants.DEFAULT_WEBHOOK_TEAM_NAME);
            LOGGER.info("configuration.get() returned = '{}'", teamName);
            if (!StringUtils.isEmpty(teamName)) {
                LOGGER.info("Returning configured team name: '{}'", teamName);
                return teamName;
            }
        } else {
            LOGGER.warn("AdaptiveCardsFormat.getTeamNameFromConfig() - configuration is null!");
        }
        LOGGER.info("Returning fallback team name: {}", Constants.DEFAULT_WEBHOOK_TEAM_NAME);
        return Constants.DEFAULT_WEBHOOK_TEAM_NAME; // fallback if configuration is not available or empty
    }

    private static String createAdaptiveCardTemplate(ProjectAnalysis analysis, String projectUrl, String imageUrl, String teamName) {
        String projectName = analysis.getProject().getName();
        String status = getAnalysisStatus(analysis);
        String qualityGate = getQualityGateInfo(analysis);
        String newViolations = getMetricValue(analysis, "new_violations");
        String newCoverage = getMetricValue(analysis, "new_coverage");
        String newDuplicatedLinesDensity = getMetricValue(analysis, "new_duplicated_lines_density");
        String newSecurityHotspotsReviewed = getMetricValue(analysis, "new_security_hotspots_reviewed");

        return String.format("""
            {
                "attachments": [
                    {
                        "contentType": "application/vnd.microsoft.card.adaptive",
                        "content": {
                            "type": "AdaptiveCard",
                            "$schema": "http://adaptivecards.io/schemas/adaptive-card.json",
                            "version": "1.5",
                            "body": [
                                {
                                    "type": "TextBlock",
                                    "size": "Medium",
                                    "weight": "Bolder",
                                    "text": "SonarQube Analysis Result"
                                },
                                {
                                    "type": "ColumnSet",
                                    "columns": [
                                        {
                                            "type": "Column",
                                            "items": [
                                                {
                                                    "type": "Image",
                                                    "style": "Person",
                                                    "url": "%s",
                                                    "altText": "%s",
                                                    "size": "Small"
                                                }
                                            ],
                                            "width": "auto"
                                        },
                                        {
                                            "type": "Column",
                                            "items": [
                                                {
                                                    "type": "TextBlock",
                                                    "weight": "Bolder",
                                                    "text": "%s",
                                                    "wrap": true
                                                }
                                            ],
                                            "width": "stretch"
                                        }
                                    ]
                                },
                                {
                                    "type": "TextBlock",
                                    "text": "%s SonarQube Analysis Result",
                                    "wrap": true,
                                    "weight": "Bolder",
                                    "color": "Accent"
                                },
                                {
                                    "type": "FactSet",
                                    "facts": [
                                        {
                                            "title": "Status",
                                            "value": "%s"
                                        },
                                        {
                                            "title": "Quality Gate",
                                            "value": "%s"
                                        },
                                        {
                                            "title": "New Violations",
                                            "value": "%s"
                                        },
                                        {
                                            "title": "New Coverage",
                                            "value": "%s"
                                        },
                                        {
                                            "title": "New Duplicated Lines Density",
                                            "value": "%s"
                                        },
                                        {
                                            "title": "New Security Hotspots Reviewed",
                                            "value": "%s"
                                        }
                                    ]
                                }
                            ],
                            "actions": [
                                {
                                    "type": "Action.OpenUrl",
                                    "title": "View Analysis",
                                    "url": "%s"
                                }
                            ]
                        },
                        "contentUrl": null
                    }
                ],
                "type": "message"
            }
            """,
            imageUrl,           // %s - Image URL in the Image element
            teamName,           // %s - Team name as altText
            teamName,           // %s - Team name in TextBlock
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
