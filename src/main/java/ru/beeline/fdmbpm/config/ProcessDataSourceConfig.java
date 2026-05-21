/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.sql.DataSource;

@Configuration
@EnableJpaRepositories(
        basePackages = "ru.beeline.fdmbpm.repository.camunda",
        entityManagerFactoryRef = "processesEntityManagerFactory",
        transactionManagerRef = "processesTransactionManager"
)
public class ProcessDataSourceConfig {

    @Bean(name = "processesDataSourceProperties")
    @ConfigurationProperties("spring.datasource.processes")
    public DataSourceProperties processesDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "processesDataSource")
    @ConfigurationProperties("spring.datasource.processes.hikari")
    public DataSource processesDataSource() {
        return processesDataSourceProperties()
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean(name = "processesEntityManagerFactory")
    @Primary
    public LocalContainerEntityManagerFactoryBean processesEntityManagerFactory() {
        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(processesDataSource());
        factory.setPackagesToScan("ru.beeline.fdmbpm.domain");
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        return factory;
    }

    @Bean(name = "processesTransactionManager")
    @Primary
    public JpaTransactionManager processesTransactionManager() {
        return new JpaTransactionManager(processesEntityManagerFactory().getObject());
    }

}