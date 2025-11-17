package databasePart1;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.Timer;
import java.util.TimerTask;
import application.User;
import application.Question;
import application.Answer;
import application.Message;
import application.PrivateMessage;
import application.Review;
import application.StudentReviewerMessage;


/**
 * The DatabaseHelper class is responsible for managing the connection to the database,
 * performing operations such as user registration, login validation, and handling invitation codes.
 */
public class DatabaseHelper {

	// JDBC driver name and database URL 
	static final String JDBC_DRIVER = "org.h2.Driver";   
	static final String DB_URL = "jdbc:h2:~/FoundationDatabase";  

	//  Database credentials 
	static final String USER = "sa"; 
	static final String PASS = ""; 

	private Connection connection = null;
	private Statement statement = null; 
	//	PreparedStatement pstmt

	public void connectToDatabase() throws SQLException {
		try {
			Class.forName(JDBC_DRIVER); // Load the JDBC driver
			System.out.println("Connecting to database...");
			connection = DriverManager.getConnection(DB_URL, USER, PASS);
			statement = connection.createStatement(); 
			// You can use this command to clear the database and restart from fresh.
			statement.execute("DROP ALL OBJECTS");

			createTables();  // Create the necessary tables if they don't exist
		} catch (ClassNotFoundException e) {
			System.err.println("JDBC Driver not found: " + e.getMessage());
		}
	}

	private void createTables() throws SQLException {
		String userTable = "CREATE TABLE IF NOT EXISTS cse360users ("
				+ "id INT AUTO_INCREMENT PRIMARY KEY, "
				+ "userName VARCHAR(255), "
				+ "password VARCHAR(255), "
				+ "role VARCHAR(20))";
		statement.execute(userTable);
		
		// Create the invitation codes table
	    String invitationCodesTable = "CREATE TABLE IF NOT EXISTS InvitationCodes ("
	            + "code VARCHAR(10) PRIMARY KEY, "
	            + "isUsed BOOLEAN DEFAULT FALSE)";
	    statement.execute(invitationCodesTable);
	    
	    // Create the users who must change password table
	    String oneTimePasswordTable = "CREATE TABLE IF NOT EXISTS oneTimePasswords ("
	    		+ "userName VARCHAR(255), "
	    		+ "password VARCHAR(255))";
	    statement.execute(oneTimePasswordTable);
	    
	    // Create the questions table. (Stores questions posted by users.)
	    String questionTable = "CREATE TABLE IF NOT EXISTS questions ("
	    		+ "id INT AUTO_INCREMENT PRIMARY KEY, "
	    		+ "userName VARCHAR(255), "
	    		+ "text VARCHAR(1000),"
	    		+ "resolved BOOLEAN DEFAULT FALSE)";
	    statement.execute(questionTable);
	    
	    // Create the answers table. (Stores answers related to questions.) 
	    String answerTable = "CREATE TABLE IF NOT EXISTS answers ("
	    		+ "id INT AUTO_INCREMENT PRIMARY KEY, "
	    		+ "questionID INT, "
	    		+ "userName VARCHAR(255), "
	    		+ "text VARCHAR(1000), "
	    		+ "resolving BOOLEAN DEFAULT FALSE, "
	    		+ "viewed BOOLEAN DEFAULT FALSE,"
	    		+ "FOREIGN KEY (questionID) REFERENCES questions(id) ON DELETE CASCADE)"; // Delete answers if question is deleted
	    statement.execute(answerTable);
	    
	    // Create the private messages table. (Stores private messages between users related to a question.)
	    String privateMessageTable = "CREATE TABLE IF NOT EXISTS privateMessages ("
	    		+ "id INT AUTO_INCREMENT PRIMARY KEY, "
	    		+ "message VARCHAR(1000),"
	    		+ "sender VARCHAR(255), "
	    		+ "receiver VARCHAR(255), "
	    		+ "questionID INT, "
	    		+ "seenStatusReceiver BOOLEAN DEFAULT FALSE)"; // Delete private message if question is deleted
	    statement.execute(privateMessageTable);
	    
	    // Create the reviews table (Stores reviews for answers)
	    String reviewsTable = "CREATE TABLE IF NOT EXISTS reviews (" +
	        "id INT AUTO_INCREMENT PRIMARY KEY, " +
	        "reviewer_username VARCHAR(255), " +
	        "answerID INT, " +
	        "rating BOOLEAN, " +
	        "description VARCHAR(1000), " +
	        "FOREIGN KEY (answerID) REFERENCES answers(id) ON DELETE CASCADE)";
	    statement.execute(reviewsTable);
	    
	    // Create the student-reviewer trust table
	    String trustTable = "CREATE TABLE IF NOT EXISTS studentReviewerTrust (" +
	        "student_username VARCHAR(255), " +
	        "reviewer_username VARCHAR(255), " +
	        "weight INT CHECK (weight BETWEEN 1 AND 3), " +
	        "PRIMARY KEY (student_username, reviewer_username))";
	    statement.execute(trustTable);
	    
	    String studentReviewerMessageTable = "CREATE TABLE IF NOT EXISTS studentReviewerMessages ("
	    		+ "id INT AUTO_INCREMENT PRIMARY KEY, "
	    		+ "message VARCHAR(1000),"
	    		+ "student VARCHAR(255), "
	    		+ "reviewer VARCHAR(255), "
	    		+ "reviewID INT, "
	    		+ "seenStatusReviewer BOOLEAN DEFAULT FALSE, "
	    		+ "FOREIGN KEY (reviewID) REFERENCES reviews(id) ON DELETE CASCADE)"; // Delete private message if question is deleted
	    statement.execute(studentReviewerMessageTable);
	}
	
	
	//-------CRUD Operations------------
	// 1. Create operations
	
