package edu.heinz.ds;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

/**
 * Author: [Your Name]
 * Andrew ID: [Your Andrew ID]
 * 
 * MongoDB connection utility class for managing database connections
 */
public class MongoDbConnection {
    
    // MongoDB Atlas connection string
    private static final String CONNECTION_STRING = "mongodb://task1user:01160916iD@ac-wca7mv4-shard-00-00.biwdvm8.mongodb.net:27017,ac-wca7mv4-shard-00-01.biwdvm8.mongodb.net:27017/task1_demo?w=majority&retryWrites=true&tls=true&authMechanism=SCRAM-SHA-1";
    
    private static final String DATABASE_NAME = "cat_explorer_logs";
    private static final String COLLECTION_NAME = "api_logs";
    
    private static MongoClient mongoClient = null;
    
    /**
     * Get MongoDB database instance
     * @return MongoDatabase instance
     */
    public static MongoDatabase getDatabase() {
        if (mongoClient == null) {
            mongoClient = MongoClients.create(CONNECTION_STRING);
        }
        return mongoClient.getDatabase(DATABASE_NAME);
    }
    
    /**
     * Get the collection name for logs
     * @return Collection name
     */
    public static String getCollectionName() {
        return COLLECTION_NAME;
    }
    
    /**
     * Close MongoDB connection (call on application shutdown)
     */
    public static void close() {
        if (mongoClient != null) {
            mongoClient.close();
            mongoClient = null;
        }
    }
}

