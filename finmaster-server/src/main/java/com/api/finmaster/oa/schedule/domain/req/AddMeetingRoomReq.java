package com.qykj.finmaster.oa.schedule.domain.req;

import lombok.Data;

/**
 * 新增/修改会议室请求
 */
@Data
public class AddMeetingRoomReq {

    /** 会议室ID（修改时传入） */
    private Long id;

    /** 会议室名称 */
    private String name;

    /** 位置 */
    private String location;

    /** 容纳人数 */
    private Integer capacity;

    /** 设施描述 */
    private String facilities;

    /** 是否启用（0=禁用，1=启用） */
    private Integer enabled;
}
