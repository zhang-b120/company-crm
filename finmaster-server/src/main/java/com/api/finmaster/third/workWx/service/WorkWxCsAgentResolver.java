package com.qykj.finmaster.third.workWx.service;

import com.qykj.finmaster.system.bizConfig.service.SysBizConfigService;
import com.qykj.finmaster.third.workWx.mapper.CsAgentMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 客服部员工解析器（WXRESP-12）：把企微群成员的 wecom_userid 映射回本系统客服部员工（username/real_name）。
 *
 * <p>客服部门ID来自业务配置 {@code wecom.cs.department_ids}（逗号分隔），留空则回退按部门 workbench_type='CS'
 * 或名称含"客服"识别。仅取已绑定 wecom_userid 的启用员工——未绑定的员工无法归因/弹窗（需先补录 sys_user.wecom_userid）。</p>
 */
@Slf4j
@Service
public class WorkWxCsAgentResolver {

    /** 客服部门ID配置键（逗号分隔）。 */
    private static final String CFG_CS_DEPT_IDS = "wecom.cs.department_ids";

    @Autowired
    private CsAgentMapper csAgentMapper;
    @Autowired
    private SysBizConfigService bizConfigService;

    /** 一个客服员工的最小画像。 */
    public static class CsAgent {
        public final String username;
        public final String realName;
        public final String wecomUserid;
        public CsAgent(String username, String realName, String wecomUserid) {
            this.username = username;
            this.realName = realName;
            this.wecomUserid = wecomUserid;
        }
    }

    /** 取租户下所有客服部员工（已绑定 wecom_userid）。 */
    public List<CsAgent> listCsAgents(Long tenantId) {
        if (tenantId == null) return Collections.emptyList();
        List<Long> deptIds = parseDeptIds();
        boolean useDeptIds = deptIds != null && !deptIds.isEmpty();
        List<Map<String, Object>> rows;
        try {
            rows = csAgentMapper.selectCsAgents(tenantId, useDeptIds ? deptIds : Collections.singletonList(-1L), useDeptIds);
        } catch (Throwable e) {
            log.warn("[cs-agent] 查询客服部员工失败：{}", e.getMessage());
            return Collections.emptyList();
        }
        List<CsAgent> out = new ArrayList<>();
        if (rows != null) {
            for (Map<String, Object> r : rows) {
                String wecom = asStr(r.get("wecom_userid"));
                if (StringUtils.isBlank(wecom)) continue;
                out.add(new CsAgent(asStr(r.get("username")), asStr(r.get("real_name")), wecom));
            }
        }
        return out;
    }

    /** wecom_userid → CsAgent 映射（便于按群成员 userid 反查客服）。 */
    public Map<String, CsAgent> mapByWecomUserid(Long tenantId) {
        Map<String, CsAgent> map = new HashMap<>();
        for (CsAgent a : listCsAgents(tenantId)) {
            if (StringUtils.isNotBlank(a.wecomUserid)) map.put(a.wecomUserid, a);
        }
        return map;
    }

    /** 读客服部门ID配置，解析为 List<Long>；异常/空返回空表（走回退识别）。 */
    private List<Long> parseDeptIds() {
        List<Long> ids = new ArrayList<>();
        try {
            String csv = bizConfigService.getString(CFG_CS_DEPT_IDS, "");
            if (StringUtils.isBlank(csv)) return ids;
            for (String s : csv.split("[,，]")) {
                String t = s.trim();
                if (t.isEmpty()) continue;
                try { ids.add(Long.parseLong(t)); } catch (NumberFormatException ignore) { }
            }
        } catch (Throwable e) {
            log.warn("[cs-agent] 读取客服部门ID配置失败，回退按 workbench_type/名称识别：{}", e.getMessage());
        }
        return ids;
    }

    private static String asStr(Object o) {
        return o == null ? null : o.toString();
    }
}
