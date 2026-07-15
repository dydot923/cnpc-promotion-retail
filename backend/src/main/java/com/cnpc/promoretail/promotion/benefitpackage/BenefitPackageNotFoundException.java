package com.cnpc.promoretail.promotion.benefitpackage;

public class BenefitPackageNotFoundException extends RuntimeException {

    public BenefitPackageNotFoundException(String packageCode) {
        super("Benefit package not found: " + packageCode);
    }
}
