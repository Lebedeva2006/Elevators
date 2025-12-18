import java.util.*;
import java.util.concurrent.*;

public class Dispatcher extends Thread {
    private final List<Elevator> elevators;
    private final BlockingQueue<PassengerRequest> requestQueue;
    private final int maxFloors;
    private volatile boolean isRunning;
    private int totalRequestsAssigned;
    private int[] requestCountPerElevator;

    public Dispatcher() {
        this(4, 10);
    }

    public Dispatcher(int numberOfElevators, int maxFloors) {
        this.maxFloors = maxFloors;
        this.elevators = new CopyOnWriteArrayList<>();
        this.requestQueue = new LinkedBlockingQueue<>();
        this.isRunning = true;
        this.totalRequestsAssigned = 0;
        this.requestCountPerElevator = new int[numberOfElevators + 1];

        for (int i = 1; i <= numberOfElevators; i++) {
            Elevator elevator = new Elevator(i, maxFloors, 1);
            elevators.add(elevator);
        }

        this.setName("Dispatcher");
    }

    @Override
    public void run() {
        LoggerUtil.logDispatcherAction("Started", "Elevators: " + elevators.size());
        LoggerUtil.printHeader("EVENT LOG");

        for (Elevator elevator : elevators) {
            elevator.start();
        }

        while (isRunning || !requestQueue.isEmpty()) {
            try {
                PassengerRequest request = requestQueue.poll(1, TimeUnit.SECONDS);

                if (request != null) {
                    LoggerUtil.logDispatcherAction("Request received", request.getShortInfo());
                    TimeUnit.MILLISECONDS.sleep(50);

                    processRequest(request);
                    totalRequestsAssigned++;
                }

            } catch (InterruptedException e) {
                break;
            }
        }

        waitForElevatorsToComplete();
        stopAllElevators();
    }

    private void processRequest(PassengerRequest request) {
        Elevator selectedElevator = selectBestElevator(request);

        if (selectedElevator != null) {
            int elevatorId = selectedElevator.getElevatorId();
            requestCountPerElevator[elevatorId]++;

            LoggerUtil.logDispatcherAction("Assignment",
                    String.format("Request #%d: %d->%d %s -> Elevator %d",
                            request.getRequestId(),
                            request.getFloor(),
                            request.getTargetFloor(),
                            request.getDirection(),
                            elevatorId));

            selectedElevator.addPassengerRequest(
                    request.getFloor(),
                    request.getDirection(),
                    request.getTargetFloor()
            );

            printLoadDistribution();

        } else {
            LoggerUtil.logDispatcherAction("Error", "No suitable elevator");
        }
    }

    private Elevator selectBestElevator(PassengerRequest request) {
        Elevator bestElevator = null;
        int bestScore = Integer.MIN_VALUE;

        for (Elevator elevator : elevators) {
            int score = calculateElevatorScore(elevator, request);
            if (score > bestScore) {
                bestScore = score;
                bestElevator = elevator;
            }
        }

        return bestElevator;
    }

    private int calculateElevatorScore(Elevator elevator, PassengerRequest request) {
        int score = 0;
        int currentFloor = elevator.getCurrentFloor();
        Direction elevatorDirection = elevator.getDirection();
        Direction requestDirection = request.getDirection();
        int requestFloor = request.getFloor();

        if (elevator.getPassengerCount() >= elevator.getMaxCapacity()) {
            return Integer.MIN_VALUE;
        }

        if (currentFloor == requestFloor && elevator.isIdle()) {
            return 1000;
        }

        if (elevator.isIdle()) {
            score += 500;

            int distance = Math.abs(currentFloor - requestFloor);
            score -= distance * 10;
            return score;
        }

        if (elevatorDirection == requestDirection) {
            score += 300;

            if (elevatorDirection == Direction.UP && currentFloor <= requestFloor) {
                score += 200;
            } else if (elevatorDirection == Direction.DOWN && currentFloor >= requestFloor) {
                score += 200;
            }
        } else {
            score -= 100;
        }

        score -= elevator.getPassengerCount() * 5;

        int load = requestCountPerElevator[elevator.getElevatorId()];
        score -= load * 3;

        return score;
    }

