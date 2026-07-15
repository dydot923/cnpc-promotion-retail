package com.cnpc.promoretail.checkout.repository;

import com.cnpc.promoretail.checkout.CheckoutTransactionQuery;
import com.cnpc.promoretail.checkout.model.CheckoutTransaction;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!dev-db & !postgres")
public class InMemoryCheckoutTransactionRepository implements CheckoutTransactionRepository {

    private final ConcurrentMap<String, CheckoutTransaction> transactions = new ConcurrentHashMap<>();

    @Override
    public CheckoutTransaction save(CheckoutTransaction transaction) {
        transactions.put(transaction.txnNo(), transaction);
        return transaction;
    }

    @Override
    public Optional<CheckoutTransaction> findByTxnNo(String txnNo) {
        return Optional.ofNullable(transactions.get(txnNo));
    }

    @Override
    public Optional<CheckoutTransaction> findByConfirmationId(String confirmationId) {
        return transactions.values().stream()
                .filter(transaction -> transaction.confirmationId().equals(confirmationId))
                .findFirst();
    }

    @Override
    public List<CheckoutTransaction> findRecent(int limit) {
        int effectiveLimit = Math.max(1, Math.min(limit, 200));
        return transactions.values().stream()
                .sorted(Comparator.comparing(CheckoutTransaction::createdAt).reversed())
                .limit(effectiveLimit)
                .toList();
    }

    @Override
    public List<CheckoutTransaction> findByQuery(CheckoutTransactionQuery query) {
        return transactions.values().stream()
                .filter(transaction -> query.memberCode().isBlank()
                        || query.memberCode().equals(transaction.memberCode()))
                .filter(transaction -> query.stationCode().isBlank()
                        || query.stationCode().equals(transaction.stationCode()))
                .filter(transaction -> query.startDate() == null
                        || !transaction.createdAt().isBefore(query.startDate()))
                .filter(transaction -> query.endDate() == null
                        || !transaction.createdAt().isAfter(query.endDate()))
                .sorted(Comparator.comparing(CheckoutTransaction::createdAt).reversed())
                .limit(query.limit())
                .toList();
    }
}
