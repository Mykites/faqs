package com.paladin.framework.core.configuration.shiro.filter;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.subject.Subject;

public interface ShiroLoginSuccessHandler {
	
	public void onLoginSuccess(AuthenticationToken token, Subject subject, ServletRequest request, ServletResponse response);
}
