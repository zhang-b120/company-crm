package com.qykj.finmaster.knowledge.attachment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
@TableName("knowledge_attachment")
public class KnowledgeAttachment {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("article_id")
    private Long articleId;

    @TableField("file_name")
    private String fileName;

    @TableField("file_url")
    private String fileUrl;

    @TableField("file_size")
    private Long fileSize;

    @TableField("file_ext")
    private String fileExt;

    @TableField("download_count")
    private Integer downloadCount;

    @TableField("uploader_id")
    private Long uploaderId;

    @TableField("uploader_name")
    private String uploaderName;

    @TableField("tenant_id")
    private Long tenantId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("create_time")
    private Date createTime;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
