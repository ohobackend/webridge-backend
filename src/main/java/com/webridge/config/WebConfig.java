package com.webridge.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.UrlHandlerFilter;

@Configuration
public class WebConfig {

    @Bean
    public FilterRegistrationBean<UrlHandlerFilter> trailingSlashFilter() {
        var filter = UrlHandlerFilter.trailingSlashHandler("/api/**").wrapRequest().build();
        var bean = new FilterRegistrationBean<>(filter);
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }
}
