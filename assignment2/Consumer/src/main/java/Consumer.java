import Model.LiftRide;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeoutException;

public class Consumer {
    private final static String QUEUE_NAME = "LIFTRIDE";
    private final static Integer NUM_THREAD = 100;
    private final static Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final static ConcurrentHashMap<Integer, CopyOnWriteArrayList<String>> map = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException, TimeoutException {
        System.out.println("Connected to RabbitMQ!");
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("54.188.67.25");
        factory.setUsername("new_user");
        factory.setPassword("new_password");

        // only for localhost test
//        factory.setHost("localhost");

        Connection connection = factory.newConnection();

        Runnable runnable = () -> {
            Channel channel;
            try {
                channel = connection.createChannel();
                channel.queueDeclare(QUEUE_NAME, false, false, false, null);
                DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                    String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                    LiftRide liftRide = gson.fromJson(message, LiftRide.class);
                    System.out.println(" [x] Received '" + liftRide.toString() + "'");

                    Integer skierID = liftRide.getSkierID();
                    String liftID = liftRide.getLiftID();
                    map.putIfAbsent(skierID, new CopyOnWriteArrayList<>());
                    map.get(liftRide.getSkierID()).add(liftID);
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                };
                channel.basicConsume(QUEUE_NAME, false, deliverCallback, consumerTag -> { });
            } catch (IOException e) {
                e.printStackTrace();
            }
        };

        for ( int i = 0; i < NUM_THREAD; i++) {
            Thread thread = new Thread(runnable);
            thread.start();
        }
    }
}
