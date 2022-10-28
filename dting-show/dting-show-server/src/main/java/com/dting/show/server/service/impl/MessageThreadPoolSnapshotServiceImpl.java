package com.dting.show.server.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.dting.show.server.entity.MessageThreadPoolSnapshot;
import com.dting.show.server.mapper.MessageThreadPoolSnapshotMapper;
import com.dting.show.server.service.MessageThreadPoolSnapshotService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 线程池采集快照
 *
 * @author huangfu
 * @date 2022年10月20日16:25:49
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class MessageThreadPoolSnapshotServiceImpl implements MessageThreadPoolSnapshotService {

    private final MessageThreadPoolSnapshotMapper messageThreadPoolSnapshotMapper;

    public MessageThreadPoolSnapshotServiceImpl(MessageThreadPoolSnapshotMapper messageThreadPoolSnapshotMapper) {
        this.messageThreadPoolSnapshotMapper = messageThreadPoolSnapshotMapper;
    }

    @Override
    public void batchSave(List<MessageThreadPoolSnapshot> messageThreadPoolSnapshotList) {
        if (CollectionUtil.isNotEmpty(messageThreadPoolSnapshotList)) {
            messageThreadPoolSnapshotMapper.batchInsert(messageThreadPoolSnapshotList);
        }

    }
}
