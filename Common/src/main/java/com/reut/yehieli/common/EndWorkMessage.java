package com.reut.yehieli.common;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import static com.reut.yehieli.common.Helper.objectMapper;

public class EndWorkMessage {

    public static String serialize(EndWorkMessage obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            System.exit(Helper.serializeErrorExitCode);
            return null;
        }
    }

    public static EndWorkMessage deserialize(String str) {
        try {
            return objectMapper.readValue(str, EndWorkMessage.class);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(Helper.deserializeErrorExitCode);
            return null;
        }
    }

    // The location of the report html
    private String outputFile;
    private String bucketName;
    private String keyPath;  // bucket+keypath = where the file in S3


    public String getOutputFile() {
        return outputFile;
    }
    public String getBucketName() {
        return bucketName;
    }
    public String getKeyPath() {
        return keyPath;
    }


    public void setOutputFile(String outputFile) {
        this.outputFile = outputFile;
    }
    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }
    public void setKeyPath(String keyPath) {
        this.keyPath = keyPath;
    }


}
