package com.neo4flix.movieservice.dto;

/**
 * DTO for user similarity calculations in collaborative filtering
 */
public class UserSimilarity {

    private String userId1;
    private String userId2;
    private String username1;
    private String username2;
    private Double similarity;
    private Integer commonRatings;

    public UserSimilarity() {}

    public UserSimilarity(String userId1, String userId2, Double similarity, Integer commonRatings) {
        this.userId1 = userId1;
        this.userId2 = userId2;
        this.similarity = similarity;
        this.commonRatings = commonRatings;
    }

    // Getters and Setters
    public String getUserId1() {
        return userId1;
    }

    public void setUserId1(String userId1) {
        this.userId1 = userId1;
    }

    public String getUserId2() {
        return userId2;
    }

    public void setUserId2(String userId2) {
        this.userId2 = userId2;
    }

    public String getUsername1() {
        return username1;
    }

    public void setUsername1(String username1) {
        this.username1 = username1;
    }

    public String getUsername2() {
        return username2;
    }

    public void setUsername2(String username2) {
        this.username2 = username2;
    }

    public Double getSimilarity() {
        return similarity;
    }

    public void setSimilarity(Double similarity) {
        this.similarity = similarity;
    }

    public Integer getCommonRatings() {
        return commonRatings;
    }

    public void setCommonRatings(Integer commonRatings) {
        this.commonRatings = commonRatings;
    }

    @Override
    public String toString() {
        return String.format("UserSimilarity{user1='%s', user2='%s', similarity=%.3f, commonRatings=%d}",
                userId1, userId2, similarity, commonRatings);
    }
}