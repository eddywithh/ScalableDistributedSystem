import cs6650.Model.EventGenerator;
import cs6650.Model.LiftRideEvent;

import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MultithreadedClient {
    private static final int INITIAL_THREADS = 32; // 初始启动32个线程
    private static final int TOTAL_REQUESTS = 200000; // 总请求数
    private static final int REQUESTS_PER_THREAD = 1000; // 每个线程发送1000个请求

    private BlockingQueue<LiftRideEvent> eventQueue;
    private AtomicInteger successfulRequests = new AtomicInteger(0);
    private AtomicInteger failedRequests = new AtomicInteger(0);

    public MultithreadedClient() {
        eventQueue = new LinkedBlockingQueue<>();
    }

    public void start() throws InterruptedException {
        long startTime = System.currentTimeMillis();

        // 创建线程池，初始32个线程
        ExecutorService executorService = Executors.newFixedThreadPool(INITIAL_THREADS);

        // 启动EventGenerator生成数据
        EventGenerator generator = new EventGenerator(eventQueue, TOTAL_REQUESTS);
        new Thread(generator).start();

        // 使用 CountDownLatch 来等待初始 32 个线程完成
        CountDownLatch latch = new CountDownLatch(INITIAL_THREADS);

        // 启动32个线程，每个线程发送1000个请求
        for (int i = 0; i < INITIAL_THREADS; i++) {
            executorService.submit(new EventSender(latch, REQUESTS_PER_THREAD));
        }

        // 等待32个线程完成
        latch.await();
        System.out.println("32 threads completed. Total requests sent: " + INITIAL_THREADS * REQUESTS_PER_THREAD);
        int remainingRequests = TOTAL_REQUESTS - successfulRequests.get() - failedRequests.get();
        System.out.println("Left requests: " + remainingRequests);
        System.out.println("Please wait for remaining processing...");
        System.out.println("*********************************************************");

        while (remainingRequests > 0) {
            // 每次启动32个线程
            CountDownLatch remainingLatch = new CountDownLatch(INITIAL_THREADS);
            int requestsToSend = Math.min(remainingRequests, INITIAL_THREADS * REQUESTS_PER_THREAD);

            // 启动32个线程
            for (int i = 0; i < INITIAL_THREADS; i++) {
                int requestsForThisThread = Math.min(REQUESTS_PER_THREAD, remainingRequests);
                remainingRequests -= requestsForThisThread;
                executorService.submit(new EventSender(remainingLatch, requestsForThisThread));
            }

            // 等待这32个线程完成
            remainingLatch.await();
        }

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        System.out.println("All threads completed.");
        System.out.println("Successful requests: " + successfulRequests.get());
        System.out.println("Failed requests: " + failedRequests.get());
        System.out.println("Total run time (ms): " + totalTime);

        // 吞吐量：成功请求数 / 运行时间（秒）
        double throughput = (successfulRequests.get() / (totalTime / 1000.0));
        System.out.println("Throughput (requests/second): " + throughput);

        executorService.shutdown();
    }

    // EventSender 类，发送请求
    private class EventSender implements Runnable {
        private CountDownLatch latch;
        private int requestsToSend;

        public EventSender(CountDownLatch latch, int requestsToSend) {
            this.latch = latch;
            this.requestsToSend = requestsToSend;
        }

        @Override
        public void run() {
            ServerClient client = new ServerClient();
            for (int i = 0; i < requestsToSend; i++) {
                try {
                    LiftRideEvent event = eventQueue.take();
                    if (event.getResortId()==11) {
                        break;
                    }
                    int attempts = 0;
                    boolean sent = false;
                    while (attempts < 5 && !sent) {
                        sent = client.sendLiftRideEvent(event);
                        attempts++;
                    }

                    // 成功计数
                    if (sent) {
                        synchronized (MultithreadedClient.this) {
                            successfulRequests.getAndIncrement();
                        }
                    } else {
                        synchronized (MultithreadedClient.this) {
                            failedRequests.getAndIncrement();
                        }
                    }
                } catch (InterruptedException | IOException e) {
                    e.printStackTrace();
                    synchronized (MultithreadedClient.this) {
                        failedRequests.getAndIncrement();
                    }
                }
            }
            // 当前线程完成，减少latch的计数
            latch.countDown();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        MultithreadedClient client = new MultithreadedClient();
        client.start();
    }
}
