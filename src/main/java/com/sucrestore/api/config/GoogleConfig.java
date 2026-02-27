package com.sucrestore.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "google.sheets")
@Data
public class GoogleConfig {

    private String applicationName;
    private String credentialsFilePath;
    private String spreadsheetId;
    private long syncRate;
}
