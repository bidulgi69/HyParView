package org.example.hyparview.configuration;

import org.example.hyparview.Snowflake;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Configuration
public class SnowflakeConfiguration {

    private final HyparViewProperties properties;

    @Autowired
    public SnowflakeConfiguration(HyparViewProperties properties) {
        this.properties = properties;
    }

    @Bean
    public Snowflake snowflake() {
        byte[] hash = Base64.getEncoder().encode(properties.getNodeId().getBytes(StandardCharsets.UTF_8));
        long idAsLong = ByteBuffer.wrap(hash).getLong();
        long mask = 0xF000000000000000L;

        long id = idAsLong & mask;
        // 1~1024 범위로 제한
        id &= 0x3FF;
        return new Snowflake(id, properties.getCustomEpoch());
    }
}
