package com.roomify.shared.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class EmailTemplateLoader {

    private final Map<String, String> templateCache = new ConcurrentHashMap<>();

    public String load(String templatePath, Map<String, String> variables) {
        String template = templateCache.computeIfAbsent(templatePath, path -> {
            try {
                return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
            } catch (IOException e) {
                log.error("Failed to load email template: {}", path);
                throw new IllegalStateException("Email template not found: " + path, e);
            }
        });
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            template = template.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return template;
    }
}
