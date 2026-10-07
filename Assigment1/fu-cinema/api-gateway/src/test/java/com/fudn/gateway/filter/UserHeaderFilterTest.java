package com.fudn.gateway.filter;

import com.fudn.gateway.config.SecurityConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.servlet.function.ServerRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UserHeaderFilterTest {
    @AfterEach
    void cleanContext() {
        SecurityContextHolder.clearContext();
    }

    private ServerRequest request() {
        MockHttpServletRequest servlet = new MockHttpServletRequest("GET", "/api/customers/me");
        servlet.addHeader("x-user-id", "99");
        servlet.addHeader("X-User-Email", "attacker@example.com");
        servlet.addHeader("X-User-Role", "ADMIN");
        servlet.addHeader("X-User-Other", "spoof");
        servlet.addHeader("Accept", "application/json");
        return ServerRequest.create(servlet, List.of());
    }

    private Jwt token(String role) {
        return Jwt.withTokenValue("test").header("alg", "HS256")
                .subject("an@gmail.com").claim("uid", 1L).claim("role", role).build();
    }

    @Test
    void anonymousRequestLosesAllSpoofedUserHeaders() {
        ServerRequest forwarded = UserHeaderFilter.forwardUserInfo().apply(request());
        assertNull(forwarded.headers().firstHeader("X-User-Id"));
        assertNull(forwarded.headers().firstHeader("X-User-Email"));
        assertNull(forwarded.headers().firstHeader("X-User-Role"));
        assertNull(forwarded.headers().firstHeader("X-User-Other"));
        assertEquals("application/json", forwarded.headers().firstHeader("Accept"));
    }

    @Test
    void authenticatedIdentityReplacesSpoofedValues() {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(token("CUSTOMER")));
        ServerRequest forwarded = UserHeaderFilter.forwardUserInfo().apply(request());
        assertEquals(List.of("1"), forwarded.headers().header("X-User-Id"));
        assertEquals(List.of("an@gmail.com"), forwarded.headers().header("X-User-Email"));
        assertEquals(List.of("CUSTOMER"), forwarded.headers().header("X-User-Role"));
        assertNull(forwarded.headers().firstHeader("X-User-Other"));
    }

    @Test
    void roleClaimBecomesSpringSecurityAuthority() {
        var converter = new SecurityConfig().jwtAuthenticationConverter();
        var auth = converter.convert(token("ADMIN"));
        assertNotNull(auth);
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        assertFalse(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER")));
    }
}
