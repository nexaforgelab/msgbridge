package com.msgbridge.security;

import com.msgbridge.config.MsgBridgeProperties;
import com.msgbridge.core.AdminRole;
import com.msgbridge.core.Constants;
import com.msgbridge.crypto.HmacSigner;
import com.msgbridge.service.AdminTokenService;
import com.msgbridge.web.GlobalErrorWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Order(10)
@Component
public class AdminApiKeyFilter extends OncePerRequestFilter {
    private static final String ADMIN_KEY = "X-MB-Admin-Key";

    private final MsgBridgeProperties properties;
    private final HmacSigner hmacSigner;
    private final AdminTokenService adminTokenService;
    private final GlobalErrorWriter errorWriter;

    public AdminApiKeyFilter(
            MsgBridgeProperties properties,
            HmacSigner hmacSigner,
            AdminTokenService adminTokenService,
            GlobalErrorWriter errorWriter) {
        this.properties = properties;
        this.hmacSigner = hmacSigner;
        this.adminTokenService = adminTokenService;
        this.errorWriter = errorWriter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/admin/") || path.equals("/admin/auth/login");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        AdminPrincipal principal = authenticate(request);
        if (principal == null) {
            errorWriter.write(response, 401, "invalid admin key");
            return;
        }
        if (!AdminPermission.allows(principal.role(), request)) {
            errorWriter.write(response, 403, "admin role is not allowed to perform this operation");
            return;
        }
        request.setAttribute(Constants.ADMIN_USERNAME_ATTR, principal.username());
        request.setAttribute(Constants.ADMIN_ROLE_ATTR, principal.role().name());
        filterChain.doFilter(request, response);
    }

    private AdminPrincipal authenticate(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return adminTokenService.verify(authorization.substring("Bearer ".length())).orElse(null);
        }
        String provided = request.getHeader(ADMIN_KEY);
        if (hmacSigner.constantTimeEquals(properties.getSecurity().getAdminKey(), provided)) {
            return new AdminPrincipal("admin-key", "Admin Key", AdminRole.SUPER_ADMIN);
        }
        return null;
    }
}
