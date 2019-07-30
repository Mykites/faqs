package com.paladin.common.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.Permission;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.paladin.common.core.permission.MenuPermission;
import com.paladin.common.core.permission.PermissionContainer;
import com.paladin.common.core.permission.Role;
import com.paladin.framework.core.session.UserSession;

public class CommonUserSession extends UserSession implements AuthorizationInfo {

	private static final long serialVersionUID = -8959595306921251181L;
	
	private static final int ROLE_LEVEL_SYSTEM_ADMIN = 99;

	public CommonUserSession(String userId, String userName, String account, String[] roleIds) {
		super(userId, userName, account);
		setRoleId(roleIds);
	}

	public CommonUserSession(String userId, String userName, String account) {
		super(userId, userName, account);
		this.roleLevel = ROLE_LEVEL_SYSTEM_ADMIN;
		this.isSystemAdmin = true;
	}

	/**
	 * 获取当前用户会话
	 * 
	 * @return
	 */
	public static CommonUserSession getCurrentUserSession() {
		return (CommonUserSession) SecurityUtils.getSubject().getPrincipal();
	}

	private List<String> roleIds;
	private int roleLevel;
	private boolean isSystemAdmin = false;

	/**
	 * 设置角色ID
	 * 
	 * @param roleIds
	 */
	private void setRoleId(String... roleIds) {
		List<String> roleIdList = new ArrayList<>(roleIds.length);
		int roleLevel = 0;
		for (int i = 0; i < roleIds.length; i++) {
			String roleId = roleIds[i];
			if (roleId != null) {
				Role role = PermissionContainer.getInstance().getRole(roleId);
				if(role != null) {
					roleIdList.add(roleId);
					roleLevel = Math.max(roleLevel, role.getRoleLevel());
				}				
			}
		}
		
		this.roleLevel = roleLevel;
		this.roleIds = roleIdList;
	}

	/**
	 * 获取角色拥有的数据等级
	 * 
	 * @return
	 */
	public int getRoleLevel() {
		return roleLevel;
	}

	/**
	 * 菜单资源
	 * 
	 * @return
	 */
	public Collection<MenuPermission> getMenuResources() {
		PermissionContainer container = PermissionContainer.getInstance();
		if (isSystemAdmin) {
			return container.getSystemAdminRole().getMenuPermissions();
		}

		if (roleIds.size() == 1) {
			return container.getRole(roleIds.get(0)).getMenuPermissions();
		}

		ArrayList<Role> roles = new ArrayList<>(roleIds.size());
		for (String rid : roleIds) {
			Role role = container.getRole(rid);
			if (role != null) {
				roles.add(role);
			}
		}
		return Role.getMultiRoleMenuPermission(roles);
	}

	@Override
	@JsonIgnore
	public Collection<String> getRoles() {
		return roleIds;
	}

	@Override
	@JsonIgnore
	public Collection<String> getStringPermissions() {
		return null;
	}

	@Override
	@JsonIgnore
	public Collection<Permission> getObjectPermissions() {
		PermissionContainer container = PermissionContainer.getInstance();
		if (isSystemAdmin) {
			return container.getSystemAdminRole().getPermissionObjects();
		}

		if (roleIds.size() == 1) {
			return container.getRole(roleIds.get(0)).getPermissionObjects();
		}

		ArrayList<Role> roles = new ArrayList<>(roleIds.size());
		for (String rid : roleIds) {
			Role role = container.getRole(rid);
			if (role != null) {
				roles.add(role);
			}
		}
		return Role.getMultiRolePermissionObject(roles);
	}

	@Override
	@JsonIgnore
	public Object getUserForView() {
		HashMap<String, Object> map = new HashMap<>();
		map.put("username", getUserName());
		map.put("account", getAccount());
		map.put("roleLevel", getRoleLevel());
		return map;
	}

	@Override
	public boolean isSystemAdmin() {
		return isSystemAdmin;
	}

}
