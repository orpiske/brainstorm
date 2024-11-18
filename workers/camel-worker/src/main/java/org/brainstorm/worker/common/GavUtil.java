package org.brainstorm.worker.common;

public class GavUtil {

    public static String group(String gav) {
        return gav.split(":")[0];
    }

    public static String artifact(String gav) {
        return gav.split(":")[1];
    }

    public static String version(String gav) {
        return gav.split(":")[2];
    }
}
