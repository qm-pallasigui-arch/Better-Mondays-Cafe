package inventory.analytics;

public final class EoqCalculator {

    private EoqCalculator() {
    }

    // EOQ = sqrt((2 * annualDemand * orderingCost) / annualHoldingCostPerUnit)
    public static double calculate(double annualDemand, double orderingCost, double annualHoldingCostPerUnit) {
        if (annualDemand <= 0 || orderingCost <= 0 || annualHoldingCostPerUnit <= 0) {
            return 0.0;
        }
        return Math.sqrt((2.0 * annualDemand * orderingCost) / annualHoldingCostPerUnit);
    }
}
