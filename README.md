# CS643 Cloud Computing – Project 1 README

## Project Overview
This project implements a distributed image-processing pipeline on AWS using two Amazon EC2 instances, Amazon S3, Amazon SQS, and Amazon Rekognition.

- **Instance A** acts as the producer.
  - Reads images from the public S3 bucket `cs643-njit-project1`
  - Uses Rekognition label detection to check whether an image contains a car
  - Sends the image index to SQS if a car is detected with confidence greater than 80%
  - Sends `-1` to SQS when all images have been processed

- **Instance B** acts as the consumer.
  - Reads image indexes from SQS
  - Uses Rekognition text detection on those images
  - Writes the image name and detected text to `output.txt` if text is found
  - Stops when it receives the `-1` termination message

---

## AWS Services Used
- Amazon EC2
- Amazon S3
- Amazon SQS
- Amazon Rekognition

---

## Step 1: Start AWS Learner Lab
1. Log in to AWS Academy.
2. Open **Modules**.
3. Open **Launch AWS Academy Learner Lab**.
4. Click **Start Lab**.
5. Click **AWS** to launch **AWS Console**.

---

## Step 2: Create the SQS Queue (FIFO)

1. In AWS Console, search for **SQS**.
2. Open **Simple Queue Service**.
3. Click **Create queue**.
4. Select **FIFO Queue**.
5. Enter queue name:
   - `project1-queue.fifo` *(must end with `.fifo`)*
6. Enable:
   - ✔ Content-based deduplication (recommended)
7. Keep other settings as default.
8. Click **Create queue**.
9. Open the queue and copy the **Queue URL**.
   - This URL must be used in both Java programs (EC2-A and EC2-B).

---

## Step 3: Launch EC2 Instance A
1. In AWS Console, search for **EC2**.
2. Click **Launch instance**.
3. Set:
   - **Name**: `EC2-A`
   - **AMI**: Amazon Linux
   - **Instance type**: `t2.micro`
   - **Key pair**: `vockey`
4. In **Network settings**, create or select a security group.
5. Add inbound rules:
   - SSH → My IP
   - HTTP → My IP
   - HTTPS → My IP
6. Keep default storage.
7. Click **Launch instance**.
8. Wait until the instance is in **Running** state.

---

## Step 4: Launch EC2 Instance B
Repeat the same steps as Instance A, but use use different name:
- **Name**: `EC2-B`

Create or select a security group.
It is recommended to reuse the same security group.

---

## Step 5: Connect to EC2 Instances
Download the private key `labsuser.pem` from the Learner Lab page.

Use SSH to connect:

```bash
chmod 400 labsuser.pem
ssh -i labsuser.pem ec2-user@PUBLIC_IP
```

Run this separately for:
- EC2-A
- EC2-B

---

## Step 6: Configure AWS Credentials on Both Instances
On each EC2 instance:

1. Open **AWS Details** in Learner Lab.
2. Click **Show** next to AWS CLI.
3. Copy the temporary AWS CLI credentials.
4. On EC2, run: (Repeat these steps on both EC2 instances).

```bash
mkdir -p ~/.aws
nano ~/.aws/credentials
```

5. Paste the credentials in this format:

```ini
[default]
aws_access_key_id=YOUR_ACCESS_KEY
aws_secret_access_key=YOUR_SECRET_KEY
aws_session_token=YOUR_SESSION_TOKEN
```

6. Save the file.
7. Set file permissions:

```bash
chmod 600 ~/.aws/credentials
```

8. Test the configuration:

```bash
aws sqs list-queues
```

---

## Step 7: Install Java and Maven on Both Instances
On each EC2 instance, run:

```bash
sudo yum update -y
sudo yum install java-17-amazon-corretto -y
sudo yum install java-17-amazon-corretto-devel -y
sudo yum install maven -y
```

Verify installation:

```bash
java -version
javac -version
```

Set `JAVA_HOME` if needed:

```bash
readlink -f $(which java)
```

If output is similar to:

```bash
/usr/lib/jvm/java-17-amazon-corretto.x86_64/bin/java
```

then run:

```bash
echo 'export JAVA_HOME=/usr/lib/jvm/java-17-amazon-corretto.x86_64' >> ~/.bashrc
echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.bashrc
source ~/.bashrc
```

Verify Maven:

```bash
mvn -version
```

---

## Step 8: Create or Copy the Maven Project
The Java project can be created on Instance A and then copied to Instance B.

