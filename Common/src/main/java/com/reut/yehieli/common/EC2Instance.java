package com.reut.yehieli.common;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;
import java.util.Base64;

public class EC2Instance {
    private boolean terminate = false;
    private Ec2Client EC2 = Ec2Client.builder().region(Region.US_EAST_1).build();
    private String name;
    private String AMI="ami-0f8a601bf74942e95";
    private Tag tag;
    private String instanceId;
    public EC2Instance(){
        EC2= Ec2Client.builder().region(Region.US_EAST_1).build();
    }
    public EC2Instance (String Name, int Min, int Max, String Data){
        name=Name;

        IamInstanceProfileSpecification IAM_role = IamInstanceProfileSpecification.builder()
                .arn("arn:aws:iam::946348875709:instance-profile/LabInstanceProfile").build();
        RunInstancesRequest runRequest = RunInstancesRequest.builder()
                .imageId(AMI)
                .instanceType(InstanceType.T2_MICRO).iamInstanceProfile(IAM_role)
                .maxCount(1)
                .minCount(1)
                .securityGroupIds("sg-047c03574a9f9bc2f")
                .userData(Base64.getEncoder().encodeToString(Data.getBytes()))
                .build();

        RunInstancesResponse response = EC2.runInstances(runRequest);
        instanceId = response.instances().get(0).instanceId();
        tag = Tag.builder()
                .key("Name")
                .value(name)
                .build();

        CreateTagsRequest tagRequest = CreateTagsRequest.builder()
                .resources(instanceId)
                .tags(tag)
                .build();
        try {
            EC2.createTags(tagRequest);
            System.out.printf("Successfully started EC2 instance %s based on AMI %s",instanceId, AMI);
        }
        catch (Ec2Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);}
        System.out.println("Done!");
    }



    public String getInstanceId() {
        return instanceId;
    }

    public Ec2Client getEC2() {
        return EC2;
    }

    public void Terminate() {
        TerminateInstancesRequest TerminateRequest = TerminateInstancesRequest.builder().instanceIds(instanceId).build();
        EC2.terminateInstances(TerminateRequest);
    }

    public void Terminate(String instanceId){
        try {
            TerminateInstancesRequest TerminateRequest = TerminateInstancesRequest.builder().instanceIds(instanceId).build();
            EC2.terminateInstances(TerminateRequest);
        }     catch (Ec2Exception e){
            System.out.println("Unable to terminate the instance:" +instanceId+ " , name:"+name+"\n");
            System.out.println(e.awsErrorDetails().errorMessage());
        }
    }

}
