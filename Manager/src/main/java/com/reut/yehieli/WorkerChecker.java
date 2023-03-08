package com.reut.yehieli;
import com.reut.yehieli.common.DataBase;
import com.reut.yehieli.common.EC2Instance;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.Reservation;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

public class WorkerChecker implements Runnable{

    @Override
    public void run() {
        EC2Instance ec2 = new EC2Instance();
        DataBase DB= DataBase.getInstance();
        while(!DB.getStateOfDone()) {
            try {
                TimeUnit.SECONDS.sleep(45);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            LinkedList<EC2Instance> workers = DB.getWorkers();
            System.out.println("'workerChecker : checking the workers list from the data base");
            if (!workers.isEmpty()) {
                for (EC2Instance worker : workers) {
                    System.out.println("workerChecker: found a worker in the list -check if it active");
                    DescribeInstancesRequest instancesRequest = DescribeInstancesRequest.builder().instanceIds(worker.getInstanceId()).build();
                    DescribeInstancesResponse instancesResponse = ec2.getEC2().describeInstances(instancesRequest);
                    for (Reservation reservation : instancesResponse.reservations()) {
                        for (Instance instance : reservation.instances()) {
                            if(instance.instanceId().equals(worker.getInstanceId())) {
                                String state = instance.state().name().toString();
                                if (state.equals("shutting-down") || state.equals("terminated") || state.equals("stopping") || state.equals("stopped")) {
                                    System.out.printf("workerChecker : found a relevent worker with instanceId of %s",worker.getInstanceId());
                                    DB.DeleteWorker(worker);
                                    DB.addActiveWorker(1);
                                    try {
                                        TimeUnit.SECONDS.sleep(45);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    }
}
