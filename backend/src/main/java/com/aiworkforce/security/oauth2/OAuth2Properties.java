package com.aiworkforce.security.oauth2;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.security.oauth2")
@Getter
@Setter
public class OAuth2Properties {

    private String successRedirectUri = "http://localhost:3000/auth/oauth2/callback";
    private String failureRedirectUri = "http://localhost:3000/login?oauth2=failed";
}