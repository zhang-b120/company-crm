package com.qykj.finmaster.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qykj.finmaster.task.domain.CommonTask;
import com.qykj.finmaster.task.domain.resp.GetCommonTaskInfoResp;
import com.qykj.finmaster.task.domain.resp.GetCommonTaskPageResp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;


@Mapper
public interface CommonTaskMapper extends BaseMapper<CommonTask> {

    List<GetCommonTaskPageResp> getCommonTaskPage(@Param("params") Map<String, Object> params);

    GetCommonTaskInfoResp getCommonTaskInfo(@Param("id") Long id);
}
