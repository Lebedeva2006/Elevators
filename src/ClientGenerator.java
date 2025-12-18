import java.util.Random;
import java.util.concurrent.TimeUnit;

public class ClientGenerator implements Runnable {
    private final Dispatcher dispatcher;
    private final int maxFloors;
    private final Random random;
    private volatile boolean isRunning;
    private final int maxRequests;
    private int generatedCount;

    public ClientGenerator(Dispatcher dispatcher, int maxFloors, int maxRequests) {
        this.dispatcher = dispatcher;
        this.maxFloors = maxFloors;
        this.random = new Random();
        this.isRunning = true;
        this.maxRequests = maxRequests;
        this.generatedCount = 0;
    }

    @Override
    public void run() {
        LoggerUtil.logSystemInfo("Request generator started (max: " + maxRequests + ")");

        while (isRunning && generatedCount < maxRequests) {
            try {
                int floor = random.nextInt(maxFloors) + 1;
                int targetFloor;

                do {
                    targetFloor = random.nextInt(maxFloors) + 1;
                } while (targetFloor == floor);

                Direction direction = targetFloor > floor ? Direction.UP : Direction.DOWN;

                PassengerRequest request = new PassengerRequest(floor, direction, targetFloor);
                dispatcher.addRequest(request);

                generatedCount++;

                System.out.println("[GENERATOR] Created request #" + generatedCount +
                        ": " + floor + "->" + targetFloor + " " + direction);

                int delay = 1500 + random.nextInt(2500);
                TimeUnit.MILLISECONDS.sleep(delay);

            } catch (InterruptedException e) {
                System.out.println("[GENERATOR] Stop command received...");
                break;
            }
        }

        LoggerUtil.logSystemInfo("Generator stopped. Created " + generatedCount + " requests.");
        System.out.println("[GENERATOR] Work completed. Total requests: " + generatedCount);
    }

    public void stopGenerator() {
        isRunning = false;
        System.out.println("[GENERATOR] Stop command sent");
    }

    public int getGeneratedCount() {
        return generatedCount;
    }
}