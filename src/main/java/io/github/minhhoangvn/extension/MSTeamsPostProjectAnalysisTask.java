package io.github.minhhoangvn.extension;

import io.github.minhhoangvn.client.MSTeamsWebHookClient;
import io.github.minhhoangvn.utils.AdaptiveCardsFormat;
import io.github.minhhoangvn.utils.Constants;
import okhttp3.Response;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.ce.posttask.PostProjectAnalysisTask;
import org.sonar.api.ce.posttask.QualityGate; // ADD THIS IMPORT
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import java.io.IOException;
import java.util.Map;

public class MSTeamsPostProjectAnalysisTask implements PostProjectAnalysisTask {

    private static final Logger LOGGER = Loggers.get(MSTeamsPostProjectAnalysisTask.class);

    public MSTeamsPostProjectAnalysisTask() {
        // Default constructor for SonarQube plugin system
    }

    @Override
    public void finished(Context context) {
        LOGGER.info("=== MS Teams Plugin: Analysis finished callback triggered ===");
        
        try {
            LOGGER.info("MS Teams Plugin: Checking pre-validated configuration...");
            
            // Check if configuration was validated by MSTeamsPreProjectAnalysisTask
            if (!MSTeamsPreProjectAnalysisTask.isConfigurationValidated()) {
                LOGGER.warn("MS Teams Plugin: Configuration not validated by pre-analysis task, falling back to direct configuration reading");
                handleWithDirectConfiguration(context);
                return;
            }
            
            // Use pre-validated configuration
            boolean isEnabled = MSTeamsPreProjectAnalysisTask.getValidatedBooleanConfig(Constants.ENABLE_NOTIFY, Constants.DEFAULT_ENABLE_NOTIFY);
            LOGGER.info("MS Teams Plugin: Plugin enabled = {} (pre-validated)", isEnabled);
            
            if (!isEnabled) {
                LOGGER.info("MS Teams notification plugin is disabled.");
                return;
            }
            
            String webhookUrl = MSTeamsPreProjectAnalysisTask.getValidatedConfig(Constants.WEBHOOK_URL, "");
            String avatarUrl = MSTeamsPreProjectAnalysisTask.getValidatedConfig(Constants.WEBHOOK_MESSAGE_AVATAR, Constants.DEFAULT_WEBHOOK_MESSAGE_AVATAR);
            boolean sendOnFailedOnly = MSTeamsPreProjectAnalysisTask.getValidatedBooleanConfig(Constants.WEBHOOK_SEND_ON_FAILED, Constants.DEFAULT_WEBHOOK_SEND_ON_FAILED);
            String baseUrl = MSTeamsPreProjectAnalysisTask.getValidatedConfig(Constants.SONAR_URL, "");
            String teamName = MSTeamsPreProjectAnalysisTask.getValidatedConfig(Constants.WEBHOOK_TEAM_NAME, Constants.DEFAULT_WEBHOOK_TEAM_NAME);
            
            LOGGER.info("MS Teams Plugin: Using pre-validated configuration:");
            LOGGER.info("  - Webhook URL: {}", webhookUrl.isEmpty() ? "[NOT SET]" : webhookUrl);
            LOGGER.info("  - Avatar URL: {}", avatarUrl);
            LOGGER.info("  - Send on failed only: {}", sendOnFailedOnly);
            LOGGER.info("  - Base URL: {}", baseUrl.isEmpty() ? "[DEFAULT]" : baseUrl);
            LOGGER.info("  - Team name: {}", StringUtils.isEmpty(teamName) ? "[NOT SET - will use 'DevOps Team']" : teamName);
            
            if (StringUtils.isEmpty(webhookUrl)) {
                LOGGER.error("MS Teams Plugin: Webhook URL not configured (this should have been caught in pre-validation)");
                return;
            }

            // REPLACE the duplicated logic with a call to sendNotification
            sendNotification(context, webhookUrl, avatarUrl, sendOnFailedOnly, baseUrl, "pre-validated");
            
        } catch (Exception e) {
            LOGGER.error("MS Teams Plugin: Unexpected error in MS Teams notification", e);
        }
        
        LOGGER.info("=== MS Teams Plugin: Analysis finished callback completed ===");
    }
    
