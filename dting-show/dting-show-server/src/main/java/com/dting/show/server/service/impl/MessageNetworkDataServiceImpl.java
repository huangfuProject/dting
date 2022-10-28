package com.dting.show.server.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.dting.show.server.entity.MessageNetworkSnapshot;
import com.dting.show.server.mapper.MessageNetworkSnapshotMapper;
import com.dting.show.server.service.MessageNetworkDataService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 网卡数据的业务服务
 *
 * @author huangfu
 * @date 2022年10月20日11:15:26
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class MessageNetworkDataServiceImpl implements MessageNetworkDataService {

    private final MessageNetworkSnapshotMapper messageNetworkSnapshotMapper;

    public MessageNetworkDataServiceImpl(MessageNetworkSnapshotMapper messageNetworkSnapshotMapper) {
        this.messageNetworkSnapshotMapper = messageNetworkSnapshotMapper;
    }

    @Override
    public void batchSave(List<MessageNetworkSnapshot> messageNetworkSnapshotList) {
        if(CollectionUtil.isNotEmpty(messageNetworkSnapshotList)) {
            messageNetworkSnapshotMapper.batchInsert(messageNetworkSnapshotList);
        }

    }
}
