package com.interview.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.util.JwtUtil;
import com.interview.util.UserContext;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

/**
 * JWT认证过滤器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    /**
     * 需要放行的路径
     */
    private static final String[] WHITE_LIST = {
            "/api/user/login",
            "/api/user/register",
            "/api/job/list",
            "/api/job/hot",
            "/api/job/{id}",
            "/api/company/{id}",
            "/api/company/{id}/jobs",
            "/api/company/search",
            "/api/company/list",
            "/api/category/tree",
            "/api/category/list",
            "/api/region/tree",
            "/api/region/children/*",
            "/api/region/path/*",
            "/api/benefit-tag/list",
            "/api/search/**",
            "/api/interview/tts",
            "/api/interview/check-complete",
            "/api/stt",
            "/api/resume/upload",
            "/api/resume/",
            "/",
            "/*.html",
            "/*.css",
            "/*.js",
            "/assets/**",
            "/favicon.ico",
            "/ws/**"
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // 放行白名单路径
        if (isWhiteListed(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 获取Token
        String token = getTokenFromRequest(request);

        if (token == null) {
            // 未携带Token，返回401
            sendError(response, 401, "请先登录");
            return;
        }

        // 验证Token
        if (!jwtUtil.validateToken(token)) {
            log.error("Token验证失败");
            sendError(response, 401, "登录已过期，请重新登录");
            return;
        }

        // 解析Token，将用户信息放入ThreadLocal
        try {
            Long userId = jwtUtil.getUserId(token);
            String username = jwtUtil.getUsername(token);
            Integer roleType = jwtUtil.getRoleType(token);

            log.info("JWT解析: userId={}, username={}, roleType={}, path={}", userId, username, roleType, path);

            UserContext.setUserId(userId);
            UserContext.setUsername(username);
            UserContext.setRoleType(roleType);

            // 管理后台接口校验管理员角色（roleType=3）
            if (path.startsWith("/api/admin/") && !path.equals("/api/admin/login")) {
                if (roleType == null || roleType != 3) {
                    sendError(response, 403, "无权限访问，仅管理员可用");
                    return;
                }
            }

            filterChain.doFilter(request, response);
        } catch (Exception e) {
            log.error("Token解析失败", e);
            sendError(response, 401, "登录已过期，请重新登录");
        } finally {
            UserContext.clear();
        }
    }

    /**
     * 从请求中获取Token
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        // 也支持从查询参数获取（WebSocket等场景）
        return request.getParameter("token");
    }

    /**
     * 判断路径是否在白名单中
     */
    private boolean isWhiteListed(String path) {
        for (String pattern : WHITE_LIST) {
            if (matchPath(path, pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 简单的路径匹配
     */
    private boolean matchPath(String path, String pattern) {
        // 处理静态资源
        if (pattern.contains("/**")) {
            String prefix = pattern.replace("/**", "");
            return path.startsWith(prefix);
        }
        // 处理通配符
        if (pattern.contains("*")) {
            String regex = pattern.replace("*", ".*");
            return path.matches(regex);
        }
        // 处理路径参数 {id} — 只匹配数字，避免 /api/company/{id} 误匹配 /api/company/my
        if (pattern.contains("{")) {
            String regex = pattern.replaceAll("\\{[^}]+}", "\\\\d+");
            return path.matches(regex);
        }
        return path.equals(pattern);
    }

    /**
     * 返回错误响应
     */
    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(
                Map.of("success", false, "message", message)
        ));
    }
}