    // Fallback method when pre-validation is not available
    private void handleWithDirectConfiguration(Context context) {
        LOGGER.info("MS Teams Plugin: Using direct configuration reading (fallback mode)");
        
        try {
            // Get configuration directly from multiple sources
            boolean isEnabled = getBooleanConfigValue(context, Constants.ENABLE_NOTIFY, Constants.DEFAULT_ENABLE_NOTIFY);
            LOGGER.info("MS Teams Plugin: Plugin enabled = {} (direct read)", isEnabled);
            
            if (!isEnabled) {
                LOGGER.info("MS Teams notification plugin is disabled.");
                return;
            }
            
            String webhookUrl = getStringConfigValue(context, Constants.WEBHOOK_URL, "");
            String avatarUrl = getStringConfigValue(context, Constants.WEBHOOK_MESSAGE_AVATAR, Constants.DEFAULT_WEBHOOK_MESSAGE_AVATAR);
            boolean sendOnFailedOnly = getBooleanConfigValue(context, Constants.WEBHOOK_SEND_ON_FAILED, Constants.DEFAULT_WEBHOOK_SEND_ON_FAILED);
            String baseUrl = getStringConfigValue(context, Constants.SONAR_URL, "");
            
            LOGGER.info("MS Teams Plugin: Direct configuration values:");
            LOGGER.info("  - Webhook URL: {}", webhookUrl.isEmpty() ? "[NOT SET]" : "[SET - length: " + webhookUrl.length() + "]");
            LOGGER.info("  - Avatar URL: {}", avatarUrl);
            LOGGER.info("  - Send on failed only: {}", sendOnFailedOnly);
            LOGGER.info("  - Base URL: {}", baseUrl.isEmpty() ? "[NOT SET]" : baseUrl);
            
            if (StringUtils.isEmpty(webhookUrl)) {
                LOGGER.warn("MS Teams webhook URL not configured. Please configure '{}' in Administration > Configuration > Microsoft Teams", Constants.WEBHOOK_URL);
                return;
            }
            
            // REPLACE the duplicated logic with a call to sendNotification
            sendNotification(context, webhookUrl, avatarUrl, sendOnFailedOnly, baseUrl, "direct read");
            
        } catch (Exception e) {
            LOGGER.error("MS Teams Plugin: Unexpected error in fallback configuration mode", e);
        }
    }
    
    private String getStringConfigValue(Context context, String key, String defaultValue) {
        // Priority: Scanner properties -> System properties -> Environment variables -> Default
        
        try {
            // 1. Try scanner properties first
            Map<String, String> scannerProperties = context.getProjectAnalysis().getScannerContext().getProperties();
            String value = scannerProperties.get(key);
            if (value != null && !value.trim().isEmpty()) {
                LOGGER.debug("Found in scanner properties: {} = {}", key, value);
                return value;
            }
        } catch (Exception e) {
            LOGGER.debug("Could not access scanner properties: {}", e.getMessage());
        }
        
        // 2. Try system properties
        String systemValue = System.getProperty(key);
        if (systemValue != null && !systemValue.trim().isEmpty()) {
            LOGGER.debug("Found in system properties: {} = {}", key, systemValue);
            return systemValue;
        }
        
        // 3. Try environment variables
        String envKey = key.replace(".", "_").toUpperCase();
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.trim().isEmpty()) {
            LOGGER.debug("Found in environment: {} = {}", envKey, envValue);
            return envValue;
        }
        
