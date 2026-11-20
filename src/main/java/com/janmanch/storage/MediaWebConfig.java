package com.janmanch.storage;

import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** With local storage, serves stored files at /media/** (range requests are supported). */
@Configuration
@ConditionalOnProperty(name = "janmanch.storage.type", havingValue = "local", matchIfMissing = true)
public class MediaWebConfig implements WebMvcConfigurer {

    private final String location;

    public MediaWebConfig(@Value("${janmanch.storage.local-dir}") String localDir) {
        String uri = Paths.get(localDir).toAbsolutePath().normalize().toUri().toString();
        this.location = uri.endsWith("/") ? uri : uri + "/";
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/media/**").addResourceLocations(location);
    }
}
