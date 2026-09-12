package com.prostriver.common.exception;

import com.prostriver.entity.User;
import com.prostriver.entity.enums.Role;
import com.prostriver.security.JwtService;
import com.prostriver.security.ProStriverUserDetails;
import com.prostriver.security.ProStriverUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code @WebMvcTest} slice for {@link GlobalExceptionHandler}.
 *
 * <p>Exercises the exception-to-HTTP-status mapping for the three
 * container-thrown exceptions that occur before any controller method body
 * runs: malformed JSON ({@link org.springframework.http.converter.HttpMessageNotReadableException}),
 * a path variable that fails type conversion
 * ({@link org.springframework.web.method.annotation.MethodArgumentTypeMismatchException}),
 * and a missing required request parameter
 * ({@link org.springframework.web.bind.MissingServletRequestParameterException}).
 *
 * <p>{@link ExceptionTriggerController} is a test-only controller that exists
 * purely to reach those exceptions; its endpoints are never expected to
 * return a successful response. It lives in its own standalone top-level
 * file ({@code ExceptionTriggerController.java}) rather than as a nested
 * class here: a direct experiment confirmed that this project's
 * {@code @WebMvcTest} classpath component scan does NOT discover a
 * package-private/static {@code @RestController} nested inside a test class,
 * whereas a standalone top-level controller in the same package is
 * discovered correctly and has its {@code @RequestMapping}s registered. The
 * real {@code GlobalExceptionHandler} is picked up automatically since
 * {@code @RestControllerAdvice} beans are included in {@code @WebMvcTest}
 * slices by default.
 *
 * <p>As in {@code StudyPlanControllerTest}, {@code @WebMvcTest} pulls in
 * {@code JwtAuthenticationFilter} regardless of which controller is sliced
 * (it is a {@code Filter}-typed {@code @Component}), so {@link JwtService}
 * and {@link ProStriverUserDetailsService} must be mocked purely to satisfy
 * its constructor. Neither is stubbed since no request here sends an
 * {@code Authorization: Bearer} header. Spring Security's default
 * {@code @WebMvcTest} auto-configuration requires authentication on every
 * endpoint, so each request below is authenticated solely via
 * {@code SecurityMockMvcRequestPostProcessors.user(...)} with a fixture
 * {@link ProStriverUserDetails} principal, exactly as in
 * {@code StudyPlanControllerTest}.
 */
@WebMvcTest(ExceptionTriggerController.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private ProStriverUserDetailsService userDetailsService;

    // ---- fixtures ---------------------------------------------------------

    private ProStriverUserDetails principal() {
        User fakeUser = new User();
        fakeUser.setId(UUID.randomUUID());
        fakeUser.setRole(Role.USER);
        return new ProStriverUserDetails(fakeUser);
    }

    @Test
    void createThing_malformedJsonBody_returns400() throws Exception {
        mockMvc.perform(post("/test/exceptions/thing")
                        .with(user(principal()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        // intentionally malformed JSON to trigger HttpMessageNotReadableException;
                        // //language=TEXT below stops the IDE from mis-parsing it as (invalid) JSON
                        .content(/*language=TEXT*/ "{not-valid-json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getById_nonUuidPathVariable_returns400() throws Exception {
        mockMvc.perform(get("/test/exceptions/{id}", "not-a-uuid")
                        .with(user(principal())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void search_missingRequiredParam_returns400() throws Exception {
        mockMvc.perform(get("/test/exceptions/search")
                        .with(user(principal())))
                .andExpect(status().isBadRequest());
    }
}
