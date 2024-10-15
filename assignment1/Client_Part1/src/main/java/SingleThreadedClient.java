import cs6650.Model.EventGenerator;
import cs6650.Model.LiftRideEvent;

import java.util.concurrent.*;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public class SingleThreadedClient {
    private static final int TOTAL_REQUESTS = 10000; // 发送 10,000 个请求
    private BlockingQueue<LiftRideEvent> eventQueue;
    private AtomicInteger successfulRequests = new AtomicInteger(0);
    private AtomicInteger failedRequests = new AtomicInteger(0);

    public SingleThreadedClient() {
        eventQueue = new LinkedBlockingQueue<>();
    }

    public void start() throws InterruptedException {
        // 记录开始时间
        long startTime = System.currentTimeMillis();

        // 创建 EventGenerator 线程生成数据
        EventGenerator generator = new EventGenerator(eventQueue, TOTAL_REQUESTS);
        new Thread(generator).start();

        // 单线程发送请求
        ServerClient client = new ServerClient();
        for (int i = 0; i < TOTAL_REQUESTS; i++) {
            try {
                LiftRideEvent event = eventQueue.take();
                boolean sent = client.sendLiftRideEvent(event);

                if (sent) {
                    successfulRequests.incrementAndGet();
                } else {
                    failedRequests.incrementAndGet();
                }
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }

        // 记录结束时间
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        // 打印结果
        System.out.println("Single thread completed.");
        System.out.println("Successful requests: " + successfulRequests.get());
        System.out.println("Failed requests: " + failedRequests.get());
        System.out.println("Total run time (ms): " + totalTime);

        // 计算吞吐量
        double throughput = (successfulRequests.get() / (totalTime / 1000.0));
        System.out.println("Throughput (requests/second): " + throughput);
    }

    public static void main(String[] args) throws InterruptedException {
        SingleThreadedClient client = new SingleThreadedClient();
        client.start();
    }
}

