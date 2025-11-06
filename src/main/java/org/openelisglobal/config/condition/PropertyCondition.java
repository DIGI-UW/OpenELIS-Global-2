package org.openelisglobal.config.condition;

import java.util.Map;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class PropertyCondition implements ConfigurationCondition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        final Map<String, Object> attributes = metadata.getAnnotationAttributes(ConditionalOnProperty.class.getName());
        final String propertyName = (String) attributes.get("property");
        final String havingValue = (String) attributes.get("havingValue");
        final boolean matchIfMissing = (boolean) attributes.get("matchIfMissing");

        String propertyValue = context.getEnvironment().getProperty(propertyName);

        return propertyValue == null ? matchIfMissing : propertyValue.equals(havingValue);
    }

    @Override
    public ConfigurationPhase getConfigurationPhase() {
        return ConfigurationPhase.REGISTER_BEAN;
    }
}
