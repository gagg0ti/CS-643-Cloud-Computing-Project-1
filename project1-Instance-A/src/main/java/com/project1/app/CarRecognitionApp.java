package com.project1.app;

import com.project1.aws.RekognitionService;
import com.project1.aws.SQSService;

public class CarRecognitionApp {

    public static void main(String[] args) {
        final String bucketName = "cs643-njit-project1";
        final String queueUrl = "https://sqs.us-east-1.amazonaws.com/381492118064/project1-queue";
        final float minConfidence = 80.0f;

        SQSService sqsService = new SQSService();
        RekognitionService rekognitionService = new RekognitionService();

        try {
            System.out.println("Starting car detection on Instance A...");

            for (int i = 1; i <= 10; i++) {
                String imageKey = i + ".jpg";
                System.out.println("Checking image: " + imageKey);

                boolean hasCar = rekognitionService.detectCar(bucketName, imageKey, minConfidence);

                if (hasCar) {
                    System.out.println("Car detected in " + imageKey + ". Sending index to SQS: " + i);
                    sqsService.sendMessage(queueUrl, String.valueOf(i));
                } else {
                    System.out.println("No car detected in " + imageKey);
                }
            }

            System.out.println("All images processed. Sending termination message: -1");
            sqsService.sendMessage(queueUrl, "-1");

            System.out.println("CarRecognitionApp finished successfully.");

        } catch (Exception e) {
            System.err.println("Error in CarRecognitionApp: " + e.getMessage());
            e.printStackTrace();
        } finally {
            sqsService.close();
            rekognitionService.close();
        }
    }
}
