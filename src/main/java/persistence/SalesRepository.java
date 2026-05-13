package persistence;

import java.util.List;
import monitoring.SalesRecord;

public interface SalesRepository {

    List<SalesRecord> findAll() throws Exception;

    void saveAll(String transactionRef, List<SalesRecord> records, double subtotal, double tax, double total, double cash, double changeAmount) throws Exception;

    void updateOrderStatus(String transactionRef, String status) throws Exception;
}
