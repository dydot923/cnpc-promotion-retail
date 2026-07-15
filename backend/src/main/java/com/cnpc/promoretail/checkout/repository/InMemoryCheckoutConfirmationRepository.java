package com.cnpc.promoretail.checkout.repository;

import com.cnpc.promoretail.checkout.model.CheckoutConfirmation;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!dev-db & !postgres")
public class InMemoryCheckoutConfirmationRepository implements CheckoutConfirmationRepository {

    private final ConcurrentMap<String, CheckoutConfirmation> confirmations = new ConcurrentHashMap<>();

    @Override
    public CheckoutConfirmation save(CheckoutConfirmation confirmation) {
        confirmations.put(confirmation.confirmationId(), confirmation);
        return confirmation;
    }

    @Override
    public Optional<CheckoutConfirmation> findByConfirmationId(String confirmationId) {
        return Optional.ofNullable(confirmations.get(confirmationId));
    }

    @Override
    public List<CheckoutConfirmation> findByCalculationId(String calculationId) {
        return confirmations.values().stream()
                .filter(confirmation -> confirmation.calculationId().equals(calculationId))
                .sorted(Comparator.comparing(CheckoutConfirmation::confirmedAt))
                .toList();
    }
}
