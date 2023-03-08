package com.reut.yehieli;
import com.reut.yehieli.common.SQS;

public class Manager {
    private static Thread ThreadForLocalManager;
    private static Thread ThreadForManagerOfWorker;


    public static void main(String[] args) throws InterruptedException {
        System.out.println("start- creating manager");
        //creating the sqsWorkerToManager
        SQS sqsWorkerToManager = new SQS("sqsWorkerToManager",2000);
        sqsWorkerToManager.creatQueue();

        //creating the sqsManagerToWorker
       SQS sqsManagerToWorker = new SQS("sqsManagerToWorker",2000);
        sqsManagerToWorker.creatQueue();

        LocalManager local_manager =  new LocalManager();
        System.out.println("start- creating local_manager");
        ManagerOfWorker manager_of_workers =  new ManagerOfWorker();
        System.out.println("start- creating manager_of_workers ");

        ThreadForLocalManager = new Thread(local_manager);
        ThreadForManagerOfWorker = new Thread(manager_of_workers);
        ThreadForLocalManager.start();
        ThreadForManagerOfWorker.start();

     try{
         ThreadForLocalManager.join();
        }
     catch(InterruptedException e){
         e.printStackTrace();
     }
        try{
            ThreadForManagerOfWorker.join();
        }
        catch(InterruptedException e){
            e.printStackTrace();
        }
    }
    }

