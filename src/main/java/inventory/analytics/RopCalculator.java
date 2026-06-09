package inventory.analytics;

public final class RopCalculator {

    private RopCalculator() {
    }

    // ROP = (average daily demand * lead time in days) + safety stock
    public static double calculate(double averageDailyDemand, double leadTimeDays, double safetyStock) {
        if (averageDailyDemand < 0 || leadTimeDays < 0 || safetyStock < 0) {
            return 0.0;
        }
        return (averageDailyDemand * leadTimeDays) + safetyStock;
    }
}
