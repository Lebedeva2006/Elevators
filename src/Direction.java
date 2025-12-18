public enum Direction {
    UP("UP"),
    DOWN("DOWN"),
    IDLE("IDLE");

    private final String symbol;

    Direction(String symbol) {
        this.symbol = symbol;
    }

    @Override
    public String toString() {
        return symbol;
    }
}