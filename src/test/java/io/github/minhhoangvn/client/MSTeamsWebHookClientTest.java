package io.github.minhhoangvn.client;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import okhttp3.Response;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.ConnectException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class MSTeamsWebHookClientTest {

    private WireMockServer wireMockServer;
    private MSTeamsWebHookClient client;
    private String webhookUrl;

    @BeforeMethod
    public void setUp() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMockServer.start();
        client = new MSTeamsWebHookClient();
        webhookUrl = "http://localhost:" + wireMockServer.port() + "/webhook";
    }

    @AfterMethod
    public void tearDown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @Test
    public void testSendNotify_Success() throws IOException {
        // Arrange
        String payload = createTestPayload();
        
        wireMockServer.stubFor(post(urlEqualTo("/webhook"))
                .withHeader("Content-Type", containing("application/json"))
                .withHeader("Accept", equalTo("application/json"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("1")));

        // Act
        try (Response response = client.sendNotify(webhookUrl, payload)) {
            // Assert
            Assert.assertTrue(response.isSuccessful());
            Assert.assertEquals(response.code(), 200);
        }

        // Verify the request was made correctly
        wireMockServer.verify(postRequestedFor(urlEqualTo("/webhook"))
                .withHeader("Content-Type", containing("application/json"))
                .withHeader("Accept", equalTo("application/json"))
                .withRequestBody(equalTo(payload)));
    }

    @Test
    public void testSendNotify_BadRequest() throws IOException {
        // Arrange
        String payload = "invalid json";
        
        wireMockServer.stubFor(post(urlEqualTo("/webhook"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withBody("Bad Request")));

        // Act
        try (Response response = client.sendNotify(webhookUrl, payload)) {
            // Assert
            Assert.assertFalse(response.isSuccessful());
            Assert.assertEquals(response.code(), 400);
        }
    }

    @Test
    public void testSendNotify_ServerError() throws IOException {
        // Arrange
        String payload = createTestPayload();
        
        wireMockServer.stubFor(post(urlEqualTo("/webhook"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        // Act
        try (Response response = client.sendNotify(webhookUrl, payload)) {
            // Assert
            Assert.assertFalse(response.isSuccessful());
            Assert.assertEquals(response.code(), 500);
        }
    }

    @Test
    public void testSendNotify_WithAdaptiveCardPayload() throws IOException {
        // Arrange
        String adaptiveCardPayload = createAdaptiveCardPayload();
        
        wireMockServer.stubFor(post(urlEqualTo("/webhook"))
                .withRequestBody(containing("\"type\": \"AdaptiveCard\""))
                .withRequestBody(containing("\"$schema\": \"http://adaptivecards.io/schemas/adaptive-card.json\""))
                .withRequestBody(containing("\"version\": \"1.5\""))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("1")));

        // Act
        try (Response response = client.sendNotify(webhookUrl, adaptiveCardPayload)) {
            // Assert
            Assert.assertTrue(response.isSuccessful());
            Assert.assertEquals(response.code(), 200);
        }
    }

    @Test(expectedExceptions = {IOException.class, ConnectException.class, IllegalArgumentException.class})
    public void testSendNotify_NetworkError() throws IOException {
        // Arrange - Use an invalid URL that will cause connection issues
        String invalidUrl = "http://nonexistent.invalid.domain.test:8080/webhook";
        String payload = createTestPayload();

        // Act - This should throw an exception
        try (Response response = client.sendNotify(invalidUrl, payload)) {
            // Should not reach here
            Assert.fail("Expected an exception to be thrown");
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testSendNotify_InvalidPort() throws IOException {
        // Arrange - Use port that's too high
        String invalidUrl = "http://localhost:99999/webhook";
        String payload = createTestPayload();

        // Act - This should throw IllegalArgumentException
        try (Response response = client.sendNotify(invalidUrl, payload)) {
            // Should not reach here
        }
    }

    private String createTestPayload() {
        return "{\n" +
                "    \"type\": \"AdaptiveCard\",\n" +
                "    \"$schema\": \"http://adaptivecards.io/schemas/adaptive-card.json\",\n" +
                "    \"version\": \"1.5\",\n" +
                "    \"body\": [\n" +
                "        {\n" +
                "            \"type\": \"TextBlock\",\n" +
                "            \"text\": \"Test Message\"\n" +
                "        }\n" +
                "    ]\n" +
                "}";
    }

    private String createAdaptiveCardPayload() {
        return "{\n" +
                "    \"type\": \"AdaptiveCard\",\n" +
                "    \"$schema\": \"http://adaptivecards.io/schemas/adaptive-card.json\",\n" +
                "    \"version\": \"1.5\",\n" +
                "    \"body\": [\n" +
                "        {\n" +
                "            \"type\": \"TextBlock\",\n" +
                "            \"size\": \"Medium\",\n" +
                "            \"weight\": \"Bolder\",\n" +
                "            \"text\": \"SonarQube Analysis Result\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"type\": \"ColumnSet\",\n" +
                "            \"columns\": [\n" +
                "                {\n" +
                "                    \"type\": \"Column\",\n" +
                "                    \"items\": [\n" +
                "                        {\n" +
                "                            \"type\": \"Image\",\n" +
                "                            \"style\": \"Person\",\n" +
                "                            \"url\": \"https://docs.sonarqube.org/latest/images/sonarqube-logo.svg\",\n" +
                "                            \"altText\": \"DevOps Team\",\n" +
                "                            \"size\": \"Small\"\n" +
                "                        }\n" +
                "                    ],\n" +
                "                    \"width\": \"auto\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"type\": \"Column\",\n" +
                "                    \"items\": [\n" +
                "                        {\n" +
                "                            \"type\": \"TextBlock\",\n" +
                "                            \"weight\": \"Bolder\",\n" +
                "                            \"text\": \"DevOps Team\",\n" +
                "                            \"wrap\": true\n" +
                "                        }\n" +
                "                    ],\n" +
                "                    \"width\": \"stretch\"\n" +
                "                }\n" +
                "            ]\n" +
                "        },\n" +
                "        {\n" +
                "            \"type\": \"TextBlock\",\n" +
                "            \"text\": \"Test Project SonarQube Analysis Result\",\n" +
                "            \"wrap\": true,\n" +
                "            \"color\": \"Accent\",\n" +
                "            \"weight\": \"Bolder\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"type\": \"FactSet\",\n" +
                "            \"facts\": [\n" +
                "                {\n" +
                "                    \"title\": \"Status\",\n" +
                "                    \"value\": \"SUCCESS\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"title\": \"Quality Gate\",\n" +
                "                    \"value\": \"Sonar way (OK)\"\n" +
                "                }\n" +
                "            ]\n" +
                "        }\n" +
                "    ],\n" +
                "    \"actions\": [\n" +
                "        {\n" +
                "            \"type\": \"Action.OpenUrl\",\n" +
                "            \"title\": \"View Analysis\",\n" +
                "            \"url\": \"http://sonarqube.example.com/dashboard?id=test-project\"\n" +
                "        }\n" +
                "    ]\n" +
                "}";
    }
}
