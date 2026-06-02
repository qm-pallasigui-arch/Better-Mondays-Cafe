package controller;

import java.util.ArrayList;
import java.util.List;
import monitoring.SalesRecord;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import persistence.SalesRepository;

class OrderControllerTest {

    @Test
    void persistsTransactionAndMarksCompleted() throws Exception {
        FakeSalesRepository repo = new FakeSalesRepository();
        OrderController controller = new OrderController(repo);

        List<SalesRecord> records = new ArrayList<>();
        records.add(new SalesRecord("Latte", 2, 100.0, 200.0));

        controller.persistCompletedTransaction("TXN001", records, 200.0, 500.0, 300.0, "Juan");

        assertTrue(repo.saved);
        assertEquals("TXN001", repo.savedRef);
        assertEquals("COMPLETED", repo.status);
    }

    private static class FakeSalesRepository implements SalesRepository {
        boolean saved;
        String savedRef;
        String status;

        @Override
        public List<SalesRecord> findAll() {
            return new ArrayList<>();
        }

        @Override
        public void saveAll(String transactionRef, List<SalesRecord> records, double subtotal, double tax, double total, double cash, double changeAmount, String customerName) {
            this.saved = true;
            this.savedRef = transactionRef;
        }

        @Override
        public void updateOrderStatus(String transactionRef, String status) {
            this.status = status;
        }
    }
}
