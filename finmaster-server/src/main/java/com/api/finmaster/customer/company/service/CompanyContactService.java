package com.qykj.finmaster.customer.company.service;

import com.qykj.finmaster.common.exception.BusinessException;
import com.qykj.finmaster.customer.company.domain.CompanyContact;
import com.qykj.finmaster.customer.company.domain.req.AddCompanyContactReq;
import com.qykj.finmaster.customer.company.domain.req.UpdateCompanyContactReq;
import com.qykj.finmaster.customer.company.manager.CompanyContactManager;
import com.qykj.finmaster.security.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 公司联系人 CRUD
 * 客户主联系人仍走 customer_info，本服务只承载公司维度的额外联系人。
 */
@Service
public class CompanyContactService {

    @Autowired
    private CompanyContactManager manager;

    /** 按公司ID取联系人列表，按创建时间倒序 */
    public List<CompanyContact> listByCompanyId(Long companyId) {
        if (companyId == null) return java.util.Collections.emptyList();
        return manager.lambdaQuery()
                .eq(CompanyContact::getCompanyId, companyId)
                .orderByDesc(CompanyContact::getCreateTime)
                .list();
    }

    public CompanyContact add(AddCompanyContactReq req, LoginUser loginUser) {
        CompanyContact entity = new CompanyContact();
        entity.setCompanyId(req.getCompanyId());
        entity.setName(req.getName());
        entity.setPhone(req.getPhone());
        entity.setPosition(req.getPosition());
        entity.setRemark(req.getRemark());
        if (loginUser != null) {
            entity.setTenantId(loginUser.getTenantId());
            entity.setCreateBy(loginUser.getUsername());
            entity.setCreateByName(loginUser.getRealName());
            entity.setUpdateBy(loginUser.getUsername());
            entity.setUpdateByName(loginUser.getRealName());
        }
        Date now = new Date();
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        manager.save(entity);
        return entity;
    }

    public boolean update(UpdateCompanyContactReq req, LoginUser loginUser) {
        CompanyContact exist = manager.getById(req.getId());
        if (exist == null) {
            throw new BusinessException("联系人不存在");
        }
        // 仅允许修改业务字段，company_id / tenant_id / create_* 不改
        CompanyContact upd = new CompanyContact();
        upd.setId(req.getId());
        upd.setName(req.getName());
        upd.setPhone(req.getPhone());
        upd.setPosition(req.getPosition());
        upd.setRemark(req.getRemark());
        if (loginUser != null) {
            upd.setUpdateBy(loginUser.getUsername());
            upd.setUpdateByName(loginUser.getRealName());
        }
        upd.setUpdateTime(new Date());
        return manager.updateById(upd);
    }

    public boolean delete(Long id) {
        if (id == null) return false;
        return manager.removeById(id);
    }
}
