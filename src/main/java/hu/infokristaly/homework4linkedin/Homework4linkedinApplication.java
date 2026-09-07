package hu.infokristaly.homework4linkedin;

import hu.infokristaly.homework4linkedin.linkedin.LinkedInProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(LinkedInProperties.class)
public class Homework4linkedinApplication {

    public static void main(String[] args) {
        SpringApplication.run(Homework4linkedinApplication.class, args);
    }

}
