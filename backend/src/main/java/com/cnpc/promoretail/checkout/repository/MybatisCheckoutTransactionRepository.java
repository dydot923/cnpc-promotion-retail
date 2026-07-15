package com.cnpc.promoretail.checkout.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cnpc.promoretail.checkout.CheckoutTransactionQuery;
import com.cnpc.promoretail.checkout.model.CheckoutTransaction;
import com.cnpc.promoretail.checkout.model.CheckoutTransactionItem;
import com.cnpc.promoretail.checkout.persistence.entity.CheckoutTransactionEntity;
import com.cnpc.promoretail.checkout.persistence.entity.CheckoutTransactionItemEntity;
import com.cnpc.promoretail.checkout.persistence.mapper.CheckoutTransactionItemMapper;
import com.cnpc.promoretail.checkout.persistence.mapper.CheckoutTransactionMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile({"dev-db", "postgres"})
public class MybatisCheckoutTransactionRepository implements CheckoutTransactionRepository {

    private final CheckoutTransactionMapper transactionMapper;
    private final CheckoutTransactionItemMapper itemMapper;

    public MybatisCheckoutTransactionRepository(
            CheckoutTransactionMapper transactionMapper,
            CheckoutTransactionItemMapper itemMapper
    ) {
        this.transactionMapper = transactionMapper;
        this.itemMapper = itemMapper;
    }

    @Override
    public CheckoutTransaction save(CheckoutTransaction transaction) {
        CheckoutTransactionEntity entity = toEntity(transaction);
        transactionMapper.insert(entity);
        for (CheckoutTransactionItem item : transaction.items()) {
            itemMapper.insert(toItemEntity(entity.getId(), item));
        }
        return transaction;
    }

    @Override
    public Optional<CheckoutTransaction> findByTxnNo(String txnNo) {
        return Optional.ofNullable(transactionMapper.selectOne(new LambdaQueryWrapper<CheckoutTransactionEntity>()
                        .eq(CheckoutTransactionEntity::getTxnNo, txnNo)))
                .map(this::toTransaction);
    }

    @Override
    public Optional<CheckoutTransaction> findByConfirmationId(String confirmationId) {
        return Optional.ofNullable(transactionMapper.selectOne(new LambdaQueryWrapper<CheckoutTransactionEntity>()
                        .eq(CheckoutTransactionEntity::getConfirmationId, confirmationId)))
                .map(this::toTransaction);
    }

    @Override
    public List<CheckoutTransaction> findRecent(int limit) {
        int effectiveLimit = Math.max(1, Math.min(limit, 200));
        return transactionMapper.selectList(new LambdaQueryWrapper<CheckoutTransactionEntity>()
                        .orderByDesc(CheckoutTransactionEntity::getCreatedAt)
                        .last("limit " + effectiveLimit))
                .stream()
                .map(this::toTransaction)
                .toList();
    }

    @Override
    public List<CheckoutTransaction> findByQuery(CheckoutTransactionQuery query) {
        LambdaQueryWrapper<CheckoutTransactionEntity> wrapper = new LambdaQueryWrapper<CheckoutTransactionEntity>()
                .eq(!query.memberCode().isBlank(), CheckoutTransactionEntity::getMemberCode, query.memberCode())
                .eq(!query.stationCode().isBlank(), CheckoutTransactionEntity::getStationCode, query.stationCode())
                .ge(query.startDate() != null, CheckoutTransactionEntity::getCreatedAt, query.startDate())
                .le(query.endDate() != null, CheckoutTransactionEntity::getCreatedAt, query.endDate())
                .orderByDesc(CheckoutTransactionEntity::getCreatedAt)
                .last("limit " + query.limit());
        return transactionMapper.selectList(wrapper).stream()
                .map(this::toTransaction)
                .toList();
    }

    private CheckoutTransactionEntity toEntity(CheckoutTransaction transaction) {
        CheckoutTransactionEntity entity = new CheckoutTransactionEntity();
        entity.setTxnNo(transaction.txnNo());
        entity.setConfirmationId(transaction.confirmationId());
        entity.setCalculationId(transaction.calculationId());
        entity.setSelectedCandidateId(transaction.selectedCandidateId());
        entity.setTotalAmount(transaction.totalAmount());
        entity.setDiscountAmount(transaction.discountAmount());
        entity.setPayableAmount(transaction.payableAmount());
        entity.setPaymentMethod(transaction.paymentMethod());
        entity.setOperatorId(transaction.operatorId());
        entity.setOperatorName(transaction.operatorName());
        entity.setMemberCode(transaction.memberCode());
        entity.setStationCode(transaction.stationCode());
        entity.setStatus(transaction.status());
        entity.setCreatedAt(transaction.createdAt());
        return entity;
    }

    private CheckoutTransactionItemEntity toItemEntity(Long transactionId, CheckoutTransactionItem item) {
        CheckoutTransactionItemEntity entity = new CheckoutTransactionItemEntity();
        entity.setTransactionId(transactionId);
        entity.setProductCode(item.productCode());
        entity.setProductName(item.productName());
        entity.setBarcode(item.barcode());
        entity.setCategory(item.category());
        entity.setUnitPrice(item.unitPrice());
        entity.setActualPrice(item.actualPrice());
        entity.setQuantity(item.quantity());
        entity.setSubtotal(item.subtotal());
        entity.setAppliedPromoId(item.appliedPromoId());
        entity.setAppliedCouponCode(item.appliedCouponCode());
        return entity;
    }

    private CheckoutTransaction toTransaction(CheckoutTransactionEntity entity) {
        return new CheckoutTransaction(
                entity.getTxnNo(),
                entity.getConfirmationId(),
                entity.getCalculationId(),
                entity.getSelectedCandidateId(),
                entity.getTotalAmount(),
                entity.getDiscountAmount(),
                entity.getPayableAmount(),
                entity.getPaymentMethod(),
                entity.getOperatorId(),
                entity.getOperatorName(),
                entity.getMemberCode(),
                entity.getStationCode(),
                entity.getStatus(),
                entity.getCreatedAt(),
                findItems(entity.getId())
        );
    }

    private List<CheckoutTransactionItem> findItems(Long transactionId) {
        return itemMapper.selectList(new LambdaQueryWrapper<CheckoutTransactionItemEntity>()
                        .eq(CheckoutTransactionItemEntity::getTransactionId, transactionId)
                        .orderByAsc(CheckoutTransactionItemEntity::getId))
                .stream()
                .map(this::toItem)
                .toList();
    }

    private CheckoutTransactionItem toItem(CheckoutTransactionItemEntity entity) {
        return new CheckoutTransactionItem(
                entity.getProductCode(),
                entity.getProductName(),
                entity.getBarcode(),
                entity.getCategory(),
                entity.getUnitPrice(),
                entity.getActualPrice(),
                entity.getQuantity(),
                entity.getSubtotal(),
                entity.getAppliedPromoId(),
                entity.getAppliedCouponCode()
        );
    }
}
