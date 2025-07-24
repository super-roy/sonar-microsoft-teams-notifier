package io.github.minhhoangvn.extension;

import io.github.minhhoangvn.client.MSTeamsWebHookClient;
import io.github.minhhoangvn.config.MSTeamsConfigurationProvider;
import io.github.minhhoangvn.utils.AdaptiveCardsFormat;
import io.github.minhhoangvn.utils.Constants;
import okhttp3.Response;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.ce.posttask.PostProjectAnalysisTask;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import java.io.IOException;

public class MSTeamsPostProjectAnalysisTask implements PostProjectAnalysisTask {

    private static final Logger LOGGER = Loggers.get(MSTeamsPostProjectAnalysisTask.class);
    
    private final MSTeamsConfigurationProvider configProvider;
    
    public MSTeamsPostProjectAnalysisTask(MSTeamsConfigurationProvider configProvider) {
        this.configProvider = configProvider;
    }

    @Override
    public void finished(Context context) {
        LOGGER.info("=== MS Teams Plugin: Analysis finished callback triggered ===");
        
        try {
            LOGGER.info("MS Teams Plugin: Checking configuration...");
            
            // Get configuration from the injected provider
            boolean isEnabled = configProvider.isEnabled();
            LOGGER.info("MS Teams Plugin: Plugin enabled = {} (from key: {})", isEnabled, Constants.ENABLE_NOTIFY);
            
            if (!isEnabled) {
                LOGGER.info("MS Teams notification plugin is disabled. Set '{}' to 'true' to enable.", Constants.ENABLE_NOTIFY);
                return;
            }
            
            String webhookUrl = configProvider.getWebhookUrl();
            String avatarUrl = configProvider.getAvatarUrl();
            boolean sendOnFailedOnly = configProvider.getSendOnFailedOnly();
            String baseUrl = configProvider.getBaseUrl();
            
            LOGGER.info("MS Teams Plugin: Configuration values from SonarQube settings:");
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
            }
        } catch (IOException e) {
            LOGGER.error("MS Teams Plugin: Error sending notification to MS Teams", e);
        } catch (Exception e) {
            LOGGER.error("MS Teams Plugin: Unexpected error in MS Teams notification", e);
        }
        
        LOGGER.info("=== MS Teams Plugin: Analysis finished callback completed ===");
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
