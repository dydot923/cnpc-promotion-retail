package com.cnpc.promoretail.promotion.benefitpackage;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("dev-db")
@Testcontainers(disabledWithoutDocker = true)
class BenefitPackageDatabaseTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private BenefitPackageService benefitPackageService;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.enabled", () -> true);
    }

    @Test
    void v26PackagesCanBeQueriedAndPurchasedWithSnapshot() {
        List<BenefitPackageResponse> packages = benefitPackageService.packages();

        assertThat(packages).hasSize(15);
        assertThat(packages.stream().mapToInt(benefitPackage -> benefitPackage.items().size()).sum())
                .isEqualTo(142);
        assertThat(packages)
                .extracting(BenefitPackageResponse::packageCode)
                .contains("benefit-package-xinjiang-tour-card-2026");

        BenefitPackagePurchaseResponse purchased = benefitPackageService.purchase(
                "benefit-package-001",
                new BenefitPackagePurchaseRequest(
                        "member-001",
                        "station-001",
                        null,
                        "checkout-benefit-package-test",
                        "operator-001",
                        "Operator"
                )
        );

        assertThat(purchased.paymentAmount()).isEqualByComparingTo(purchased.salePrice());
        assertThat(purchased.entitlementSnapshot()).hasSize(packages.getFirst().items().size());
        assertThat(benefitPackageService.memberPurchases("member-001"))
                .extracting(BenefitPackagePurchaseResponse::purchaseId)
                .contains(purchased.purchaseId());
    }
}
