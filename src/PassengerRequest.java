public class PassengerRequest {
    private final int floor;
    private final Direction direction;
    private final int targetFloor;
    private final long timestamp;
    private static int requestCounter = 0;
    private final int requestId;

    public PassengerRequest(int floor, Direction direction, int targetFloor) {
        this.floor = floor;
        this.direction = direction;
        this.targetFloor = targetFloor;
        this.timestamp = System.currentTimeMillis();
        this.requestId = ++requestCounter;
    }

    public int getFloor() { return floor; }
    public Direction getDirection() { return direction; }
    public int getTargetFloor() { return targetFloor; }
    public long getTimestamp() { return timestamp; }
    public int getRequestId() { return requestId; }

    @Override
    public String toString() {
        return String.format("Request #%d: %d->%d %s",
                requestId, floor, targetFloor, direction);
    }

    public String getShortInfo() {
        return String.format("#%d: %d->%d %s", requestId, floor, targetFloor, direction);
    }
}