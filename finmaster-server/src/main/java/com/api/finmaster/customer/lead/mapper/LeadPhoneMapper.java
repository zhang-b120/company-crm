package com.qykj.finmaster.customer.lead.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qykj.finmaster.customer.lead.entity.LeadPhone;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 线索手机号子表 Mapper
 */
@Mapper
public interface LeadPhoneMapper extends BaseMapper<LeadPhone> {

    /**
     * 按手机号 / openId / 微信号查找命中的 leadId 列表（主表 ∪ 子表）。
     * 仅统计未软删 lead 与未软删子表行。
     */
    List<Long> selectLeadIdsByContact(@Param("phone") String phone,
                                       @Param("openId") String openId,
                                       @Param("wechat") String wechat,
                                       @Param("tenantId") Long tenantId);

    /**
     * 仅查询 lead_phone 子表中能命中的 leadId 列表（不含主表）。
     */
    List<Long> selectLeadIdsByContactInPhoneTable(@Param("phone") String phone,
                                                   @Param("openId") String openId,
                                                   @Param("wechat") String wechat);
}
