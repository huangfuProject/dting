package com.dting.show.server.vos.monitoring;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 系统内存数据
 *
 * @author huangfu
 * @date 2022年10月28日15:37:39
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SystemMemoryData  extends BaseMonitorData {
    private static final long serialVersionUID = -1853627540645249038L;


    /**
     * 已经使用的内存
     */
    private long useSystemMemory;

    /**
     * 可用的最大内存
     */
    private long maxSystemMemory;
}
