import Model.EventGenerator;
import Model.LiftRideEvent;

import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Queue;
import java.util.LinkedList;

public class MultithreadedClient {
    private static final int INITIAL_THREADS = 32;
    private static final int TOTAL_REQUESTS = 200000;
    private static final int REQUESTS_PER_THREAD = TOTAL_REQUESTS / INITIAL_THREADS;

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

        ExecutorService executorService = Executors.newFixedThreadPool(INITIAL_THREADS);

        EventGenerator generator = new EventGenerator(eventQueue, TOTAL_REQUESTS);
        new Thread(generator).start();

        CountDownLatch latch = new CountDownLatch(INITIAL_THREADS);

        for (int i = 0; i < INITIAL_THREADS; i++) {
            executorService.submit(new EventSender(latch, REQUESTS_PER_THREAD));
        }

        latch.await();
        System.out.println("32 threads completed. Total requests sent: " + INITIAL_THREADS * REQUESTS_PER_THREAD);

        EventSender.calculateStats();

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        System.out.println("All threads completed.");
        System.out.println("Successful requests: " + successfulRequests.get());
        System.out.println("Failed requests: " + failedRequests.get());
        System.out.println("Total run time (ms): " + totalTime);

        double throughput = (successfulRequests.get() / (totalTime / 1000.0));
        System.out.println("Throughput (requests/second): " + throughput);

        executorService.shutdown();
    }

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
                    long startTime = System.currentTimeMillis();
                    while (attempts < 5 && !sent) {
                        sent = client.sendLiftRideEvent(event);
                        attempts++;
                    }
                    long endTime = System.currentTimeMillis();
                    long latency = endTime - startTime;

                    try (FileWriter writer = new FileWriter("request_logs.csv", true)) {
                        writer.append(String.format("%d, POST, %d, %d\n", startTime, latency, 200)); // 假设所有响应为200
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    if (sent) {
                        synchronized (MultithreadedClient.this) {
                            successfulRequests.getAndIncrement();
                        }
                    } else {
                        synchronized (MultithreadedClient.this) {
                            failedRequests.getAndIncrement();
                        }
                    }

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
            latch.countDown();
        }

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
