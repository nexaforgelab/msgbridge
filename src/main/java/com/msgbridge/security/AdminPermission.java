package com.msgbridge.security;

import com.msgbridge.core.AdminRole;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;
import org.springframework.http.HttpMethod;

public final class AdminPermission {
    private AdminPermission() {
    }

    public static boolean allows(AdminRole role, HttpServletRequest request) {
        if (role == AdminRole.SUPER_ADMIN) {
            return true;
        }
        String method = request.getMethod();
        String path = request.getRequestURI();
        if (isRead(method) && canRead(role, path)) {
            return true;
        }
        return switch (role) {
            case PLATFORM_ADMIN -> platformAdminWrite(path);
            case OPS -> opsWrite(path);
            case DEVELOPER -> developerWrite(path);
            case BUSINESS_ADMIN -> businessAdminWrite(path);
            case AUDITOR -> false;
            case SUPER_ADMIN -> true;
        };
    }

    private static boolean canRead(AdminRole role, String path) {
        if (role == AdminRole.AUDITOR) {
            return path.startsWith("/admin/dashboard")
                    || path.startsWith("/admin/messages")
                    || path.startsWith("/admin/audit-logs")
                    || path.startsWith("/admin/channels/health")
                    || path.startsWith("/admin/system/status");
        }
        if (role == AdminRole.DEVELOPER) {
            return path.startsWith("/admin/dashboard")
                    || path.startsWith("/admin/channels")
                    || path.startsWith("/admin/templates")
                    || path.startsWith("/admin/routes")
                    || path.startsWith("/admin/messages")
                    || path.startsWith("/admin/system/status");
        }
        return path.startsWith("/admin/");
    }

    private static boolean platformAdminWrite(String path) {
        return starts(path, Set.of("/admin/apps", "/admin/channels", "/admin/templates", "/admin/routes"));
    }

    private static boolean opsWrite(String path) {
        return path.matches("^/admin/messages/[^/]+/(retry|terminate)$")
                || path.matches("^/admin/messages/bulk-(retry|terminate)$")
                || path.matches("^/admin/channels/[^/]+/test$");
    }

    private static boolean developerWrite(String path) {
        return path.startsWith("/admin/templates/preview")
                || path.startsWith("/admin/routes/simulate")
                || path.matches("^/admin/channels/[^/]+/test$");
    }

    private static boolean businessAdminWrite(String path) {
        return path.matches("^/admin/messages/[^/]+/retry$");
    }

    private static boolean starts(String path, Set<String> prefixes) {
        return prefixes.stream().anyMatch(path::startsWith);
    }

    private static boolean isRead(String method) {
        return HttpMethod.GET.matches(method);
    }
}
