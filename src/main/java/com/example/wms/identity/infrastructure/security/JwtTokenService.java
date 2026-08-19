package com.example.wms.identity.infrastructure.security;

import java.time.Clock;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenService {
    private final JwtEncoder encoder;
    private final SecurityProperties properties;
    private final Clock clock;

    public JwtTokenService(JwtEncoder encoder, SecurityProperties properties, Clock clock) {
        this.encoder = encoder;
        this.properties = properties;
        this.clock = clock;
    }

    public Token issue(Authentication authentication) {
        var issuedAt = clock.instant();
        var expiresAt = issuedAt.plus(properties.tokenTtl());
        var roles = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority().replaceFirst("^ROLE_", ""))
                .toList();
        var claims = JwtClaimsSet.builder()
                .issuer("wms")
                .subject(authentication.getName())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("roles", roles)
                .build();
        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        var value = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new Token(value, "Bearer", expiresAt, List.copyOf(roles));
    }

    public record Token(String accessToken, String tokenType, java.time.Instant expiresAt, List<String> roles) {}
}
