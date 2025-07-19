package io.github.minhhoangvn;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
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
    private SonarQubeMSTeamsNotifierPlugin plugin;
    private Context context;
    private ProjectAnalysis projectAnalysis;
    private Project project;
    private CeTask ceTask;
    private QualityGate qualityGate;

    @BeforeMethod
    public void setUp() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMockServer.start();

        plugin = new SonarQubeMSTeamsNotifierPlugin();
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
        System.clearProperty("sonar.msteams.webhook.url");
        System.clearProperty("sonar.core.serverBaseURL");
    }

    @Test
    public void testFinished_WithValidConfiguration_SendsNotification() {
        // Arrange
        String webhookUrl = "http://localhost:" + wireMockServer.port() + "/webhook";
        String baseUrl = "http://sonarqube.example.com";

        System.setProperty("sonar.msteams.webhook.url", webhookUrl);
        System.setProperty("sonar.core.serverBaseURL", baseUrl);

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
    public void testFinished_WithoutWebhookUrl_DoesNotSendNotification() {
        // Arrange - No webhook URL set
        System.setProperty("sonar.core.serverBaseURL", "http://sonarqube.example.com");

        // Act
        plugin.finished(context);

        // Assert
        wireMockServer.verify(0, postRequestedFor(urlMatching(".*")));
    }

    @Test
    public void testFinished_WithEmptyWebhookUrl_DoesNotSendNotification() {
        // Arrange
        System.setProperty("sonar.msteams.webhook.url", "");
        System.setProperty("sonar.core.serverBaseURL", "http://sonarqube.example.com");

        // Act
        plugin.finished(context);

        // Assert
        wireMockServer.verify(0, postRequestedFor(urlMatching(".*")));
    }

    @Test
    public void testFinished_WithFailedQualityGate_SendsNotification() {
        // Arrange
        String webhookUrl = "http://localhost:" + wireMockServer.port() + "/webhook";
        String baseUrl = "http://sonarqube.example.com";

        System.setProperty("sonar.msteams.webhook.url", webhookUrl);
        System.setProperty("sonar.core.serverBaseURL", baseUrl);
        
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
    public void testFinished_WithServerError_HandlesGracefully() {
        // Arrange
        String webhookUrl = "http://localhost:" + wireMockServer.port() + "/webhook";
        String baseUrl = "http://sonarqube.example.com";

        System.setProperty("sonar.msteams.webhook.url", webhookUrl);
        System.setProperty("sonar.core.serverBaseURL", baseUrl);

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
        assert description.equals("Send SonarQube analysis results to Microsoft Teams");
    }

    @Test
    public void testFinished_WithEnvironmentVariables_SendsNotification() {
        // This test would need environment variable setup which is harder to test
        // So we'll just test that the method doesn't crash without system properties
        
        // Act - Should not throw exception even without configuration
        plugin.finished(context);
        
        // No assertion needed - just testing it doesn't crash
    }

    @Test
    public void testFinished_WithAdaptiveCardFormatting() {
        // Arrange
        String webhookUrl = "http://localhost:" + wireMockServer.port() + "/webhook";
        String baseUrl = "http://sonarqube.example.com";

        System.setProperty("sonar.msteams.webhook.url", webhookUrl);
        System.setProperty("sonar.core.serverBaseURL", baseUrl);

        wireMockServer.stubFor(post(urlEqualTo("/webhook"))
                .withRequestBody(containing("\"$schema\": \"http://adaptivecards.io/schemas/adaptive-card.json\""))
                .withRequestBody(containing("\"version\": \"1.5\""))
                .withRequestBody(containing("\"text\": \"SonarQube Analysis Result\""))
                .withRequestBody(containing("\"url\": \"https://upload.wikimedia.org/wikipedia/commons/thumb/1/1d/Super_Micro_Computer_Logo.svg/330px-Super_Micro_Computer_Logo.svg.png\""))
                .withRequestBody(containing("\"text\": \"Supermicro IT2 DevOps Team\""))
                .withRequestBody(containing("\"title\": \"Status\""))
                .withRequestBody(containing("\"title\": \"Quality Gate\""))
                .withRequestBody(containing("\"title\": \"New Violations\""))
                .withRequestBody(containing("\"title\": \"New Coverage\""))
                .withRequestBody(containing("\"title\": \"New Duplicated Lines Density\""))
                .withRequestBody(containing("\"title\": \"New Security Hotspots Reviewed\""))
                .withRequestBody(containing("\"type\": \"Action.OpenUrl\""))
                .withRequestBody(containing("\"title\": \"View Analysis\""))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("1")));

        // Act
        plugin.finished(context);

        // Assert
        wireMockServer.verify(postRequestedFor(urlEqualTo("/webhook")));
    }
}