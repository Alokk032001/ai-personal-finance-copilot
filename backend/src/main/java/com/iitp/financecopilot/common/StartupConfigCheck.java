package com.iitp.financecopilot.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.util.PlaceholderResolutionException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StartupConfigCheck {

    private static final Logger log = LoggerFactory.getLogger(StartupConfigCheck.class);
    private static final List<String> PLACEHOLDERS = List.of(
            "your_project_ref",
            "your_db_password",
            "xxxxxx",
            "change_me"
    );

    private final Environment environment;

    public StartupConfigCheck(Environment environment) {
        this.environment = environment;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warnIfPlaceholdersRemain() {
        String blob = String.join(" ",
                property("POSTGRES_URL"),
                property("spring.datasource.url"),
                property("POSTGRES_USER"),
                property("spring.datasource.username"),
                property("POSTGRES_PASSWORD"),
                property("spring.datasource.password"),
                property("MONGODB_URI"),
                property("spring.data.mongodb.uri"),
                property("JWT_SECRET"),
                property("app.jwt.secret"));
        for (String token : PLACEHOLDERS) {
            if (blob.contains(token)) {
                log.warn("Configuration still contains placeholder '{}'. Copy backend/.env.example to backend/.env and replace secrets.", token);
            }
        }
    }

    private static String nz(String value) {
        return value == null ? "" : value;
    }

    /**
     * An unresolved placeholder is expected in a test profile where the related
     * auto-configuration is disabled. A configuration warning must never stop
     * an otherwise valid application context from starting.
     */
    private String property(String key) {
        try {
            return nz(environment.getProperty(key));
        } catch (PlaceholderResolutionException ignored) {
            return "";
        }
    }
}
