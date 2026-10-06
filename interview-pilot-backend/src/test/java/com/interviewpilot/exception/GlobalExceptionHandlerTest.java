package com.interviewpilot.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.interviewpilot.auth.controller.AuthController;
import com.interviewpilot.auth.service.AuthService;
import com.interviewpilot.resume.validator.ResumeFileValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.unit.DataSize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(OutputCaptureExtension.class)
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AuthController(authService), new FailingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void unexpectedExceptionReturns500WithoutInternalDetailsAndIsLogged(CapturedOutput output) throws Exception {
        mockMvc.perform(get("/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.path").value("/test/unexpected"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(content().string(not(containsString("jdbc"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))))
                .andExpect(content().string(not(containsString("trace"))));

        assertThat(output).contains("Unhandled exception on /test/unexpected");
        assertThat(output).contains("java.lang.IllegalStateException");
    }

    @Test
    void invalidRequestBodyReturns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.data.validationErrors.email").value("Email must be valid"))
                .andExpect(jsonPath("$.data.validationErrors.password").value("Password is required"))
                .andExpect(jsonPath("$.path").value("/api/auth/login"));
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(content().string(not(containsString("JsonParseException"))));
    }

    @Test
    void badCredentialsKeepExisting401() throws Exception {
        when(authService.login(any())).thenThrow(new InvalidCredentialsException("Invalid email or password"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"))
                .andExpect(jsonPath("$.data.error").value("UNAUTHORIZED"));
    }

    @Test
    void missingResourceReturns404() throws Exception {
        mockMvc.perform(get("/test/resumes/123"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Resume not found"))
                .andExpect(jsonPath("$.path").value("/test/resumes/123"));
    }

    @Test
    void unknownEndpointReturns404() throws Exception {
        mockMvc.perform(get("/api/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("The requested resource was not found"))
                .andExpect(jsonPath("$.path").value("/api/does-not-exist"));
    }

    @Test
    void oversizedUploadReturns413() throws Exception {
        mockMvc.perform(get("/test/oversized"))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.status").value(413))
                .andExpect(jsonPath("$.message").value("File size exceeds the maximum allowed limit"))
                .andExpect(jsonPath("$.data.error").value("FILE_TOO_LARGE"));
    }

    @Test
    void unsupportedFileReturns400() throws Exception {
        MockMultipartFile textFile = new MockMultipartFile("file", "notes.txt", "text/plain", "hello".getBytes());

        mockMvc.perform(multipart("/test/upload").file(textFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only PDF, DOC, and DOCX files are supported"));
    }

    @Test
    void missingUploadPartReturns400() throws Exception {
        mockMvc.perform(multipart("/test/upload"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void aiServiceFailureReturns503WithoutUpstreamDetails() throws Exception {
        mockMvc.perform(get("/test/ai"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.data.error").value("AI_SERVICE_UNAVAILABLE"))
                .andExpect(content().string(not(containsString("11434"))));
    }

    @Test
    void storageFailureReturns500WithoutFilesystemPath() throws Exception {
        mockMvc.perform(get("/test/storage"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Unable to store the uploaded file"))
                .andExpect(content().string(not(containsString("/var/app"))));
    }

    /** Throws the exceptions the real application can raise, so each mapping can be checked in isolation. */
    @RestController
    static class FailingController {

        private final ResumeFileValidator fileValidator = new ResumeFileValidator(DataSize.ofMegabytes(10));

        @GetMapping("/test/unexpected")
        String unexpected() {
            throw new IllegalStateException("Connection to jdbc:mysql://db-host:3306/prod failed");
        }

        @GetMapping("/test/resumes/{id}")
        String resume(@PathVariable Long id) {
            throw new ResourceNotFoundException("Resume not found");
        }

        @GetMapping("/test/oversized")
        String oversized() {
            throw new MaxUploadSizeExceededException(DataSize.ofMegabytes(10).toBytes());
        }

        @PostMapping("/test/upload")
        String upload(@RequestParam("file") MultipartFile file) {
            fileValidator.validate(file);
            return "ok";
        }

        @GetMapping("/test/ai")
        String ai() {
            throw new OllamaException("Failed to call Ollama: I/O error on POST request for \"http://localhost:11434/api/generate\"");
        }

        @GetMapping("/test/storage")
        String storage() {
            throw new StorageException("Failed to store resume file: /var/app/uploads/resume/2026/10/x.pdf");
        }
    }
}
