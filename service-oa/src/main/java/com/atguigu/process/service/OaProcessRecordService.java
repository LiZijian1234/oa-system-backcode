package com.atguigu.process.service;


import com.atguigu.model.process.ProcessRecord;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 审批记录 服务类
 * </p>
 *
 * @author atguigu
 * @since 2023-03-26
 */
public interface OaProcessRecordService extends IService<ProcessRecord> {

    /**
     * 流程的id，
     * @param processId
     * @param status
     */
    void record(Long processId,
                Integer status,String description);
}