        LOGGER.debug("Property {} not found, using default: {}", key, defaultValue);
        return defaultValue;
    }
    
    private boolean getBooleanConfigValue(Context context, String key, boolean defaultValue) {
        String stringValue = getStringConfigValue(context, key, String.valueOf(defaultValue));
        return Boolean.parseBoolean(stringValue);
    }
    
    private String buildProjectUrl(String baseUrl, String projectKey) {
        if (StringUtils.isEmpty(baseUrl)) {
            // Try to get from environment or system properties as fallback
            baseUrl = System.getProperty(Constants.SONAR_URL, "");
            if (baseUrl.isEmpty()) {
                baseUrl = System.getenv("SONAR_CORE_SERVERBASEURL");
                if (baseUrl == null) {
                    baseUrl = "http://localhost:9000"; // Default fallback
                }
            }
        }
        
        // Ensure baseUrl doesn't end with slash
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        
        return baseUrl + "/dashboard?id=" + projectKey;
    }
    
    private boolean isAnalysisFailed(ProjectAnalysis projectAnalysis) {
        return projectAnalysis.getQualityGate() != null && 
               projectAnalysis.getQualityGate().getStatus() == 
               org.sonar.api.ce.posttask.QualityGate.Status.ERROR;
    }

    @Override
    public String getDescription() {
        return "MS Teams notification extension for SonarQube analysis results";
    }
    
    private void sendNotification(Context context, String webhookUrl, String avatarUrl, 
                        boolean sendOnFailedOnly, String baseUrl, String configSource) {
        try {
            // Inject pre-validated configuration into AdaptiveCardsFormat
            AdaptiveCardsFormat.setConfiguration(new PreValidatedConfiguration());
            
            ProjectAnalysis projectAnalysis = context.getProjectAnalysis();
            LOGGER.info("MS Teams Plugin: Project analysis details:");
            LOGGER.info("  - Project name: {}", projectAnalysis.getProject().getName());
            LOGGER.info("  - Project key: {}", projectAnalysis.getProject().getKey());
            
            // Check if we should send notification based on settings
            boolean isAnalysisFailed = isAnalysisFailed(projectAnalysis);
            LOGGER.info("  - Analysis failed: {}", isAnalysisFailed);
            LOGGER.info("  - Send on failed only setting: {}", sendOnFailedOnly);
            
            if (sendOnFailedOnly && !isAnalysisFailed) {
                LOGGER.info("Analysis passed and 'Send on failed only' is enabled. Skipping notification.");
                return;
            }
            
            String projectKey = projectAnalysis.getProject().getKey();
            String projectUrl = buildProjectUrl(baseUrl, projectKey);
            
            LOGGER.info("MS Teams Plugin: Building notification payload...");
            
            // Wrap the payload creation in try-catch to handle NO_VALUE conditions
            String payload;
            try {
                payload = AdaptiveCardsFormat.createMessageCardJSONPayload(projectAnalysis, projectUrl, avatarUrl);
            } catch (IllegalStateException e) {
                if (e.getMessage() != null && e.getMessage().contains("NO_VALUE")) {
                    LOGGER.warn("MS Teams Plugin: Quality gate contains conditions with NO_VALUE status, creating NO_VALUE-aware payload");
                    // Use the NO_VALUE-aware payload instead of skipping
                    payload = createNoValueAwarePayload(projectAnalysis, projectUrl, avatarUrl);
                } else {
                    throw e; // Re-throw if it's a different issue
                }
            } catch (Exception e) {
                LOGGER.error("MS Teams Plugin: Error creating notification payload: {}", e.getMessage());
                return; // Skip sending notification
            }
            
            LOGGER.info("MS Teams Plugin: Sending notification to MS Teams for project: {} (config: {})", 
                       projectAnalysis.getProject().getName(), configSource);
            LOGGER.info("MS Teams Plugin: Webhook URL: {}", webhookUrl.substring(0, Math.min(50, webhookUrl.length())) + "...");
            LOGGER.debug("MS Teams Plugin: Payload: {}", payload);
            
            MSTeamsWebHookClient client = new MSTeamsWebHookClient();
            try (Response response = client.sendNotify(webhookUrl, payload)) {
                if (response.isSuccessful()) {
                    LOGGER.info("MS Teams Plugin: Successfully sent notification to MS Teams (HTTP {})", response.code());
                } else {
                    String responseBody = "null";
                    try {
                        responseBody = response.body() != null ? response.body().string() : "null";
                    } catch (IOException e) {
                        LOGGER.debug("Could not read response body: {}", e.getMessage());
                        responseBody = "Could not read response body";
                    }
                    LOGGER.error("MS Teams Plugin: Failed to send notification to MS Teams. Response code: {}, body: {}", 
                            response.code(), responseBody);
                }
            }
        } catch (IOException e) {
            LOGGER.error("MS Teams Plugin: IO error sending notification to MS Teams", e);
        } catch (Exception e) {
            LOGGER.error("MS Teams Plugin: Unexpected error sending notification to MS Teams", e);
        }
    }
    
    // Add this helper method:
    private String createSimplifiedPayload(ProjectAnalysis projectAnalysis, String projectUrl, String avatarUrl) {
        try {
            // Create a simplified AdaptiveCard payload without problematic quality gate conditions
            QualityGate qualityGate = projectAnalysis.getQualityGate();
            String status = projectAnalysis.getCeTask().getStatus().name();
            String qgStatus = qualityGate != null ? qualityGate.getStatus().name() : "UNKNOWN";
            String qgName = qualityGate != null ? qualityGate.getName() : "Unknown";
            
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
                                        "type": "TextBlock",
                                        "text": "%s SonarQube Analysis Result Simplified",
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
                                                "value": "%s (%s)"
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
                projectAnalysis.getProject().getName(),
                status,
                qgName,
                qgStatus,
                projectUrl);
        } catch (Exception e) {
            LOGGER.error("Failed to create simplified payload", e);
            return "{ \"error\": \"Failed to create notification payload\" }";
        }
    }
    
    private String createNoValueAwarePayload(ProjectAnalysis projectAnalysis, String projectUrl, String avatarUrl) {
        try {
            QualityGate qualityGate = projectAnalysis.getQualityGate();
            String status = projectAnalysis.getCeTask().getStatus().name();
            String qgStatus = qualityGate != null ? qualityGate.getStatus().name() : "UNKNOWN";
            String qgName = qualityGate != null ? qualityGate.getName() : "Unknown";
            // Use the same configuration source as AdaptiveCardsFormat by calling it directly
            String teamName = AdaptiveCardsFormat.getTeamNameFromConfig();

            // Build facts array with safe condition handling
            StringBuilder factsJson = new StringBuilder();
            factsJson.append("\"facts\": [");
            factsJson.append(String.format("{\"title\": \"Status\", \"value\": \"%s\"}", status));
            factsJson.append(String.format(",{\"title\": \"Quality Gate\", \"value\": \"%s (%s)\"}", qgName, qgStatus));
            
            // Add quality gate conditions safely
            if (qualityGate != null && qualityGate.getConditions() != null) {
                for (QualityGate.Condition condition : qualityGate.getConditions()) {
                    try {
                        String conditionValue;
                        if (condition.getStatus() == QualityGate.EvaluationStatus.NO_VALUE) {
                            conditionValue = "N/A (No new code)";
                        } else {
                            conditionValue = condition.getValue() != null ? condition.getValue() : "N/A";
                        }
                        
                        String conditionName = getConditionDisplayName(condition.getMetricKey());
                        factsJson.append(String.format(",{\"title\": \"%s\", \"value\": \"%s\"}", 
                                                      conditionName, conditionValue));
                    } catch (Exception e) {
                        LOGGER.debug("Error processing condition {}: {}", condition.getMetricKey(), e.getMessage());
                        // Continue with other conditions
                    }
                }
            }
            
            factsJson.append("]");
            
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
                                        "text": "%s SonarQube Analysis Result NO_VALUE Aware",
                                        "wrap": true,
                                        "weight": "Bolder",
                                        "color": "Accent"
                                    },
                                    {
                                        "type": "FactSet",
                                        %s
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
                avatarUrl,
                teamName,
                teamName,
                projectAnalysis.getProject().getName(),
                factsJson.toString(),
                projectUrl);
        } catch (Exception e) {
            LOGGER.error("Failed to create NO_VALUE-aware payload", e);
            return createSimplifiedPayload(projectAnalysis, projectUrl, avatarUrl);
        }
    }

    private String getConditionDisplayName(String metricKey) {
        // Map metric keys to user-friendly names
        switch (metricKey) {
            case "new_violations": return "New Issues";
            case "new_coverage": return "New Coverage";
            case "new_duplicated_lines_density": return "New Duplicated Lines Density";
            case "new_security_hotspots_reviewed": return "New Security Hotspots Reviewed";
            case "new_maintainability_rating": return "New Maintainability Rating";
            case "new_reliability_rating": return "New Reliability Rating";
            case "new_security_rating": return "New Security Rating";
            default: return metricKey.replace("_", " ").toUpperCase();
        }
    }
    
    /**
     * Configuration wrapper that uses pre-validated configuration from MSTeamsPreProjectAnalysisTask
     */
    private static class PreValidatedConfiguration implements org.sonar.api.config.Configuration {
        
        @Override
        public java.util.Optional<String> get(String key) {
            String value = MSTeamsPreProjectAnalysisTask.getValidatedConfig(key, "");
            LOGGER.info("PreValidatedConfiguration.get({}) = '{}' (empty: {})", key, value, StringUtils.isEmpty(value));
            return StringUtils.isEmpty(value) ? java.util.Optional.empty() : java.util.Optional.of(value);
        }
        
        @Override
        public boolean hasKey(String key) {
            boolean has = get(key).isPresent();
            LOGGER.info("PreValidatedConfiguration.hasKey({}) = {}", key, has);
            return has;
        }
        
        @Override
        public String[] getStringArray(String key) {
            return get(key).map(value -> value.split(",")).orElse(new String[0]);
        }
    }
}
