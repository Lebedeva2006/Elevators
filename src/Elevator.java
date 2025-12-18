import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

public class Elevator extends Thread {
    public static final int MOVEMENT_TIME_PER_FLOOR = 800;
    public static final int DOOR_OPERATION_TIME = 2000;

    private final int id;
    private final int maxFloors;
    private final ReentrantLock lock = new ReentrantLock();

    private final Set<Integer> targetFloors = ConcurrentHashMap.newKeySet();
    private final Map<Integer, List<Integer>> floorDestinations = new ConcurrentHashMap<>();
    private final Map<Integer, Direction> floorDirections = new ConcurrentHashMap<>();

    private int currentFloor;
    private Direction direction;
    private ElevatorStatus status;
    private volatile boolean isRunning;
    private int passengerCount;
    private final int maxCapacity = 10;
    private int totalRequestsProcessed;
    private long totalMovementTime;
    private long totalDoorTime;

    public Elevator() {
        this(0, 10, 1);
    }

    public Elevator(int id, int maxFloors, int startFloor) {
        this.id = id;
        this.maxFloors = maxFloors;
        this.currentFloor = startFloor;
        this.direction = Direction.IDLE;
        this.status = ElevatorStatus.STOPPED;
        this.isRunning = true;
        this.passengerCount = 0;
        this.totalRequestsProcessed = 0;
        this.totalMovementTime = 0;
        this.totalDoorTime = 0;
        this.setName("Elevator-" + id);
    }

    @Override
    public void run() {
        LoggerUtil.logElevatorAction(id, "Started", "Floor " + currentFloor);

        while (isRunning || !targetFloors.isEmpty() || passengerCount > 0) {
            try {
                lock.lock();
                if (shouldStopAtCurrentFloor()) {
                    processStop();
                }
                Integer nextTarget = getOptimizedNextTarget();

                if (nextTarget != null) {
                    moveToTarget(nextTarget);
                } else {
                    direction = Direction.IDLE;
                    status = ElevatorStatus.STOPPED;
                }

                lock.unlock();
                TimeUnit.MILLISECONDS.sleep(100);

            } catch (InterruptedException e) {
                if (!isRunning && targetFloors.isEmpty() && passengerCount == 0) {
                    break;
                }
            }
        }

        finalizeWork();
    }

    private void moveToTarget(int targetFloor) throws InterruptedException {
        if (targetFloor == currentFloor) return;

        long startTime = System.currentTimeMillis();
        status = ElevatorStatus.MOVING;

        if (targetFloor > currentFloor) {
            direction = Direction.UP;

            while (currentFloor < targetFloor && (isRunning || !targetFloors.isEmpty() || passengerCount > 0)) {
                int fromFloor = currentFloor;
                currentFloor++;
                if (shouldStopAtCurrentFloor()) {
                    LoggerUtil.logElevatorAction(id, "Moving",
                            String.format("Floor %d -> %d UP", fromFloor, currentFloor));
                    break;
                }

                LoggerUtil.logElevatorAction(id, "Moving",
                        String.format("Floor %d -> %d UP", fromFloor, currentFloor));

                TimeUnit.MILLISECONDS.sleep(MOVEMENT_TIME_PER_FLOOR);
            }

        } else {
            direction = Direction.DOWN;

            while (currentFloor > targetFloor && (isRunning || !targetFloors.isEmpty() || passengerCount > 0)) {
                int fromFloor = currentFloor;
                currentFloor--;

                if (shouldStopAtCurrentFloor()) {
                    LoggerUtil.logElevatorAction(id, "Moving",
                            String.format("Floor %d -> %d DOWN", fromFloor, currentFloor));
                    break;
                }

                LoggerUtil.logElevatorAction(id, "Moving",
                        String.format("Floor %d -> %d DOWN", fromFloor, currentFloor));

                TimeUnit.MILLISECONDS.sleep(MOVEMENT_TIME_PER_FLOOR);
            }
        }

        totalMovementTime += (System.currentTimeMillis() - startTime);
    }

    private Integer getOptimizedNextTarget() {
        if (targetFloors.isEmpty()) {
            return null;
        }
        if (direction == Direction.UP) {
            Integer closestAbove = null;
            int minDistance = Integer.MAX_VALUE;

            for (Integer floor : targetFloors) {
                if (floor >= currentFloor) {
                    int distance = floor - currentFloor;
                    if (distance < minDistance) {
                        minDistance = distance;
                        closestAbove = floor;
                    }
                }
            }

            if (closestAbove != null) {
                return closestAbove;
            }
            return Collections.min(targetFloors);

        } else if (direction == Direction.DOWN) {
            Integer closestBelow = null;
            int minDistance = Integer.MAX_VALUE;

            for (Integer floor : targetFloors) {
                if (floor <= currentFloor) {
                    int distance = currentFloor - floor;
                    if (distance < minDistance) {
                        minDistance = distance;
                        closestBelow = floor;
                    }
                }
            }

            if (closestBelow != null) {
                return closestBelow;
            }
            return Collections.max(targetFloors);

        } else {
            Integer closest = null;
            int minDistance = Integer.MAX_VALUE;

            for (Integer floor : targetFloors) {
                int distance = Math.abs(currentFloor - floor);
                if (distance < minDistance) {
                    minDistance = distance;
                    closest = floor;
                }
            }

            if (closest != null) {
                direction = (closest > currentFloor) ? Direction.UP : Direction.DOWN;
            }

            return closest;
        }
    }

