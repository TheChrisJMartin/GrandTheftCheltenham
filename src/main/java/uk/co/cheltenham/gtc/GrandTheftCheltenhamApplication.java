package uk.co.cheltenham.gtc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class GrandTheftCheltenhamApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(GrandTheftCheltenhamApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(GrandTheftCheltenhamApplication.class, args);
    }
}
