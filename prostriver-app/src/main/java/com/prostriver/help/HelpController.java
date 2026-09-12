package com.prostriver.help;

import com.prostriver.auth.dto.MessageResponse;
import com.prostriver.common.exception.ApiException;
import com.prostriver.security.ProStriverUserDetails;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The help endpoint. Authenticated by {@code SecurityConfig}'s existing
 * {@code .anyRequest().authenticated()}, so nothing was added to the security chain for it.
 *
 * <p>Response shapes, so the client needs exactly one branch:
 * <ul>
 *   <li>200 returns {@link HelpAnswer}, for all three modes including refusals.</li>
 *   <li>Everything else returns {@code MessageResponse}, the same shape
 *       {@code GlobalExceptionHandler} already returns for 400, 401 and 500.</li>
 * </ul>
 * The 429 carries {@code Retry-After} in seconds, which is where the wait belongs rather than in a
 * bespoke body field.
 */
@RestController
@Profile("api")
@RequestMapping("/api/help")
public class HelpController {

    /**
     * How long to tell a client to wait when the limiter itself is down. Short, because the outage
     * may clear quickly, but long enough that clients are not hammering a struggling Redis.
     */
    private static final int LIMITER_UNAVAILABLE_BACKOFF_SECONDS = 30;

    private final HelpService helpService;
    private final HelpRateLimiter rateLimiter;

    public HelpController(HelpService helpService, HelpRateLimiter rateLimiter) {
        this.helpService = helpService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/ask")
    public ResponseEntity<Object> ask(@AuthenticationPrincipal ProStriverUserDetails user,
                                      @RequestBody HelpAskRequest request) {

        HelpRateLimiter.Decision decision;
        try {
            decision = rateLimiter.check(user.getUserId());
        } catch (ApiException e) {
            // The limiter refused because it could not reach Redis. GlobalExceptionHandler would
            // map the status correctly on its own, but it cannot attach Retry-After, and a 503
            // without one invites an immediate retry into an outage. So it is answered here.
            return ResponseEntity.status(e.getStatus())
                    .header(HttpHeaders.RETRY_AFTER, String.valueOf(LIMITER_UNAVAILABLE_BACKOFF_SECONDS))
                    .body(new MessageResponse(e.getMessage()));
        }

        if (!decision.allowed()) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header(HttpHeaders.RETRY_AFTER, String.valueOf(decision.retryAfterSeconds()))
                    .body(new MessageResponse("Too many help questions. Try again shortly."));
        }

        HelpAnswer answer = helpService.ask(request.question());

        return ResponseEntity.ok()
                .header("X-Rate-Limit-Remaining", String.valueOf(decision.remaining()))
                .body(answer);
    }
}
