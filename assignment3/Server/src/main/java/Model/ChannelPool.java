package Model;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeoutException;

public class ChannelPool {
    private Connection connection;
    private BlockingQueue<Channel> pool;
    private final static int capacity = 100;
    private final static String QUEUE_NAME = "LIFTRIDE";

    public ChannelPool() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
//        factory.setHost("localhost");
        factory.setHost("54.188.67.25");
        factory.setUsername("new_user");
        factory.setPassword("new_password");

        this.connection = factory.newConnection();
        System.out.println("Connected to RabbitMQ");

        this.pool = new LinkedBlockingQueue<>();
        for (int i = 0; i < capacity; i++) {
            try {
                Channel channel = this.connection.createChannel();
                channel.queueDeclare(QUEUE_NAME, false, false, false, null);
                this.pool.add(channel);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public Channel takeChannel() throws InterruptedException {
        return this.pool.take();
    }

    public void add(Channel channel) {
        this.pool.offer(channel);
    }

    public void close() {
        for (Channel channel : pool) {
            try {
                if (channel != null && channel.isOpen()) {
                    channel.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
            if (connection != null && connection.isOpen()) {
                connection.close();
                System.out.println("Connection to RabbitMQ closed");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    public static void main(String[] args) {
//        try {
//            ChannelPool channelPool = new ChannelPool();
//            System.out.println("ChannelPool connected successfully.");
//        } catch (Exception e) {
//            System.err.println("ChannelPool failed to connect.");
//            e.printStackTrace();
//        }
//    }
}
