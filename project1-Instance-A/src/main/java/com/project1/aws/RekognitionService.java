package com.project1.aws;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.DetectLabelsRequest;
import software.amazon.awssdk.services.rekognition.model.DetectLabelsResponse;
import software.amazon.awssdk.services.rekognition.model.Image;
import software.amazon.awssdk.services.rekognition.model.Label;
import software.amazon.awssdk.services.rekognition.model.S3Object;

import java.util.List;

public class RekognitionService {

    private final RekognitionClient rekognitionClient;

    public RekognitionService() {
        this.rekognitionClient = RekognitionClient.builder()
                .region(Region.US_EAST_1)
                .build();
    }

    public boolean detectCar(String bucketName, String imageKey, float minConfidence) {
        S3Object s3Object = S3Object.builder()
                .bucket(bucketName)
                .name(imageKey)
                .build();

        Image image = Image.builder()
                .s3Object(s3Object)
                .build();

        DetectLabelsRequest request = DetectLabelsRequest.builder()
                .image(image)
                .maxLabels(20)
                .minConfidence(minConfidence)
                .build();

        DetectLabelsResponse response = rekognitionClient.detectLabels(request);
        List<Label> labels = response.labels();

        for (Label label : labels) {
            System.out.println("Detected label: " + label.name() + " | Confidence: " + label.confidence());

            if ("Car".equalsIgnoreCase(label.name()) && label.confidence() >= minConfidence) {
                return true;
            }
        }

        return false;
    }

    public void close() {
        rekognitionClient.close();
    }
}
