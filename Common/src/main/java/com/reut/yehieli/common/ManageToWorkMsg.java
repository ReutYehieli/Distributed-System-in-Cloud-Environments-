package com.reut.yehieli.common;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import static com.reut.yehieli.common.Helper.objectMapper;

public class ManageToWorkMsg {
    public static String serialize(ManageToWorkMsg obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            System.exit(Helper.serializeErrorExitCode);
            return null;
        }
    }

    public static ManageToWorkMsg deserialize(String str) {
        try {
            return objectMapper.readValue(str, ManageToWorkMsg.class);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(Helper.deserializeErrorExitCode);
            return null;
        }
    }

    // The location of the report html
    private String operationType;
    private String urlMsg;
    private String taskKey;
    private String bucketName;
    private String NameSqsBetweenManagerToLocalApp;
    private int numberOfMsg;
    private String summeryFilePath;  // only to pass this


    public String getOperationType(){return operationType;}
    public String getUrlMsg(){return urlMsg;}
    public String getTaskKey(){return taskKey;}
    public int getNumberOfMsg(){return numberOfMsg;}
    public String getBucketName(){return bucketName; }
    public String getNameSqsBetweenManagerToLocalApp() { return NameSqsBetweenManagerToLocalApp;}
    public String getsummeryFilePath(){return summeryFilePath;}


    public void setOperationType(String typeMsg) {this.operationType = typeMsg; }
    public void setUrlMsg(String urlMsg) {
        this.urlMsg = urlMsg;
    }
    public void setTaskKey(String taskKey) {
        this.taskKey = taskKey;
    }
    public void setNumberOfMsg(int numberOfMsg) {
        this.numberOfMsg = numberOfMsg;
    }
    public void setBucketName(String bucketName) { this.bucketName = bucketName; }
    public void setNameSqsBetweenManagerToLocalApp(String nameSqs){this.NameSqsBetweenManagerToLocalApp = nameSqs;}
    public void setSummeryFilePath(String pathFile){this.summeryFilePath = pathFile;}
}
