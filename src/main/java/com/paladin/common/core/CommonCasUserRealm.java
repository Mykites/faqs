package com.paladin.common.core;

import java.util.Arrays;
import java.util.List;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.pac4j.core.profile.CommonProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.paladin.common.model.syst.SysUser;
import com.paladin.common.service.syst.SysUserService;

import io.buji.pac4j.realm.Pac4jRealm;
import io.buji.pac4j.subject.Pac4jPrincipal;
import io.buji.pac4j.token.Pac4jToken;

public class CommonCasUserRealm extends Pac4jRealm {
	private Logger logger = LoggerFactory.getLogger(this.getClass());

	private SysUserService sysUserService;

	public CommonCasUserRealm(SysUserService sysUserService) {
		super();
		this.sysUserService = sysUserService;
		this.setAuthenticationTokenClass(Pac4jToken.class);
	}

	/**
	 * 认证信息.(身份验证) : Authentication 是用来验证用户身份
	 * 
	 * @param token
	 * @return
	 * @throws AuthenticationException
	 */
	@Override
	protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken) throws AuthenticationException {

		logger.debug("后台登录：SysUserRealm.doGetAuthenticationInfo()");

		final Pac4jToken token = (Pac4jToken) authenticationToken;
        final List<CommonProfile> profiles = token.getProfiles();
        final Pac4jPrincipal principal = new Pac4jPrincipal(profiles, getPrincipalNameAttribute());
        
		String username = principal.getName();
		SysUser sysUser = sysUserService.getUserByAccount(username);

		if (sysUser == null) {
			throw new UnknownAccountException();
		}

		if (sysUser.getState() != SysUser.STATE_ENABLED) {
			throw new LockedAccountException(); // 帐号锁定
		}

		CommonUserSession userSession = new CommonUserSession(sysUser.getId(), username, username);
		List<Object> principals = Arrays.asList(userSession,principal);
        PrincipalCollection principalCollection = new SimplePrincipalCollection(principals, getName());       
        SimpleAuthenticationInfo authenticationInfo=  new SimpleAuthenticationInfo(principalCollection, token.getCredentials());
		
		logger.info("===>用户[" + username + ":" + userSession.getUserName() + "]登录系统<===");
		// 登录日志与更新最近登录时间
		sysUserService.updateLastTime(username);

		return authenticationInfo;
	}

	@Override
	protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) throws AuthenticationException {
		// 废弃shiro缓存验证信息策略
		return null;
	}

	protected AuthorizationInfo getAuthorizationInfo(PrincipalCollection principals) {
		if (principals == null) {
			return null;
		}
		return (AuthorizationInfo) principals.getPrimaryPrincipal();
	}
}
