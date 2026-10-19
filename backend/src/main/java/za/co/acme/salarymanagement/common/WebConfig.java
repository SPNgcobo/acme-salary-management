package za.co.acme.salarymanagement.common;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

/**
 * Web-layer configuration.
 *
 * <p>{@code @EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)} tells
 * Spring Data to serialise {@code Page<T>} as a stable DTO instead of the
 * internal {@code PageImpl}, avoiding the "PageImpl serialisation is not
 * supported" warning and guaranteeing the JSON shape of the paged responses
 * will not change between Spring Data versions.
 */
@Configuration
@EnableSpringDataWebSupport(
        pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO
)
public class WebConfig {
}