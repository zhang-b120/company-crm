package com.qykj.finmaster.knowledge.attachment.controller;

import cn.hutool.core.io.file.FileNameUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qykj.finmaster.common.core.controller.BaseController;
import com.qykj.finmaster.common.core.domain.constansts.FileConstants;
import com.qykj.finmaster.common.core.domain.resp.Result;
import com.qykj.finmaster.knowledge.article.entity.KnowledgeArticle;
import com.qykj.finmaster.knowledge.article.manager.KnowledgeArticleManager;
import com.qykj.finmaster.knowledge.attachment.entity.KnowledgeAttachment;
import com.qykj.finmaster.knowledge.attachment.manager.KnowledgeAttachmentManager;
import com.qykj.finmaster.security.LoginUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Random;

@Slf4j
@RestController
@RequestMapping("/knowledge/attachment")
public class KnowledgeAttachmentController extends BaseController {

    @Autowired
    private KnowledgeAttachmentManager attachmentManager;

    @Autowired
    private KnowledgeArticleManager articleManager;

    @GetMapping("/list/{articleId}")
    public Result<?> list(@PathVariable Long articleId) {
        List<KnowledgeAttachment> list = attachmentManager.list(new LambdaQueryWrapper<KnowledgeAttachment>()
                .eq(KnowledgeAttachment::getArticleId, articleId)
                .orderByAsc(KnowledgeAttachment::getCreateTime));
        return Result.ok(list);
    }

    /**
     * 上传附件（PDF/Word/Excel等）
     */
    @PostMapping("/upload/{articleId}")
    public Result<?> upload(@PathVariable Long articleId,
                            @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return Result.error("文件不能为空");
        LoginUser u = getLoginUser();
        KnowledgeArticle article = articleManager.getById(articleId);
        if (article == null) return Result.error("文章不存在");

        boolean isAuthor = article.getAuthorId() != null && article.getAuthorId().equals(u.getUserId());
        boolean hasAdd = u.getPermissions() != null && u.getPermissions().contains("knowledge:article:add");
        boolean hasEdit = u.getPermissions() != null && u.getPermissions().contains("knowledge:article:edit");
        if (!isAuthor && !hasAdd && !hasEdit) {
            return Result.error("无权上传附件");
        }

        try {
            String ext = FileNameUtil.getSuffix(file.getOriginalFilename());
            String fileName = "KBA_" + articleId + "_" + System.currentTimeMillis() + "_"
                    + new Random().nextInt(10000) + (ext != null && !ext.isBlank() ? "." + ext : "");
            File dir = new File(FileConstants.FILE_URL_KNOWLEDGE_ATTACHMENT);
            if (!dir.exists()) dir.mkdirs();
            file.transferTo(new File(FileConstants.FILE_URL_KNOWLEDGE_ATTACHMENT + File.separator + fileName));

            KnowledgeAttachment att = new KnowledgeAttachment();
            att.setArticleId(articleId);
            att.setFileName(file.getOriginalFilename());
            att.setFileUrl(FileConstants.FILE_GET_KNOWLEDGE_ATTACHMENT + fileName);
            att.setFileSize(file.getSize());
            att.setFileExt(ext);
            att.setDownloadCount(0);
            att.setUploaderId(u.getUserId());
            att.setUploaderName(u.getRealName());
            att.setTenantId(u.getTenantId());
            att.setCreateTime(new Date());
            attachmentManager.save(att);
            return Result.ok(att);
        } catch (Exception e) {
            log.error("知识库附件上传失败 articleId={}", articleId, e);
            return Result.error("附件上传失败：" + e.getMessage());
        }
    }

    /**
     * 下载次数 +1（前端 a 标签下载前调用）
     */
    @PostMapping("/download/{id}")
    public Result<?> markDownload(@PathVariable Long id) {
        attachmentManager.lambdaUpdate()
                .setSql("download_count = COALESCE(download_count,0) + 1")
                .eq(KnowledgeAttachment::getId, id).update();
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        LoginUser u = getLoginUser();
        KnowledgeAttachment att = attachmentManager.getById(id);
        if (att == null) return Result.ok();
        boolean isUploader = att.getUploaderId() != null && att.getUploaderId().equals(u.getUserId());
        boolean hasEdit = u.getPermissions() != null && u.getPermissions().contains("knowledge:article:edit");
        if (!isUploader && !hasEdit) return Result.error("无权删除附件");
        attachmentManager.removeById(id);
        return Result.ok();
    }
}
