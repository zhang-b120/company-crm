package com.qykj.finmaster.third.workWx.controller;

import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.third.workWx.service.WorkWxInvoiceKeywordCollectServiceImpl;
import com.qykj.finmaster.third.workWx.service.WorkWxMsgAuditPocServiceImpl;
import com.qykj.finmaster.third.workWx.service.WorkWxMsgAuditSyncServiceImpl;
import com.qykj.finmaster.third.workWx.service.WorkWxServiceImpl;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @Author zhb
 * @Date 2025-10-16
 * @Description
 */
@RestController
@RequestMapping("/workWx")
public class WorkWxController {

    @Autowired
    private WorkWxServiceImpl workWxService;

    @Autowired
    private WorkWxMsgAuditPocServiceImpl workWxMsgAuditPocService;

    @Autowired
    private WorkWxMsgAuditSyncServiceImpl workWxMsgAuditSyncService;

    @Autowired
    private WorkWxInvoiceKeywordCollectServiceImpl invoiceCollectService;

    @GetMapping("/saveGroupChatData")
    @PermitAll
    public AjaxResult saveGroupChatData(@RequestParam Integer limit, @RequestParam Boolean isFullSync){
        return AjaxResult.success(workWxService.saveGroupChatData(limit, isFullSync));
    }

    /**
     * 会话存档阶段0 PoC（工单 73 / WXMSG-01，临时接口）：拉 1 条群消息→解密→落库→关联群名/发送人。
     * 物料未就位时返回 PREREQ 状态、不实跑。
     */
    @GetMapping("/poc/syncOneMessage")
    @PermitAll
    public AjaxResult syncOneMessagePoc(){
        return AjaxResult.success(workWxMsgAuditPocService.syncOneMessage());
    }

    /**
     * 会话存档增量同步手动触发（工单 75 / WXMSG-02，运维联调用）。
     * 物料未就位时返回 PREREQ 状态、不实跑。
     */
    @GetMapping("/msgaudit/syncIncremental")
    @PermitAll
    public AjaxResult syncIncremental(){
        return AjaxResult.success(workWxMsgAuditSyncService.syncIncremental());
    }

    /**
     * 一次性回填历史客户名（工单 82 / WXRESP-03 修复，运维手动触发）：
     * 把 work_wx_message 中 from_type=2 且 from_name=from_user 的行按群 member_list 重映射 from_name。
     */
    @GetMapping("/msgaudit/backfillCustomerNames")
    @PermitAll
    public AjaxResult backfillCustomerNames(){
        return AjaxResult.success(workWxMsgAuditSyncService.backfillCustomerNames());
    }

    /**
     * 历史乱码内容直接回刷（工单85/WXMSG-06，运维手动触发一次）：
     * 对 work_wx_message.content/raw_content 应用守卫式 GBK→UTF-8 还原，不重拉、不依赖 5 天窗口。
     */
    @GetMapping("/msgaudit/backfillMojibakeContent")
    @PermitAll
    public AjaxResult backfillMojibakeContent(){
        return AjaxResult.success(workWxMsgAuditSyncService.backfillMojibakeContent());
    }

    /**
     * 发票业务采集一次性补传财务系统（工单92 / WXKW-08，运维手动触发）：
     * 查 [from,to] 内 INVOICE 命中按群补传，不读写采集水位线；dryRun 默认 true 仅预览不推。
     * @param from 起始时间 yyyy-MM-dd HH:mm:ss
     * @param to   结束时间 yyyy-MM-dd HH:mm:ss
     */
    @GetMapping("/invoice/backfill")
    @PermitAll
    public AjaxResult invoiceBackfill(@RequestParam("from") String from,
                                      @RequestParam("to") String to,
                                      @RequestParam(value = "dryRun", required = false, defaultValue = "true") Boolean dryRun) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date fromDate;
        Date toDate;
        try {
            fromDate = sdf.parse(from);
            toDate = sdf.parse(to);
        } catch (Exception e) {
            return AjaxResult.error("时间格式不正确（应为 yyyy-MM-dd HH:mm:ss）");
        }
        return AjaxResult.success(invoiceCollectService.backfill(fromDate, toDate, dryRun != null && dryRun));
    }
}