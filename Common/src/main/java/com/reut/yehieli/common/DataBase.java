package com.reut.yehieli.common;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DataBase {
    private static class singletonHolder {
        private static DataBase instance = new DataBase();
    }
    private static String s3BucketName = "reut-yehieli-dsps";
    private int activeWorker = 0;
    private int amountOfTask = 0;
    private HashMap<String,Integer> tasksAndNumberOfMsg = new HashMap<>();
    private HashMap<String,File> tasksAndFile = new HashMap<>();
    private HashMap<String,FileOutputStream> fileAndFileOutputStream = new HashMap<>();
    public static DataBase getInstance() {
        return singletonHolder.instance;
    }
    private S3Client s3Client =S3Client.builder().region(Region.US_EAST_1).build();
    private LinkedList<EC2Instance> workers = new LinkedList<>();
    private String typeText = "The Operator type :";
    private byte[] spaceText = "\n".getBytes();
    private String urlText = "The url is :";
    private String workerData = "#cloud-config\n" +
            "runcmd:\n" +
            "  - aws s3 sync s3://"+s3BucketName+"/assmbly/ /home/ec2-user/\n" +
            "  - java -cp '/home/ec2-user/Worker-1.0-SNAPSHOT-jar-with-dependencies.jar' 'com.reut.yehieli.Worker' \n";
    private boolean Done = false;
    public synchronized LinkedList<EC2Instance> getWorkers(){return workers;}
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Lock writeLock = readWriteLock.writeLock();
    private final Lock readLock = readWriteLock.readLock();
    private final ReadWriteLock readWriteLockForTasks = new ReentrantReadWriteLock();
    private final Lock writeLockForTasks = readWriteLock.writeLock();
    private final Lock readLockForTasks = readWriteLock.readLock();

    public void setS3BucketName(String s3BucketName){this.s3BucketName = s3BucketName;}

    // switch the state . Someone send termination Task.
    public  void isDone(){
        writeLock.lock();
        try{
            Done = true;
        }
        finally {
            // To unlock the acquired write thread
            writeLock.unlock();
        }
    }

    public synchronized boolean getStateOfDone(){
        readLock.lock();
        try{
            return Done;
        }
        finally {
            // To unlock the acquired read thread
            readLock.unlock();
        }
    }

    public void DeleteWorker(EC2Instance w){
        workers.remove(w);
        activeWorker--;
    }
    public boolean  AddMsgFromTask(String file, int numberOfMsg, S3Key s3PathResultKey, String originalUrl, String operatorType, boolean IsfailedParsing, String failedMessage) throws IOException {
        if(!tasksAndNumberOfMsg.containsKey(file)){
            System.out.println("DataBase : ManagerOfWorker :The task create for the first time");
            File summeryFile = new File(file);
            tasksAndFile.put(file,summeryFile);
            tasksAndNumberOfMsg.put(file,numberOfMsg);
            FileOutputStream summeryFileOutputStreaminitalized = new FileOutputStream(summeryFile);
            String text = "Summery File : \n";
            byte[] objectDatatext = text.getBytes();
            summeryFileOutputStreaminitalized.write(objectDatatext);
            summeryFileOutputStreaminitalized.flush();
            fileAndFileOutputStream.put(file,summeryFileOutputStreaminitalized);
        }

        FileOutputStream summeryFileOutputStream = fileAndFileOutputStream.get(file);
        summeryFileOutputStream.write((operatorType).getBytes());
        summeryFileOutputStream.flush();
        summeryFileOutputStream.write((":   ").getBytes());
        summeryFileOutputStream.flush();
        summeryFileOutputStream.write((originalUrl+"    ").getBytes());
        summeryFileOutputStream.flush();
        if(IsfailedParsing) {
            System.out.printf("DataBase : ManagerOfWorker :The output of the message has failed : %s\n type of parse: %s the urlstring %s \n ",failedMessage,operatorType,originalUrl);
            summeryFileOutputStream.write(failedMessage.getBytes());
            summeryFileOutputStream.flush();
        }
        else{
            summeryFileOutputStream.write(s3PathResultKey.toString().getBytes());
            summeryFileOutputStream.flush();
            System.out.printf("DataBase : ManagerOfWorker :The curr message succe. the  url string: %s\n opertator: %s,output in:%s \n",originalUrl,operatorType,s3PathResultKey.toString());
        }

        if(tasksAndNumberOfMsg.get(file) == 1){
            System.out.printf("DataBase : ManagerOfWorker :The s3PathResultKey : %s\n",s3PathResultKey.getKey());
            System.out.println("ManagerOfWorker : Task is finish\n");
            summeryFileOutputStream.close();
            tasksAndNumberOfMsg.remove(file);
            tasksAndFile.remove(file);
            tasksAndNumberOfMsg.remove(file);
          return true;
        }
        else{
            tasksAndNumberOfMsg.replace(file, tasksAndNumberOfMsg.get(file)-1);
            System.out.printf("DataBase : ManagerOfWorker :There are left more %d message in the path:%s \n",tasksAndNumberOfMsg.get(file),file);
            summeryFileOutputStream.write(spaceText);
            summeryFileOutputStream.flush();
            return false;
        }
    }


    public synchronized int getActiveWorker(){
        return activeWorker;
    }

    public int getAmountOfTask(){
        readLockForTasks.lock();
        try{
            return amountOfTask;
        }
        finally {
            // To unlock the acquired read thread
            readLockForTasks.unlock();
        }
    }

    public void addTask(){
        writeLockForTasks.lock();
        try{
            this.amountOfTask++;
        }
        finally {
            // To unlock the acquired write thread
            writeLockForTasks.unlock();
        }
    }

    public void deleteTask(){
        writeLockForTasks.lock();
        try{
            this.amountOfTask--;
        }
        finally {
            // To unlock the acquired write thread
            writeLockForTasks.unlock();
        }
    }


    public synchronized void addActiveWorker(int numberOfWorkerNeed) {
        while (activeWorker < 19 && numberOfWorkerNeed > 0){
            String nameType = "worker"+String.valueOf(activeWorker+1);
            System.out.println("ManagerOfWorker : create worker");
           EC2Instance worker = new EC2Instance(nameType,1,1,workerData);
            System.out.printf("The workerData is :%s",workerData);
            workers.add(worker);
            numberOfWorkerNeed--;
            activeWorker++;
        }
    }
}
