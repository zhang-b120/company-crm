package com.qykj.finmaster.order.domain.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuickOrderResp {
    private Long orderId;
    private Long customerId;
    private Long companyId;
}
