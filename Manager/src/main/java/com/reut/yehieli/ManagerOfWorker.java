package com.reut.yehieli;
import com.reut.yehieli.common.*;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

public class ManagerOfWorker  implements Runnable {
    //this thread only take the msg from the worker and create outputfile that will be send to the local app
    private SqsClient sqsClient = SqsClient.builder().region(Region.US_EAST_1).build();
    private S3Client s3Client =S3Client.builder().region(Region.US_EAST_1).build();
    private DataBase dataBase = DataBase.getInstance();
    private static SQS sqsWorkerToManager;


public void sendSummeryToLocalApp(String summeryFilePath, String bucketName, String sqsManagerToLocalApp){
// first we want to load the file to s3
    S3Key s3SummeryFile = new S3Key(bucketName, UUID.randomUUID().toString());
    s3Client.putObject(PutObjectRequest.builder().key(s3SummeryFile.getKey()).bucket(s3SummeryFile.getBucket()).build(), Paths.get(summeryFilePath));

 // creating the object message
    EndWorkMessage endWorkMessage =  new EndWorkMessage();
    endWorkMessage.setBucketName(bucketName);
    endWorkMessage.setKeyPath(s3SummeryFile.toString());
   SQS LocalAppQueue = new SQS(sqsManagerToLocalApp);
    LocalAppQueue.getUrl();
    LocalAppQueue.sendMessage(endWorkMessage);
    System.out.println("MannagerOfWorker : sending a msg to sqsUrlManagerToLocalApp" );
}

public void checkWorkersManagerSqs() throws IOException {
    // want to check if there are msg in the sqs
    List<Message> messages = sqsWorkerToManager.getMessages();
    if (!messages.isEmpty()) {
        // want to take the msg and take the details from the text msg->to object
        for (Message message : messages) {  // to delete msg
            WorkerToManagerMessage msg = WorkerToManagerMessage.deserialize(message.body());
            S3Key msgS3Key = S3Key.valueOf(msg.getUrlKey());
            String summeryFilePath = msg.getSummeryFilePath();
            String sqsUrlManagerToLocalApp =  msg.getNameSqsBetweenManagerToLocalApp();
            int numberOfMsg = msg.getNumberOfMsg();
            String bucketName = msg.getBucketName();
            String originalUrl = msg.getOriginalUrl();
            String operatorType = msg.getOperatorType();
            boolean IsfailedParsing = msg.getFailedParsing();
            String failedMessage = msg.getFailedStringDescription();
           boolean finishTheTask =  dataBase.AddMsgFromTask(summeryFilePath, numberOfMsg, msgS3Key, originalUrl, operatorType, IsfailedParsing, failedMessage);
            System.out.printf("MannagerOfWorker : Saving the work of the worker in %s path\n",summeryFilePath );
           if(finishTheTask == true){
               //we finish our task --> need to send a msg to the local app.
               System.out.printf("MannagerOfWorker : We get true on finish the task . number n data base: %d\n",dataBase.getAmountOfTask() );
               sendSummeryToLocalApp(summeryFilePath, bucketName, sqsUrlManagerToLocalApp);
               dataBase.deleteTask();
           }
            // delete the msg from the sqs
            sqsWorkerToManager.deleteMessages(message);
        }
    }
    }

    public void run() {
        sqsWorkerToManager = new SQS("sqsWorkerToManager");
         sqsWorkerToManager.getUrl();
            while (!dataBase.getStateOfDone()) {
                 try {
                    checkWorkersManagerSqs();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        sqsWorkerToManager.deleteSQS();
        System.out.printf("delete the queue %s\n",sqsWorkerToManager);
    }
}

