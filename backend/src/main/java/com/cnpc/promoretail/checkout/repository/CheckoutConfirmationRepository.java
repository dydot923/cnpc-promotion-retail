package com.cnpc.promoretail.checkout.repository;

import com.cnpc.promoretail.checkout.model.CheckoutConfirmation;
import java.util.List;
import java.util.Optional;

public interface CheckoutConfirmationRepository {

    CheckoutConfirmation save(CheckoutConfirmation confirmation);

    Optional<CheckoutConfirmation> findByConfirmationId(String confirmationId);

    List<CheckoutConfirmation> findByCalculationId(String calculationId);
}
