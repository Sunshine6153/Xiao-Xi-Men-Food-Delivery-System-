package com.neu.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class OrderOverviewVO implements Serializable {

    private Integer pendingPreparationOrders;
    private Integer preparingOrders;
    private Integer completedPreparationOrders;
    private Integer readyForPickupOrders;
    private Integer deliveringOrders;
    private Integer pendingReceiptOrders;
    private Integer completedOrders;
    private Integer cancelledOrders;
    private Integer allOrders;
}