	// Adds a question to database
	public boolean createQuestion(Question question) throws SQLException {
		String query;
		PreparedStatement pstmt;
		// If duplicate match return false
		if(doesQuestionExist(question)) {
			return false;
		}
		// otherwise insert new question in database and return true
		else {
			query = "INSERT INTO questions (userName, text) VALUES (?,?)";
			pstmt = connection.prepareStatement(query);
			pstmt.setString(1, question.getAuthor());
			pstmt.setString(2, question.getText());
			pstmt.executeUpdate();
			
			return true;
		}
	}

	public boolean createPrivateMessage(PrivateMessage privateMessage) throws SQLException {
		String query;
		PreparedStatement pstmt;
		
		// otherwise insert new question in database and return true
		query = "INSERT INTO privateMessages (message, sender, receiver, questionID) VALUES (?,?,?,?)";
		pstmt = connection.prepareStatement(query);
		pstmt.setString(1, privateMessage.getMessage());
		pstmt.setString(2, privateMessage.getSender());
		pstmt.setString(3, privateMessage.getReceiver());
		pstmt.setInt(4, privateMessage.getQuestionID());
		pstmt.executeUpdate();
		return true;
	}	

	public boolean createStudentReviewerMessage(StudentReviewerMessage studentReviewerMessage) throws SQLException {
		String query;
		PreparedStatement pstmt;
		
		// otherwise insert new question in database and return true
		query = "INSERT INTO studentReviewerMessages (message, student, reviewer, reviewID) VALUES (?,?,?,?)";
		pstmt = connection.prepareStatement(query);
		pstmt.setString(1, studentReviewerMessage.getMessage());
		pstmt.setString(2, studentReviewerMessage.getStudent());
		pstmt.setString(3, studentReviewerMessage.getReviewer());
		pstmt.setInt(4, studentReviewerMessage.getReviewID());
		pstmt.executeUpdate();
		return true;
	}	


	// Adds an answer for a question to the database
	public boolean createAnswer(Answer answer) throws SQLException {
		// If there answer already exist return false
		if (doesAnswerExist(answer)) {
			return true;
		}
		// Otherwise insert answer into database and return true
		else {
			String insertAnswer = "INSERT INTO answers (questionID, text, userName) VALUES (?, ?, ?)";
			
	        try (PreparedStatement pstmt = connection.prepareStatement(insertAnswer)) {
	            pstmt.setInt(1, answer.getParentQuestionID());
	            pstmt.setString(2, answer.getAnswerText());
	            pstmt.setString(3, answer.getAuthor());
	            pstmt.executeUpdate();
	        }
	        return false;
		}
	}
	
	// Creates a new review for an answer
	public boolean createReview(Review review) throws SQLException {
		// Check if reviewer already reviewed this answer
		String checkQuery = "SELECT COUNT(*) FROM reviews WHERE reviewer_username = ? AND answerID = ?";
		PreparedStatement checkStmt = connection.prepareStatement(checkQuery);
		checkStmt.setString(1, review.getReviewerUsername());
		checkStmt.setInt(2, review.getAnswerID());
		ResultSet rs = checkStmt.executeQuery();
		if (rs.next() && rs.getInt(1) > 0) {
			return false; // Review already exists
		}

		// Insert new review
		String insertQuery = "INSERT INTO reviews (reviewer_username, answerID, rating, description) VALUES (?, ?, ?, ?)";
		PreparedStatement insertStmt = connection.prepareStatement(insertQuery);
		insertStmt.setString(1, review.getReviewerUsername());
		insertStmt.setInt(2, review.getAnswerID());
		insertStmt.setBoolean(3, review.getRating());
		insertStmt.setString(4, review.getDescription());
		insertStmt.executeUpdate();
		return true;
	}
	
	
	// 2. Read operations
	
