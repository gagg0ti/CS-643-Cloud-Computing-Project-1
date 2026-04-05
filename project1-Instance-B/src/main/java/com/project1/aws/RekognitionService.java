package com.project1.aws;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.DetectLabelsRequest;
import software.amazon.awssdk.services.rekognition.model.DetectLabelsResponse;
import software.amazon.awssdk.services.rekognition.model.DetectTextRequest;
import software.amazon.awssdk.services.rekognition.model.DetectTextResponse;
import software.amazon.awssdk.services.rekognition.model.Image;
import software.amazon.awssdk.services.rekognition.model.Label;
import software.amazon.awssdk.services.rekognition.model.S3Object;
import software.amazon.awssdk.services.rekognition.model.TextDetection;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

    public List<String> detectText(String bucketName, String imageKey, float minConfidence) {
        S3Object s3Object = S3Object.builder()
                .bucket(bucketName)
                .name(imageKey)
                .build();

        Image image = Image.builder()
                .s3Object(s3Object)
                .build();

        DetectTextRequest request = DetectTextRequest.builder()
                .image(image)
                .build();

        DetectTextResponse response = rekognitionClient.detectText(request);
        List<TextDetection> detections = response.textDetections();

        Set<String> uniqueTexts = new LinkedHashSet<>();

        for (TextDetection detection : detections) {
            if (detection.confidence() != null
                    && detection.confidence() >= minConfidence
                    && detection.detectedText() != null
                    && !detection.detectedText().isBlank()
                    && "LINE".equalsIgnoreCase(detection.typeAsString())) {

                System.out.println("Detected text: " + detection.detectedText()
                        + " | Confidence: " + detection.confidence());

                uniqueTexts.add(detection.detectedText().trim());
            }
        }

        return new ArrayList<>(uniqueTexts);
    }

    public void close() {
        rekognitionClient.close();
    }
}
