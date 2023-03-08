package com.reut.yehieli;

import com.reut.yehieli.common.*;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.sqs.model.*;
import java.io.*;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class LocalManager implements Runnable{
    private static SQS sqsLocalAppToManager;
    private static SQS sqsManagerToWorker;
    private boolean Done = false;
    private S3Client s3Client =S3Client.builder().region(Region.US_EAST_1).build();
    private DataBase dataBase = DataBase.getInstance();
    private int numberOfTask = 0;
    private Thread Workerschecker;


    private void Terminate() throws  InterruptedException{
        // Does not accept anymore input files from localapplication
        Done = true;
        System.out.println("LocalManager : getting a terminal task");
        while(dataBase.getAmountOfTask() > 0){
            try{
                TimeUnit.SECONDS.sleep(45);
            } catch (InterruptedException e) {
                e.printStackTrace();}
        }
        dataBase.isDone();
        System.out.println("LocalManager : All the tasks are finished");
        Workerschecker.interrupt();
        System.out.println("LocalManager : closing the workerChecker thread");
        System.out.println("clear all the workers instance");
        List<EC2Instance> workers = dataBase.getWorkers();
            for (EC2Instance worker : workers) {
                worker.Terminate();
        }
        // delete all the sqs
        //To/from manager
        System.out.printf("LocalManager : delete the queue %s\n",sqsManagerToWorker);
        sqsManagerToWorker.deleteSQS();
        sqsLocalAppToManager.deleteSQS();
        EC2Instance ec2 = new EC2Instance();
        try {
            DescribeInstancesRequest req = DescribeInstancesRequest.builder().build();
            DescribeInstancesResponse res = ec2.getEC2().describeInstances(req);
            for (Reservation reservation : res.reservations()) {
                for (Instance instance : reservation.instances()) {
                    String name=instance.tags().get(0).value();
                    String state=instance.state().name().toString();
                    if(name.equals("Manager")&&(state.equals("running")||state.equals("pending"))) {
                        ec2.Terminate(instance.instanceId());
                    }
                }
            }
        } catch (Ec2Exception e) {
            System.out.println("Problem in function: TerminateManager");
            System.out.println(e.awsErrorDetails().errorMessage());
        }
        System.out.println("LocalManger : delete the Manager instance ");
    }


    private int getNumberOfWorker(int urlCount, int n){
        return ((int) (Math.ceil((double) urlCount / n)-dataBase.getActiveWorker()));
    }

    private void CreateSqsMsg(File InputFile, String task_key, String bucketName ,String nameQueueBetweenManagerToLocalApp, String summeryFilePath, int numberOfUrl){
        try{
            int count = 1;
            BufferedReader reader = new BufferedReader(new FileReader(InputFile));
            String line = reader.readLine();
            while(line != null && line.length() > 1 ){
                String [] msgBody = line.split("\t");
                ManageToWorkMsg msg = new ManageToWorkMsg();
                msg.setOperationType(msgBody[0]);
                msg.setUrlMsg(msgBody[1]);
                msg.setNumberOfMsg(numberOfUrl);
                msg.setTaskKey(task_key);
                msg.setBucketName(bucketName);
                msg.setSummeryFilePath(summeryFilePath);
                msg.setNameSqsBetweenManagerToLocalApp(nameQueueBetweenManagerToLocalApp);
                sqsManagerToWorker.sendMessage(msg);
                System.out.printf("LocalManager : send the %d message from the current task\n", count);
                count++;
                line = reader.readLine();
            }
            reader.close();
        }
        catch (IOException e){
            System.out.println("LocalManager : Problem in function: CreateSqsMsg");
            e.printStackTrace();
        }
    }

    public int numberOfMessage(File InputFile) throws IOException {

        int countUrl = 0;
        BufferedReader reader = new BufferedReader(new FileReader(InputFile));
        String line = reader.readLine();
        while(line != null && line.length() > 1) {
            countUrl++;
            line = reader.readLine();
        }
        reader.close();
        return countUrl;
    }


    public String CreatingSummeryFile(String taskName){
        String pathSummaryFile = System.getProperty("user.dir")+"/"+taskName+".txt";
        return pathSummaryFile;
    }

    private void NewTask(StartWorkMessage message) throws IOException {
        System.out.println("LocalManger : getting a new task");
        if (!Done) {

            String TaskName = "Task" + new Date().getTime();
            S3Key msgS3Key = S3Key.valueOf(message.getInputFile());
           // dataBase.setS3BucketName(msgS3Key.getBucket());
            int n = message.getItemsPerWorker();
            String nameQueueBetweenManagerToLocalApp = message.getNameManagerToLocal();  //so the second thread manager will know which local manager send it to him

            //Downloads the input file from S3.
            String path = System.getProperty("user.dir")+"/"+TaskName+".txt";
            File InputFile = new File(path);

            GetObjectRequest request = GetObjectRequest.builder().key(msgS3Key.getKey()).bucket(msgS3Key.getBucket()).build();
            ResponseBytes<GetObjectResponse> responseBytes = s3Client.getObjectAsBytes(request);
            System.out.printf("LocalManger : getting a the inputFile from S3 to file:%s\n",path);
            byte [] objectData =  responseBytes.asByteArray();
            OutputStream outputStream= new FileOutputStream(InputFile);
            outputStream.write(objectData);
            outputStream.flush();
            outputStream.close();

            //creating summery file and add it to the object
           String summeryFilePath =  CreatingSummeryFile(TaskName+"SummaryFile");

            //calculate the number of the url + sending the msg to the relevant queue
            int numberOfUrl= numberOfMessage(InputFile);
            CreateSqsMsg(InputFile, TaskName, msgS3Key.getBucket(), nameQueueBetweenManagerToLocalApp, summeryFilePath, numberOfUrl);
            dataBase.addTask();  // task was added.
            System.out.printf("LocalManger : create the object msg -> the msg to send to worker\n");
            int numberOfWorkerNeed = getNumberOfWorker(numberOfUrl, n);
            dataBase.addActiveWorker(numberOfWorkerNeed); // the worker is start to run
            System.out.printf("LocalManger : activate the %d of workers\n",numberOfWorkerNeed);
            if(numberOfTask == 0){
                numberOfTask++;
                Workerschecker.start();
                System.out.printf("LocalManger : We apply the workerchecker\n");
            }
            //delete the file from S3
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(msgS3Key.getBucket()).key(msgS3Key.getKey()).build());
            System.out.printf("LocalManger : delete the inputFile from S3. key:%s\n",msgS3Key.getKey());
            InputFile.delete();
            System.out.printf("LocalManger : delete the inputFile.path of the file: %s\n",path);
        }
    }

    public void CheckLocalManagerMsg()throws InterruptedException, IOException {

        try {
            List<Message> messages = sqsLocalAppToManager.getMessages();
            if (!messages.isEmpty()) {
                for (Message message : messages) {
                    System.out.println("LocalManger : Receiving messages from sqsLocalApplicationToManager.\n");
                    StartWorkMessage msg = StartWorkMessage.deserialize(message.body());
                    if (!msg.isTerminate()) {
                        NewTask(msg);
                        sqsLocalAppToManager.deleteMessages(message);
                        System.out.println("LocalManger : delete the last msg from sqsLocalAppToManager");
                    } else {
                        Terminate();
                    }
                    //delete the msg from the sqs
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();

        }
    }

    public void run() {
        try{//sqsManagerToWorkerQueueUrl
            System.out.println("LocalManger : start the local manager ");
            sqsLocalAppToManager = new SQS("sqsLocalAppToManager",2000);
             sqsLocalAppToManager.getUrl();
            System.out.println("LocalManger : creating the sqsLocalAppToManager ");
            //creating the sqsManagerToWorker queue
            sqsManagerToWorker = new SQS("sqsManagerToWorker",2000);
            sqsManagerToWorker.getUrl();
            System.out.println("LocalManger : creating the sqsManagerToWorker ");
            // create the thread that check that all the workers are working
            WorkerChecker workCheckerClass = new WorkerChecker();
            Workerschecker = new Thread(workCheckerClass);
            while(!Done) {
                CheckLocalManagerMsg();
            }
            }catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
    }
}
