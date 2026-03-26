package com.ProStriver.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "prostriver.security")
public class SecurityProperties {

    private Jwt jwt = new Jwt();
    private Cookies cookies = new Cookies();

    @Getter @Setter
    public static class Jwt {
        private String issuer;
        private int accessTokenMinutes;
        private int refreshTokenDays;
        private String secretBase64;
    }

    @Getter @Setter
    public static class Cookies {
        private String refreshTokenName;
        private String refreshTokenPath;
        private boolean refreshTokenSecure;
        private String refreshTokenSameSite;
    }
}