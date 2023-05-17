package com.reut.yehieli;

import com.reut.yehieli.common.*;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;
import java.io.*;
import java.util.*;

public class LocalApplication {

    private static String s3BucketName = "reut-yehieli-dsps";
    private static String sqsLocalApplicationToManagerQueueUrl = "https://sqs.us-east-1.amazonaws.com/946348875709/LocalApplicationToManagerQueueUrl";
    private static String sqsManagerToLocalApplicationQueueUrl = "https://sqs.us-east-1.amazonaws.com/946348875709/ManagerToLocalApplication";
    private static String sqsManagerToLocalApplicationQueueUrlTmp ;
    private static SQS sqsLocalAppToManager;
    private static boolean terminate = false;
    private static Ec2Client ec2Client = Ec2Client.builder().region(Region.US_EAST_1).build();
    private static S3Client s3Client = S3Client.builder().region(Region.US_EAST_1).build();
    private static SqsClient sqsClient = SqsClient.builder().region(Region.US_EAST_1).build();
    private static String  inputFile;
    private static String  outputFile;
    private static DataBase dataBase = DataBase.getInstance();
    private static SQS sqsManagerToLocalApp;
    private static String ManagerData = "#cloud-config\n" +
            "runcmd:\n" +
            "  - aws s3 sync s3://"+s3BucketName+"/assmbly/ /home/ec2-user/\n" +
            "  - java -cp '/home/ec2-user/Manager-1.0-SNAPSHOT-jar-with-dependencies.jar' 'com.reut.yehieli.Manager' \n";



    private static boolean checkExistsManager() {
        try {
            DescribeInstancesRequest req = DescribeInstancesRequest.builder().build();
            DescribeInstancesResponse res = ec2Client.describeInstances(req);
            for (Reservation reservation : res.reservations()) {
                for (Instance instance : reservation.instances()) {
                    System.out.printf("LocalApp: The instance is: %s",instance.tags());
                    String name = instance.tags().get(0).value();
                    String state = instance.state().name().toString();
                    if (name.equals("Manager") && (state.equals("running") || state.equals("pending")))
                        return true;
                }
            }
            return false;
        } catch (Ec2Exception e) {
            System.out.println("Problem in function: CheckExistsManager");
            System.out.println(e.awsErrorDetails().errorMessage());
            return false;
        }
    }

    private static void  managerActive(){
        boolean foundManager = checkExistsManager();
        if(!foundManager){
            sqsLocalApplicationToManagerQueueUrl = sqsLocalAppToManager.creatQueue();
            System.out.println("LocalApp: The manager is not found");
            new EC2Instance("Manager",1,1,ManagerData);
        }
        else{
            sqsLocalApplicationToManagerQueueUrl = sqsLocalAppToManager.getUrl();
        }
    }

