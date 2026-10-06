package com.interviewpilot.common.config;

import java.util.List;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Fails startup with a clear message when a required secret is not configured.
 * Spring Boot's property binding (datasource, mail) keeps an unresolved "${VAR}" placeholder
 * as a literal value, so without this check the app would start with broken credentials.
 * JWT_SECRET needs no entry here: it is read through @Value, which already fails on a missing value.
 *
 * Runs as a BeanFactoryPostProcessor so the check happens before any bean (and any database
 * connection) is created.
 */
@Component
public class RequiredPropertiesValidator implements BeanFactoryPostProcessor, EnvironmentAware {

    private static final List<String> REQUIRED_PROPERTIES = List.of(
            "spring.datasource.url",
            "spring.data.mongodb.uri",
            "spring.datasource.username",
            "spring.datasource.password",
            "spring.mail.username",
            "spring.mail.password");

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        // Throws "Could not resolve placeholder 'X'" if the backing environment variable is missing.
        REQUIRED_PROPERTIES.forEach(environment::getRequiredProperty);
    }
}
