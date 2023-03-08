package com.reut.yehieli.common;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;
import java.util.*;

public class SQS {
    private final String nameQueue;
    private SqsClient sqsClient;
    private String url;
    private Region region;
    private int visibilityTimeout;


    public SQS(String nameQueue, int visibilityTimeout) {
        this.nameQueue = nameQueue;
        this.visibilityTimeout = visibilityTimeout;
        this.region = Region.US_EAST_1;
        this.sqsClient= SqsClient.builder().region(region).build();
    }

    public SQS(String nameQueue) {
        this.nameQueue = nameQueue;
        this.visibilityTimeout = 1000;
        this.region = Region.US_EAST_1;
        this.sqsClient= SqsClient.builder().region(region).build();
    }

    // creating a queue and return the url string
    public String creatQueue(){
        System.out.println("Creating a new SQS queue called MyQueue.\n");
        Map<QueueAttributeName,String> att = new HashMap<>();
        att.put(QueueAttributeName.VISIBILITY_TIMEOUT,"60");
        String queueName = nameQueue;
        CreateQueueRequest qRequest= CreateQueueRequest.builder().queueName(queueName).attributes(att).build();
        sqsClient.createQueue(qRequest);
        GetQueueUrlRequest req= GetQueueUrlRequest.builder().queueName(queueName).build();
       this.url = sqsClient.getQueueUrl(req).queueUrl();
       return url;
    }

    public String getUrl() {
    if(url==null){
        GetQueueUrlRequest req= GetQueueUrlRequest.builder().queueName(nameQueue).build();
        url = sqsClient.getQueueUrl(req).queueUrl();
    }
        return url;
}
// send msg type of startWorkMessage
    public void sendMessage(StartWorkMessage startWorkMessage){
        try {
            sqsClient.sendMessage(SendMessageRequest.builder().queueUrl(url)
                    .messageBody(StartWorkMessage.serialize(startWorkMessage))
                    .build());
            System.out.printf("sending a msg to %s/n",nameQueue);

        } catch (QueueNameExistsException e) {
            System.err.println(Arrays.toString(e.getStackTrace()));
            throw e;
        }
    }

    // send msg type of endWorkMessage
    public void sendMessage(EndWorkMessage endWorkMessage){
        try {
            sqsClient.sendMessage(SendMessageRequest.builder().queueUrl(url)
                    .messageBody(EndWorkMessage.serialize(endWorkMessage))
                    .build());
            System.out.printf("sending a msg to %s",nameQueue);

        } catch (QueueNameExistsException e) {
            System.err.println(Arrays.toString(e.getStackTrace()));
            throw e;
        }
    }

    // send msg type of manageToWorkMsg
    public void sendMessage(ManageToWorkMsg manageToWorkMsg){
        try {
            sqsClient.sendMessage(SendMessageRequest.builder().queueUrl(url)
                    .messageBody(ManageToWorkMsg.serialize(manageToWorkMsg))
                    .build());
            System.out.printf("sending a msg to %s",nameQueue);

        } catch (QueueNameExistsException e) {
            System.err.println(Arrays.toString(e.getStackTrace()));
            throw e;
        }
    }

    // send msg type of workerToManagerMessage
    public void sendMessage(WorkerToManagerMessage workerToManagerMessage){
        try {
            sqsClient.sendMessage(SendMessageRequest.builder().queueUrl(url)
                    .messageBody(WorkerToManagerMessage.serialize(workerToManagerMessage))
                    .delaySeconds(5)
                    .build());
            System.out.printf("sending a msg to %s\n",nameQueue);

        } catch (QueueNameExistsException e) {
            System.err.println(Arrays.toString(e.getStackTrace()));
            throw e;
        }
    }

    public List<Message> getMessages(){
        ReceiveMessageResponse messages = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                .queueUrl(url).maxNumberOfMessages(1).visibilityTimeout(visibilityTimeout).build());
        return messages.messages();
    }

    public void deleteMessages(Message message){
        DeleteMessageRequest deleteMessageRequest = DeleteMessageRequest.builder()
                .queueUrl(url)
                .receiptHandle(message.receiptHandle())
                .build();
        sqsClient.deleteMessage(deleteMessageRequest);
        System.out.printf("Delete the current msg from the sqs :%s\n",nameQueue);
    }
    public void deleteSQS(){
        if (url!=null) {
            System.out.printf("Delete the queue : %s\n",nameQueue);
            DeleteQueueRequest deleteQueueRequest = DeleteQueueRequest.builder().queueUrl(url).build();
            sqsClient.deleteQueue(deleteQueueRequest);
        }
    }
}
