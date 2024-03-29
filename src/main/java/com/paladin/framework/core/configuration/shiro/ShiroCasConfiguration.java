package com.paladin.framework.core.configuration.shiro;

import org.apache.shiro.authc.pam.AuthenticationStrategy;
import org.apache.shiro.authc.pam.FirstSuccessfulStrategy;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.session.mgt.eis.SessionDAO;
import org.apache.shiro.spring.LifecycleBeanPostProcessor;
import org.apache.shiro.spring.security.interceptor.AuthorizationAttributeSourceAdvisor;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.mgt.WebSecurityManager;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.apache.shiro.web.session.mgt.WebSessionManager;
import org.pac4j.cas.client.CasClient;
import org.pac4j.cas.config.CasConfiguration;
import org.pac4j.cas.config.CasProtocol;
import org.pac4j.core.client.Clients;
import org.pac4j.core.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.autoproxy.AbstractAdvisorAutoProxyCreator;
import org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.redis.core.RedisTemplate;

import com.paladin.framework.core.configuration.shiro.filter.CallbackHttpActionAdapter;
import com.paladin.framework.core.configuration.shiro.filter.PaladinCasAuthenticationFilter;
import com.paladin.framework.core.configuration.shiro.filter.PaladinCasLogoutFilter;
import com.paladin.framework.core.configuration.shiro.filter.ShiroLoginSuccessHandler;
import com.paladin.framework.core.configuration.shiro.session.ClusterSessionFactory;
import com.paladin.framework.core.configuration.shiro.session.PaladinWebSessionManager;
import com.paladin.framework.core.configuration.shiro.session.ShiroRedisSessionDAO;
import com.paladin.framework.utils.LogContentUtil;

import io.buji.pac4j.context.ShiroSessionStore;
import io.buji.pac4j.filter.CallbackFilter;
import io.buji.pac4j.subject.Pac4jSubjectFactory;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.Filter;

/**
 * 
 * <h2>shiro配置</h2>
 * <p>
 * 修改了部分shiro的代码，从而提高效率，减少session的重复读取
 * </p>
 * 
 * @author TontoZhou
 * @since 2018年3月21日
 */
@Configuration
@ConditionalOnProperty(prefix = "paladin", value = "cas-enabled", havingValue = "true", matchIfMissing = false)
@EnableConfigurationProperties(ShiroCasProperties.class)
public class ShiroCasConfiguration {

	private static Logger logger = LoggerFactory.getLogger(ShiroCasConfiguration.class);

	@Bean(name = "redisSessionDAO")
	public ShiroRedisSessionDAO redisSessionDAO(ShiroCasProperties shiroCasProperties, RedisTemplate<String, Object> jdkRedisTemplate) {
		logger.info(LogContentUtil.createComponent(SessionDAO.class, ShiroRedisSessionDAO.class));

		ShiroRedisSessionDAO sessionDao = new ShiroRedisSessionDAO(shiroCasProperties, jdkRedisTemplate);
		return sessionDao;
	}

	@Bean(name = "sessionManager")
	public DefaultWebSessionManager defaultWebSessionManager(ShiroCasProperties shiroCasProperties, ShiroRedisSessionDAO redisSessionDAO) {
		logger.info(LogContentUtil.createComponent(WebSessionManager.class, DefaultWebSessionManager.class));

		DefaultWebSessionManager sessionManager = new PaladinWebSessionManager(shiroCasProperties);

		if (shiroCasProperties.isRedisEnabled()) {
			// 如果设置集群共享session，需要redis来存放session
			sessionManager.setSessionDAO(redisSessionDAO);
			// 用户权限，认证等缓存设置，因为验证权限部分用其他方式实现，所以不需要缓存
			// sessionManager.setCacheManager(new RedisCacheManager());
			sessionManager.setSessionFactory(new ClusterSessionFactory());
		}

		// session 监听
		// Collection<SessionListener> sessionListeners = new ArrayList<>();
		// sessionListeners.add(new CustomSessionListener());
		// sessionManager.setSessionListeners(sessionListeners);

		// 单位为毫秒（1秒=1000毫秒） 3600000毫秒为1个小时
		sessionManager.setSessionValidationInterval(3600000);
		// 3600000 milliseconds = 1 hour
		sessionManager.setGlobalSessionTimeout(shiroCasProperties.getSessionTime() * 60 * 1000);
		// 是否删除无效的，默认也是开启
		sessionManager.setDeleteInvalidSessions(true);
		// 是否开启 检测，默认开启
		sessionManager.setSessionValidationSchedulerEnabled(true);
		// 是否在url上显示检索得到的sessionid
		sessionManager.setSessionIdUrlRewritingEnabled(false);

		return sessionManager;
	}

	@Bean(name = "securityManager")
	public DefaultWebSecurityManager defaultWebSecurityManage(DefaultWebSessionManager defaultWebSessionManager, List<Realm> realms) {
		logger.info(LogContentUtil.createComponent(WebSecurityManager.class, DefaultWebSecurityManager.class));

		DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();
		securityManager.setRealms(realms);
		securityManager.setSubjectFactory(new Pac4jSubjectFactory()); // cas

		// 注入缓存管理器;
		// securityManager.setCacheManager(redisCacheManager());
		securityManager.setSessionManager(defaultWebSessionManager);
		return securityManager;
	}

