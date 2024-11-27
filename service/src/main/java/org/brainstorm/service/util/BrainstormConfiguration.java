package org.brainstorm.service.util;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "brainstorm")
public interface BrainstormConfiguration {


    String bootstrapHost();

    @WithDefault("9092")
    int bootstrapPort();

    Worker worker();

    interface Worker {

        interface Acquisition {

            String path();
        }

        interface Runner {
            String path();
        }

        Acquisition acquisition();

        Runner runner();
    }
}
