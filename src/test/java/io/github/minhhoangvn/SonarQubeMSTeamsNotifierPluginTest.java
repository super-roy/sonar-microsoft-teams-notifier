package io.github.minhhoangvn;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.github.minhhoangvn.config.MSTeamsConfigurationProvider;
import io.github.minhhoangvn.extension.MSTeamsPostProjectAnalysisTask;
import io.github.minhhoangvn.utils.Constants;
import org.sonar.api.ce.posttask.PostProjectAnalysisTask.Context;
import org.sonar.api.ce.posttask.PostProjectAnalysisTask.ProjectAnalysis;
import org.sonar.api.ce.posttask.Project;
import org.sonar.api.ce.posttask.CeTask;
import org.sonar.api.ce.posttask.QualityGate;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.mockito.Mockito.*;

public class SonarQubeMSTeamsNotifierPluginTest {

    private WireMockServer wireMockServer;
    private MSTeamsPostProjectAnalysisTask plugin;
    private Context context;
    private ProjectAnalysis projectAnalysis;
    private Project project;
    private CeTask ceTask;
    private QualityGate qualityGate;

    @BeforeMethod
    public void setUp() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMockServer.start();

        // Create a mock config provider
        MSTeamsConfigurationProvider mockConfigProvider = mock(MSTeamsConfigurationProvider.class);
        when(mockConfigProvider.isEnabled()).thenReturn(true);
        when(mockConfigProvider.getWebhookUrl()).thenReturn("http://localhost:" + wireMockServer.port() + "/webhook");
        when(mockConfigProvider.getAvatarUrl()).thenReturn(Constants.DEFAULT_WEBHOOK_MESSAGE_AVATAR);
        when(mockConfigProvider.getSendOnFailedOnly()).thenReturn(false);
        when(mockConfigProvider.getBaseUrl()).thenReturn("http://sonarqube.example.com");

        plugin = new MSTeamsPostProjectAnalysisTask(mockConfigProvider);
        
        context = mock(Context.class);
        projectAnalysis = mock(ProjectAnalysis.class);
        project = mock(Project.class);
        ceTask = mock(CeTask.class);
        qualityGate = mock(QualityGate.class);

        when(context.getProjectAnalysis()).thenReturn(projectAnalysis);
        when(projectAnalysis.getProject()).thenReturn(project);
        when(projectAnalysis.getCeTask()).thenReturn(ceTask);
        when(projectAnalysis.getQualityGate()).thenReturn(qualityGate);

