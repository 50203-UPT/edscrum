package pt.up.edscrum;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Mapeia URLs que começam por "/uploads/" para a pasta física no servidor
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}
