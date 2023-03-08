package com.reut.yehieli.common;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import static com.reut.yehieli.common.Helper.objectMapper;
public class StartWorkMessage {

    public static String serialize(StartWorkMessage obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            System.exit(Helper.serializeErrorExitCode);
            return null;
        }
    }

    public static StartWorkMessage deserialize(String str) {
        try {
            return objectMapper.readValue(str, StartWorkMessage.class);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(Helper.deserializeErrorExitCode);
            return null;
        }
    }

    private String inputFile;
    private int itemsPerWorker;
    private boolean terminate;
    private String nameQueueManagerToLocal;
    private String bucketName;

    public String getInputFile() {
        return inputFile;
    }
    public void setInputFile(String inputFile) {
        this.inputFile = inputFile;
    }
    public void setBucketName(String bucketName){this.bucketName = bucketName;}
    public String getBucketName(){return bucketName;}
    public int getItemsPerWorker() {
        return itemsPerWorker;
    }
    public void setItemsPerWorker(int itemsPerWorker) {
        this.itemsPerWorker = itemsPerWorker;
    }
    public String getNameManagerToLocal(){return nameQueueManagerToLocal;}
    public void setNameManagerToLocal(String nameQueue){this.nameQueueManagerToLocal = nameQueue;}
    public boolean isTerminate() {
        return terminate;
    }
    public void setTerminate(boolean terminate) {
        this.terminate = terminate;
    }
}

