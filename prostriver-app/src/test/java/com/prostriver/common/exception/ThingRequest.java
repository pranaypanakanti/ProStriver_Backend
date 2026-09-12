package com.prostriver.common.exception;

/**
 * Test-only DTO for {@link ExceptionTriggerController}'s malformed-body
 * endpoint, used by {@link GlobalExceptionHandlerTest}.
 */
record ThingRequest(String name) {
}