    private void waitForElevatorsToComplete() {
        System.out.println("\n[DISPATCHER] Finishing processing...");

        boolean allIdle = false;
        int waitCycles = 0;
        final int MAX_WAIT_CYCLES = 15;

        while (!allIdle && waitCycles < MAX_WAIT_CYCLES) {
            allIdle = true;

            for (Elevator elevator : elevators) {
                if (!elevator.isIdle()) {
                    allIdle = false;
                    break;
                }
            }

            if (!allIdle) {
                try {
                    if (waitCycles % 3 == 0) {
                        System.out.println("\n[DISPATCHER] Waiting for elevators to finish...");
                        for (Elevator elevator : elevators) {
                            if (!elevator.isIdle()) {
                                System.out.printf("   Elevator %d: floor %d, passengers=%d, targets=%d%n",
                                        elevator.getElevatorId(),
                                        elevator.getCurrentFloor(),
                                        elevator.getPassengerCount(),
                                        elevator.getRequestCount());
                            }
                        }
                    }
                    TimeUnit.SECONDS.sleep(2);
                    waitCycles++;
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        if (allIdle) {
            System.out.println("\n[DISPATCHER] All elevators finished work");
        } else {
            System.out.println("\n[DISPATCHER] WARNING: Not all elevators finished in time");
        }
    }

    private void stopAllElevators() {
        for (Elevator elevator : elevators) {
            elevator.stopElevator();
        }

        for (Elevator elevator : elevators) {
            try {
                elevator.join(3000);
                if (elevator.isAlive()) {
                    System.out.println("[DISPATCHER] WARNING: Elevator " + elevator.getElevatorId() + " did not finish in time");
                }
            } catch (InterruptedException e) {
                System.out.println("[DISPATCHER] WARNING: Could not wait for elevator " + elevator.getElevatorId());
            }
        }

        LoggerUtil.logDispatcherAction("Stopped", "Requests assigned: " + totalRequestsAssigned);
        LoggerUtil.printFooter();
    }

    public void addRequest(PassengerRequest request) {
        if (isRunning) {
            requestQueue.offer(request);
        }
    }

    private void printLoadDistribution() {
        System.out.print("[DISPATCHER] Load distribution: ");
        for (int i = 1; i < requestCountPerElevator.length; i++) {
            System.out.printf("E%d:%d ", i, requestCountPerElevator[i]);
        }
        System.out.println();
    }

    public void stopDispatcher() {
        if (!isRunning) {
            return;
        }

        System.out.println("\n[DISPATCHER] Stop command received...");
        isRunning = false;
        this.interrupt();
    }

    public List<Elevator> getElevators() {
        return elevators;
    }

    public int getTotalRequestsAssigned() {
        return totalRequestsAssigned;
    }

    public void printStatus() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("                       CURRENT ELEVATOR STATUS");
        System.out.println("=".repeat(80));

        System.out.printf("%-10s %-10s %-12s %-15s %-15s %-15s%n",
                "Elevator", "Floor", "Direction", "Status", "Passengers", "Targets in queue");
        System.out.println("-".repeat(80));

        for (Elevator elevator : elevators) {
            System.out.printf("%-10d %-10d %-12s %-15s %-15d %-15d%n",
                    elevator.getElevatorId(),
                    elevator.getCurrentFloor(),
                    elevator.getDirection().toString(),
                    elevator.getStatus().toString(),
                    elevator.getPassengerCount(),
                    elevator.getRequestCount());
        }

        System.out.println("=".repeat(80));
        System.out.printf("Dispatcher: processed %d requests, in queue: %d%n",
                totalRequestsAssigned, requestQueue.size());
        printLoadDistribution();
    }

    public void printFinalStatistics() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("                         FINAL STATISTICS");
        System.out.println("=".repeat(80));

        System.out.printf("%n%-10s %-12s %-15s %-15s %-15s %-15s%n",
                "Elevator", "Requests", "Movement time", "Door time", "Total time", "Efficiency");
        System.out.println("-".repeat(80));

        for (Elevator elevator : elevators) {
            int elevatorId = elevator.getElevatorId();
            long totalTime = elevator.getTotalMovementTime() + elevator.getTotalDoorTime();
            double efficiency = totalTime > 0 ?
                    (double) requestCountPerElevator[elevatorId] / (totalTime / 1000.0) * 60 : 0;

            System.out.printf("%-10d %-12d %-15.1f %-15.1f %-15.1f %-15.1f%n",
                    elevatorId,
                    requestCountPerElevator[elevatorId],
                    elevator.getTotalMovementTime() / 1000.0,
                    elevator.getTotalDoorTime() / 1000.0,
                    totalTime / 1000.0,
                    efficiency);
        }

        System.out.println("\n" + "=".repeat(80));
        System.out.println("                     LOAD BALANCING ANALYSIS");
        System.out.println("=".repeat(80));

        int totalRequests = 0;
        int maxRequests = 0;
        int minRequests = Integer.MAX_VALUE;

        for (int i = 1; i < requestCountPerElevator.length; i++) {
            int requests = requestCountPerElevator[i];
            totalRequests += requests;
            maxRequests = Math.max(maxRequests, requests);
            minRequests = Math.min(minRequests, requests);
        }

        double avgRequests = (double) totalRequests / (requestCountPerElevator.length - 1);
        double imbalance = maxRequests - minRequests;
        double imbalancePercent = avgRequests > 0 ? (imbalance / avgRequests) * 100 : 0;

        System.out.printf("Total requests: %d%n", totalRequests);
        System.out.printf("Average per elevator: %.1f%n", avgRequests);
        System.out.printf("Maximum: %d requests%n", maxRequests);
        System.out.printf("Minimum: %d requests%n", minRequests);
        System.out.printf("Imbalance: %.1f requests (%.1f%%)%n", imbalance, imbalancePercent);

        if (minRequests == 0) {
            System.out.println("PROBLEM: There is an elevator that did not receive any requests!");
        } else if (imbalancePercent > 50) {
            System.out.println("WARNING: Strong load imbalance between elevators!");
        } else if (imbalancePercent > 20) {
            System.out.println("WARNING: Moderate load imbalance");
        } else {
            System.out.println("Good load balancing");
        }

        System.out.println("=".repeat(80));
    }
}