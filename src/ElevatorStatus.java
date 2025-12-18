public enum ElevatorStatus {
    MOVING("MOVING"),
    STOPPED("STOPPED"),
    DOORS_OPEN("DOORS_OPEN"),
    DOORS_CLOSING("DOORS_CLOSING"),
    DOORS_OPENING("DOORS_OPENING");

    private final String symbol;

    ElevatorStatus(String symbol) {
        this.symbol = symbol;
    }

    @Override
    public String toString() {
        return symbol;
    }
}