package com.msgbridge.security;

import com.msgbridge.config.MsgBridgeProperties;
import com.msgbridge.core.Constants;
import com.msgbridge.crypto.CryptoService;
import com.msgbridge.crypto.HmacSigner;
import com.msgbridge.domain.MbApp;
import com.msgbridge.repository.AppRepository;
import com.msgbridge.web.GlobalErrorWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Order(20)
@Component
public class RequestAuthFilter extends OncePerRequestFilter {
    private static final String APP_ID = "X-MB-App-Id";
    private static final String TIMESTAMP = "X-MB-Timestamp";
    private static final String NONCE = "X-MB-Nonce";
    private static final String SIGNATURE = "X-MB-Signature";

    private final AppRepository appRepository;
    private final CryptoService cryptoService;
    private final HmacSigner hmacSigner;
    private final AppRateLimiter rateLimiter;
    private final MsgBridgeProperties properties;
    private final GlobalErrorWriter errorWriter;

    public RequestAuthFilter(
            AppRepository appRepository,
            CryptoService cryptoService,
            HmacSigner hmacSigner,
            AppRateLimiter rateLimiter,
            MsgBridgeProperties properties,
            GlobalErrorWriter errorWriter) {
        this.appRepository = appRepository;
        this.cryptoService = cryptoService;
        this.hmacSigner = hmacSigner;
        this.rateLimiter = rateLimiter;
        this.properties = properties;
        this.errorWriter = errorWriter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || !(path.startsWith("/api/v1/messages") || path.startsWith("/webhook/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        byte[] body = StreamUtils.copyToByteArray(request.getInputStream());
        CachedBodyHttpServletRequest wrapped = new CachedBodyHttpServletRequest(request, body);
        String appId = resolveAppId(wrapped);
        if (!StringUtils.hasText(appId)) {
            errorWriter.write(response, 401, "missing X-MB-App-Id");
            return;
        }

        MbApp app = appRepository.findByAppId(appId).orElse(null);
        if (app == null || app.getStatus() == null || app.getStatus() != Constants.STATUS_ENABLED) {
            errorWriter.write(response, 401, "app is not found or disabled");
            return;
        }

        if (!IpWhitelist.allows(app.getIpWhitelist(), clientIp(wrapped))) {
            errorWriter.write(response, 403, "client IP is not allowed");
            return;
        }

        if (!rateLimiter.allow(appId, app.getRateLimitPerMin() == null ? 600 : app.getRateLimitPerMin())) {
            errorWriter.write(response, 429, "app rate limit exceeded");
            return;
        }

        String timestamp = wrapped.getHeader(TIMESTAMP);
        String nonce = wrapped.getHeader(NONCE);
        String signature = wrapped.getHeader(SIGNATURE);
        if (!StringUtils.hasText(timestamp) || !StringUtils.hasText(nonce) || !StringUtils.hasText(signature)) {
            errorWriter.write(response, 401, "missing signature headers");
            return;
        }

        if (!timestampAllowed(timestamp)) {
            errorWriter.write(response, 401, "timestamp is outside allowed skew");
            return;
        }

        String secret = cryptoService.decrypt(app.getAppSecretEncrypted());
        String expected = hmacSigner.hmacSha256Hex(secret, canonical(wrapped, body));
        if (!hmacSigner.constantTimeEquals(expected, signature.toLowerCase())) {
            errorWriter.write(response, 401, "invalid signature");
            return;
        }

        wrapped.setAttribute(Constants.CURRENT_APP_ID_ATTR, appId);
        filterChain.doFilter(wrapped, response);
    }

    private String resolveAppId(HttpServletRequest request) {
        String headerAppId = request.getHeader(APP_ID);
        if (StringUtils.hasText(headerAppId)) {
            return headerAppId;
        }
        String path = request.getRequestURI();
        if (path.startsWith("/webhook/")) {
            String[] parts = path.split("/");
            if (parts.length >= 3) {
                return parts[2];
            }
        }
        return null;
    }

    private boolean timestampAllowed(String timestampValue) {
        try {
            long raw = Long.parseLong(timestampValue);
            long seconds = raw > 10_000_000_000L ? raw / 1000 : raw;
            long diff = Math.abs(Instant.now().getEpochSecond() - seconds);
            return diff <= properties.getSecurity().getTimestampSkewSeconds();
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String canonical(HttpServletRequest request, byte[] body) {
        String path = request.getRequestURI();
        if (StringUtils.hasText(request.getQueryString())) {
            path += "?" + request.getQueryString();
        }
        return request.getMethod().toUpperCase()
                + "\n" + path
                + "\n" + request.getHeader(TIMESTAMP)
                + "\n" + request.getHeader(NONCE)
                + "\n" + new String(body, StandardCharsets.UTF_8);
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
