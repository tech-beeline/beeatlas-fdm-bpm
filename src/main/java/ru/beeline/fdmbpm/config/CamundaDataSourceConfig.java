/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.config;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class CamundaDataSourceConfig {

    @Bean(name = "secondaryDataSourceProperties")
    @ConfigurationProperties("spring.datasource.camunda")
    public DataSourceProperties secondaryDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "camundaDataSource")
    @Primary
    public DataSource camundaDataSource() {
        return secondaryDataSourceProperties().initializeDataSourceBuilder().build();
    }
}
