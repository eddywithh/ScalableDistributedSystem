import Model.EventGenerator;
import Model.LiftRideEvent;

import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Queue;
import java.util.LinkedList;

public class MultithreadedClient {
    private static final int INITIAL_THREADS = 32; // 保持线程数为32
    private static final int TOTAL_REQUESTS = 200000; // 总请求数
    private static final int REQUESTS_PER_THREAD = TOTAL_REQUESTS / INITIAL_THREADS; // 每个线程发送的请求数

    private static BlockingQueue<LiftRideEvent> eventQueue;
    private AtomicInteger successfulRequests = new AtomicInteger(0);
    private AtomicInteger failedRequests = new AtomicInteger(0);

    // Latency statistics
    private static Queue<Long> requestLatencies = new LinkedList<>();

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

        // 启动32个线程，每个线程发送6,250个请求
        for (int i = 0; i < INITIAL_THREADS; i++) {
            executorService.submit(new EventSender(latch, REQUESTS_PER_THREAD));
        }

        // 等待32个线程完成
        latch.await();
        System.out.println("32 threads completed. Total requests sent: " + INITIAL_THREADS * REQUESTS_PER_THREAD);

        // 计算和打印统计信息
        EventSender.calculateStats();

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

    // EventSender 类，发送请求并记录延迟
    public class EventSender implements Runnable {
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
                    if (event.getResortId() == 11) {
                        break;
                    }

                    int attempts = 0;
                    boolean sent = false;
                    long startTime = System.currentTimeMillis(); // 记录请求发送时间
                    while (attempts < 5 && !sent) {
                        sent = client.sendLiftRideEvent(event);
                        attempts++;
                    }
                    long endTime = System.currentTimeMillis(); // 记录请求响应时间
                    long latency = endTime - startTime;

                    // 记录延迟和响应状态到 CSV
                    try (FileWriter writer = new FileWriter("request_logs.csv", true)) {
                        writer.append(String.format("%d, POST, %d, %d\n", startTime, latency, 200)); // 假设所有响应为200
                    } catch (IOException e) {
                        e.printStackTrace();
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

                    // 记录延迟到队列中
                    synchronized (MultithreadedClient.class) {
                        requestLatencies.add(latency);
                    }
                } catch (InterruptedException | IOException e) {
                    e.printStackTrace();
                    synchronized (MultithreadedClient.this) {
                        failedRequests.getAndIncrement();
                    }
                }
            }
            latch.countDown(); // 当前线程完成，减少latch的计数
        }

        // 计算延迟的统计信息
        public static void calculateStats() {
            long totalLatency = 0;
            long minLatency = Long.MAX_VALUE;
            long maxLatency = Long.MIN_VALUE;
            synchronized (MultithreadedClient.class) {
                for (long latency : requestLatencies) {
                    totalLatency += latency;
                    if (latency < minLatency) minLatency = latency;
                    if (latency > maxLatency) maxLatency = latency;
                }
            }

            long mean = totalLatency / requestLatencies.size();
            System.out.println("Mean response time: " + mean + " ms");
            System.out.println("Min response time: " + minLatency + " ms");
            System.out.println("Max response time: " + maxLatency + " ms");

            // Calculate p99 (99th percentile)
            long[] latenciesArray = requestLatencies.stream().mapToLong(Long::longValue).toArray();
            java.util.Arrays.sort(latenciesArray);
            long p99 = latenciesArray[(int) (latenciesArray.length * 0.99)];
            System.out.println("P99 response time: " + p99 + " ms");
        }
    }

    public static void main(String[] args) throws InterruptedException {
        MultithreadedClient client = new MultithreadedClient();
        client.start();
    }
}
