package sirius.samples.bankofsirius.endpoints;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC configuration class that aliases the version endpoint to be
 * available at the root in addition to the /actuator subdirectory.
 */
@Configuration
public class AliasVersionEndpoint implements WebMvcConfigurer {
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/version")
                .setViewName("forward:/actuator/version");
    }
}
