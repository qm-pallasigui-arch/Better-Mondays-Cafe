package controller;

import java.util.List;
import monitoring.SalesRecord;
import persistence.SalesRepository;

public class OrderController {

    private final SalesRepository salesRepository;

    public OrderController(SalesRepository salesRepository) {
        this.salesRepository = salesRepository;
    }

    public void persistCompletedTransaction(String transactionRef,
                                            List<SalesRecord> salesList,
                                            double subTotal,
                                            double cash,
                                            double change) throws Exception {
        salesRepository.saveAll(
                transactionRef,
                salesList,
                subTotal / 1.12,
                subTotal * 0.12 / 1.12,
                subTotal,
                cash,
                change
        );
        salesRepository.updateOrderStatus(transactionRef, "COMPLETED");
    }
}