	// Returns all questions in database
	public ArrayList<Question> getAllQuestions() throws SQLException {
		ArrayList<Question> questions = new ArrayList<>();
        String query = "SELECT * FROM questions ORDER BY id DESC";
        try (ResultSet rs = statement.executeQuery(query)) {
            while (rs.next()) {
                Question q = new Question(
                        rs.getString("userName"),
                        rs.getString("text"),
                        rs.getBoolean("resolved"),
                        rs.getInt("id")
                );
                questions.add(q);
            }
        }
        return questions;
	}
	
	// Returns all users in database
	public ArrayList<User> getAllUsers() throws SQLException {
		ArrayList<User> users = new ArrayList<>();
        String query = "SELECT * FROM cse360users ORDER BY userName";
        try (ResultSet rs = statement.executeQuery(query)) {
            while (rs.next()) {
                User u = new User(
                        rs.getString("userName"),
                        rs.getString("password"),
                        rs.getString("role")
                );
                users.add(u);
            }
        }
        return users;
	}
	
	
	//Returns all privateMessages in database
	public ArrayList<PrivateMessage> getAllPrivateMessages() throws SQLException {
		ArrayList<PrivateMessage> privateMessages = new ArrayList<>();
        String query = "SELECT * FROM privateMessages ORDER BY id ASC";
        try (ResultSet rs = statement.executeQuery(query)) {
            while (rs.next()) {
                PrivateMessage p = new PrivateMessage(
                		rs.getString("message"),
                        rs.getString("sender"),
                        rs.getString("receiver"),
                        rs.getInt("questionID"),
                        rs.getInt("id"),
                        rs.getBoolean("seenStatusReceiver")
                );
                privateMessages.add(p);
            }
        }
        return privateMessages;
	}

	//Returns all studentReviewerMessages in database
	public ArrayList<StudentReviewerMessage> getAllStudentReviewerMessages() throws SQLException {
		ArrayList<StudentReviewerMessage> studentReviewerMessages = new ArrayList<>();
        String query = "SELECT * FROM studentReviewerMessages ORDER BY id ASC";
        try (ResultSet rs = statement.executeQuery(query)) {
            while (rs.next()) {
            	StudentReviewerMessage p = new StudentReviewerMessage(
                		rs.getString("message"),
                        rs.getString("student"),
                        rs.getString("reviewer"),
                        rs.getInt("reviewID"),
                        rs.getInt("id"),
                        rs.getBoolean("seenStatusReviewer")
                );
            	studentReviewerMessages.add(p);
            }
        }
        return studentReviewerMessages;
	}	
	


	// Returns a specific question by its ID
	public Question getQuestionByID(int questionID) throws SQLException {
		String query = "SELECT * FROM questions WHERE id = ?";
		PreparedStatement pstmt = connection.prepareStatement(query);
		pstmt.setInt(1, questionID);
		ResultSet rs = pstmt.executeQuery();

		if (rs.next()) {
			Question q = new Question(
				rs.getString("userName"),
				rs.getString("text"),
				rs.getBoolean("resolved"),
				rs.getInt("id")
			);
			return q;
		}
		return null;
	}
	
