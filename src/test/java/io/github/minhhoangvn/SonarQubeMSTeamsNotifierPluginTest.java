package io.github.minhhoangvn;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.github.minhhoangvn.extension.MSTeamsPostProjectAnalysisTask;
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

        plugin = new MSTeamsPostProjectAnalysisTask();
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
        // Arrange
        String webhookUrl = "http://localhost:" + wireMockServer.port() + "/webhook";
        String baseUrl = "http://sonarqube.example.com";

        System.setProperty("sonar.msteams.enable", "true");
        System.setProperty("sonar.msteams.webhook.url", webhookUrl);
        System.setProperty("sonar.core.serverBaseURL", baseUrl);
        System.setProperty("sonar.msteams.send.on.failed", "false");

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
        // Arrange - Plugin disabled
        String webhookUrl = "http://localhost:" + wireMockServer.port() + "/webhook";
        String baseUrl = "http://sonarqube.example.com";

        System.setProperty("sonar.msteams.enable", "false");
        System.setProperty("sonar.msteams.webhook.url", webhookUrl);
        System.setProperty("sonar.core.serverBaseURL", baseUrl);

        // Act
        plugin.finished(context);

        // Assert
        wireMockServer.verify(0, postRequestedFor(urlMatching(".*")));
    }

    @Test
    public void testFinished_WithoutWebhookUrl_DoesNotSendNotification() {
        // Arrange - No webhook URL set but plugin enabled
        System.setProperty("sonar.msteams.enable", "true");
        System.setProperty("sonar.core.serverBaseURL", "http://sonarqube.example.com");

        // Act
        plugin.finished(context);

        // Assert
        wireMockServer.verify(0, postRequestedFor(urlMatching(".*")));
    }

    @Test
    public void testFinished_WithEmptyWebhookUrl_DoesNotSendNotification() {
        // Arrange
        System.setProperty("sonar.msteams.enable", "true");
        System.setProperty("sonar.msteams.webhook.url", "");
        System.setProperty("sonar.core.serverBaseURL", "http://sonarqube.example.com");

        // Act
        plugin.finished(context);

        // Assert
        wireMockServer.verify(0, postRequestedFor(urlMatching(".*")));
    }

    @Test
    public void testFinished_WithFailedQualityGateAndSendOnFailedOnly_SendsNotification() {
        // Arrange
        String webhookUrl = "http://localhost:" + wireMockServer.port() + "/webhook";
        String baseUrl = "http://sonarqube.example.com";

        System.setProperty("sonar.msteams.enable", "true");
        System.setProperty("sonar.msteams.webhook.url", webhookUrl);
        System.setProperty("sonar.core.serverBaseURL", baseUrl);
        System.setProperty("sonar.msteams.send.on.failed", "true");
        
        when(ceTask.getStatus()).thenReturn(CeTask.Status.FAILED);
        when(qualityGate.getStatus()).thenReturn(QualityGate.Status.ERROR);

        wireMockServer.stubFor(post(urlEqualTo("/webhook"))
                .withRequestBody(containing("\"value\": \"FAILED\""))
                .withRequestBody(containing("\"value\": \"Sonar way (ERROR)\""))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("1")));

        // Act
        plugin.finished(context);

        // Assert
        wireMockServer.verify(postRequestedFor(urlEqualTo("/webhook")));
    }

    @Test
    public void testFinished_WithPassedQualityGateAndSendOnFailedOnly_DoesNotSendNotification() {
        // Arrange
        String webhookUrl = "http://localhost:" + wireMockServer.port() + "/webhook";
        String baseUrl = "http://sonarqube.example.com";

        System.setProperty("sonar.msteams.enable", "true");
        System.setProperty("sonar.msteams.webhook.url", webhookUrl);
        System.setProperty("sonar.core.serverBaseURL", baseUrl);
        System.setProperty("sonar.msteams.send.on.failed", "true");
        
        // Quality gate passes
        when(ceTask.getStatus()).thenReturn(CeTask.Status.SUCCESS);
        when(qualityGate.getStatus()).thenReturn(QualityGate.Status.OK);

        // Act
        plugin.finished(context);

        // Assert
        wireMockServer.verify(0, postRequestedFor(urlMatching(".*")));
    }

    @Test
    public void testFinished_WithServerError_HandlesGracefully() {
        // Arrange
        String webhookUrl = "http://localhost:" + wireMockServer.port() + "/webhook";
        String baseUrl = "http://sonarqube.example.com";

        System.setProperty("sonar.msteams.enable", "true");
        System.setProperty("sonar.msteams.webhook.url", webhookUrl);
        System.setProperty("sonar.core.serverBaseURL", baseUrl);
        System.setProperty("sonar.msteams.send.on.failed", "false");

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
        // Arrange
        String webhookUrl = "http://localhost:" + wireMockServer.port() + "/webhook";
        String baseUrl = "http://sonarqube.example.com";
        String customAvatarUrl = "https://example.com/custom-avatar.png";

        System.setProperty("sonar.msteams.enable", "true");
        System.setProperty("sonar.msteams.webhook.url", webhookUrl);
        System.setProperty("sonar.core.serverBaseURL", baseUrl);
        System.setProperty("sonar.msteams.avatar.url", customAvatarUrl);
        System.setProperty("sonar.msteams.send.on.failed", "false");

        wireMockServer.stubFor(post(urlEqualTo("/webhook"))
                .withRequestBody(containing("\"url\": \"" + customAvatarUrl + "\""))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("1")));

        // Act
        plugin.finished(context);

        // Assert
        wireMockServer.verify(postRequestedFor(urlEqualTo("/webhook"))
                .withRequestBody(containing("\"url\": \"" + customAvatarUrl + "\"")));
    }

    @Test
    public void testFinished_WithDefaultAvatar_SendsNotification() {
        // Arrange
        String webhookUrl = "http://localhost:" + wireMockServer.port() + "/webhook";
        String baseUrl = "http://sonarqube.example.com";

        System.setProperty("sonar.msteams.enable", "true");
        System.setProperty("sonar.msteams.webhook.url", webhookUrl);
        System.setProperty("sonar.core.serverBaseURL", baseUrl);
        System.setProperty("sonar.msteams.send.on.failed", "false");
        // Don't set avatar URL - should use default

        wireMockServer.stubFor(post(urlEqualTo("/webhook"))
                // Update this to expect the Supermicro logo URL instead of toilatester
                .withRequestBody(containing("\"url\": \"https://upload.wikimedia.org/wikipedia/commons/thumb/1/1d/Super_Micro_Computer_Logo.svg/330px-Super_Micro_Computer_Logo.svg.png\""))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("1")));

        // Act
        plugin.finished(context);

        // Assert
        wireMockServer.verify(postRequestedFor(urlEqualTo("/webhook"))
                .withRequestBody(containing("\"url\": \"https://upload.wikimedia.org/wikipedia/commons/thumb/1/1d/Super_Micro_Computer_Logo.svg/330px-Super_Micro_Computer_Logo.svg.png\"")));
    }
}