    private static void  sendTerminateMsg(){
        StartWorkMessage terminateMessage = new StartWorkMessage();
        terminateMessage.setTerminate(true);
        sqsLocalAppToManager.sendMessage(terminateMessage);
        System.out.println("LocalApp: sending terminate msg to sqsManagerToLocalApplicationQueueUrlTmp" );
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        // first it reads  the input file from the user:
        if(args.length < 3 || args.length > 4) {
            printUsage();
        }

         inputFile = args[0];
         outputFile = args[1];
        int itemsPerWorker = Integer.parseInt(args[2]);
        if (args.length == 4) {
            terminate = args[3].equalsIgnoreCase("--terminate");
        }
        System.out.println("***LocalApp: We read the args ***");

        //creating the sqs of the localApp -> Manager
         sqsLocalAppToManager = new SQS("sqsLocalAppToManager");
        managerActive();

        // Generate random UUID because file names doesn't matter we don't want to override files from
        // other instances of Local Application.
        S3Key s3InputFileName = new S3Key(s3BucketName, UUID.randomUUID().toString());

        //upload the inputFile to s3
        s3Client.putObject(PutObjectRequest.builder().key(s3InputFileName.getKey()).bucket(s3InputFileName.getBucket()).build(),
                new java.io.File(inputFile).toPath());

        System.out.println("LocalApp:The path of inputFile is in S3");

        StartWorkMessage startWorkMessage = new StartWorkMessage();
        //  Creating a msg for the location where we uploaded the received input file
        startWorkMessage.setInputFile(s3InputFileName.toString());
        startWorkMessage.setItemsPerWorker(itemsPerWorker);
        startWorkMessage.setBucketName(s3BucketName);
        startWorkMessage.setTerminate(false);

         // Create a queue that will connect between Manager to *this* local app and save the url
        System.out.println("LocalApp:Creating a new SQS queue called MyQueue.\n");
        String sqsManagerToLocalAppName = "sqsManagerToLocalApp" + new Date().getTime();
        sqsManagerToLocalApp = new SQS(sqsManagerToLocalAppName);
        sqsManagerToLocalApplicationQueueUrlTmp = sqsManagerToLocalApp.creatQueue();

        startWorkMessage.setNameManagerToLocal(sqsManagerToLocalAppName);
        sqsLocalAppToManager.sendMessage(startWorkMessage);
        System.out.println("LocalApp : sending a msg to sqsLocalApplicationToManagerQueueUrl" );
        boolean gotResponse = false;
        while (!gotResponse) {
            List<Message> messages= sqsManagerToLocalApp.getMessages();
            if (!messages.isEmpty()) {
                System.out.printf("LocalApp :receving a msg to %s /n",sqsManagerToLocalApplicationQueueUrlTmp );
                for (Message endMessage : messages) {
                    EndWorkMessage endWorkMessage = EndWorkMessage.deserialize(endMessage.body());
                     S3Key outputS3Key = S3Key.valueOf(endWorkMessage.getKeyPath());

                    // Download from S3
                    String summaryFpath= System.getProperty("user.dir")+"/src/summaryFiles.txt"; // it still not the final output
                    File summaryF= new File(summaryFpath);
                    GetObjectRequest request = GetObjectRequest.builder().key(outputS3Key.getKey()).bucket(outputS3Key.getBucket()).build();
                    ResponseBytes<GetObjectResponse> responseBytes = s3Client.getObjectAsBytes(request);
                    System.out.printf("LocalApp : getting a the inputFile from S3 to file:%s\n",summaryFpath);
                    byte [] objectData =  responseBytes.asByteArray();
                    OutputStream outputStream= new FileOutputStream(summaryF);
                    outputStream.write(objectData);
                    outputStream.flush();
                    outputStream.close();

                    System.out.printf("LocalApp: we download the summery file from S3. bucket: %s key:%s \n",outputS3Key.getBucket(),outputS3Key.getKey());
                    sqsClient.deleteMessage(DeleteMessageRequest.builder().queueUrl(sqsManagerToLocalApplicationQueueUrlTmp)
                            .receiptHandle(endMessage.receiptHandle()).build());
                    System.out.printf("LocalApp: delete the summery file message from the queue : %s\n",sqsManagerToLocalApplicationQueueUrlTmp);
                    File outputFileFile = new File(outputFile);
                    createHtmlFile(summaryFpath, outputFileFile);
                    summaryF.delete();
                    s3Client.deleteObject(DeleteObjectRequest.builder().bucket(outputS3Key.getBucket()).key(outputS3Key.getKey()).build());
                    System.out.printf("LocalApp : delete the outputFile from S3. key:%s\n",outputS3Key.getKey());
                    // if in the args were terminate we need to send terminate msg
                    if(terminate){
                        sendTerminateMsg();
                        gotResponse = true;
                    }
                    sqsManagerToLocalApp.deleteSQS();
                    System.out.printf("delete the queue %s\n",sqsLocalAppToManager);

                }
            }
        }
    }
/*
    #cloud-config
    runcmd:
            - aws s3 cp s3://bucket/key.jar .
            - java -jar key.jar arguments
*/
public static void createHtmlFile(String inputPath, File html_file) throws IOException {
    Scanner scan = new Scanner(new File(inputPath));
    BufferedWriter writer = new BufferedWriter(new FileWriter(html_file));
    writer.write("<html><body>");
    while (scan.hasNext()){
        writer.write(scan.nextLine());
        writer.write("<br/>");
    }
    writer.write("</html><body>");
    scan.close();
    writer.close();
    }


    private static void printUsage() {
        System.out.println("Usage: LocalApplication input-file output-file items-per-worker [--terminate]");
        System.exit(Helper.wrongUsageExitCode);
    }
}
