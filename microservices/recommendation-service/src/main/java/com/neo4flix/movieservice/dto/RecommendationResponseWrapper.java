package com.neo4flix.movieservice.dto;

import java.util.List;

/**
 * Wrapper DTO for recommendation responses
 */
public class RecommendationResponseWrapper {

    private boolean success;
    private String message;
    private DataWrapper data;

    // Convenience setters for controller
    public void setRecommendations(List<RecommendationResponse> recommendations) {
        if (this.data == null) {
            this.data = new DataWrapper();
        }
        this.data.setRecommendations(recommendations);
    }

    public void setAlgorithm(String algorithm) {
        if (this.data == null) {
            this.data = new DataWrapper();
        }
        this.data.setAlgorithm(algorithm);
    }

    public void setConfidence(double confidence) {
        if (this.data == null) {
            this.data = new DataWrapper();
        }
        this.data.setConfidence(confidence);
    }

    public void setTotalResults(int totalResults) {
        if (this.data == null) {
            this.data = new DataWrapper();
        }
        this.data.setTotalResults(totalResults);
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public DataWrapper getData() {
        return data;
    }

    public void setData(DataWrapper data) {
        this.data = data;
    }

    public static class DataWrapper {
        private List<RecommendationResponse> recommendations;
        private String algorithm;
        private double confidence;
        private int totalResults;

        public List<RecommendationResponse> getRecommendations() {
            return recommendations;
        }

        public void setRecommendations(List<RecommendationResponse> recommendations) {
            this.recommendations = recommendations;
        }

        public String getAlgorithm() {
            return algorithm;
        }

        public void setAlgorithm(String algorithm) {
            this.algorithm = algorithm;
        }

        public double getConfidence() {
            return confidence;
        }

        public void setConfidence(double confidence) {
            this.confidence = confidence;
        }

        public int getTotalResults() {
            return totalResults;
        }

        public void setTotalResults(int totalResults) {
            this.totalResults = totalResults;
        }
    }
}
