import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class ElevatorSystem {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("========================================================");
        System.out.println("          ELEVATOR CONTROL SYSTEM   ");
        System.out.println("========================================================");

        System.out.print("\nEnter number of floors in the building: ");
        int maxFloors = scanner.nextInt();

        System.out.print("Enter number of elevators: ");
        int numberOfElevators = scanner.nextInt();

        scanner.nextLine();

        System.out.print("\nNumber of requests to generate (0 - manual input only): ");
        int maxRequests = scanner.nextInt();
        scanner.nextLine();

        Dispatcher dispatcher = new Dispatcher(numberOfElevators, maxFloors);
        ClientGenerator clientGenerator = null;
        Thread generatorThread = null;

        if (maxRequests > 0) {
            clientGenerator = new ClientGenerator(dispatcher, maxFloors, maxRequests);
            generatorThread = new Thread(clientGenerator);
        }

        System.out.println("\n" + "-".repeat(70));
        System.out.println("LAUNCHING SYSTEM...");
        dispatcher.start();

        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (generatorThread != null) {
            generatorThread.start();
        }

        boolean menuActive = true;
        while (menuActive) {
            System.out.println("\n" + "-".repeat(50));
            System.out.println("              CONTROL MENU");
            System.out.println("-".repeat(50));
            System.out.println("1. Add manual request");
            System.out.println("2. Show current elevator status");
            System.out.println("3. Show work statistics");
            System.out.println("4. Stop and exit");
            System.out.print("Select action: ");

            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    addManualRequest(scanner, dispatcher, maxFloors);
                    break;

                case "2":
                    System.out.println("\n" + "=".repeat(70));
                    System.out.println("              CURRENT SYSTEM STATUS");
                    System.out.println("=".repeat(70));
                    dispatcher.printStatus();
                    break;

                case "3":
                    dispatcher.printFinalStatistics();
                    break;

                case "4":
                    handleSystemStop(scanner, dispatcher, clientGenerator, generatorThread);
                    menuActive = false;
                    break;

                default:
                    System.out.println("ERROR: Invalid choice");
            }
        }

        scanner.close();

        System.out.println("\n" + "=".repeat(70));
        System.out.println("          PROGRAM COMPLETED. THANK YOU FOR USING!");
        System.out.println("=".repeat(70));

        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.exit(0);
    }

    private static void handleSystemStop(Scanner scanner, Dispatcher dispatcher,
                                         ClientGenerator clientGenerator, Thread generatorThread) {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("                 SYSTEM STOPPING");
        System.out.println("=".repeat(70));

        if (clientGenerator != null && generatorThread != null) {
            System.out.println("Stopping request generator...");
            clientGenerator.stopGenerator();

            try {
                generatorThread.join(3000);
                if (generatorThread.isAlive()) {
                    System.out.println("WARNING: Generator did not stop in time, interrupting...");
                    generatorThread.interrupt();
                } else {
                    System.out.println("Generator stopped. Requests generated: " +
                            (clientGenerator != null ? clientGenerator.getGeneratedCount() : 0));
                }
            } catch (InterruptedException e) {
                System.out.println("Interrupted while waiting for generator");
            }
        }

        System.out.println("Stopping dispatcher and elevators...");
        dispatcher.stopDispatcher();

        try {
            dispatcher.join(5000);
            if (dispatcher.isAlive()) {
                System.out.println("WARNING: Dispatcher did not stop in time, continuing...");
            } else {
                System.out.println("Dispatcher stopped");
            }
        } catch (InterruptedException e) {
            System.out.println("Interrupted while waiting");
        }

        System.out.println("\nProcessing remaining requests...");
        System.out.println("Waiting for elevators to finish...");

        boolean allIdle = false;
        for (int i = 0; i < 5 && !allIdle; i++) {
            allIdle = true;
            for (Elevator elevator : dispatcher.getElevators()) {
                if (!elevator.isIdle()) {
                    allIdle = false;
                    break;
                }
            }

            if (!allIdle) {
                try {
                    System.out.println("   Waiting cycle " + (i+1) + "/5...");
                    TimeUnit.SECONDS.sleep(2);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        if (!allIdle) {
            System.out.println("WARNING: Some elevators are still working...");
        }

        System.out.println("\n" + "=".repeat(70));
        System.out.println("                    FINAL STATISTICS");
        System.out.println("=".repeat(70));

        dispatcher.printFinalStatistics();

        System.out.println("\n" + "=".repeat(70));
        System.out.println("               SIMULATION COMPLETED");
        System.out.println("=".repeat(70));
        System.out.println("\nSummary:");
        System.out.println("Requests generated: " + (clientGenerator != null ? clientGenerator.getGeneratedCount() : 0));
        System.out.println("Requests assigned: " + dispatcher.getTotalRequestsAssigned());
        System.out.println("\nTo start new simulation, restart the program.");
        System.out.println("=".repeat(70));
    }

    private static void addManualRequest(Scanner scanner, Dispatcher dispatcher, int maxFloors) {
        try {
            System.out.println("\n" + "-".repeat(50));
            System.out.println("      ADD MANUAL REQUEST");
            System.out.println("-".repeat(50));

            System.out.print("Call floor (1-" + maxFloors + "): ");
            int floor = scanner.nextInt();

            System.out.print("Target floor (1-" + maxFloors + "): ");
            int targetFloor = scanner.nextInt();

            scanner.nextLine();

            if (floor < 1 || floor > maxFloors || targetFloor < 1 || targetFloor > maxFloors) {
                System.out.println("ERROR: Floors must be from 1 to " + maxFloors);
                return;
            }

            if (floor == targetFloor) {
                System.out.println("ERROR: Floors cannot be the same");
                return;
            }

            Direction direction = targetFloor > floor ? Direction.UP : Direction.DOWN;
            PassengerRequest request = new PassengerRequest(floor, direction, targetFloor);
            dispatcher.addRequest(request);

            System.out.println("Request successfully added to system!");

        } catch (Exception e) {
            System.out.println("ERROR: Input data error");
            scanner.nextLine();
        }
    }
}