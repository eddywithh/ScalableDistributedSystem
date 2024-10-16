import cs6650.Model.EventGenerator;
import cs6650.Model.LiftRideEvent;

import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MultithreadedClient {
    private static final int INITIAL_THREADS = 32;
    private static final int TOTAL_REQUESTS = 200000;
    private static final int REQUESTS_PER_THREAD = 1000;

    private BlockingQueue<LiftRideEvent> eventQueue;
    private AtomicInteger successfulRequests = new AtomicInteger(0);
    private AtomicInteger failedRequests = new AtomicInteger(0);

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
        int remainingRequests = TOTAL_REQUESTS - successfulRequests.get() - failedRequests.get();
        System.out.println("Left requests: " + remainingRequests);
        System.out.println("Please wait for remaining processing...");
        System.out.println("*********************************************************");

        while (remainingRequests > 0) {
            CountDownLatch remainingLatch = new CountDownLatch(INITIAL_THREADS);
            int requestsToSend = Math.min(remainingRequests, INITIAL_THREADS * REQUESTS_PER_THREAD);

            for (int i = 0; i < INITIAL_THREADS; i++) {
                int requestsForThisThread = Math.min(REQUESTS_PER_THREAD, remainingRequests);
                remainingRequests -= requestsForThisThread;
                executorService.submit(new EventSender(remainingLatch, requestsForThisThread));
            }
            remainingLatch.await();
        }

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
            latch.countDown();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        MultithreadedClient client = new MultithreadedClient();
        client.start();
    }
}
