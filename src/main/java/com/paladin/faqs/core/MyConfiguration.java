package com.paladin.faqs.core;

import org.apache.shiro.realm.AuthorizingRealm;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.paladin.common.core.CommonCasUserRealm;
import com.paladin.common.core.CommonHandlerExceptionResolver;
import com.paladin.common.core.CommonUserRealm;
import com.paladin.common.core.LoginSuccessHandler;
import com.paladin.common.core.TontoDialect;
import com.paladin.common.service.syst.SysUserService;

import io.buji.pac4j.realm.Pac4jRealm;

@Configuration
public class MyConfiguration {

	@Bean
	public TontoDialect getTontoDialect() {
		return new TontoDialect();
	}

	@Bean("casRealm")
	@ConditionalOnProperty(prefix = "paladin", value = "cas-enabled", havingValue = "true", matchIfMissing = false)
	public Pac4jRealm getCasRealm(SysUserService sysUserService) {
		return new CommonCasUserRealm(sysUserService);
	}

	@Bean("localRealm")
	public AuthorizingRealm getLocalRealm(SysUserService sysUserService) {
		return new CommonUserRealm(sysUserService);
	}

	@Bean
	public HandlerExceptionResolver getHandlerExceptionResolver() {
		return new CommonHandlerExceptionResolver();
	}

	@Bean
	public LoginSuccessHandler getRepairLoginSuccessHandler() {
		return new LoginSuccessHandler();
	}
	
}
