package io.github.minhhoangvn.extension;

import io.github.minhhoangvn.client.MSTeamsWebHookClient;
import io.github.minhhoangvn.utils.AdaptiveCardsFormat;
import io.github.minhhoangvn.utils.Constants;
import okhttp3.Response;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.ce.posttask.PostProjectAnalysisTask;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import java.io.IOException;
import java.util.Optional;

public class MSTeamsPostProjectAnalysisTask implements PostProjectAnalysisTask {

    private static final Logger LOGGER = Loggers.get(MSTeamsPostProjectAnalysisTask.class);

    @Override
    public void finished(Context context) {
        try {
            // Try to get configuration from context first (for newer SonarQube versions)
            Configuration config = getConfiguration(context);
            
            // Check if plugin is enabled
            boolean isEnabled = config.getBoolean(Constants.ENABLE_NOTIFY).orElse(false);
            if (!isEnabled) {
                LOGGER.info("MS Teams notification plugin is disabled");
                return;
            }
            
            String webhookUrl = config.get(Constants.WEBHOOK_URL).orElse(null);
            String avatarUrl = config.get(Constants.WEBHOOK_MESSAGE_AVATAR).orElse("");
            boolean sendOnFailedOnly = config.getBoolean(Constants.WEBHOOK_SEND_ON_FAILED).orElse(true);
            String baseUrl = getServerBaseUrl(config);
            
            if (StringUtils.isEmpty(webhookUrl)) {
                LOGGER.warn("MS Teams webhook URL not configured. Please configure it in Administration > Configuration > Microsoft Teams");
                return;
            }

            ProjectAnalysis projectAnalysis = context.getProjectAnalysis();
            
            // Check if we should send notification based on settings
            if (sendOnFailedOnly && !isAnalysisFailed(projectAnalysis)) {
                LOGGER.info("Analysis passed and 'Send on failed only' is enabled. Skipping notification.");
                return;
            }
            
            String projectKey = projectAnalysis.getProject().getKey();
            String projectUrl = baseUrl + "/dashboard?id=" + projectKey;
            
            String payload = AdaptiveCardsFormat.createMessageCardJSONPayload(projectAnalysis, projectUrl, avatarUrl);
            
            LOGGER.info("Sending notification to MS Teams for project: {}", projectAnalysis.getProject().getName());
            LOGGER.debug("Payload: {}", payload);
            
            MSTeamsWebHookClient client = new MSTeamsWebHookClient();
            try (Response response = client.sendNotify(webhookUrl, payload)) {
                if (response.isSuccessful()) {
                    LOGGER.info("Successfully sent notification to MS Teams");
                } else {
                    String responseBody = response.body() != null ? response.body().string() : "null";
                    LOGGER.error("Failed to send notification to MS Teams. Response code: {}, body: {}", 
                            response.code(), responseBody);
                }
            }
        } catch (IOException e) {
            LOGGER.error("Error sending notification to MS Teams", e);
        } catch (Exception e) {
            LOGGER.error("Unexpected error in MS Teams notification", e);
        }
    }
    
    private Configuration getConfiguration(Context context) {
        // For SonarQube 10.x, try to get configuration from system properties as fallback
        return new ConfigurationWrapper(context);
    }
    
    private String getServerBaseUrl(Configuration config) {
        // Try to get from various sources
        String baseUrl = config.get("sonar.core.serverBaseURL").orElse("");
        if (baseUrl.isEmpty()) {
            baseUrl = System.getProperty("sonar.core.serverBaseURL", "");
        }
        if (baseUrl.isEmpty()) {
            baseUrl = System.getenv("SONAR_CORE_SERVERBASEURL");
            if (baseUrl == null) {
                baseUrl = "";
            }
        }
        return baseUrl;
    }
    
    private boolean isAnalysisFailed(ProjectAnalysis projectAnalysis) {
        return projectAnalysis.getQualityGate() != null && 
               projectAnalysis.getQualityGate().getStatus() == org.sonar.api.ce.posttask.QualityGate.Status.ERROR;
    }

    @Override
    public String getDescription() {
        return "MS Teams notification extension for SonarQube analysis results";
    }
    
    // Wrapper class to handle configuration access
    private static class ConfigurationWrapper implements Configuration {
        private final Context context;
        
        public ConfigurationWrapper(Context context) {
            this.context = context;
        }
        
        @Override
        public Optional<String> get(String key) {
            // Try system property first
            String value = System.getProperty(key);
            if (value != null) {
                return Optional.of(value);
            }
            
            // Try environment variable
            String envKey = key.replace(".", "_").toUpperCase();
            value = System.getenv(envKey);
            if (value != null) {
                return Optional.of(value);
            }
            
            return Optional.empty();
        }
        
        @Override
        public Optional<Boolean> getBoolean(String key) {
            return get(key).map(Boolean::parseBoolean);
        }
        
        @Override
        public Optional<Integer> getInt(String key) {
            return get(key).map(Integer::parseInt);
        }
        
        @Override
        public Optional<Long> getLong(String key) {
            return get(key).map(Long::parseLong);
        }
        
        @Override
        public Optional<Double> getDouble(String key) {
            return get(key).map(Double::parseDouble);
        }
        
        @Override
        public String[] getStringArray(String key) {
            return get(key).map(s -> s.split(",")).orElse(new String[0]);
        }
        
        @Override
        public boolean hasKey(String key) {
            return get(key).isPresent();
        }
    }
}
