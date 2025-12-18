import java.text.SimpleDateFormat;
import java.util.Date;

public class LoggerUtil {
    private static final SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm:ss");
    private static final int COLUMN_TIME = 12;
    private static final int COLUMN_SOURCE = 20;
    private static final int COLUMN_ACTION = 30;
    private static final int COLUMN_DETAILS = 40;

    private static String getCurrentTime() {
        return timeFormatter.format(new Date());
    }

    private static void printRow(String time, String source, String action, String details) {
        String format = "| %-" + (COLUMN_TIME-2) + "s | %-" + (COLUMN_SOURCE-2) + "s | %-" +
                (COLUMN_ACTION-2) + "s | %-" + (COLUMN_DETAILS-2) + "s |";
        System.out.printf(format + "%n", time, source, action, details);
    }

    public static void printHeader(String title) {
        int totalWidth = COLUMN_TIME + COLUMN_SOURCE + COLUMN_ACTION + COLUMN_DETAILS + 5;
        System.out.println();
        System.out.println("=".repeat(totalWidth));
        System.out.println(" " + title);
        System.out.println("=".repeat(totalWidth));

        System.out.print("+");
        System.out.print("-".repeat(COLUMN_TIME - 1));
        System.out.print("+");
        System.out.print("-".repeat(COLUMN_SOURCE - 1));
        System.out.print("+");
        System.out.print("-".repeat(COLUMN_ACTION - 1));
        System.out.print("+");
        System.out.print("-".repeat(COLUMN_DETAILS - 1));
        System.out.println("+");

        printRow("Time", "Source", "Action", "Details");

        System.out.print("+");
        System.out.print("-".repeat(COLUMN_TIME - 1));
        System.out.print("+");
        System.out.print("-".repeat(COLUMN_SOURCE - 1));
        System.out.print("+");
        System.out.print("-".repeat(COLUMN_ACTION - 1));
        System.out.print("+");
        System.out.print("-".repeat(COLUMN_DETAILS - 1));
        System.out.println("+");
    }

    public static void printFooter() {
        int totalWidth = COLUMN_TIME + COLUMN_SOURCE + COLUMN_ACTION + COLUMN_DETAILS + 5;
        System.out.print("+");
        System.out.print("-".repeat(COLUMN_TIME - 1));
        System.out.print("+");
        System.out.print("-".repeat(COLUMN_SOURCE - 1));
        System.out.print("+");
        System.out.print("-".repeat(COLUMN_ACTION - 1));
        System.out.print("+");
        System.out.print("-".repeat(COLUMN_DETAILS - 1));
        System.out.println("+");
    }

    public static void logRequest(String source, String action, String details) {
        printRow(getCurrentTime(), source, action, details);
    }

    public static void logElevatorAction(int elevatorId, String action, String details) {
        printRow(getCurrentTime(), "Elevator " + elevatorId, action, details);
    }

    public static void logDispatcherAction(String action, String details) {
        printRow(getCurrentTime(), "Dispatcher", action, details);
    }

    public static void logSystemInfo(String message) {
        System.out.println("[SYSTEM] " + message);
    }

    public static void printElevatorStatsHeader() {
        System.out.println();
        System.out.println("+--------+----------+------------+----------+------------------+----------+");
        System.out.println("| Elev # |  Floor   | Direction  |  Status  |   Passengers     | Requests |");
        System.out.println("+--------+----------+------------+----------+------------------+----------+");
    }

    public static void printElevatorStat(int id, int floor, String direction, String status,
                                         int passengers, int requests) {
        String passengerInfo = passengers + "/10";
        String requestInfo = requests + " requests";

        System.out.printf("|   %-4d |   %-6d |   %-8s | %-8s |     %-12s | %-8s |%n",
                id, floor, direction, status, passengerInfo, requestInfo);
    }

    public static void printElevatorStatsFooter() {
        System.out.println("+--------+----------+------------+----------+------------------+----------+");
    }
}