        when(project.getName()).thenReturn("Test Project");
        when(project.getKey()).thenReturn("test-project-key");
        when(ceTask.getStatus()).thenReturn(CeTask.Status.SUCCESS);
        when(qualityGate.getStatus()).thenReturn(QualityGate.Status.OK);
        when(qualityGate.getName()).thenReturn("Sonar way");
        when(qualityGate.getConditions()).thenReturn(Collections.emptyList());
    }

    @AfterMethod
    public void tearDown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
        // Clear system properties
        System.clearProperty("sonar.msteams.enable");
        System.clearProperty("sonar.msteams.webhook.url");
        System.clearProperty("sonar.msteams.avatar.url");
        System.clearProperty("sonar.msteams.send.on.failed");
        System.clearProperty("sonar.core.serverBaseURL");
    }

    @Test
    public void testFinished_WithValidConfiguration_SendsNotification() {
        // Arrange - The mock config provider is already set up in setUp()
        // We don't need to set system properties anymore since we're using mocked config

        wireMockServer.stubFor(post(urlEqualTo("/webhook"))
                .withHeader("Content-Type", containing("application/json"))
                .withHeader("Accept", equalTo("application/json"))
                .withRequestBody(containing("\"type\": \"AdaptiveCard\""))
                .withRequestBody(containing("\"text\": \"Test Project SonarQube Analysis Result\""))
                .withRequestBody(containing("\"value\": \"SUCCESS\""))
                .withRequestBody(containing("\"value\": \"Sonar way (OK)\""))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("1")));

        // Act
        plugin.finished(context);

        // Assert
        wireMockServer.verify(postRequestedFor(urlEqualTo("/webhook"))
                .withHeader("Content-Type", containing("application/json"))
                .withHeader("Accept", equalTo("application/json")));
    }

    @Test
    public void testFinished_WithPluginDisabled_DoesNotSendNotification() {
        // Arrange - Update the mock to disable the plugin
        MSTeamsConfigurationProvider disabledMockConfigProvider = mock(MSTeamsConfigurationProvider.class);
        when(disabledMockConfigProvider.isEnabled()).thenReturn(false);
        when(disabledMockConfigProvider.getWebhookUrl()).thenReturn("http://localhost:" + wireMockServer.port() + "/webhook");
        when(disabledMockConfigProvider.getAvatarUrl()).thenReturn(Constants.DEFAULT_WEBHOOK_MESSAGE_AVATAR);
        when(disabledMockConfigProvider.getSendOnFailedOnly()).thenReturn(false);
        when(disabledMockConfigProvider.getBaseUrl()).thenReturn("http://sonarqube.example.com");
        
        MSTeamsPostProjectAnalysisTask disabledPlugin = new MSTeamsPostProjectAnalysisTask(disabledMockConfigProvider);

        // Act
        disabledPlugin.finished(context);

        // Assert
        wireMockServer.verify(0, postRequestedFor(urlMatching(".*")));
    }

    @Test
    public void testFinished_WithoutWebhookUrl_DoesNotSendNotification() {
        // Arrange - Mock with empty webhook URL
        MSTeamsConfigurationProvider emptyUrlMockConfigProvider = mock(MSTeamsConfigurationProvider.class);
        when(emptyUrlMockConfigProvider.isEnabled()).thenReturn(true);
        when(emptyUrlMockConfigProvider.getWebhookUrl()).thenReturn("");
        when(emptyUrlMockConfigProvider.getAvatarUrl()).thenReturn(Constants.DEFAULT_WEBHOOK_MESSAGE_AVATAR);
        when(emptyUrlMockConfigProvider.getSendOnFailedOnly()).thenReturn(false);
        when(emptyUrlMockConfigProvider.getBaseUrl()).thenReturn("http://sonarqube.example.com");
        
        MSTeamsPostProjectAnalysisTask emptyUrlPlugin = new MSTeamsPostProjectAnalysisTask(emptyUrlMockConfigProvider);

        // Act
        emptyUrlPlugin.finished(context);

        // Assert
        wireMockServer.verify(0, postRequestedFor(urlMatching(".*")));
    }

    @Test
    public void testFinished_WithEmptyWebhookUrl_DoesNotSendNotification() {
        // Arrange - This is the same as the previous test
        MSTeamsConfigurationProvider emptyUrlMockConfigProvider = mock(MSTeamsConfigurationProvider.class);
        when(emptyUrlMockConfigProvider.isEnabled()).thenReturn(true);
        when(emptyUrlMockConfigProvider.getWebhookUrl()).thenReturn("");
        when(emptyUrlMockConfigProvider.getAvatarUrl()).thenReturn(Constants.DEFAULT_WEBHOOK_MESSAGE_AVATAR);
        when(emptyUrlMockConfigProvider.getSendOnFailedOnly()).thenReturn(false);
        when(emptyUrlMockConfigProvider.getBaseUrl()).thenReturn("http://sonarqube.example.com");
        
        MSTeamsPostProjectAnalysisTask emptyUrlPlugin = new MSTeamsPostProjectAnalysisTask(emptyUrlMockConfigProvider);

        // Act
        emptyUrlPlugin.finished(context);

        // Assert
        wireMockServer.verify(0, postRequestedFor(urlMatching(".*")));
    }

    @Test
    public void testFinished_WithFailedQualityGateAndSendOnFailedOnly_SendsNotification() {
        // Arrange - Mock with send on failed only
        MSTeamsConfigurationProvider sendOnFailedMockConfigProvider = mock(MSTeamsConfigurationProvider.class);
        when(sendOnFailedMockConfigProvider.isEnabled()).thenReturn(true);
        when(sendOnFailedMockConfigProvider.getWebhookUrl()).thenReturn("http://localhost:" + wireMockServer.port() + "/webhook");
        when(sendOnFailedMockConfigProvider.getAvatarUrl()).thenReturn(Constants.DEFAULT_WEBHOOK_MESSAGE_AVATAR);
        when(sendOnFailedMockConfigProvider.getSendOnFailedOnly()).thenReturn(true);
        when(sendOnFailedMockConfigProvider.getBaseUrl()).thenReturn("http://sonarqube.example.com");
        
        MSTeamsPostProjectAnalysisTask sendOnFailedPlugin = new MSTeamsPostProjectAnalysisTask(sendOnFailedMockConfigProvider);
        
        when(ceTask.getStatus()).thenReturn(CeTask.Status.FAILED);
        when(qualityGate.getStatus()).thenReturn(QualityGate.Status.ERROR);

        wireMockServer.stubFor(post(urlEqualTo("/webhook"))
                .withRequestBody(containing("\"value\": \"FAILED\""))
                .withRequestBody(containing("\"value\": \"Sonar way (ERROR)\""))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("1")));

        // Act
        sendOnFailedPlugin.finished(context);

        // Assert
        wireMockServer.verify(postRequestedFor(urlEqualTo("/webhook")));
    }

    @Test
    public void testFinished_WithPassedQualityGateAndSendOnFailedOnly_DoesNotSendNotification() {
        // Arrange - Mock with send on failed only, but quality gate passes
        MSTeamsConfigurationProvider sendOnFailedMockConfigProvider = mock(MSTeamsConfigurationProvider.class);
        when(sendOnFailedMockConfigProvider.isEnabled()).thenReturn(true);
        when(sendOnFailedMockConfigProvider.getWebhookUrl()).thenReturn("http://localhost:" + wireMockServer.port() + "/webhook");
        when(sendOnFailedMockConfigProvider.getAvatarUrl()).thenReturn(Constants.DEFAULT_WEBHOOK_MESSAGE_AVATAR);
        when(sendOnFailedMockConfigProvider.getSendOnFailedOnly()).thenReturn(true);
        when(sendOnFailedMockConfigProvider.getBaseUrl()).thenReturn("http://sonarqube.example.com");
        
        MSTeamsPostProjectAnalysisTask sendOnFailedPlugin = new MSTeamsPostProjectAnalysisTask(sendOnFailedMockConfigProvider);
        
        // Quality gate passes
        when(ceTask.getStatus()).thenReturn(CeTask.Status.SUCCESS);
        when(qualityGate.getStatus()).thenReturn(QualityGate.Status.OK);

        // Act
        sendOnFailedPlugin.finished(context);

        // Assert
        wireMockServer.verify(0, postRequestedFor(urlMatching(".*")));
    }

    @Test
    public void testFinished_WithServerError_HandlesGracefully() {
        // Arrange - Use the default setup
        wireMockServer.stubFor(post(urlEqualTo("/webhook"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        // Act - Should not throw exception
        plugin.finished(context);

        // Assert
        wireMockServer.verify(postRequestedFor(urlEqualTo("/webhook")));
    }

    @Test
    public void testGetDescription() {
        // Act
        String description = plugin.getDescription();

        // Assert
        assert description.equals("MS Teams notification extension for SonarQube analysis results");
    }

    @Test
    public void testFinished_WithCustomAvatarUrl_SendsNotification() {
        // Arrange - Mock with custom avatar
        String customAvatarUrl = "https://example.com/custom-avatar.png";
        MSTeamsConfigurationProvider customAvatarMockConfigProvider = mock(MSTeamsConfigurationProvider.class);
        when(customAvatarMockConfigProvider.isEnabled()).thenReturn(true);
        when(customAvatarMockConfigProvider.getWebhookUrl()).thenReturn("http://localhost:" + wireMockServer.port() + "/webhook");
        when(customAvatarMockConfigProvider.getAvatarUrl()).thenReturn(customAvatarUrl);
        when(customAvatarMockConfigProvider.getSendOnFailedOnly()).thenReturn(false);
        when(customAvatarMockConfigProvider.getBaseUrl()).thenReturn("http://sonarqube.example.com");
        
        MSTeamsPostProjectAnalysisTask customAvatarPlugin = new MSTeamsPostProjectAnalysisTask(customAvatarMockConfigProvider);

        wireMockServer.stubFor(post(urlEqualTo("/webhook"))
                .withRequestBody(containing("\"url\": \"" + customAvatarUrl + "\""))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("1")));

        // Act
        customAvatarPlugin.finished(context);

        // Assert
        wireMockServer.verify(postRequestedFor(urlEqualTo("/webhook"))
                .withRequestBody(containing("\"url\": \"" + customAvatarUrl + "\"")));
    }

    @Test
    public void testFinished_WithDefaultAvatar_SendsNotification() {
        // Arrange - Use the default setup which already has the default avatar

        wireMockServer.stubFor(post(urlEqualTo("/webhook"))
                .withRequestBody(containing("\"url\": \"" + Constants.DEFAULT_WEBHOOK_MESSAGE_AVATAR + "\""))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("1")));

        // Act
        plugin.finished(context);

        // Assert
        wireMockServer.verify(postRequestedFor(urlEqualTo("/webhook"))
                .withRequestBody(containing("\"url\": \"" + Constants.DEFAULT_WEBHOOK_MESSAGE_AVATAR + "\"")));
    }

    @Test
    public void testDebug_CheckSystemProperties() {
        // This test is mainly for debugging - the mock setup handles configuration now
        System.out.println("=== Mock Configuration Debug ===");
        System.out.println("Plugin enabled: " + plugin.toString());
        
        wireMockServer.stubFor(post(urlEqualTo("/webhook"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("1")));

        // Act
        plugin.finished(context);

        // Print the mock server's request log
        System.out.println("=== WireMock Request Log ===");
        wireMockServer.getAllServeEvents().forEach(event -> {
            System.out.println("Request: " + event.getRequest().getMethod() + " " + event.getRequest().getUrl());
            System.out.println("Body: " + event.getRequest().getBodyAsString());
        });
    }
}