# Distributed-System-in-Cloud-Environments-
Developed a distributed system that processes a list of text files, analyzes them in various levels, upload the analyzed text to S3, and generates an output HTML file. The system is composed of a local application and multiple services running on EC2 instances. Synchronization between the master server and the workers is achieved via SQS.



Details about the work:
- AMI: ami-0f8a601bf74942e95
- InstanceID - T2_micro
- IAM_Role - arn:aws:iam::946348875709:instance-profile/LabInstanceProfile

- Instructions - Running the project - 
	1. update the credential - on the local dir /.aws/credentials
	2. upload the Manager.jar & Worker.jar in a folder inside a bucket in S3  -> "reut-yehieli-dsps" :  this buckt will use for all the instance, and as a store for the files.
	3. make the files (Manager.jar & Worker.jar) public
	4. insert the input file to the src folder- *create src folder if needed* 
	5. put the Local.jar in the same path as the src folder
	6. run the localApplication- java -jar Local.jar <inputFileName> <outputFileName> <n> <terminate>
while : 
Local.jar = C:\Users\reuty\Desktop\ReutAssTest\LocalApplication\target\LocalApplication-1.0-SNAPSHOT-jar-with-dependencies.jar
Manager.jar = C:\Users\reuty\Desktop\ReutAssTest\Manager\target\Manager-1.0-SNAPSHOT-jar-with-dependencies.jar
Worker.jar = C:\Users\reuty\Desktop\ReutAssTest\Worker\target\Worker-1.0-SNAPSHOT-jar-with-dependencies.jar

The inputFile and outputfile in path : ReutAssTest\src\main\resources

In order to be able to work in parallel and serve unlimited amount of  clients, the manager implementation
is LocalManger , and in such way we make the program more scalable.

1.Starting the local Application:
The main of Local Application checks which arguments it has :
	# inputPath
	# OutputPath
	# n -for calculate how many jobs per worker (the argument is must).
	# terminate -the argument  indicate to manger if its should stop(the argument  optional )
2. Activate Manager - > The local application checks if there is already exsist and runing manager.
    if yes  ->  it gets the URL of the already existing sqsLocalAppToManager queue .
    if not -> it creates a new instance of InatnceEc2 , with manager tag. And also create Sqs queue "LocalAppToManager".
3. Upload the input file to s3 bucket.
    we create key, and upload the input file into s3 .
4. Creating the SQS queue between the manager to local application -
    each local application get its uniq sqs queue.
5. creating "startWorkMessage" -> this message is the message between the localApp to the manager.
    its has the details of:
        # the path of inputFile in S3,
        # the number of items per worker,
        # terminate - which symbolized if it a terminate message
        # the name queue of ManagerToLocalApp
6. local application send the message to the sqs queue "LocalAppToManager".


 The manager:
1.The manager creates the following sqs queue:
    # sqsWorkerToManager
    # sqsManagerToWorker
    # create and starts 2 threads :  LocalManager and ManagerOfWorker.
	 The LocalManager thread:
		The thread of the LocalManager works in an endless loop (until it gets a termination message from the LocalAppToMangaer queue),
		and checks if there are new messages in the LocalAppToManager SQS queue.
		- The function in the LocalManager:
  		  # CheckLocalManagerMsg -  checks the sqs LocalAppToManager queue and if the queue is not empty it check the type of message.
     		 ->if the field of the termination message is true ->  its termination message.
			- Done = true : means he wont get more message from new local application.
			- check if we the workers +managerofworker finished all there tasks.
			----> if not :  The thread sleeping for 45 seconds, and than try again.
			---->else:  Doning interupt for WorkersChecker thread.
         			              Terminate al the instance of workers.
     			             delete the  sqsManagerToWorker queue.
			             delete the sqsLocalAppToManager queue.
	 			  terminate the instance of manager.

		->else  it a New Task - we get the information from the message:
					#bucket name
					#key of the inputFile from s3
					#number of items per worker
					#save the name queue of the specific LocalApp
                        - Download the inputFile from s3.
		- Creating the summery file path - it match for all the links from the same inputFile.
		- Count how many url we have in the file.
		-We create the message to the SQS ManagerToWorker. 
		explantion about the creating the message : each url in the input file enter into a different type "ManagerToWorkMsg" message .
		which contain details like:
		# the type of operation 
		#url string ,
		#summertFilePath ->this file  indecate from which inputFile the worker take the job.
		# saving the queue name of the local app.
		-Adding one to the numberof tasks.
		- Checking how many worker we need to create for the current task, and activated them by creating InstanceEc2.
		- Each worker that activated saved in a linkedList  :  "Workers".
		-If the number of task until now it 0 - means that we just now initialized the first workers.
 		so we will start a new thread Workerschecker, that will check that all the workers we need are working all the time, and start new one if not.
		- delete the inputFile from S3


Worker:
1.Each worker run until  someone Terminates his instance.
2.Each worker run in infinite loop, that check every loop if there is a new message in the sqsManagerToWorker  queue.
	if there is a new message- > we get the:  # type operation 
                                                                         #url linked. 
3.detect which parsing to do by the operation type.
4. The worker download the url link and do parsing .
5.After he finishes he loads the output to S3. 
6. create a  "WorkerToManagerMsg" message , and sent it to the SQS "WorkerToManager"  queue.


 The ManagerOfWorker thread:
1.The thread of the ManagerOfWorker works in an endless loop,
and checks if there are new messages in the WorkerToManager SQS queue.
2. if there is a message ->  its add a new line for summery file on DataBase (if the worker finish the job).
3If we finished with all the message that connected to the same "New Task". we send the file that we create to the specific local app.




LocalApplication after sending first message to the manager:
1.The app in the local Application is an endless loop.
2.Waiting for a  message.
3.If receving a msg :
- download  the summery file from S3 .
-Turn the summeryfile to html file.
 -delete the receving message from the queue.
4. if in the args was field of " terminated" :
 its sending a terminate message to sqsLocalAppToManager.
5. if finished delete the sqsManagerToLocalApp queue.


- Running Times -
	 n=1 time= 37 min
	 

- Scalability - 
	The manager contain 3 threads in order to divide his work.
	One thread -  always check the sqsLocalAppToManager queue.
            Second thread- always check thr sqsWorkerToManager queue.
	Which means that the manager always doning something.
	Third thread- is the workerChecker that create from the first thread. - and he check that if one werker is terminates he load another one instade.
	We also have the DataBase class - which is made fore the connection between the threads.
	All the details does not saved in the thread. And they are save in S3 , data base, or as a file on the local computer.
	The 2 first threads cause the manager to never wait because one thread checks the LocalsManagerSQS 
	and the other checks the WorkersManagerSQS.
	We created the DataBase class to handle the connection between the thread.
	

- Persistence - 
	worker will delete a task from the queue only after its done. If something happend and the worker couldn't finish the task, the task will go back to the queue and another worker will take it.
	In order to deal with the case thet a node stalls for a while, we updated to a longer time the visibility time out, so the worker has eonogh time to finish his job.


- Termination -
	After a local application get respone, the local require to:
	- Delete the manager's response file from S3
	- Delete his own buccket from S3

- Division of tasks - 
	I desided to limit the queues to one message at a time, in that way the tasks are shared equally among the workers.
