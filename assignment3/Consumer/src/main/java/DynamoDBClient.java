import Model.LiftRide;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;

public class DynamoDBClient {
//    private static final DynamoDbClient dynamoDbClient = DynamoDbClient.create();
    public static final DynamoDbClient dynamoDbClient = DynamoDbClient.builder()
            .credentialsProvider(DefaultCredentialsProvider.create())
            .region(Region.US_WEST_2)
            .build();

    static {
        ListTablesResponse response = dynamoDbClient.listTables(ListTablesRequest.builder().build());
        System.out.println("Tables in DynamoDB: " + response.tableNames());
    }
//
//
//    public static void insertLiftRide(LiftRide liftRide) {
//        Map<String, AttributeValue> item = new HashMap<>();
//        item.put("SkierID", AttributeValue.builder().n(String.valueOf(liftRide.getSkierID())).build());
//        item.put("SeasonID-DayID-Time", AttributeValue.builder()
//                .s(String.format("%d-%d-%s", liftRide.getSeasonID(), liftRide.getDayID(), liftRide.getTime()))
//                .build());
//        item.put("ResortID", AttributeValue.builder().n(String.valueOf(liftRide.getResortID())).build());
//        item.put("LiftID", AttributeValue.builder().s(liftRide.getLiftID()).build());
//        item.put("Vertical", AttributeValue.builder()
//                .n(String.valueOf(Integer.parseInt(liftRide.getLiftID()) * 10))
//                .build());
//
//        dynamoDbClient.putItem(PutItemRequest.builder()
//                .tableName("LiftRides")
//                .item(item)
//                .build());
//    }

    public static void insertLiftRidesBatch(List<LiftRide> liftRides) {
        List<WriteRequest> writeRequests = new ArrayList<>();

        for (LiftRide liftRide : liftRides) {
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("SkierID", AttributeValue.builder().n(String.valueOf(liftRide.getSkierID())).build());
            item.put("SeasonID-DayID-Time", AttributeValue.builder()
                    .s(String.format("%d-%d-%s", liftRide.getSeasonID(), liftRide.getDayID(), liftRide.getTime()))
                    .build());
            item.put("ResortID", AttributeValue.builder().n(String.valueOf(liftRide.getResortID())).build());
            item.put("LiftID", AttributeValue.builder().s(liftRide.getLiftID()).build());
            item.put("Vertical", AttributeValue.builder()
                    .n(String.valueOf(Integer.parseInt(liftRide.getLiftID()) * 10))
                    .build());

            writeRequests.add(WriteRequest.builder()
                    .putRequest(PutRequest.builder().item(item).build())
                    .build());

            if (writeRequests.size() == 25) {
                flushBatch(writeRequests);
            }
        }

        if (!writeRequests.isEmpty()) {
            flushBatch(writeRequests);
        }
    }

    private static void flushBatch(List<WriteRequest> writeRequests) {
        try {
            dynamoDbClient.batchWriteItem(BatchWriteItemRequest.builder()
                    .requestItems(Collections.singletonMap("LiftRides", writeRequests))
                    .build());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            writeRequests.clear();
        }
    }
}
