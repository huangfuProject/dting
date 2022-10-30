package com.dting.show.server.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dting.show.server.conditions.MemoryBatchCondition;
import com.dting.show.server.constant.RedisKeyUtil;
import com.dting.show.server.entity.MessageMemorySnapshot;
import com.dting.show.server.mapper.MessageMemorySnapshotMapper;
import com.dting.show.server.service.MessageMemorySnapshotService;
import com.dting.show.server.tasks.MemoryDataRefreshTask;
import com.dting.show.server.utils.ScheduledTaskManagement;
import com.dting.show.server.vos.monitoring.*;
import org.springframework.aop.framework.AopContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 内存业务实现类
 *
 * @author huangfu
 * @date 2022年10月20日08:21:11
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class MessageMemorySnapshotServiceImpl implements MessageMemorySnapshotService {

    private final MessageMemorySnapshotMapper messageMemorySnapshotMapper;

    private final StringRedisTemplate redisTemplate;

    public MessageMemorySnapshotServiceImpl(MessageMemorySnapshotMapper messageMemorySnapshotMapper, StringRedisTemplate redisTemplate) {
        this.messageMemorySnapshotMapper = messageMemorySnapshotMapper;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public MemoryDataMonitoringVo memoryMonitoring(MemoryBatchCondition memoryBatchCondition, boolean enablePlan) {
        long endTime = memoryBatchCondition.getEndTime();
        if (endTime < 0) {
            memoryBatchCondition.setEndTime(System.currentTimeMillis());
        }
        MemoryDataVo memoryDataVo = ((MessageMemorySnapshotService) AopContext.currentProxy()).memoryQueryByCondition(memoryBatchCondition);
        MemoryDataMonitoringVo memoryDataMonitoringVo = new MemoryDataMonitoringVo();
        String monitorId = IdUtil.fastSimpleUUID();
        memoryDataMonitoringVo.setMonitorId(monitorId);
        memoryDataMonitoringVo.setMemoryDataVo(memoryDataVo);
        if (enablePlan) {
            //判断redis
            String sessionActiveKey = RedisKeyUtil.sessionActiveKeyFormat(monitorId);
            //开启数据 并设置过期时间
            redisTemplate.opsForValue().set(sessionActiveKey, "1", 120, TimeUnit.SECONDS);
            //将结束时间设置为开始时间
            memoryBatchCondition.setStartTime(memoryBatchCondition.getEndTime());
            //生成任务
            MemoryDataRefreshTask memoryDataRefreshTask = new MemoryDataRefreshTask(memoryBatchCondition, monitorId);
            //5秒后重新执行
            ScheduledTaskManagement.addJob(memoryDataRefreshTask, System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(5));
        }
        return memoryDataMonitoringVo;
    }

    @Override
    public MemoryDataVo memoryQueryByCondition(MemoryBatchCondition memoryBatchCondition) {
        //查询数据
        List<MessageMemorySnapshot> messageMemorySnapshots = ((MessageMemorySnapshotService) AopContext.currentProxy()).memoryBatchFindByCondition(memoryBatchCondition);
        //数据转换
        MemoryDataVo memoryDataVo = new MemoryDataVo();
        messageMemorySnapshots.forEach(messageMemorySnapshot -> {
            //系统内存数据
            SystemMemoryData systemMemoryData = new SystemMemoryData();
            //jvm内存数据
            JvmMemoryData jvmMemoryData = new JvmMemoryData();
            //交换区数据
            SystemSwapData systemSwapData = new SystemSwapData();

            //系统内存数据
            systemMemoryData.setMaxSystemMemory(memoryDataFormat(messageMemorySnapshot.getTotalMemory()));
            systemMemoryData.setUseSystemMemory(memoryDataFormat(messageMemorySnapshot.getUseMemory()));
            systemMemoryData.setDateValue(messageMemorySnapshot.getCollectTime());
            //jvm内存数据
            jvmMemoryData.setMaxJvmMemory(memoryDataFormat(messageMemorySnapshot.getJvmTotalMemory()));
            jvmMemoryData.setUseJvmMemory(memoryDataFormat(messageMemorySnapshot.getJvmUseMemory()));
            jvmMemoryData.setDateValue(messageMemorySnapshot.getCollectTime());
            //交换区数据
            systemSwapData.setMaxSystemSwap(memoryDataFormat(messageMemorySnapshot.getTotalSwap()));
            systemSwapData.setUseSystemSwap(memoryDataFormat(messageMemorySnapshot.getUseSwap()));
            systemSwapData.setDateValue(messageMemorySnapshot.getCollectTime());
            //开始追加数据
            memoryDataVo.addSystemMemoryData(systemMemoryData);
            memoryDataVo.addJvmMemoryData(jvmMemoryData);
            memoryDataVo.addSystemSwapData(systemSwapData);

        });
        if (CollectionUtil.isNotEmpty(messageMemorySnapshots)) {
            memoryDataVo.setLastTime(messageMemorySnapshots.get(messageMemorySnapshots.size() - 1).getCollectTime());
        }
        return memoryDataVo;
    }

    /**
     * 内存数据  将b转换为m
     *
     * @param memoryData 内存数据
     * @return 转换号的数据
     */
    private long memoryDataFormat(long memoryData) {
        return memoryData / 1024 / 1024;
    }

    @Override
    public List<MessageMemorySnapshot> memoryBatchFindByCondition(MemoryBatchCondition memoryBatchCondition) {
        QueryWrapper<MessageMemorySnapshot> queryWrapper = new QueryWrapper<>();

        String serverEnv = memoryBatchCondition.getServerEnv();
        String serverKey = memoryBatchCondition.getServerKey();
        String instanceKey = memoryBatchCondition.getInstanceKey();


        if (StrUtil.isBlank(serverEnv) || StrUtil.isBlank(serverKey) || StrUtil.isBlank(instanceKey)) {
            return new ArrayList<>();
        }

        Long startTime = memoryBatchCondition.getStartTime();
        Long endTime = memoryBatchCondition.getEndTime();

        // 消息标签
        queryWrapper.eq("server_env", serverEnv);
        queryWrapper.eq("server_key", serverKey);
        queryWrapper.eq("instance_key", instanceKey);

        if (startTime != null && startTime > 0) {
            queryWrapper.gt("collect_time", startTime);
        }

        if (endTime != null && endTime > 0) {
            queryWrapper.le("collect_time", endTime);
        }
        //根据时间 正序排列
        queryWrapper.orderByAsc("collect_time");
        return messageMemorySnapshotMapper.selectList(queryWrapper);
    }

    @Override
    public void batchSave(List<MessageMemorySnapshot> messageMemorySnapshotList) {
        if (CollectionUtil.isNotEmpty(messageMemorySnapshotList)) {
            messageMemorySnapshotMapper.batchInsert(messageMemorySnapshotList);
        }
    }
}
