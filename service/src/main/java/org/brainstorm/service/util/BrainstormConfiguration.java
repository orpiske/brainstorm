package org.brainstorm.service.util;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "brainstorm")
public interface BrainstormConfiguration {


    String bootstrapHost();

    Data data();

    Cache cache();

    interface Data {
        String path();
    }

    interface Cache {
        String path();
    }

    @WithDefault("9092")
    int bootstrapPort();
}
