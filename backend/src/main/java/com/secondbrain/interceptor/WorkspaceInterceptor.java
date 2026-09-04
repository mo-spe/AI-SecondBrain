package com.secondbrain.interceptor;

import com.secondbrain.entity.Workspace;
import com.secondbrain.entity.WorkspaceMember;
import com.secondbrain.service.WorkspaceService;
import com.secondbrain.util.WorkspaceRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 工作区权限拦截器.
 * <p>对 /workspace/** 路径校验用户是否为工作区成员，并根据角色控制访问权限</p>
 *
 * <p>权限规则：
 * <ul>
 *   <li>GET 请求：所有成员（viewer+）可访问</li>
 *   <li>POST/PUT/DELETE：仅 admin/owner 可访问</li>
 *   <li>DELETE 工作区本身：仅 owner 可访问（由 Service 层二次校验）</li>
 * </ul>
 */
@Component
public class WorkspaceInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceInterceptor.class);

    private final WorkspaceService workspaceService;

    /**
     * 构造器注入工作区服务.
     *
     * @param workspaceService 工作区服务
     */
    public WorkspaceInterceptor(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    /**
     * 请求预处理，校验用户对目标工作区的访问权限.
     *
     * @param request  HTTP 请求
     * @param response HTTP 响应
     * @param handler  处理器
     * @return 校验通过返回 true，否则返回 false
     * @throws Exception 处理异常
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestUri = request.getRequestURI();

        if (!requestUri.startsWith("/api/workspace/")) {
            return true;
        }

        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"请先登录\"}");
            return false;
        }

        Long workspaceId = extractWorkspaceId(requestUri);
        if (workspaceId == null) {
            return true;
        }

        Workspace workspace = workspaceService.getWorkspaceById(workspaceId);
        if (workspace == null || workspace.getDeleted() == 1) {
            response.setStatus(404);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":404,\"message\":\"工作区不存在\"}");
            return false;
        }

        WorkspaceMember member = workspaceService.getMemberByWorkspaceAndUser(workspaceId, userId);
        if (member == null) {
            response.setStatus(403);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":403,\"message\":\"您不是该工作区的成员\"}");
            return false;
        }

        String method = request.getMethod();

        // pending 成员确认加入工作区时，允许跳过写权限校验
        if (requestUri.matches("/api/workspace/\\d+/members/accept") && "pending".equals(member.getStatus())) {
            request.setAttribute("workspaceId", workspaceId);
            request.setAttribute("memberRole", member.getRole());
            return true;
        }

        // 已确认成员切换工作区（签发新JWT，非数据变更），允许所有已确认成员访问
        if (requestUri.matches("/api/workspace/\\d+/switch") && isAcceptedMember(member)) {
            request.setAttribute("workspaceId", workspaceId);
            request.setAttribute("memberRole", member.getRole());
            return true;
        }

        if (isWriteMethod(method) && !WorkspaceRole.isAdmin(member.getRole())) {
            response.setStatus(403);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":403,\"message\":\"权限不足：当前角色为 " + member.getRole() + "\"}");
            return false;
        }

        request.setAttribute("workspaceId", workspaceId);
        request.setAttribute("memberRole", member.getRole());
        return true;
    }

    /**
     * 从请求URI中提取工作区ID.
     * <p>匹配模式：/api/workspace/{id}/... 或 /api/workspace/{id}</p>
     *
     * @param uri 请求URI
     * @return 工作区ID，无法解析时返回 null
     */
    private Long extractWorkspaceId(String uri) {
        String prefix = "/api/workspace/";
        if (!uri.startsWith(prefix)) {
            return null;
        }
        String suffix = uri.substring(prefix.length());
        int slashIndex = suffix.indexOf('/');
        String idStr = slashIndex > 0 ? suffix.substring(0, slashIndex) : suffix;
        try {
            return Long.valueOf(idStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 判断请求方法是否为写操作.
     *
     * @param method HTTP 请求方法
     * @return 是写操作返回 true，否则返回 false
     */
    private boolean isWriteMethod(String method) {
        return "POST".equals(method) || "PUT".equals(method) || "DELETE".equals(method) || "PATCH".equals(method);
    }

    /**
     * 判断成员是否可以访问工作区。
     *
     * @param member 工作区成员
     * @return 已确认或旧版本兼容成员返回 true
     */
    private boolean isAcceptedMember(WorkspaceMember member) {
        // V10 之前创建的成员记录 status 为空，但其本身已经代表有效成员，不能因此阻断切换。
        return member != null
                && (member.getStatus() == null || "accepted".equalsIgnoreCase(member.getStatus()));
    }
}
