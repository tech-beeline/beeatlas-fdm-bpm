package ru.beeline.fdmbpm.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.sql.DataSource;


@Configuration
@EnableJpaRepositories(
        basePackages = "ru.beeline.fdmbpm.gitrepository",
        entityManagerFactoryRef = "gitEntityManagerFactory",
        transactionManagerRef = "gitTransactionManager"
)
public class GitDataSourceConfig {

    @Bean(name = "gitDataSourceProperties")
    @ConfigurationProperties("spring.datasource.git")
    public DataSourceProperties gitDataSourceProperties() {
        return new DataSourceProperties();
    }


    @Bean(name = "gitDataSource")
    @ConfigurationProperties("spring.datasource.git.hikari")
    public DataSource gitDataSource() {
        return gitDataSourceProperties()
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean(name = "gitEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean gitEntityManagerFactory() {
        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(gitDataSource());
        factory.setPackagesToScan("ru.beeline.fdmbpm.gitdomain");
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        return factory;
    }

    @Bean(name = "gitTransactionManager")
    public JpaTransactionManager gitTransactionManager() {
        return new JpaTransactionManager(gitEntityManagerFactory().getObject());
    }

}
