package io.github.minhhoangvn.settings;

import io.github.minhhoangvn.utils.Constants;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.PropertyType;

import java.util.Arrays;
import java.util.List;

public class MSTeamsNotifyProperties {

    private MSTeamsNotifyProperties() {
        // Utility class
    }

    public static List<PropertyDefinition> getProperties() {
        return Arrays.asList(
            getEnableNotifyProperty(),
            getWebhookUrlProperty(),
            getWebhookMessageAvatarProperty(),
            getWebhookSendOnFailedProperty(),
            getWebhookTeamNameProperty()
        );
    }

    public static PropertyDefinition getEnableNotifyProperty() {
        return PropertyDefinition.builder(Constants.ENABLE_NOTIFY)
                .name("Enable Plugin")
                .description("Enable push SonarQube result to Microsoft Teams")
                .category(Constants.CATEGORY)
                .type(PropertyType.BOOLEAN)
                .defaultValue(String.valueOf(Constants.DEFAULT_ENABLE_NOTIFY))
                .index(0)
                .build();
    }

    public static PropertyDefinition getWebhookUrlProperty() {
        return PropertyDefinition.builder(Constants.WEBHOOK_URL)
                .name("Webhook URL")
                .description("Input your Webhook URL for sending SonarQube quality gate result")
                .category(Constants.CATEGORY)
                .type(PropertyType.TEXT)
                .defaultValue("")
                .index(1)
                .build();
    }

    public static PropertyDefinition getWebhookMessageAvatarProperty() {
        return PropertyDefinition.builder(Constants.WEBHOOK_MESSAGE_AVATAR)
                .name("Webhook Message Avatar")
                .description("Input your Webhook avatar URL")
                .category(Constants.CATEGORY)
                .type(PropertyType.STRING)
                .defaultValue(Constants.DEFAULT_WEBHOOK_MESSAGE_AVATAR)
                .index(2)
                .build();
    }

    public static PropertyDefinition getWebhookSendOnFailedProperty() {
        return PropertyDefinition.builder(Constants.WEBHOOK_SEND_ON_FAILED)
                .name("Webhook Send On Failed")
                .description("Only send notify to webhook when analysis failed")
                .category(Constants.CATEGORY)
                .type(PropertyType.BOOLEAN)
                .defaultValue(String.valueOf(Constants.DEFAULT_WEBHOOK_SEND_ON_FAILED))
                .index(3)
                .build();
    }

    public static PropertyDefinition getWebhookTeamNameProperty() {
        return PropertyDefinition.builder(Constants.WEBHOOK_TEAM_NAME)
                .name("Team Name")
                .description("Team name to display in MS Teams notifications")
                .category(Constants.CATEGORY)
                .type(PropertyType.STRING)
                .defaultValue(Constants.DEFAULT_WEBHOOK_TEAM_NAME)
                .index(4)
                .build();
    }
}
