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
    private final static String QUEUE_NAME = "LiftRide";

    public ChannelPool() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("54.188.67.25");
//        factory.setHost("localhost");
        try {
            this.connection = factory.newConnection();
        } catch (IOException | TimeoutException e) {
            System.err.println("Something Wrong with Connection");
            e.printStackTrace();
        }

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
}
