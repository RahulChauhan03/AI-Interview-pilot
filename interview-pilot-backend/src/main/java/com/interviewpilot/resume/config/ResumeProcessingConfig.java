package com.interviewpilot.resume.config;

import java.time.Duration;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableAsync
@EnableScheduling
public class ResumeProcessingConfig {

    /** Used for Ollama calls; the timeouts make sure a hanging model call cannot block a worker forever. */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder,
                                     @Value("${resume.ollama.connect-timeout}") Duration connectTimeout,
                                     @Value("${resume.ollama.read-timeout}") Duration readTimeout) {
        return builder.connectTimeout(connectTimeout).readTimeout(readTimeout).build();
    }

    @Bean(name = "resumeProcessingExecutor")
    public Executor resumeProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("resume-processor-");
        executor.initialize();
        return executor;
    }
}
