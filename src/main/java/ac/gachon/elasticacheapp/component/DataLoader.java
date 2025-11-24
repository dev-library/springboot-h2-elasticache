package ac.gachon.elasticacheapp.component;

import ac.gachon.elasticacheapp.entity.Order;
import ac.gachon.elasticacheapp.entity.User;
import ac.gachon.elasticacheapp.repository.OrderRepository;
import ac.gachon.elasticacheapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepo;
    private final OrderRepository orderRepo;

    private static final int THREAD_COUNT = 10; // 스레드 개수
    private static final int TOTAL_USERS = 100_000;
    private static final int USERS_PER_THREAD = TOTAL_USERS / THREAD_COUNT; // 각 스레드가 처리할 User 수

    @Override
    public void run(String... args) throws Exception {
        long start = System.currentTimeMillis();

        // 1단계: User 먼저 모두 생성 (멀티스레드)
        ExecutorService userExecutor = Executors.newFixedThreadPool(THREAD_COUNT);
        
        for (int threadId = 0; threadId < THREAD_COUNT; threadId++) {
            final int id = threadId;
            userExecutor.submit(() -> {
                long threadStart = (long) id * USERS_PER_THREAD + 1;
                long threadEnd = threadStart + USERS_PER_THREAD;
                createUsers(threadStart, threadEnd, id + 1);
            });
        }
        
        userExecutor.shutdown();
        userExecutor.awaitTermination(5, TimeUnit.MINUTES);
        
        long userEnd = System.currentTimeMillis();
        System.out.println(">>> User 10만 건 삽입 완료! (" + (userEnd - start) + " ms)");

        // 2단계: Order 생성 (멀티스레드)
        ExecutorService orderExecutor = Executors.newFixedThreadPool(THREAD_COUNT);
        
        for (int threadId = 0; threadId < THREAD_COUNT; threadId++) {
            final int id = threadId;
            orderExecutor.submit(() -> {
                long threadStart = (long) id * USERS_PER_THREAD + 1;
                long threadEnd = threadStart + USERS_PER_THREAD;
                createOrders(threadStart, threadEnd, id + 1);
            });
        }
        
        orderExecutor.shutdown();
        orderExecutor.awaitTermination(5, TimeUnit.MINUTES);

        long end = System.currentTimeMillis();
        System.out.println(">>> 총 데이터 생성 완료! (" + (end - start) + " ms)");
        System.out.println(">>> User: " + userRepo.count() + " 건, Order: " + orderRepo.count() + " 건");
    }

    private void createUsers(long startId, long endId, int threadNum) {
        List<User> userBatch = new ArrayList<>();

        for (long userId = startId; userId < endId; userId++) {
            User user = new User(userId, "User" + userId, (int) (20 + (userId % 30)));
            userBatch.add(user);

            // 배치 크기가 1000이 되면 저장
            if (userBatch.size() >= 1000) {
                userRepo.saveAll(userBatch);
                userBatch.clear();
            }
        }

        // 남은 데이터 저장
        if (!userBatch.isEmpty()) {
            userRepo.saveAll(userBatch);
        }

        System.out.println(">>> User Thread " + threadNum + " 완료 (User: " + startId + " ~ " + (endId - 1) + ")");
    }

    private void createOrders(long startId, long endId, int threadNum) {
        Random random = new Random();
        String[] products = {"Laptop", "Mouse", "Keyboard", "Monitor", "Headset", 
                             "Webcam", "SSD", "RAM", "GPU", "CPU"};

        List<Order> orderBatch = new ArrayList<>();

        for (long userId = startId; userId < endId; userId++) {
            User user = userRepo.findById(userId).orElseThrow();
            int orderCount = 10 + random.nextInt(6); // 10-15개

            for (int j = 0; j < orderCount; j++) {
                String product = products[random.nextInt(products.length)];
                int price = 10000 + random.nextInt(90000);
                LocalDateTime orderDate = LocalDateTime.now().minusDays(random.nextInt(365));
                
                Order order = new Order(user, product, price, orderDate);
                orderBatch.add(order);

                // 배치 크기가 5000이 되면 저장
                if (orderBatch.size() >= 5000) {
                    orderRepo.saveAll(orderBatch);
                    orderBatch.clear();
                }
            }
        }

        // 남은 데이터 저장
        if (!orderBatch.isEmpty()) {
            orderRepo.saveAll(orderBatch);
        }

        System.out.println(">>> Order Thread " + threadNum + " 완료 (User: " + startId + " ~ " + (endId - 1) + ")");
    }
}