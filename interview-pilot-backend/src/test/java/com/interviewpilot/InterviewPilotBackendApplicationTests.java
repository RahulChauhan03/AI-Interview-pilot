package com.interviewpilot;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

@SpringBootTest
class InterviewPilotBackendApplicationTests {

	@Autowired
	private Environment environment;

	@Test
	void contextLoads() {
	}

	@Test
	void testsNeverUseTheDevelopmentDatabase() {
		assertThat(environment.getActiveProfiles()).contains("test");
		assertThat(environment.getProperty("spring.datasource.url")).doesNotContain("interview_pilot_db");
		assertThat(environment.getProperty("spring.data.mongodb.uri")).doesNotContain("interview_pilot_ai");
	}

}
