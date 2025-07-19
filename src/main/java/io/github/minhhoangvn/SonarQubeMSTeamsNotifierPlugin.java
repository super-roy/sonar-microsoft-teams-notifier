package io.github.minhhoangvn;

import io.github.minhhoangvn.client.MSTeamsWebHookClient;
import io.github.minhhoangvn.utils.AdaptiveCardsFormat;
import org.sonar.api.ce.posttask.PostProjectAnalysisTask;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import okhttp3.Response;

import java.io.IOException;
import java.util.Optional;

public class SonarQubeMSTeamsNotifierPlugin implements PostProjectAnalysisTask {

    private static final Logger LOGGER = Loggers.get(SonarQubeMSTeamsNotifierPlugin.class);
    private static final String WEBHOOK_URL_PROPERTY = "sonar.msteams.webhook.url";
    private static final String PROJECT_URL_PROPERTY = "sonar.core.serverBaseURL";

    @Override
    public void finished(Context context) {
        // For SonarQube 10.x, we need to use a different approach to get configuration
        // Since we can't access configuration directly, we'll use system properties
        String webhookUrl = System.getProperty(WEBHOOK_URL_PROPERTY);
        String baseUrl = System.getProperty(PROJECT_URL_PROPERTY, "");
        
        // Try environment variables as fallback
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            webhookUrl = System.getenv("SONAR_MSTEAMS_WEBHOOK_URL");
        }
        if (baseUrl.isEmpty()) {
            baseUrl = System.getenv("SONAR_CORE_SERVERBASEURL");
            if (baseUrl == null) {
                baseUrl = "";
            }
        }
        
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            LOGGER.warn("MS Teams webhook URL not configured. Set system property: {} or environment variable: SONAR_MSTEAMS_WEBHOOK_URL", WEBHOOK_URL_PROPERTY);
            return;
        }

        try {
            ProjectAnalysis projectAnalysis = context.getProjectAnalysis();
            String projectKey = projectAnalysis.getProject().getKey();
            String projectUrl = baseUrl + "/dashboard?id=" + projectKey;
            
            String payload = AdaptiveCardsFormat.createMessageCardJSONPayload(projectAnalysis, projectUrl);
            
            LOGGER.info("Sending notification to MS Teams for project: {}", projectAnalysis.getProject().getName());
            LOGGER.debug("Payload: {}", payload);
            
            MSTeamsWebHookClient client = new MSTeamsWebHookClient();
            try (Response response = client.sendNotify(webhookUrl, payload)) {
                if (response.isSuccessful()) {
                    LOGGER.info("Successfully sent notification to MS Teams");
                } else {
                    LOGGER.error("Failed to send notification to MS Teams. Response code: {}, body: {}", 
                            response.code(), response.body() != null ? response.body().string() : "null");
                }
            }
        } catch (IOException e) {
            LOGGER.error("Error sending notification to MS Teams", e);
        } catch (Exception e) {
            LOGGER.error("Unexpected error in MS Teams notification", e);
        }
    }

    @Override
    public String getDescription() {
        return "Send SonarQube analysis results to Microsoft Teams";
    }
}