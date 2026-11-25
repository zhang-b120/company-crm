package com.qykj.finmaster.cs.profile.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qykj.finmaster.cs.profile.domain.req.GetProfilePageReq;
import com.qykj.finmaster.cs.profile.domain.resp.ProfilePageResp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CsProfileMapper {

    /**
     * 客户档案分页查询（联表 finmaster + financial）
     * 只显示在合同中有"代账报税-正式"或"代账报税-体验"业务类型的公司
     */
    IPage<ProfilePageResp> getProfilePage(IPage<ProfilePageResp> page,
                                          @Param("req") GetProfilePageReq req,
                                          @Param("tenantId") Long tenantId);
}
