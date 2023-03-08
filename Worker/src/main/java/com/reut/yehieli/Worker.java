package com.reut.yehieli;
import com.reut.yehieli.common.ManageToWorkMsg;
import com.reut.yehieli.common.S3Key;
import com.reut.yehieli.common.SQS;
import com.reut.yehieli.common.WorkerToManagerMessage;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import java.io.*;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Paths;
import java.util.*;


public class Worker {
    private static SQS sqsManagerToWorker;
    private static String sqsManagerToWorkerQueueUrl;
    private static SQS sqsWorkerToManager;
    private static String sqsWorkerToManagerQueueUrl;
    private static S3Client s3Client = S3Client.builder().region(Region.US_EAST_1).build();
    private static boolean failedParsing = false;
    private static String failedMessage = "";

    public static void downloadFile(URL url) {
        try        (InputStream in = url.openStream();
                 ReadableByteChannel rbc = Channels.newChannel(in);
                 FileOutputStream fos = new FileOutputStream("Parsefile"))

        {
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            System.out.println("finish to download");
        }
        catch (IOException ex) {
            System.out.print("Invalid Path");
        }
    }

    public static String do_pars(String input_path, String pars_value, String urlString ){
        try {
            LexicalizedParser.main(new String[]{"-sentences", "newline", "-retainTMPSubcategories", "-writeOutputFiles", "-outputFormat", get_value_pars(pars_value),"edu\\stanford\\nlp\\models\\lexparser\\englishPCFG.ser.gz", input_path });
            System.out.println("finish the pars");
            return input_path + ".stp";
        }catch (Exception e){
            System.out.println("There is a problem with parse");
             e.printStackTrace();
            failedParsing = true;
            failedMessage = "Unable to open the file\n input file: "+ urlString+" \n" +"the operator type:" + pars_value+
                    "A short description of the exception: "+ e.getMessage() + "\n";
            System.out.printf("%s\n",failedMessage);
        }
        return "";
    }

    public static String get_value_pars(String pars_type){
        if (pars_type.equals("POS"))
            return "wordsAndTags";
        else if (pars_type.equals("DEPENDENCY"))
            return "typedDependencies";
        else if (pars_type.equals("CONSTITUENCY"))
            return "penn";
        else return "the parsing type must be choose";
    }

    public static void main(String[] args) throws IOException {
        System.out.println("start worker");
        //creating the sqsManagerToWorker
        sqsManagerToWorker = new SQS("sqsManagerToWorker");
        sqsManagerToWorkerQueueUrl = sqsManagerToWorker.getUrl();
        //creating the sqsworkerToManager
        sqsWorkerToManager = new SQS("sqsWorkerToManager");
        sqsWorkerToManagerQueueUrl = sqsWorkerToManager.getUrl();

        while (true) {
            //gets a message from an sqs queue
            failedParsing = false;
            failedMessage="";
            List<Message> messages = sqsManagerToWorker.getMessages();
            if (!messages.isEmpty()) {
                for (Message message : messages) {
                    System.out.println("worker : Receiving messages from sqsManagerToWorker.\n");
                    ManageToWorkMsg msg = ManageToWorkMsg.deserialize(message.body());
                    ///Downloads the text file indicated in the message
                    String urlString =  msg.getUrlMsg();
                     URL my_URL = new URL(urlString);
                    downloadFile(my_URL);
                    ///checking the type of the file
                    String parsing_type = msg.getOperationType();
                    String pathOutputFile = do_pars("Parsefile", parsing_type, urlString);
                    File filepathOutputFile = new File(pathOutputFile);

                    //upload the resulting to S3
                    String bucketName = msg.getBucketName();
                    S3Key s3OutPutFile = new S3Key(bucketName, UUID.randomUUID().toString());
                    System.out.printf("worker : s3key id %s .\n",s3OutPutFile.getKey());
                    if(!failedParsing) {
                        System.out.printf("worker : the parser succed the msg %s .\n",msg.getTaskKey());
                        s3Client.putObject(PutObjectRequest.builder().key(s3OutPutFile.getKey()).bucket(s3OutPutFile.getBucket()).build(), Paths.get(pathOutputFile));
                    }///
                    System.out.printf("worker : there is erroe msg ?? :  %s.\n", failedMessage);
                    //create the msg to the Manager
                    filepathOutputFile.delete();
                    System.out.println("worker : delete the file.\n");
                    WorkerToManagerMessage workerToManagerMsg = new WorkerToManagerMessage();
                    workerToManagerMsg.setBucketName(bucketName);
                    workerToManagerMsg.setUrlKey(s3OutPutFile.toString());
                    workerToManagerMsg.setNameSqsBetweenManagerToLocalApp(msg.getNameSqsBetweenManagerToLocalApp());
                    workerToManagerMsg.setNumberOfMsg(msg.getNumberOfMsg());
                    workerToManagerMsg.setSummeryFilePath(msg.getsummeryFilePath());
                    workerToManagerMsg.setOperatorType(msg.getOperationType());
                    workerToManagerMsg.setOriginalUrl(msg.getUrlMsg());
                    workerToManagerMsg.setFailedParsing(failedParsing);
                    workerToManagerMsg.setFailedStringDescription(failedMessage);
                    sqsWorkerToManager.sendMessage(workerToManagerMsg);
                    System.out.printf("worker : send msg to %s.\n",sqsWorkerToManager);
                    //delete msg from the ManageToWorkMsg queue:
                    sqsManagerToWorker.deleteMessages(message);
                    System.out.printf("worker : delete msg from %s.\n",sqsWorkerToManager);
                }
            }

        }
    }


}