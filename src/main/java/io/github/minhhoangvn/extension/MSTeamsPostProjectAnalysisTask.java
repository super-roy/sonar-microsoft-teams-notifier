package io.github.minhhoangvn.extension;

import io.github.minhhoangvn.client.MSTeamsWebHookClient;
import io.github.minhhoangvn.utils.AdaptiveCardsFormat;
import io.github.minhhoangvn.utils.Constants;
import okhttp3.Response;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.ce.posttask.PostProjectAnalysisTask;
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
                // Fallback to direct configuration reading
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
            
            LOGGER.info("MS Teams Plugin: Using pre-validated configuration:");
            LOGGER.info("  - Webhook URL: {}", webhookUrl.isEmpty() ? "[NOT SET]" : "[VALIDATED]");
            LOGGER.info("  - Avatar URL: {}", avatarUrl);
            LOGGER.info("  - Send on failed only: {}", sendOnFailedOnly);
            LOGGER.info("  - Base URL: {}", baseUrl.isEmpty() ? "[DEFAULT]" : baseUrl);
            
            if (StringUtils.isEmpty(webhookUrl)) {
                LOGGER.error("MS Teams Plugin: Webhook URL not configured (this should have been caught in pre-validation)");
                return;
            }

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
            String payload = AdaptiveCardsFormat.createMessageCardJSONPayload(projectAnalysis, projectUrl, avatarUrl);
            
            LOGGER.info("MS Teams Plugin: Sending notification to MS Teams for project: {}", projectAnalysis.getProject().getName());
            LOGGER.info("MS Teams Plugin: Webhook URL: {}", webhookUrl.substring(0, Math.min(50, webhookUrl.length())) + "...");
            LOGGER.debug("MS Teams Plugin: Payload: {}", payload);
            
            MSTeamsWebHookClient client = new MSTeamsWebHookClient();
            try (Response response = client.sendNotify(webhookUrl, payload)) {
                if (response.isSuccessful()) {
                    LOGGER.info("MS Teams Plugin: Successfully sent notification to MS Teams (HTTP {})", response.code());
                } else {
                    String responseBody = response.body() != null ? response.body().string() : "null";
                    LOGGER.error("MS Teams Plugin: Failed to send notification to MS Teams. Response code: {}, body: {}", 
                            response.code(), responseBody);
                }
            }
        } catch (IOException e) {
            LOGGER.error("MS Teams Plugin: Error sending notification to MS Teams", e);
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
            String payload = AdaptiveCardsFormat.createMessageCardJSONPayload(projectAnalysis, projectUrl, avatarUrl);
            
            LOGGER.info("MS Teams Plugin: Sending notification to MS Teams for project: {}", projectAnalysis.getProject().getName());
            LOGGER.info("MS Teams Plugin: Webhook URL: {}", webhookUrl.substring(0, Math.min(50, webhookUrl.length())) + "...");
            LOGGER.debug("MS Teams Plugin: Payload: {}", payload);
            
            MSTeamsWebHookClient client = new MSTeamsWebHookClient();
            try (Response response = client.sendNotify(webhookUrl, payload)) {
                if (response.isSuccessful()) {
                    LOGGER.info("MS Teams Plugin: Successfully sent notification to MS Teams (HTTP {})", response.code());
                } else {
                    String responseBody = response.body() != null ? response.body().string() : "null";
                    LOGGER.error("MS Teams Plugin: Failed to send notification to MS Teams. Response code: {}, body: {}", 
                            response.code(), responseBody);
                }
            } catch (IOException e) {
                LOGGER.error("MS Teams Plugin: IO error during notification sending in fallback mode", e);
            }
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
}
