package application;


/**
 * The StudentReviewerMessage class defines a single student-reviewer message
 * It holds the student, reviewer, message, reviewID, messageID, and seenStatusReviewer
 */


public class StudentReviewerMessage{
	//Set public and private static variables
	private String message;
	private String student;
	private String reviewer;
	private int reviewID;
	private int messageID;
	private boolean seenStatusReviewer;
	/**
     * Initializes new StudentReviewerMessage object
     * 
     * @param student             The student of the student-reviewer message.
     * @param reviewer            The reviewer of the student-reviewer message.
     * @param message             The message text.
     * @param reviewID            The unique id of the review that the student-reviewer message relates to.
     * @param messageID           The unique id for the student-reviewer message.
     * @param seenStatusReviewer  The status of whether message has been seen.
     */
	//Set public and private static functions
	public StudentReviewerMessage(String message, String student, String reviewer, int reviewID, int messageID, boolean seenStatusReviewer) {
		this.message = message;
		this.student = student;
		this.reviewer = reviewer;
		this.reviewID = reviewID;
		this.messageID = messageID;
		this.seenStatusReviewer = seenStatusReviewer;
	}
	public String getMessage() {
		return message;
	}
	public String getStudent() {
		return student;
	}
	public String getReviewer() {
		return reviewer;
	}
	public int getReviewID() {
		return reviewID;
	}
	public int getMessageID() {
		return messageID;
	}

	public boolean getSeenStatusReviewer() {
		return seenStatusReviewer;
	}
	public void setSeenStatusReviewer(boolean newValue) {
		this.seenStatusReviewer = newValue;
	}
}