	@Bean(name = "shiroFilter")
	@ConditionalOnMissingBean(ShiroFilterFactoryBean.class)
	public ShiroFilterFactoryBean shiroFilterFactoryBean(DefaultWebSecurityManager securityManager, ShiroCasProperties shiroCasProperties, List<ShiroLoginSuccessHandler> loginSuccessHandlers) {
		logger.info(LogContentUtil.createComponent(ShiroFilterFactoryBean.class, ShiroFilterFactoryBean.class));

		ShiroFilterFactoryBean shiroFilterFactoryBean = new PaladinShiroFilterFactoryBean(shiroCasProperties);
		// 必须设置 SecurityManager
		shiroFilterFactoryBean.setSecurityManager(securityManager);
		shiroFilterFactoryBean.setUnauthorizedUrl(shiroCasProperties.getUnauthorizedUrl());

		// 增加自定义过滤
		Map<String, Filter> filters = new HashMap<>();
		Config config = getConfig(shiroCasProperties);

		CallbackFilter callbackFilter = new CallbackFilter();
		callbackFilter.setConfig(config);
		callbackFilter.setDefaultUrl(shiroCasProperties.getSuccessUrl());
		callbackFilter.setHttpActionAdapter(new CallbackHttpActionAdapter(shiroCasProperties));
		filters.put("callback", callbackFilter);

		PaladinCasLogoutFilter logoutFilter = new PaladinCasLogoutFilter(shiroCasProperties, config);
		filters.put("logout", logoutFilter);

		PaladinCasAuthenticationFilter authenticationFilter = new PaladinCasAuthenticationFilter(shiroCasProperties, config);
		authenticationFilter.setLoginSuccessHandlers(loginSuccessHandlers);
		filters.put("authc", authenticationFilter);

		shiroFilterFactoryBean.setFilters(filters);
		// 拦截器.
		Map<String, String> filterChainDefinitionMap = new LinkedHashMap<String, String>();

		filterChainDefinitionMap.put(shiroCasProperties.getCasFilterUrlPattern(), "callback");
		filterChainDefinitionMap.put(shiroCasProperties.getLogoutUrl(), "logout");
		filterChainDefinitionMap.put("/**", "authc");
		shiroFilterFactoryBean.setFilterChainDefinitionMap(filterChainDefinitionMap);
		return shiroFilterFactoryBean;
	}

	private Config getConfig(ShiroCasProperties cas) {
		CasConfiguration casConfiguration = new CasConfiguration(cas.getCasServerLoginUrl(), cas.getCasServerUrl() + "/");
		casConfiguration.setAcceptAnyProxy(true);
		casConfiguration.setProtocol(CasProtocol.valueOf(cas.getCasProtocol()));

		CasClient casClient = new CasClient(casConfiguration);
		casClient.setCallbackUrl(cas.getClientServerUrl() + cas.getCasFilterUrlPattern() + "?client_name=CasClient");

		Clients clients = new Clients(cas.getClientServerUrl() + cas.getCasFilterUrlPattern() + "?client_name=CasClient", casClient);
		Config config = new Config(clients);
		config.setSessionStore(new ShiroSessionStore());
		return config;
	}

	@Bean(name = "authenticationStrategy")
	public AuthenticationStrategy authenticationStrategy() {
		logger.info(LogContentUtil.createComponent(AuthenticationStrategy.class, FirstSuccessfulStrategy.class));
		return new FirstSuccessfulStrategy();
	}

	@Bean(name = "authorizationAttributeSourceAdvisor")
	public AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor(DefaultWebSecurityManager securityManager) {
		logger.info(LogContentUtil.createComponent(AuthorizationAttributeSourceAdvisor.class, AuthorizationAttributeSourceAdvisor.class));

		AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor = new AuthorizationAttributeSourceAdvisor();
		authorizationAttributeSourceAdvisor.setSecurityManager(securityManager);
		return authorizationAttributeSourceAdvisor;
	}

	@Bean(name = "lifecycleBeanPostProcessor")
	public LifecycleBeanPostProcessor lifecycleBeanPostProcessor() {
		logger.info(LogContentUtil.createComponent(LifecycleBeanPostProcessor.class, LifecycleBeanPostProcessor.class));
		return new LifecycleBeanPostProcessor();
	}

	@ConditionalOnMissingBean
	@Bean
	@DependsOn("lifecycleBeanPostProcessor")
	public DefaultAdvisorAutoProxyCreator getDefaultAdvisorAutoProxyCreator() {
		logger.info(LogContentUtil.createComponent(AbstractAdvisorAutoProxyCreator.class, DefaultAdvisorAutoProxyCreator.class));

		DefaultAdvisorAutoProxyCreator daap = new DefaultAdvisorAutoProxyCreator();
		daap.setProxyTargetClass(true);
		return daap;
	}

}