	// Returns all answers for a specific question
    public ArrayList<Answer> getAllAnswers(int questionId) throws SQLException {
        ArrayList<Answer> answers = new ArrayList<>();
        String query = "SELECT * FROM answers WHERE questionID = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, questionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Answer curr = new Answer(
                    		rs.getString("userName"),
                            rs.getString("text"),
                            rs.getBoolean("resolving"),
                    		rs.getBoolean("viewed"),
                            rs.getInt("id"),
                            rs.getInt("questionID")
                    );
                    answers.add(curr);
                }
            }
        }
        return answers;
    }
	
	 // Checks if a question with the same text already exists
    public boolean doesQuestionExist(Question question) throws SQLException {
        String query = "SELECT COUNT(*) FROM questions WHERE text = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, question.getText());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }
		
	 // Checks if an answer with the same text already exists for a given question
    public boolean doesAnswerExist(Answer answer) throws SQLException {
        String query = "SELECT COUNT(*) FROM answers WHERE questionID = ? AND text = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, answer.getParentQuestionID());
            pstmt.setString(2, answer.getAnswerText());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }
    
	// Returns all reviews written by a specific reviewer
	public ArrayList<Review> getAllUserReviews(String reviewerUsername) throws SQLException {
	    ArrayList<Review> reviews = new ArrayList<>();
	    String query = "SELECT * FROM reviews WHERE reviewer_username = ?";
	    PreparedStatement pstmt = connection.prepareStatement(query);
	    pstmt.setString(1, reviewerUsername);
	    ResultSet rs = pstmt.executeQuery();

	    while (rs.next()) {
	        Review review = new Review(
	            rs.getInt("id"),
	            rs.getString("reviewer_username"),
	            rs.getInt("answerID"),
	            rs.getBoolean("rating"),
	            rs.getString("description")
	        );
	        reviews.add(review);
	    }
	    return reviews;
	}
	
	public ArrayList<Review> getReviewsForAnswer(int answerID, String viewerUsername, boolean trustedOnly) throws SQLException {
	    ArrayList<Review> reviews = new ArrayList<>();
	    HashMap<String, Integer> trustMap = new HashMap<>();

	    // Only load trust weights if trustedOnly is true
	    if (trustedOnly) {
	        String trustQuery = "SELECT reviewer_username, weight FROM studentReviewerTrust WHERE student_username = ?";
	        PreparedStatement trustStmt = connection.prepareStatement(trustQuery);
	        trustStmt.setString(1, viewerUsername);
	        ResultSet trustRs = trustStmt.executeQuery();
	        while (trustRs.next()) {
	            trustMap.put(trustRs.getString("reviewer_username"), trustRs.getInt("weight"));
	        }
	    }

	    // Get all reviews for the answer
	    String reviewQuery = "SELECT * FROM reviews WHERE answerID = ?";
	    PreparedStatement reviewStmt = connection.prepareStatement(reviewQuery);
	    reviewStmt.setInt(1, answerID);
	    ResultSet reviewRs = reviewStmt.executeQuery();

	    while (reviewRs.next()) {
	        String reviewer = reviewRs.getString("reviewer_username");

	        // Filter if trustedOnly is true
	        if (trustedOnly && !trustMap.containsKey(reviewer)) {
	            continue;
	        }

	        Review review = new Review(
	            reviewRs.getInt("id"),
	            reviewer,
	            answerID,
	            reviewRs.getBoolean("rating"),
	            reviewRs.getString("description")
	        );
	        reviews.add(review);
	    }

	    // Sort reviews by trust weight if applicable
	    if (trustedOnly) {
	        reviews.sort((r1, r2) -> {
	            int w1 = trustMap.getOrDefault(r1.getReviewerUsername(), 0);
	            int w2 = trustMap.getOrDefault(r2.getReviewerUsername(), 0);
	            return Integer.compare(w2, w1); // descending
	        });
	    }

	    return reviews;
	}
    
    // 3. Update operations
    
    // Updates the text of a question given its id
	public void updateQuestion(int id, String newText) throws SQLException {
		String updateText = "UPDATE questions SET text = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(updateText)) {
            pstmt.setString(1, newText);
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        }
	}
	
	public void updatePrivateMessage(PrivateMessage pm) throws SQLException {
		String updateText = "UPDATE privateMessages SET message = ?, sender = ?, receiver = ?, questionID = ?, seenStatusReceiver = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(updateText)) {
        	pstmt.setInt(6, pm.getPrivateMessageID());
        	pstmt.setString(1, pm.getMessage());
        	pstmt.setString(2, pm.getSender());
        	pstmt.setString(3, pm.getReceiver());
        	pstmt.setInt(4, pm.getQuestionID());
        	pstmt.setBoolean(5, pm.getSeenStatusReceiver());
            pstmt.executeUpdate();
        }
	}
	
	public void updateStudentReviewerMessage(StudentReviewerMessage srm) throws SQLException {
		String updateText = "UPDATE studentReviewerMessages SET message = ?, student = ?, reviewer = ?, reviewID = ?, seenStatusReviewer = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(updateText)) {
        	pstmt.setInt(6, srm.getMessageID());
        	pstmt.setString(1, srm.getMessage());
        	pstmt.setString(2, srm.getStudent());
        	pstmt.setString(3, srm.getReviewer());
        	pstmt.setInt(4, srm.getReviewID());
        	pstmt.setBoolean(5, srm.getSeenStatusReviewer());
            pstmt.executeUpdate();
        }
	}
	// Updates the text of an answer given the id
	public boolean updateAnswer(int answerID, String answerText) throws SQLException {
		String query;
		PreparedStatement pstmt;
		ResultSet rs;
		int questionID = -1;
		
		// get questionid
		query = "SELECT questionID FROM answers WHERE id = ?";
		pstmt = connection.prepareStatement(query);
		pstmt.setInt(1, answerID);
		rs = pstmt.executeQuery();
		if(rs.next()) {
			questionID = rs.getInt("questionID");
		}
		else {
			return false;
		}
		
		// check for exact questionid, answertext match
		query = "SELECT * FROM answers WHERE questionID = ? AND text = ?";
		pstmt = connection.prepareStatement(query);
		pstmt.setInt(1, questionID);
		pstmt.setString(2, answerText);
		rs = pstmt.executeQuery();
		if(rs.next()) {
			return false;
		}
		
		query = "UPDATE answers SET text = ? WHERE id = ?";
		pstmt = connection.prepareStatement(query);
		pstmt.setString(1, answerText);
		pstmt.setInt(2, answerID);
		pstmt.executeUpdate();
		return true;
	}

	// Updates the resolving status of a specific answer
	public void updateAnswerResolving(int answerID, boolean isResolving) throws SQLException {
		String query = "UPDATE answers SET resolving = ? WHERE id = ?";
		PreparedStatement pstmt = connection.prepareStatement(query);
		pstmt.setBoolean(1, isResolving);
		pstmt.setInt(2, answerID);
		pstmt.executeUpdate();
	}

	// Clears resolving status from all answers for a given question
	public void clearResolvingAnswer(int questionID) throws SQLException {
		String query = "UPDATE answers SET resolving = FALSE WHERE questionID = ?";
		PreparedStatement pstmt = connection.prepareStatement(query);
		pstmt.setInt(1, questionID);
		pstmt.executeUpdate();
	}

	// Marks a question as resolved
	public void markQuestionResolved(int questionID) throws SQLException {
		String query = "UPDATE questions SET resolved = TRUE WHERE id = ?";
		PreparedStatement pstmt = connection.prepareStatement(query);
		pstmt.setInt(1, questionID);
		pstmt.executeUpdate();
	}
	
	// Marks a question as unresolved
	public void markQuestionUnresolved(int questionID) throws SQLException {
	    String query = "UPDATE questions SET resolved = FALSE WHERE id = ?";
	    PreparedStatement pstmt = connection.prepareStatement(query);
	    pstmt.setInt(1, questionID);
	    pstmt.executeUpdate();
	}
	
	// Marks all answers for a question as viewed
	public void markAnswersViewed(int questionID) throws SQLException {
		String query = "UPDATE answers SET viewed = TRUE WHERE questionID = ?";
		PreparedStatement pstmt = connection.prepareStatement(query);
		pstmt.setInt(1, questionID);
		pstmt.executeUpdate();
	}
	
	// Inserts a new version of a review instead of updating the existing one.
	// This preserves review history by creating a new entry.
	public boolean updateReview(String reviewerUsername, int answerID, boolean rating, String description) throws SQLException {
	    String query = "INSERT INTO reviews (reviewer_username, answerID, rating, description) VALUES (?, ?, ?, ?)";
	    PreparedStatement pstmt = connection.prepareStatement(query);
	    pstmt.setString(1, reviewerUsername);
	    pstmt.setInt(2, answerID);
	    pstmt.setBoolean(3, rating);
	    pstmt.setString(4, description);
	    int rowsAffected = pstmt.executeUpdate();
	    return rowsAffected > 0;
	}
	
	// Updates or inserts a trust relationship between a student and a reviewer.
	public void updateStudentTrust(String studentUsername, String reviewerUsername, int weight) throws SQLException {
	    String query = "MERGE INTO studentReviewerTrust (student_username, reviewer_username, weight) KEY(student_username, reviewer_username) VALUES (?, ?, ?)";
	    PreparedStatement pstmt = connection.prepareStatement(query);
	    pstmt.setString(1, studentUsername);
	    pstmt.setString(2, reviewerUsername);
	    pstmt.setInt(3, weight);
	    pstmt.executeUpdate();
	}
	
	// 4. Delete operations
	
	
	// Deletes a question from the database
	public void deleteQuestion(int id) throws SQLException {
		String deleteQuestion = "DELETE FROM questions WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(deleteQuestion)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
	}
	
	//Deletes a privateMessage from the database
	public void deletePrivateMessage(int id) throws SQLException {
		String deletePrivateMessage = "DELETE FROM privateMessages WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(deletePrivateMessage)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
	}
	
	// Deletes an answer from the database
	public boolean deleteAnswer(int answerID) throws SQLException {
		String query = "DELETE FROM answers WHERE id = ?";
		PreparedStatement pstmt = connection.prepareStatement(query);
		pstmt.setInt(1, answerID);
		pstmt.executeUpdate();
		return true;
	}
	
	// Deletes a review by its ID
	public boolean deleteReview(int reviewID) throws SQLException {
	    String query = "DELETE FROM reviews WHERE id = ?";
	    PreparedStatement pstmt = connection.prepareStatement(query);
	    pstmt.setInt(1, reviewID);
	    int rowsAffected = pstmt.executeUpdate();
	    return rowsAffected > 0;
	}
	

	 // Removes the trust relationship between a student and a reviewer.
	public void removeTrust(String studentUsername, String reviewerUsername) throws SQLException {
	    String query = "DELETE FROM studentReviewerTrust WHERE student_username = ? AND reviewer_username = ?";
	    PreparedStatement pstmt = connection.prepareStatement(query);
	    pstmt.setString(1, studentUsername);
	    pstmt.setString(2, reviewerUsername);
	    pstmt.executeUpdate();
	}
	
	// convoStarter is the person who sent the first private message, sender is who wrote the current message
	public boolean sendMessage(int questionID, String convoStarter, String sender, String text) throws SQLException {
		String query = "INSERT INTO messages (questionID, mainUser, userName, text) VALUES (?,?,?,?)";
		PreparedStatement pstmt = connection.prepareStatement(query);
		pstmt.setInt(1, questionID);
		pstmt.setString(2, convoStarter);
		pstmt.setString(3, sender);
		pstmt.setString(4, text);
		pstmt.executeUpdate();
		return true;
	}
	
	 // Retrieves the trust weight assigned by a student to a specific reviewer.
	 // Return the trust weight (1 to 3), or 0 if no trust relationship exists.
	public int getTrustWeight(String studentUsername, String reviewerUsername) throws SQLException {
	    String query = "SELECT weight FROM studentReviewerTrust WHERE student_username = ? AND reviewer_username = ?";
	    PreparedStatement pstmt = connection.prepareStatement(query);
	    pstmt.setString(1, studentUsername);
	    pstmt.setString(2, reviewerUsername);
	    ResultSet rs = pstmt.executeQuery();
	    if (rs.next()) {
	        return rs.getInt("weight");
	    }
	    return 0; // default if not trusted
	}
	
	 // Return ArrayList of reviewer usernames trusted by the student.
	public ArrayList<String> getTrustedReviewers(String studentUsername) throws SQLException {
	    ArrayList<String> trusted = new ArrayList<>();
	    String query = "SELECT reviewer_username FROM studentReviewerTrust WHERE student_username = ?";
	    PreparedStatement pstmt = connection.prepareStatement(query);
	    pstmt.setString(1, studentUsername);
	    ResultSet rs = pstmt.executeQuery();
	    while (rs.next()) {
	        trusted.add(rs.getString("reviewer_username"));
	    }
	    return trusted;
	}
	
	/**
	 * Calculates the trust-weighted rating for a given answer, where each review contributes a score of 1.0 (if marked helpful) or 0.0 (if not),
	 * multiplied by the trust weight assigned by the student to the reviewer.If no trust record exists, a default weight of 1 is used.
	 *
	 * @param answerID The ID of the answer being rated.
	 * @param studentUsername The student viewing the answer (used to fetch trust weights).
	 * @return A weighted average rating between 0.0 and 1.0. Returns 0.0 if no reviews exist.
	 * @throws SQLException If database access fails.
	 */
	public double calculateAnswerRating(int answerID, String studentUsername) throws SQLException {
	    String query = "SELECT r.rating, srt.weight " +
	                   "FROM Reviews r " +
	                   "LEFT JOIN studentReviewerTrust srt " +
	                   "ON r.reviewer_username = srt.reviewer_username AND srt.student_username = ? " +
	                   "WHERE r.answerID = ?";

	    PreparedStatement stmt = connection.prepareStatement(query);
	    stmt.setString(1, studentUsername);
	    stmt.setInt(2, answerID);

	    ResultSet rs = stmt.executeQuery();

	    double weightedSum = 0.0;
	    int totalWeight = 0;

	    while (rs.next()) {
	        boolean rating = rs.getBoolean("rating");
	        int weight = rs.getInt("weight");
	        if (rs.wasNull()) weight = 1; // default weight if no trust record

	        weightedSum += (rating ? 1.0 : 0.0) * weight;
	        totalWeight += weight;
	    }

	    rs.close();
	    stmt.close();

	    return totalWeight == 0 ? 0.0 : weightedSum / totalWeight;
	}
	

	// Check if the database is empty
	public boolean isDatabaseEmpty() throws SQLException {
		String query = "SELECT COUNT(*) AS count FROM cse360users";
		ResultSet resultSet = statement.executeQuery(query);
		if (resultSet.next()) {
			return resultSet.getInt("count") == 0;
		}
		return true;
	}

	// Registers a new user in the database.
	public void register(User user) throws SQLException {
		String insertUser = "INSERT INTO cse360users (userName, password, role) VALUES (?, ?, ?)";
		try (PreparedStatement pstmt = connection.prepareStatement(insertUser)) {
			pstmt.setString(1, user.getUserName());
			pstmt.setString(2, user.getPassword());
			pstmt.setString(3, user.getRole());
			pstmt.executeUpdate();
		}
	}
	
	// Deletes a user from the database.
	public void deleteUser(String userName) throws SQLException {
		String deleteUser = "DELETE FROM cse360users WHERE userName = ?";
		try(PreparedStatement pstmt = connection.prepareStatement(deleteUser)) {
			pstmt.setString(1, userName);
			pstmt.executeUpdate();
		}
	}
	
	// Removes a user's role from the database.
	public void deleteRole(String userName, String role) throws SQLException {
		String query = "DELETE FROM cse360users WHERE username = ? AND role = ?";
		try(PreparedStatement pstmt = connection.prepareStatement(query)) {
			pstmt.setString(1, userName);
			pstmt.setString(2, role);
			pstmt.executeUpdate();
		}
	}
	
	// Returns all user information from the database, as an ArrayList of users.
	public ArrayList<User> returnAllUsers() throws SQLException {
		
		ArrayList<User> users = new ArrayList<>();
		
		String query = "SELECT DISTINCT userName AS users FROM cse360users";
		ResultSet resultSet = statement.executeQuery(query);
		while(resultSet.next()) {
			
			// Finds password of user.
			query = "SELECT password FROM cse360users WHERE userName = ?";
			PreparedStatement pstmt = connection.prepareStatement(query);
			pstmt.setString(1, resultSet.getString("users"));
			ResultSet passwords = pstmt.executeQuery();
			String password = "";
			if(passwords.next()) {
				password = passwords.getString("password");
			}
			
			// Finds all roles of user.
			query = "SELECT role FROM cse360users WHERE userName = ?";
			pstmt = connection.prepareStatement(query);
			pstmt.setString(1, resultSet.getString("users"));
			ResultSet userRoles = pstmt.executeQuery();
			String roles = "";
			while(userRoles.next()) {
				roles += userRoles.getString("role"); // Return the role if user exists
	            roles += ",";
			}
			roles = roles.substring(0,roles.length()-1);
			User user = new User(resultSet.getString("users"),password,roles);
			users.add(user);
		}
		
		return users;
	}
	
	// Generates temporary password for a user.
	public String getTempPassword(String userName) throws SQLException {
		// Generates new password.
		String tempPass = UUID.randomUUID().toString().substring(0, 4); // Generate a random 4-character temporary password
		
		// Updates cse360users table with new password.
		setPassword(userName, tempPass);
		
		String query = "SELECT userName FROM oneTimePasswords WHERE userName = ?";
		PreparedStatement pstmt = connection.prepareStatement(query);
		pstmt.setString(1, userName);
		ResultSet rs = pstmt.executeQuery();
		if(rs.next()) {
			// Updates oneTimePasswords with new tempPass for provided user.
			query = "UPDATE oneTimePasswords SET password = ? WHERE userName = ?";
			pstmt = connection.prepareStatement(query);
			pstmt.setString(1, tempPass);
			pstmt.setString(2, userName);
			pstmt.executeUpdate();
		}
		else {
			// Adds user to oneTimePasswords, with tempPass.
			query = "INSERT INTO oneTimePasswords (userName) VALUES (?)";
			pstmt = connection.prepareStatement(query);
			pstmt.setString(1, userName);
			pstmt.executeUpdate();
		}
		
		// Returns generated password.
		return tempPass;
	}
	
	// Sets new password for a user.
	public void setPassword(String userName, String password) throws SQLException {
		String query = "UPDATE cse360users SET password = ? WHERE userName = ?";
		PreparedStatement pstmt = connection.prepareStatement(query);
		pstmt.setString(1, password);
		pstmt.setString(2, userName);
		pstmt.executeUpdate();
	}
	
	// Checks if a user needs to change passwords.
	public boolean needsNewPassword(String userName) throws SQLException {
		String query = "SELECT userName FROM oneTimePasswords WHERE userName = ?";
		PreparedStatement pstmt = connection.prepareStatement(query);
		pstmt.setString(1, userName);
		ResultSet rs = pstmt.executeQuery();
		if(rs.next()) {
			query = "DELETE FROM oneTimePasswords WHERE userName = ?";
			pstmt = connection.prepareStatement(query);
			pstmt.setString(1, userName);
			pstmt.executeUpdate();
			return true;
		}
		else {
			return false;
		}
	}
	
	// Validates a user's login credentials.
	public boolean login(User user) throws SQLException {
		String query = "SELECT * FROM cse360users WHERE userName = ? AND password = ?";
		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			pstmt.setString(1, user.getUserName());
			pstmt.setString(2, user.getPassword());
			try (ResultSet rs = pstmt.executeQuery()) {
				return rs.next();
			}
		}
	}
	
	// Checks if a user already exists in the database based on their userName.
	public boolean doesUserExist(String userName) {
	    String query = "SELECT COUNT(*) FROM cse360users WHERE userName = ?";
	    try (PreparedStatement pstmt = connection.prepareStatement(query)) {
	        
	        pstmt.setString(1, userName);
	        ResultSet rs = pstmt.executeQuery();
	        
	        if (rs.next()) {
	            // If the count is greater than 0, the user exists
	            return rs.getInt(1) > 0;
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	    return false; // If an error occurs, assume user doesn't exist
	}
	
	// Retrieves the role of a user from the database using their UserName.
	public String getUserRole(String userName) {
	    String query = "SELECT role FROM cse360users WHERE userName = ?";
	    try (PreparedStatement pstmt = connection.prepareStatement(query)) {
	        pstmt.setString(1, userName);
	        ResultSet rs = pstmt.executeQuery();
	        String roles = "";
	        while (rs.next()) {
	            roles += rs.getString("role"); // Return the role if user exists
	            roles += ",";
	        }
	        roles = roles.substring(0,roles.length()-1);
	        return roles;
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	    return null; // If no user exists or an error occurs
	}
	
	// Generates a new invitation code and inserts it into the database.
	public String generateInvitationCode() {
	    String code = UUID.randomUUID().toString().substring(0, 4); // Generate a random 4-character code
	    String query = "INSERT INTO InvitationCodes (code, isUsed) VALUES (?, FALSE)"; // added FALSE cause it continued to not validate my codes

	    try (PreparedStatement pstmt = connection.prepareStatement(query)) {
	        pstmt.setString(1, code);
	        pstmt.executeUpdate();
	        Timer timer = new Timer();
	        TimerTask task = new TimerTask() {
	        	@Override
	        	public void run() {
	        		try {
	        			String taskQuery = "DELETE FROM InvitationCodes WHERE code = ?";
	        			PreparedStatement taskpstmt = connection.prepareStatement(taskQuery);
	        			taskpstmt.setString(1, code);
	        			taskpstmt.executeUpdate();
	        		} catch(Exception e) {
	        			e.printStackTrace();
	        		}
	        	}
	        };
	        timer.schedule(task, 5 * 60 * 1000);
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }

	    return code;
	}
	
	// Validates an invitation code to check if it is unused.
	public boolean validateInvitationCode(String code) {
	    String query = "SELECT * FROM InvitationCodes WHERE code = ? AND isUsed = FALSE";
	    try (PreparedStatement pstmt = connection.prepareStatement(query)) {
	        pstmt.setString(1, code);
	        ResultSet rs = pstmt.executeQuery();
	        if (rs.next()) {
	            // Mark the code as used
	            markInvitationCodeAsUsed(code);
	            return true;
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	    return false;
	}
	
	// Marks the invitation code as used in the database.
	private void markInvitationCodeAsUsed(String code) {
	    String query = "UPDATE InvitationCodes SET isUsed = TRUE WHERE code = ?";
	    try (PreparedStatement pstmt = connection.prepareStatement(query)) {
	        pstmt.setString(1, code);
	        pstmt.executeUpdate();
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	}

	// Closes the database connection and statement.
	public void closeConnection() {
		try{ 
			if(statement!=null) statement.close(); 
		} catch(SQLException se2) { 
			se2.printStackTrace();
		} 
		try { 
			if(connection!=null) connection.close(); 
		} catch(SQLException se){ 
			se.printStackTrace(); 
		} 
	}

}
