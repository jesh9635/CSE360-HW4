package application;

/**
 * <p> Title: Review </p>
 *
 * <p> Description: This class represents a review submitted by a reviewer for a specific answer.
 * Each review includes a unique ID, the reviewer's username, the ID of the answer being reviewed,
 * a boolean rating indicating whether the answer was helpful, and a descriptive explanation. </p>
 *
 * <p> Copyright: © 2025 </p>
 *
 *
 * @version 1.00        2025        Initial implementation of TP3 review model
 */
public class Review {
    private int reviewID;
    private String reviewerUsername;
    private int answerID;
    private boolean rating;
    private String description;

    /**
     * Constructor for Review object.
     *
     * @param reviewID          Unique ID of the review.
     * @param reviewerUsername  Username of the reviewer.
     * @param answerID          ID of the answer being reviewed.
     * @param rating            Boolean rating (true = helpful, false = not helpful).
     * @param description       Text description of the review.
     */
    public Review(int reviewID, String reviewerUsername, int answerID, boolean rating, String description) {
        this.reviewID = reviewID;
        this.reviewerUsername = reviewerUsername;
        this.answerID = answerID;
        this.rating = rating;
        this.description = description;
    }

    // Getters
    public int getReviewID() {
        return reviewID;
    }

    public String getReviewerUsername() {
        return reviewerUsername;
    }

    public int getAnswerID() {
        return answerID;
    }

    public boolean getRating() {
        return rating;
    }

    public String getDescription() {
        return description;
    }

    public void setRating(boolean rating) {
        this.rating = rating;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
