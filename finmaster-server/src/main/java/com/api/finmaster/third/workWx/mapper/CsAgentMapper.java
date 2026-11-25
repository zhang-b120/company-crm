package com.qykj.finmaster.third.workWx.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 客服部员工 ↔ 企微 userid 解析（WXRESP-12）。
 *
 * <p>跨模块只读 sys_user + sys_user_department + sys_department，取"客服部"下、已绑定 wecom_userid 的启用员工。
 * 不放进 system 模块是为了让会话存档功能自包含；纯只读、无写入。</p>
 */
@Mapper
public interface CsAgentMapper {

    /**
     * 取租户下客服部员工：username / real_name / wecom_userid。
     * @param deptIds     客服部门ID列表；为空则回退按 workbench_type='CS' 或部门名含"客服"识别
     * @param useDeptIds  deptIds 是否有效（非空）——XML 里据此二选一
     */
    List<Map<String, Object>> selectCsAgents(@Param("tenantId") Long tenantId,
                                             @Param("deptIds") List<Long> deptIds,
                                             @Param("useDeptIds") boolean useDeptIds);
}
