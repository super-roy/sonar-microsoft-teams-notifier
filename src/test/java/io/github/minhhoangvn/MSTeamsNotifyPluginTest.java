package io.github.minhhoangvn;

import io.github.minhhoangvn.extension.MSTeamsPostProjectAnalysisTask;
import io.github.minhhoangvn.settings.MSTeamsNotifyProperties;
import org.sonar.api.Plugin;
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

import static org.mockito.Mockito.*;

public class MSTeamsNotifyPluginTest {

    private MSTeamsNotifyPlugin plugin;
    private Plugin.Context context;

    @BeforeMethod
    public void setUp() {
        plugin = new MSTeamsNotifyPlugin();
        context = mock(Plugin.Context.class);
    }

    /** Method under test: {@link MSTeamsNotifyPlugin#define(Plugin.Context)} */
    @Test
    public void testDefinePluginWithCorrectExtensions() {
        // Act
        plugin.define(context);

        // Assert - Verify the main task is registered
        verify(context).addExtension(MSTeamsPostProjectAnalysisTask.class);
        
        // Verify that properties are registered (at least once for each property)
        verify(context, atLeastOnce()).addExtension(any());
    }

    @Test
    public void testDefine_DoesNotThrowException() {
        // Act & Assert - Should not throw any exception
        plugin.define(context);
    }

    @Test
    public void testDefine_RegistersAllProperties() {
        // Act
        plugin.define(context);

        // Assert - Verify that all properties from MSTeamsNotifyProperties are registered
        // We expect at least 5 extensions: 1 task + 4 properties
        verify(context, atLeast(5)).addExtension(any());
        
        // Verify the main task is registered
        verify(context, times(1)).addExtension(MSTeamsPostProjectAnalysisTask.class);
    }

    @Test
    public void testMSTeamsNotifyProperties_GetProperties() {
        // Act
        List<PropertyDefinition> properties = MSTeamsNotifyProperties.getProperties();

        // Assert
        Assert.assertNotNull(properties);
        Assert.assertEquals(properties.size(), 4);
        
        // Verify each property exists
        PropertyDefinition enableProperty = properties.get(0);
        Assert.assertEquals(enableProperty.key(), "sonar.msteams.enable");
        Assert.assertEquals(enableProperty.name(), "Enable Plugin");
        Assert.assertEquals(enableProperty.type(), PropertyType.BOOLEAN);
        Assert.assertEquals(enableProperty.defaultValue(), "false");

        PropertyDefinition webhookUrlProperty = properties.get(1);
        Assert.assertEquals(webhookUrlProperty.key(), "sonar.msteams.webhook.url");
        Assert.assertEquals(webhookUrlProperty.name(), "Webhook URL");
        Assert.assertEquals(webhookUrlProperty.type(), PropertyType.TEXT);
        Assert.assertEquals(webhookUrlProperty.defaultValue(), "");

        PropertyDefinition avatarProperty = properties.get(2);
        Assert.assertEquals(avatarProperty.key(), "sonar.msteams.avatar.url");
        Assert.assertEquals(avatarProperty.name(), "Webhook Message Avatar");
        Assert.assertEquals(avatarProperty.type(), PropertyType.STRING);
        Assert.assertNotNull(avatarProperty.defaultValue());

        PropertyDefinition sendOnFailedProperty = properties.get(3);
        Assert.assertEquals(sendOnFailedProperty.key(), "sonar.msteams.send.on.failed");
        Assert.assertEquals(sendOnFailedProperty.name(), "Webhook Send On Failed");
        Assert.assertEquals(sendOnFailedProperty.type(), PropertyType.BOOLEAN);
        Assert.assertEquals(sendOnFailedProperty.defaultValue(), "true");
    }

    @Test
    public void testMSTeamsNotifyProperties_EnableNotifyProperty() {
        // Act
        PropertyDefinition property = MSTeamsNotifyProperties.getEnableNotifyProperty();

        // Assert
        Assert.assertNotNull(property);
        Assert.assertEquals(property.key(), "sonar.msteams.enable");
        Assert.assertEquals(property.name(), "Enable Plugin");
        Assert.assertEquals(property.category(), "Microsoft Teams");
        Assert.assertEquals(property.type(), PropertyType.BOOLEAN);
        Assert.assertEquals(property.defaultValue(), "false");
    }

    @Test
    public void testMSTeamsNotifyProperties_WebhookUrlProperty() {
        // Act
        PropertyDefinition property = MSTeamsNotifyProperties.getWebhookUrlProperty();

        // Assert
        Assert.assertNotNull(property);
        Assert.assertEquals(property.key(), "sonar.msteams.webhook.url");
        Assert.assertEquals(property.name(), "Webhook URL");
        Assert.assertEquals(property.category(), "Microsoft Teams");
        Assert.assertEquals(property.type(), PropertyType.TEXT);
        Assert.assertEquals(property.defaultValue(), "");
    }

    @Test
    public void testMSTeamsNotifyProperties_AvatarProperty() {
        // Act
        PropertyDefinition property = MSTeamsNotifyProperties.getWebhookMessageAvatarProperty();

        // Assert
        Assert.assertNotNull(property);
        Assert.assertEquals(property.key(), "sonar.msteams.avatar.url");
        Assert.assertEquals(property.name(), "Webhook Message Avatar");
        Assert.assertEquals(property.category(), "Microsoft Teams");
        Assert.assertEquals(property.type(), PropertyType.STRING);
        Assert.assertNotNull(property.defaultValue());
    }

    @Test
    public void testMSTeamsNotifyProperties_SendOnFailedProperty() {
        // Act
        PropertyDefinition property = MSTeamsNotifyProperties.getWebhookSendOnFailedProperty();

        // Assert
        Assert.assertNotNull(property);
        Assert.assertEquals(property.key(), "sonar.msteams.send.on.failed");
        Assert.assertEquals(property.name(), "Webhook Send On Failed");
        Assert.assertEquals(property.category(), "Microsoft Teams");
        Assert.assertEquals(property.type(), PropertyType.BOOLEAN);
        Assert.assertEquals(property.defaultValue(), "true");
    }
}
