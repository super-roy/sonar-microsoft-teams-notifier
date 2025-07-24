package io.github.minhhoangvn;

import io.github.minhhoangvn.config.MSTeamsConfigurationProvider;
import io.github.minhhoangvn.extension.MSTeamsPostProjectAnalysisTask;
import io.github.minhhoangvn.settings.MSTeamsNotifyProperties;
import org.sonar.api.Plugin;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

public class MSTeamsNotifyPlugin implements Plugin {
    
    private static final Logger LOGGER = Loggers.get(MSTeamsNotifyPlugin.class);
    
    @Override
    public void define(Context context) {
        LOGGER.info("MS Teams Plugin: Registering extensions...");
        
        // Register the configuration provider
        context.addExtension(MSTeamsConfigurationProvider.class);
        LOGGER.info("MS Teams Plugin: Registered MSTeamsConfigurationProvider");
        
        // Register the post-analysis task
        context.addExtension(MSTeamsPostProjectAnalysisTask.class);
        LOGGER.info("MS Teams Plugin: Registered MSTeamsPostProjectAnalysisTask");
        
        // Register the property definitions to make them visible in SonarQube admin
        MSTeamsNotifyProperties.getProperties().forEach(propertyDefinition -> {
            context.addExtension(propertyDefinition);
            LOGGER.info("MS Teams Plugin: Registered property: {}", propertyDefinition.key());
        });
        
        LOGGER.info("MS Teams Plugin: Plugin registration completed");
    }
}
