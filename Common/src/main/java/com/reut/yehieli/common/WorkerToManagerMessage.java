package com.reut.yehieli.common;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.IOException;

import static com.reut.yehieli.common.Helper.objectMapper;

public class WorkerToManagerMessage {

    public static String serialize(WorkerToManagerMessage obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            System.exit(Helper.serializeErrorExitCode);
            return null;
        }
    }

    public static WorkerToManagerMessage deserialize(String str) {
        try {
            return objectMapper.readValue(str, WorkerToManagerMessage.class);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(Helper.deserializeErrorExitCode);
            return null;
        }
    }
    private String urlKey;
    private String bucketName;
    private String nameSqsBetweenManagerToLocalApp;
    private String summeryFilePath;
    private boolean finish;
    private String  originalUrl;
    private String operatorType;
    private int numberOfMsg;
    private boolean failedParsing;
    private String failedStringDescription;

    public String getBucketName() { return bucketName; }
    public String getNameSqsBetweenManagerToLocalApp() { return nameSqsBetweenManagerToLocalApp; }
    public String getSummeryFilePath() { return summeryFilePath; }
    public String getUrlKey() { return urlKey; }
    public boolean isFinish(){ return finish;}
    public String getOriginalUrl(){return originalUrl;}
    public int getNumberOfMsg(){return numberOfMsg;}
    public String getOperatorType(){return operatorType;}
    public String getFailedStringDescription(){return failedStringDescription;}
    public boolean getFailedParsing(){return failedParsing;}

    public void setBucketName(String bucketName) { this.bucketName = bucketName; }
    public void setNameSqsBetweenManagerToLocalApp(String nameSqs) { this.nameSqsBetweenManagerToLocalApp = nameSqs; }
    public void setSummeryFilePath(String filePath) { this.summeryFilePath = filePath; }
    public void setUrlKey(String keyName) { this.urlKey = keyName; }
    public void setFinish(boolean finish){this.finish = finish;}
    public void setOriginalUrl(String originalUrl){this.originalUrl = originalUrl;}
    public void setNumberOfMsg(int numberOfMsg){this.numberOfMsg = numberOfMsg;}
    public void setOperatorType(String operatorType){this.operatorType = operatorType;}
    public void setFailedParsing(boolean failedParsing){ this.failedParsing = failedParsing;}
    public void setFailedStringDescription(String failedStringDescription){this.failedStringDescription = failedStringDescription;}
}
