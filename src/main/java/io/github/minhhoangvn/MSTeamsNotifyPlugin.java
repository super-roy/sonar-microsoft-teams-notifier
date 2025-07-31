package io.github.minhhoangvn;

import io.github.minhhoangvn.extension.MSTeamsPreProjectAnalysisTask;
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
        
        // Register the pre-analysis configuration validator first
        context.addExtension(MSTeamsPreProjectAnalysisTask.class);
        LOGGER.info("MS Teams Plugin: Registered MSTeamsPreProjectAnalysisTask");
        
        // Register the post-analysis notification task
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
