package com.paladin.common.core;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.crypto.hash.SimpleHash;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;

import com.paladin.common.model.syst.SysUser;
import com.paladin.common.service.syst.SysUserService;
import com.paladin.framework.core.configuration.shiro.filter.ShiroLoginSuccessHandler;

public class LoginSuccessHandler implements ShiroLoginSuccessHandler {

	@Autowired
	private SysUserService sysUserService;

	@Override
	public void onLoginSuccess(AuthenticationToken token, Subject subject, ServletRequest request, ServletResponse response) {
		if (token instanceof UsernamePasswordToken) {
			// 为了兼容东软CAS服务器认证规则（使用MD5加密不加盐），这里在第一次登录情况下判断是否有cas适用的密码，没有则生成一个
			UsernamePasswordToken upToken = (UsernamePasswordToken) token;
			SysUser user = sysUserService.getUserByAccount(upToken.getUsername());
			String casPassword = user.getCasPassword();
			if (casPassword == null || casPassword.length() == 0) {
				casPassword = new SimpleHash("md5", upToken.getPassword()).toBase64();
				SysUser updateUser = new SysUser();
				updateUser.setId(user.getId());
				updateUser.setCasPassword(casPassword);
				sysUserService.updateSelective(updateUser);
			}
		}
	}

}
