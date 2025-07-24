package io.github.minhhoangvn.config;

import io.github.minhhoangvn.utils.Constants;
import org.sonar.api.config.Configuration;
import org.sonar.api.server.ServerSide;

@ServerSide
public class MSTeamsConfigurationProvider {
    
    private final Configuration configuration;
    
    public MSTeamsConfigurationProvider(Configuration configuration) {
        this.configuration = configuration;
    }
    
    public boolean isEnabled() {
        return configuration.getBoolean(Constants.ENABLE_NOTIFY).orElse(Constants.DEFAULT_ENABLE_NOTIFY);
    }
    
    public String getWebhookUrl() {
        return configuration.get(Constants.WEBHOOK_URL).orElse("");
    }
    
    public String getAvatarUrl() {
        return configuration.get(Constants.WEBHOOK_MESSAGE_AVATAR).orElse(Constants.DEFAULT_WEBHOOK_MESSAGE_AVATAR);
    }
    
    public boolean getSendOnFailedOnly() {
        return configuration.getBoolean(Constants.WEBHOOK_SEND_ON_FAILED).orElse(Constants.DEFAULT_WEBHOOK_SEND_ON_FAILED);
    }
    
    public String getBaseUrl() {
        return configuration.get(Constants.SONAR_URL).orElse("");
    }
}