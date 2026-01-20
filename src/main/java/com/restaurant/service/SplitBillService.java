package com.restaurant.service;

import com.restaurant.model.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Split Bill Service
 * Handles split payment business logic
 */
public class SplitBillService {
    
    private static final Logger logger = LogManager.getLogger(SplitBillService.class);
    private static SplitBillService instance;
    
    private SplitBillService() {}
    
    public static synchronized SplitBillService getInstance() {
        if (instance == null) {
            instance = new SplitBillService();
        }
        return instance;
    }
    
    // ==================== EQUAL SPLIT ====================
    
    /**
     * Create an equal split for an order
     * @param order The order to split
     * @param numberOfParts Number of people paying
     * @return ServiceResult with SplitBill
     */
    public ServiceResult<SplitBill> createEqualSplit(Order order, int numberOfParts) {
        if (order == null) {
            return ServiceResult.error("Đơn hàng không hợp lệ");
        }
        if (numberOfParts < 2 || numberOfParts > 20) {
            return ServiceResult.error("Số người phải từ 2 đến 20");
        }
        
        BigDecimal totalAmount = order.getTotalAmount();
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return ServiceResult.error("Đơn hàng chưa có món để chia");
        }
        
        SplitBill splitBill = new SplitBill(order.getId(), SplitBill.SplitType.EQUAL, 
            numberOfParts, totalAmount);
        splitBill.setOrderCode(order.getOrderCode());
        
        // Create equal parts
        splitBill.createEqualParts(numberOfParts);
        
        // TODO: Persist to database
        // int id = splitBillDAO.create(splitBill);
        // splitBill.setId(id);
        
        logger.info("Created equal split for order {} with {} parts, {} each", 
            order.getOrderCode(), numberOfParts, 
            totalAmount.divide(new BigDecimal(numberOfParts), 0, RoundingMode.CEILING));
        
        return ServiceResult.success(splitBill, 
            String.format("Đã chia đều thành %d phần", numberOfParts));
    }
    
    // ==================== BY ITEM SPLIT ====================
    
    /**
     * Create a split bill for BY_ITEM mode
     * Items will be assigned to parts later via assignItemToPart()
     */
    public ServiceResult<SplitBill> createItemSplit(Order order, int numberOfParts) {
        if (order == null) {
            return ServiceResult.error("Đơn hàng không hợp lệ");
        }
        if (numberOfParts < 2 || numberOfParts > 20) {
            return ServiceResult.error("Số người phải từ 2 đến 20");
        }
        
        SplitBill splitBill = new SplitBill(order.getId(), SplitBill.SplitType.BY_ITEM, 
            numberOfParts, order.getTotalAmount());
        splitBill.setOrderCode(order.getOrderCode());
        
        // Create empty parts
        for (int i = 1; i <= numberOfParts; i++) {
            SplitBillPart part = new SplitBillPart(i, BigDecimal.ZERO);
            part.setSplitBillId(splitBill.getId());
            splitBill.getParts().add(part);
        }
        
        logger.info("Created item-based split for order {} with {} parts", 
            order.getOrderCode(), numberOfParts);
        
        return ServiceResult.success(splitBill, "Đã tạo chia bill theo món");
    }
    
    /**
     * Assign an order item to a specific part
     * @param splitBill The split bill
     * @param orderItemId The order item ID
     * @param partNumber The part number to assign to
     * @param itemTotal The total price of this item
     * @param shareCount If > 1, this item is shared among multiple parts
     */
    public void assignItemToPart(SplitBill splitBill, int orderItemId, int partNumber, 
            BigDecimal itemTotal, int shareCount) {
        
        if (shareCount < 1) shareCount = 1;
        
        BigDecimal shareAmount = itemTotal.divide(
            new BigDecimal(shareCount), 0, RoundingMode.CEILING);
        
        // Find the part and add the amount
        splitBill.getParts().stream()
            .filter(p -> p.getPartNumber() == partNumber)
            .findFirst()
            .ifPresent(part -> {
                part.setAmount(part.getAmount().add(shareAmount));
                logger.debug("Assigned item {} to part {} with amount {}", 
                    orderItemId, partNumber, shareAmount);
            });
    }
    
    /**
     * Assign a shared item across multiple parts
     */
    public void assignSharedItem(SplitBill splitBill, int orderItemId, 
            List<Integer> partNumbers, BigDecimal itemTotal) {
        
        int shareCount = partNumbers.size();
        BigDecimal shareAmount = itemTotal.divide(
            new BigDecimal(shareCount), 0, RoundingMode.CEILING);
        
        for (Integer partNumber : partNumbers) {
            assignItemToPart(splitBill, orderItemId, partNumber, shareAmount, 1);
        }
        
        logger.info("Assigned shared item {} to {} parts", orderItemId, shareCount);
    }
    
    // ==================== PAYMENT ====================
    
    /**
     * Process payment for one part of the split
     */
    public ServiceResult<SplitBillPart> payPart(SplitBill splitBill, int partNumber, 
            String paymentMethod) {
        
        SplitBillPart part = splitBill.getParts().stream()
            .filter(p -> p.getPartNumber() == partNumber)
            .findFirst()
            .orElse(null);
        
        if (part == null) {
            return ServiceResult.error("Không tìm thấy phần thanh toán #" + partNumber);
        }
        
        if (part.isPaid()) {
            return ServiceResult.error("Phần này đã được thanh toán");
        }
        
        if (part.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return ServiceResult.error("Số tiền không hợp lệ");
        }
        
        // Mark as paid
        part.markPaid(paymentMethod);
        
        // Update split bill status
        if (splitBill.isFullyPaid()) {
            splitBill.setStatus(SplitBill.Status.COMPLETED);
        } else {
            splitBill.setStatus(SplitBill.Status.PARTIAL);
        }
        
        // TODO: Persist to database
        // splitBillDAO.updatePart(part);
        // splitBillDAO.updateStatus(splitBill);
        
        logger.info("Paid part {} of order {} via {} - Amount: {}", 
            partNumber, splitBill.getOrderCode(), paymentMethod, part.getAmount());
        
        return ServiceResult.success(part, 
            String.format("Đã thanh toán phần %d", partNumber));
    }
    
    /**
     * Check if the order can be closed (all parts paid)
     */
    public boolean canCloseOrder(SplitBill splitBill) {
        return splitBill.isFullyPaid();
    }
    
    /**
     * Get summary text for display
     */
    public String getSummaryText(SplitBill splitBill) {
        return String.format("Đã thanh toán %d/%d phần (%s/%s)", 
            splitBill.getPaidPartsCount(),
            splitBill.getTotalSplits(),
            formatCurrency(splitBill.getPaidAmount()),
            formatCurrency(splitBill.getTotalAmount()));
    }
    
    private String formatCurrency(BigDecimal amount) {
        return String.format("%,.0f₫", amount);
    }
}
