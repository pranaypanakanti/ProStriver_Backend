package com.prostriver.common.exception;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Test-only controller whose sole purpose is to trigger the exceptions
 * handled by {@link GlobalExceptionHandler} in {@link GlobalExceptionHandlerTest}.
 * None of its endpoints are expected to be reached successfully in those tests.
 *
 * <p>This is deliberately a standalone top-level class rather than a nested
 * static class inside the test: a direct experiment confirmed that this
 * project's {@code @WebMvcTest} classpath component scan does NOT discover a
 * package-private/static {@code @RestController} nested inside a test class,
 * whereas a standalone top-level controller in the same package is discovered
 * and has its {@code @RequestMapping}s registered correctly.
 */
@RestController
public class ExceptionTriggerController {

    @PostMapping("/test/exceptions/thing")
    public String createThing(@RequestBody ThingRequest request) {
        return request.name();
    }

    @GetMapping("/test/exceptions/{id}")
    public String getById(@PathVariable UUID id) {
        return id.toString();
    }

    @GetMapping("/test/exceptions/search")
    public String search(@RequestParam String value) {
        return value;
    }
}