    private boolean shouldStopAtCurrentFloor() {
        return targetFloors.contains(currentFloor);
    }

    private void processStop() throws InterruptedException {
        long startTime = System.currentTimeMillis();

        LoggerUtil.logElevatorAction(id, "Arrived", "Floor " + currentFloor);
        status = ElevatorStatus.DOORS_OPENING;
        LoggerUtil.logElevatorAction(id, "Doors", "Opening");
        TimeUnit.MILLISECONDS.sleep(DOOR_OPERATION_TIME / 2);

        status = ElevatorStatus.DOORS_OPEN;
        LoggerUtil.logElevatorAction(id, "Doors", "Open");
        if (floorDestinations.containsKey(currentFloor)) {
            List<Integer> destinations = floorDestinations.get(currentFloor);
            int exitingCount = destinations.size();

            if (exitingCount > 0 && passengerCount >= exitingCount) {
                passengerCount -= exitingCount;
                totalRequestsProcessed += exitingCount;
                LoggerUtil.logElevatorAction(id, "Exit", exitingCount + " passengers exited");
                floorDestinations.remove(currentFloor);
            }
        }
        if (floorDirections.containsKey(currentFloor)) {
            Direction callDirection = floorDirections.get(currentFloor);
            if (direction == Direction.IDLE || direction == callDirection) {
                int availableSpace = maxCapacity - passengerCount;

                if (availableSpace > 0) {
                    int boardingCount = Math.min(availableSpace, 1);
                    passengerCount += boardingCount;
                    LoggerUtil.logElevatorAction(id, "Enter", boardingCount + " passengers entered");
                }
            }
        }
        targetFloors.remove(currentFloor);
        floorDirections.remove(currentFloor);
        if (passengerCount > 0) {
            TimeUnit.MILLISECONDS.sleep(1000);
            LoggerUtil.logElevatorAction(id, "Passengers", "Total: " + passengerCount);
        }
        status = ElevatorStatus.DOORS_CLOSING;
        LoggerUtil.logElevatorAction(id, "Doors", "Closing");
        TimeUnit.MILLISECONDS.sleep(DOOR_OPERATION_TIME / 2);

        status = ElevatorStatus.MOVING;
        LoggerUtil.logElevatorAction(id, "Doors", "Closed");

        totalDoorTime += (System.currentTimeMillis() - startTime);
    }

    private void finalizeWork() {
        if (passengerCount > 0) {
            LoggerUtil.logElevatorAction(id, "Final exit",
                    passengerCount + " passengers exited");
            passengerCount = 0;
            totalRequestsProcessed += passengerCount;
        }
        if (currentFloor != 1) {
            LoggerUtil.logElevatorAction(id, "Return", "To floor 1");
            currentFloor = 1;
        }

        status = ElevatorStatus.STOPPED;
        direction = Direction.IDLE;

        LoggerUtil.logElevatorAction(id, "Stopped", "Floor " + currentFloor);
        logStatistics();
    }

    private void logStatistics() {
        long totalTime = totalMovementTime + totalDoorTime;
        LoggerUtil.logElevatorAction(id, "Statistics",
                String.format("Requests: %d", totalRequestsProcessed));
        LoggerUtil.logElevatorAction(id, "Statistics",
                String.format("Time: %.1fs", totalTime / 1000.0));
    }

    public void addPassengerRequest(int callFloor, Direction callDir, int targetFloor) {
        lock.lock();
        try {
            if (callFloor < 1 || callFloor > maxFloors ||
                    targetFloor < 1 || targetFloor > maxFloors) {
                return;
            }
            targetFloors.add(callFloor);
            floorDirections.put(callFloor, callDir);
            targetFloors.add(targetFloor);
            floorDestinations.computeIfAbsent(targetFloor, k -> new ArrayList<>())
                    .add(targetFloor);
            floorDirections.put(targetFloor, Direction.IDLE);

            LoggerUtil.logElevatorAction(id, "Request",
                    String.format("Pickup from %d (%s) -> deliver to %d",
                            callFloor, callDir, targetFloor));

        } finally {
            lock.unlock();
        }
    }

    public int getElevatorId() { return id; }

    public int getCurrentFloor() {
        lock.lock();
        try { return currentFloor; }
        finally { lock.unlock(); }
    }

    public Direction getDirection() {
        lock.lock();
        try { return direction; }
        finally { lock.unlock(); }
    }

    public ElevatorStatus getStatus() {
        lock.lock();
        try { return status; }
        finally { lock.unlock(); }
    }

    public int getPassengerCount() {
        lock.lock();
        try { return passengerCount; }
        finally { lock.unlock(); }
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public boolean isIdle() {
        lock.lock();
        try {
            return direction == Direction.IDLE &&
                    targetFloors.isEmpty() &&
                    passengerCount == 0 &&
                    floorDestinations.isEmpty();
        } finally { lock.unlock(); }
    }

    public int getTotalRequests() { return totalRequestsProcessed; }

    public long getTotalMovementTime() { return totalMovementTime; }

    public long getTotalDoorTime() { return totalDoorTime; }

    public int getRequestCount() {
        lock.lock();
        try { return targetFloors.size(); }
        finally { lock.unlock(); }
    }

    public Set<Integer> getTargetFloorsCopy() {
        lock.lock();
        try {
            return new HashSet<>(targetFloors);
        } finally {
            lock.unlock();
        }
    }

    public void stopElevator() {
        isRunning = false;
        this.interrupt();
    }
}