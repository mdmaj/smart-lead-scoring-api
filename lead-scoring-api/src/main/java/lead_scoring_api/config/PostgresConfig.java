package lead_scoring_api.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

@Configuration
public class PostgresConfig {

    @Bean(name = "mysqlDataSource")
    @Primary
    public DataSource mysqlDataSource(
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password) {

        return createDataSource(
                url,
                username,
                password,
                "com.mysql.cj.jdbc.Driver"
        );
    }

    @Bean(name = "postgresDataSource")
    public DataSource postgresDataSource(
            @Value("${spring.postgres.datasource.url}") String url,
            @Value("${spring.postgres.datasource.username}") String username,
            @Value("${spring.postgres.datasource.password}") String password) {

        System.out.println("POSTGRES USER = " + username);
System.out.println("POSTGRES PASSWORD LENGTH = " + password.length());
System.out.println("POSTGRES URL = " + url);

        return createDataSource(
                url,
                username,
                password,
                "org.postgresql.Driver"
        );
    }

    @Bean(name = "postgresJdbcTemplate")
    public JdbcTemplate postgresJdbcTemplate(
            @Qualifier("postgresDataSource") DataSource dataSource) {

        return new JdbcTemplate(dataSource);
    }

    private DataSource createDataSource(
            String url,
            String username,
            String password,
            String driverClassName) {

        DriverManagerDataSource dataSource =
                new DriverManagerDataSource();

        dataSource.setDriverClassName(driverClassName);
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);

        return dataSource;
    }
}