### On Instance A
Create project folder:

```bash
mkdir project1
cd project1
```

Generate Maven project:

```bash
mvn archetype:generate -DgroupId=com.project1 -DartifactId=project1 -DarchetypeArtifactId=maven-archetype-quickstart -DinteractiveMode=false
```

### Create package folders

```bash
mkdir -p src/main/java/com/project1/app
mkdir -p src/main/java/com/project1/aws
```

### Copy project to Instance B
Either copy the whole project or only the jar file.

---

## Step 9: Add AWS SDK Dependencies to `pom.xml`
Edit `pom.xml` and add these dependencies inside `<dependencies>`:

```xml
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>sqs</artifactId>
    <version>2.25.20</version>
</dependency>

<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>rekognition</artifactId>
    <version>2.25.20</version>
</dependency>

<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-simple</artifactId>
    <version>1.7.36</version>
</dependency>
```

Also add Java 17 compiler plugin inside `<build>`:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.11.0</version>
            <configuration>
                <source>17</source>
                <target>17</target>
            </configuration>
        </plugin>
    </plugins>
</build>
```

---

## Step 10: Add Java Source Files
Place the application files in these paths:

### Instance A
- `src/main/java/com/project1/app/CarRecognitionApp.java`
- `src/main/java/com/project1/aws/SQSService.java`
- `src/main/java/com/project1/aws/RekognitionService.java`

### Instance B
- `src/main/java/com/project1/app/TextRecognitionApp.java`
- `src/main/java/com/project1/aws/SQSService.java`
- `src/main/java/com/project1/aws/RekognitionService.java`

Both programs must use:
- bucket name: `cs643-njit-project1`
- the correct SQS queue URL `https://sqs.us-east-1.amazonaws.com/381492118064/project1-queue`
- region: `us-east-1`

---

## Step 11: Build the Project
On each instance, from the Maven project root, run:

```bash
mvn clean package
```

If the build succeeds, a jar file will be created in:

```bash
target/project1-1.0-SNAPSHOT.jar
```

---

## Step 12: Run the Consumer on Instance B
On Instance B, go to the project directory and run:

```bash
mvn exec:java -Dexec.mainClass="com.project1.app.TextRecognitionApp"
```

The consumer will start polling the SQS queue and wait for messages.

---

## Step 13: Run the Producer on Instance A
On Instance A, go to the project directory and run:

```bash
mvn exec:java -Dexec.mainClass="com.project1.app.CarRecognitionApp"
```

This program will:
1. Read images `1.jpg` through `10.jpg` from the S3 bucket
2. Detect whether each image contains a car
3. Send the matching image indexes to SQS
4. Send `-1` after finishing

---

## Step 14: Check Final Output on Instance B
After Instance B finishes, check the output file:

```bash
cat output.txt
```

The file should contain only the images that have both:
- a car detected by Instance A
- text detected by Instance B

Example format:

```text
1.jpg -> S BR8167
3.jpg -> 45, P, 11:50, 85%, PARKING
7.jpg -> Lamborghini, LP 610 LB
```

Note:
- Rekognition detects general text, not only license plate numbers.
- Some characters may be slightly misread due to image quality or reflections.

---

## Step 15: Test Both Startup Orders
The project requires the application to work regardless of which instance starts first.

### Test Case 1
1. Start Instance B first
2. Start Instance A second

### Test Case 2
1. Start Instance A first
2. Start Instance B second

Both scenarios should work successfully.

---

## Step 16: Optional Debugging Tips
### Purge old SQS test messages
If old test messages remain in the queue, purge the queue in AWS Console before running the final test.

### Check queue messages
Use AWS CLI:

```bash
aws sqs receive-message --queue-url "https://sqs.us-east-1.amazonaws.com/381492118064/project1-queue" --max-number-of-messages 1 --wait-time-seconds 5
```

### List objects in S3 bucket

```bash
aws s3 ls s3://cs643-njit-project1
```

---

## Step 17: Clean Up AWS Resources
After testing is finished:
1. Stop or terminate both EC2 instances
2. Delete or ignore the queue if no longer needed
3. End the AWS Learner Lab session

---

## Files Submitted for the Project
- Java source code for Instance A
- Java source code for Instance B
- README file

## youtube video and github repository links
- [Demo video link](https://youtu.be/2-ANvdjJFcM)
- [Github repository Link](https://github.com/gagg0ti/CS-643-Cloud-Computing-Project-1)


