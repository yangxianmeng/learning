package com.yxm.demo.readlisting.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author Yxm
 **/
@Component
@ConfigurationProperties(prefix = "datasource")
@Data
public class DBConfig {
    private String url;
    private String password;
    private String username;
}
