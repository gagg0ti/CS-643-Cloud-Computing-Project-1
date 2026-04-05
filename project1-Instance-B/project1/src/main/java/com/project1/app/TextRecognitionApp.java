package com.project1.app;

import com.project1.aws.RekognitionService;
import com.project1.aws.SQSService;
import software.amazon.awssdk.services.sqs.model.Message;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class TextRecognitionApp {

    public static void main(String[] args) {
        final String bucketName = "cs643-njit-project1";
        final String queueUrl = "https://sqs.us-east-1.amazonaws.com/381492118064/project1-queue.fifo";
        final float minConfidence = 80.0f;
        final String outputFile = "output.txt";

        SQSService sqsService = new SQSService();
        RekognitionService rekognitionService = new RekognitionService();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, false))) {
            System.out.println("Starting text detection on Instance B...");

            while (true) {
                List<Message> messages = sqsService.receiveMessages(queueUrl);

                if (messages == null || messages.isEmpty()) {
                    System.out.println("No messages available. Waiting...");
                    continue;
                }

                for (Message message : messages) {
                    String body = message.body().trim();
                    System.out.println("Received message: " + body);

                    if ("-1".equals(body)) {
                        System.out.println("Termination message received. Stopping consumer...");
                        sqsService.deleteMessage(queueUrl, message.receiptHandle());
                        return;
                    }

                    String imageKey = body + ".jpg";
                    System.out.println("Detecting text in image: " + imageKey);

                    List<String> detectedTexts = rekognitionService.detectText(bucketName, imageKey, minConfidence);

                    if (!detectedTexts.isEmpty()) {
                        String joinedText = String.join(", ", detectedTexts);
                        String line = imageKey + " -> " + joinedText;

                        writer.write(line);
                        writer.newLine();
                        writer.flush();

                        System.out.println("Written to output.txt: " + line);
                    } else {
                        System.out.println("No text detected in " + imageKey);
                    }

                    sqsService.deleteMessage(queueUrl, message.receiptHandle());
                    System.out.println("Deleted processed message: " + body);
                }
            }

        } catch (Exception e) {
            System.err.println("Error in TextRecognitionApp: " + e.getMessage());
            e.printStackTrace();
        } finally {
            sqsService.close();
            rekognitionService.close();
        }
    }
}
