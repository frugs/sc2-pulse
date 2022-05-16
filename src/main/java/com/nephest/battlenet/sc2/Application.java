// Copyright (C) 2020-2022 Oleksandr Masniuk
// SPDX-License-Identifier: AGPL-3.0-or-later

package com.nephest.battlenet.sc2;

import com.nephest.battlenet.sc2.config.GlobalRestTemplateCustomizer;
import com.nephest.battlenet.sc2.config.convert.IdentifiableToIntegerConverter;
import com.nephest.battlenet.sc2.config.convert.IntegerToDecisionConverter;
import com.nephest.battlenet.sc2.config.convert.IntegerToLeagueTierTypeConverter;
import com.nephest.battlenet.sc2.config.convert.IntegerToLeagueTypeConverter;
import com.nephest.battlenet.sc2.config.convert.IntegerToMatchTypeConverter;
import com.nephest.battlenet.sc2.config.convert.IntegerToPlayerCharacterReportTypeConverter;
import com.nephest.battlenet.sc2.config.convert.IntegerToQueueTypeConverter;
import com.nephest.battlenet.sc2.config.convert.IntegerToRaceConverter;
import com.nephest.battlenet.sc2.config.convert.IntegerToRegionConverter;
import com.nephest.battlenet.sc2.config.convert.IntegerToSC2PulseAuthority;
import com.nephest.battlenet.sc2.config.convert.IntegerToSocialMediaConverter;
import com.nephest.battlenet.sc2.config.convert.IntegerToTeamTypeConverter;
import com.nephest.battlenet.sc2.config.filter.AverageSessionCacheFilter;
import com.nephest.battlenet.sc2.config.filter.MaintenanceFilter;
import com.nephest.battlenet.sc2.model.Region;
import com.nephest.battlenet.sc2.web.service.WebServiceUtil;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.sql.DataSource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableCaching
@EnableRetry
@EnableScheduling
@EnableTransactionManagement
@PropertySource("classpath:application-private.properties")
public class Application
extends SpringBootServletInitializer
{

    public static final String VERSION = Application.class.getPackage().getImplementationVersion() != null
        ? Application.class.getPackage().getImplementationVersion()
        : "unknown";
    /*
        Don't change the DB thread count. The DB code doesn't handle concurrent transactions, so deadlocks can happen.
        There is no need to have more than 1 thread here because most of the CPU work is done on the DB side, so
        there is no need to properly handle concurrency because it will waste CPU resources without any performance
        boost whatsoever. Concurrency/CPU intensive work is done by web threads.
     */
    public static final int DB_THREADS = 1;
    //+2 for background tasks
    public static final int WEB_THREADS = Region.values().length + 2;

    public static void main(String[] args)
    {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public PlatformTransactionManager txManager(DataSource dataSource)
    {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    public NamedParameterJdbcTemplate sc2StatsNamedTemplate(DataSource dataSource)
    {
        return new NamedParameterJdbcTemplate(dataSource);
    }

    @Bean
    public ConversionService sc2StatsConversionService()
    {
        DefaultFormattingConversionService service = new DefaultFormattingConversionService();
        service.addConverter(new IdentifiableToIntegerConverter());
        service.addConverter(new IntegerToQueueTypeConverter());
        service.addConverter(new IntegerToLeagueTierTypeConverter());
        service.addConverter(new IntegerToLeagueTypeConverter());
        service.addConverter(new IntegerToRegionConverter());
        service.addConverter(new IntegerToTeamTypeConverter());
        service.addConverter(new IntegerToRaceConverter());
        service.addConverter(new IntegerToSocialMediaConverter());
        service.addConverter(new IntegerToMatchTypeConverter());
        service.addConverter(new IntegerToDecisionConverter());
        service.addConverter(new IntegerToSC2PulseAuthority());
        service.addConverter(new IntegerToPlayerCharacterReportTypeConverter());
        return service;
    }

    @Bean
    public ClientHttpConnector clientHttpConnector()
    {
        return WebServiceUtil.getClientHttpConnector();
    }

    @Bean
    public RestTemplateCustomizer restTemplateCustomizer() {
        return new GlobalRestTemplateCustomizer();
    }

    @Bean
    public Random simpleRng()
    {
        return new Random();
    }

    @Bean
    public ExecutorService dbExecutorService()
    {
        return Executors.newFixedThreadPool(DB_THREADS);
    }

    @Bean
    public ExecutorService webExecutorService()
    {
        return Executors.newFixedThreadPool(WEB_THREADS);
    }

    @Bean
    public FilterRegistrationBean<AverageSessionCacheFilter> sessionWebCacheFilter(){
        FilterRegistrationBean<AverageSessionCacheFilter> registrationBean = new FilterRegistrationBean<>();

        registrationBean.setFilter(new AverageSessionCacheFilter());
        registrationBean.addUrlPatterns("/api/ladder/stats/*");

        return registrationBean;
    }

    @Bean @Profile("maintenance")
    public FilterRegistrationBean<MaintenanceFilter> maintenanceFilterRegistration()
    {
        FilterRegistrationBean<MaintenanceFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(new MaintenanceFilter());
        reg.addUrlPatterns("/*");
        return reg;
    }

}
