package com.cnpc.promoretail.checkout.repository;

import com.cnpc.promoretail.checkout.model.CheckoutTransaction;
import com.cnpc.promoretail.checkout.CheckoutTransactionQuery;
import java.util.List;
import java.util.Optional;

public interface CheckoutTransactionRepository {

    CheckoutTransaction save(CheckoutTransaction transaction);

    Optional<CheckoutTransaction> findByTxnNo(String txnNo);

    Optional<CheckoutTransaction> findByConfirmationId(String confirmationId);

    List<CheckoutTransaction> findRecent(int limit);

    List<CheckoutTransaction> findByQuery(CheckoutTransactionQuery query);
}
