package com.qykj.finmaster.history;

import com.qykj.finmaster.common.core.domain.constansts.DictConstants;
import com.qykj.finmaster.contract.domain.Contract;
import com.qykj.finmaster.contract.domain.ContractDetail;
import com.qykj.finmaster.contract.mapper.ContractDetailMapper;
import com.qykj.finmaster.contract.mapper.ContractMapper;
import com.qykj.finmaster.history.mapper.HistoryCompanyServiceMapper;
import com.qykj.finmaster.history.mapper.HistoryContractMapper;
import com.qykj.finmaster.history.mapper.HistoryOrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 历史订单批量生成合同的服务
 * <p>
 * 规则：
 * 1. 仅处理 order_type = NORMAL 且 status = PAID 的订单；
 * 2. 仅处理 order_detail_type in (NEW, RENEW) 的订单明细；
 * 3. 按 tenantId + companyId + businessId + orderDetailType 分组，每组生成一份合同；
 * 4. 合同类型：NEW -> contract_new；RENEW -> contract_renew；
 * 5. 合同起止日期：单条订单取该条 effective_date/end_date，多条取最小 effective_date 和最大 end_date；
 * 6. 合同状态固定为 COMPLETED，remark 固定为“历史订单批量生成”；
 * 7. 幂等：同一分组下若已存在 remark = “历史订单批量生成” 的合同则跳过。
 */
@Service
public class HistoryContractGenerateService {

    /**
     * 历史批量生成合同的备注标识
     */
    public static final String HISTORY_REMARK = "历史订单批量生成";

    @Autowired
    private HistoryOrderMapper historyOrderMapper;

    @Autowired
    private HistoryContractMapper historyContractMapper;

    @Autowired
    private ContractMapper contractMapper;

    @Autowired
    private ContractDetailMapper contractDetailMapper;

    @Autowired
    private HistoryCompanyServiceMapper historyCompanyServiceMapper;

    /**
     * 批量生成历史合同
     *
     * @param deployTime 上线时间边界，可为 null；格式与 orders.create_time 一致（例如 yyyy-MM-dd HH:mm:ss）
     */
    public void generateHistoryContracts(String deployTime) {
        List<HistoryOrderDetailRecord> records = historyOrderMapper.selectHistoryOrderDetails(deployTime);
        if (records == null || records.isEmpty()) {
            return;
        }

        // 按 tenantId + companyId + businessId + orderDetailType 分组
        records.stream()
                .collect(Collectors.groupingBy(r -> new HistoryOrderGroupKey(
                        r.getTenantId(),
                        r.getCompanyId(),
                        r.getBusinessId(),
                        r.getOrderDetailType()
                )))
                .forEach(this::generateOneContractForGroup);
    }

    /**
     * 单个分组生成一份合同（每组一个事务）
     */
    @Transactional(rollbackFor = Exception.class)
    public void generateOneContractForGroup(HistoryOrderGroupKey key,
                                               List<HistoryOrderDetailRecord> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        // 安全保护：按设计历史合同是“按公司+业务”生成，缺少公司或业务ID的分组直接跳过
        if (key.getCompanyId() == null || key.getBusinessId() == null) {
            return;
        }

        String contractType = mapToContractType(key.getOrderDetailType());

        // 幂等检查：同一租户+公司+业务+合同类型下是否已存在“历史订单批量生成”的合同
        int existCount = historyContractMapper.countHistoryBatchContract(
                key.getTenantId(),
                key.getCompanyId(),
                key.getBusinessId(),
                contractType,
                HISTORY_REMARK
        );
        if (existCount > 0) {
            return;
        }

        // 计算合同起止日期：
        // 1）优先根据 company_service 中对应业务的服务日期（effective_date / actual_end_date）
        // 2）如果不存在对应服务，则退回到订单明细的最小 effective_date / 最大 end_date
        LocalDate startDate = null;
        LocalDate endDate = null;

        HistoryCompanyServiceDateRange serviceDateRange =
                historyCompanyServiceMapper.selectServiceDateRangeByCompanyAndBusiness(
                        key.getCompanyId(), key.getBusinessId(), key.getTenantId());
        if (serviceDateRange != null
                && (serviceDateRange.getEffectiveDate() != null || serviceDateRange.getActualEndDate() != null)) {
            startDate = serviceDateRange.getEffectiveDate();
            endDate = serviceDateRange.getActualEndDate();
        }

        // 若公司服务中未查到对应业务的服务，或日期为空，则使用订单明细时间计算
        if (startDate == null && endDate == null) {
            startDate = list.stream()
                    .map(HistoryOrderDetailRecord::getEffectiveDate)
                    .filter(Objects::nonNull)
                    .min(LocalDate::compareTo)
                    .orElse(null);

            endDate = list.stream()
                    .map(HistoryOrderDetailRecord::getEndDate)
                    .filter(Objects::nonNull)
                    .max(LocalDate::compareTo)
                    .orElse(startDate);
        }

        // 构造合同主表
        Contract contract = new Contract();
        contract.setTenantId(key.getTenantId());
        contract.setCompanyId(key.getCompanyId());
        contract.setBusinessId(key.getBusinessId());
        contract.setContractType(contractType);
        contract.setStatus(DictConstants.CONTRACT_STATUS_COMPLETED);
        contract.setContractNo(generateContractNo());
        contract.setRemark(HISTORY_REMARK);
        if (startDate != null) {
            contract.setContractStartTime(java.util.Date.from(
                    startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        }
        if (endDate != null) {
            contract.setContractEndTime(java.util.Date.from(
                    endDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        }
        // 历史订单生成的合同一定存在订单；地址此处不处理，后续如有需要可补充。
        contract.setHaveOrder(1);
        contract.setHaveAddress(0);
        contract.setTransferTask(DictConstants.TRANSFER_TASK_NO);

        contractMapper.insert(contract);

        // 生成合同明细
        for (HistoryOrderDetailRecord record : list) {
            ContractDetail detail = new ContractDetail();
            detail.setTenantId(record.getTenantId());
            detail.setContractId(contract.getId());
            detail.setOrderDetailId(record.getOrderDetailId());
            detail.setOrderNo(record.getOrderNo());
            // productId / companyAddressId 如有需要可根据业务再做补充，这里先不绑定
            contractDetailMapper.insert(detail);
        }
    }

    /**
     * 将订单明细类型映射为合同类型
     */
    private String mapToContractType(String orderDetailType) {
        if (DictConstants.ORDER_DETAIL_TYPE_NEW.equalsIgnoreCase(orderDetailType)) {
            return DictConstants.CONTRACT_TYPE_NEW;
        }
        if (DictConstants.ORDER_DETAIL_TYPE_RENEW.equalsIgnoreCase(orderDetailType)) {
            return DictConstants.CONTRACT_TYPE_RENEW;
        }
        // 理论上不会走到这里，兜底按新增合同处理
        return DictConstants.CONTRACT_TYPE_NEW;
    }

    /**
     * 合同编号生成：HT + yyyyMMdd + 6位随机数
     * 可与现有生成逻辑保持一致或尽量相似
     */
    private String generateContractNo() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int randomNum = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return "HT" + datePart + randomNum;
    }
}

