package com.reut.yehieli.common;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Helper {
    static ObjectMapper objectMapper = new ObjectMapper();

    public static int wrongUsageExitCode = 1;
    public static int serializeErrorExitCode = 2;
    public static int deserializeErrorExitCode = 3;
    public static int wrongS3KeyExitCode = 4;
}
