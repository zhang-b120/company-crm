package com.qykj.finmaster.customer.lead.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qykj.finmaster.customer.lead.entity.Lead;
import com.qykj.finmaster.customer.lead.entity.resp.GetLeadPageResp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 线索管理Mapper接口
 */
@Mapper
public interface LeadMapper extends BaseMapper<Lead> {
    /**
     * 查询线索列表
     *
     * @param lead 线索信息
     * @return 线索集合
     */
    List<GetLeadPageResp> selectLeadList(@Param("params") Map<String, Object> params);

    /**
     * 查询线索列表（用于导出，包含已删除的记录）
     *
     * @param lead 线索信息
     * @return 线索集合
     */
    List<Lead> selectLeadListForExport(Lead lead);

    /**
     * 查询线索详细信息
     *
     * @param id 线索ID
     * @param tenantId 租户ID
     * @return 线索信息
     */
    Lead selectLeadById(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /**
     * 根据手机号查询线索
     *
     * @param phone 手机号
     * @param tenantId 租户ID
     * @return 线索信息
     */
    Lead selectLeadByPhone(@Param("phone") String phone, @Param("tenantId") Long tenantId);

    /**
     * 根据微信号查询线索
     *
     * @param wechat 微信号
     * @param tenantId 租户ID
     * @return 线索信息
     */
    Lead selectLeadByWechat(@Param("wechat") String wechat, @Param("tenantId") Long tenantId);

    /**
     * 新增线索
     *
     * @param lead 线索信息
     * @return 结果
     */
    int insertLead(Lead lead);

    /**
     * 修改线索
     *
     * @param lead 线索信息
     * @return 结果
     */
    int updateLead(Lead lead);

    /**
     * 删除线索
     *
     * @param id 线索ID
     * @param tenantId 租户ID
     * @return 结果
     */
    int deleteLeadById(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /**
     * 批量删除线索
     *
     * @param ids 需要删除的线索ID数组
     * @param tenantId 租户ID
     * @return 结果
     */
    int deleteLeadByIds(@Param("ids") Long[] ids, @Param("tenantId") Long tenantId);

    /**
     * 分配线索
     *
     * @param id 线索ID
     * @param ownerUser 负责人userName
     * @param ownerUserName 负责人名称
     * @param tenantId 租户ID
     * @return 结果
     */
    int assignLead(@Param("id") Long id, @Param("ownerUser") String ownerUser, @Param("ownerUserName") String ownerUserName, @Param("tenantId") Long tenantId);

    /**
     * 转化线索
     *
     * @param id 线索ID
     * @param updateBy 更新者
     * @param updateByName 更新者名称
     * @return 结果
     */
    int convertLead(@Param("id") Long id, @Param("updateBy") String updateBy, @Param("updateByName") String updateByName);

    int exists(@Param("params") Map<String, Object> params);

    void updateOpenIdByPhone(@Param("openId") String openId, @Param("phone") String phone);

    List<Lead> getOutLeadList(@Param("params") Map<String, Object> params);
}
