package hu.infokristaly.homework4linkedin;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    OpenAPI linkedInOpenApi() {
        return new OpenAPI().info(new Info()
                .title("LinkedIn Post API")
                .version("v1")
                .description("LinkedIn OAuth 2.0 kapcsolódás után szöveges posztok publikálása."));
    }
}
