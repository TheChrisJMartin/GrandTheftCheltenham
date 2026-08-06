package uk.co.cheltenham.gtc.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Applies classpath:db/migration/*.sql in name order on every WAR boot.
 * Scripts are written to be idempotent (IF NOT EXISTS / ON CONFLICT).
 * Includes V3__map_features.sql and any future V4__… files automatically.
 */
@Component
@Order(0)
public class SchemaBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaBootstrap.class);

    private final DataSource dataSource;

    public SchemaBootstrap(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:db/migration/*.sql");
            if (resources.length == 0) {
                log.warn("No SQL migrations found on classpath:db/migration/");
                return;
            }
            Arrays.sort(resources, Comparator.comparing(r -> {
                try {
                    return r.getFilename() != null ? r.getFilename() : "";
                } catch (Exception e) {
                    return "";
                }
            }));

            ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
            populator.setContinueOnError(true);
            populator.setIgnoreFailedDrops(true);
            populator.setSeparator(";");
            for (Resource r : resources) {
                log.info("Applying SQL migration: {}", r.getFilename());
                populator.addScript(r);
            }
            populator.execute(dataSource);
            log.info("SQL migrations complete ({} script(s))", resources.length);
        } catch (Exception e) {
            log.error("SQL migration bootstrap failed: {}", e.toString());
        }
    }
}
