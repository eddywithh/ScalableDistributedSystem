import Model.LiftRide;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import java.util.ArrayList;
import java.util.List;

public class Consumer {
    private final static String QUEUE_NAME = "LIFTRIDE";
    private final static Integer NUM_THREAD = 100;
    private final static Integer BATCH_SIZE = 100;
    private final static Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final static List<LiftRide> messageBuffer = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) throws IOException, TimeoutException {
        DynamoDbClient client = DynamoDBClient.dynamoDbClient;
        System.out.println("Connected to RabbitMQ!");
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("54.188.67.25");
        factory.setUsername("new_user");
        factory.setPassword("new_password");

        Connection connection = factory.newConnection();

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            synchronized (messageBuffer) {
                if (!messageBuffer.isEmpty()) {
                    List<LiftRide> batch = new ArrayList<>(messageBuffer);
                    messageBuffer.clear();
                    new Thread(() -> DynamoDBClient.insertLiftRidesBatch(batch)).start();
                }
            }
        }, 500, 500, TimeUnit.MILLISECONDS);

        Runnable runnable = () -> {
            try {
                Channel channel = connection.createChannel();
                channel.queueDeclare(QUEUE_NAME, false, false, false, null);
                channel.basicQos(1000);

                DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                    String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                    LiftRide liftRide = gson.fromJson(message, LiftRide.class);

                    synchronized (messageBuffer) {
                        messageBuffer.add(liftRide);
                        if (messageBuffer.size() >= BATCH_SIZE) {
                            List<LiftRide> batch = new ArrayList<>(messageBuffer);
                            messageBuffer.clear();
                            new Thread(() -> DynamoDBClient.insertLiftRidesBatch(batch)).start();
                        }
                    }

                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                };

                channel.basicConsume(QUEUE_NAME, false, deliverCallback, consumerTag -> {
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        };

        for (int i = 0; i < NUM_THREAD; i++) {
            Thread thread = new Thread(runnable);
            thread.start();
        }
    }
}


