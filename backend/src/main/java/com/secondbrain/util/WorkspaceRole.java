package com.secondbrain.util;

/** 工作区角色常量. <p>定义工作区成员角色及权限判断工具方法</p> */
public final class WorkspaceRole {

    /** 工作区拥有者角色. */
    public static final String OWNER = "owner";
    /** 工作区管理员角色. */
    public static final String ADMIN = "admin";
    /** 工作区编辑者角色. */
    public static final String EDITOR = "editor";
    /** 工作区只读访客角色. */
    public static final String VIEWER = "viewer";

    /**
     * 是否具有管理权限（owner 或 admin）.
     *
     * @param role 角色名
     * @return 具有管理权限返回 true，否则返回 false
     */
    public static boolean isAdmin(String role) {
        return OWNER.equals(role) || ADMIN.equals(role);
    }

    /**
     * 是否具有编辑权限（owner/admin/editor）.
     *
     * @param role 角色名
     * @return 具有编辑权限返回 true，否则返回 false
     */
    public static boolean canEdit(String role) {
        return OWNER.equals(role) || ADMIN.equals(role) || EDITOR.equals(role);
    }

    /**
     * 验证角色名是否合法.
     *
     * @param role 角色名
     * @return 合法返回 true，否则返回 false
     */
    public static boolean isValid(String role) {
        return OWNER.equals(role) || ADMIN.equals(role) || EDITOR.equals(role) || VIEWER.equals(role);
    }

    private WorkspaceRole() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }
}
