/*
 * Copyright (c) 2008-2015, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hazelcast.simulator.tests.helpers;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceAware;
import com.hazelcast.core.IExecutorService;
import com.hazelcast.core.Member;
import com.hazelcast.core.Partition;
import com.hazelcast.core.PartitionService;
import com.hazelcast.instance.HazelcastInstanceImpl;
import com.hazelcast.instance.HazelcastInstanceProxy;
import com.hazelcast.instance.Node;
import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;
import com.hazelcast.spi.OperationService;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static com.hazelcast.simulator.utils.ReflectionUtils.getObjectFromField;

public final class HazelcastTestUtils {

    private static final ILogger LOGGER = Logger.getLogger(HazelcastTestUtils.class);

    private HazelcastTestUtils() {
    }

    public static String getPartitionDistributionInformation(HazelcastInstance hz) {
        Map<Member, Integer> partitionCountMap = new HashMap<Member, Integer>();
        int totalPartitions = 0;
        for (Partition partition : hz.getPartitionService().getPartitions()) {
            totalPartitions++;
            Member member = partition.getOwner();
            Integer count = partitionCountMap.get(member);
            if (count == null) {
                count = 0;
            }

            count++;
            partitionCountMap.put(member, count);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("total partitions:").append(totalPartitions).append("\n");
        for (Map.Entry<Member, Integer> entry : partitionCountMap.entrySet()) {
            Member member = entry.getKey();
            long count = entry.getValue();
            double percentage = count * 100d / totalPartitions;
            sb.append(member).append(" total=").append(count).append(" percentage=").append(percentage).append("%\n");
        }
        return sb.toString();
    }

    public static String getOperationCountInformation(HazelcastInstance hz) {
        Map<Member, Long> operationCountMap = getOperationCount(hz);

        long totalOps = 0;
        for (Long count : operationCountMap.values()) {
            totalOps += count;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("total operations: ").append(totalOps).append("\n");
        for (Map.Entry<Member, Long> entry : operationCountMap.entrySet()) {
            Member member = entry.getKey();
            long opsOnMember = entry.getValue();
            double percentage = opsOnMember * 100d / totalOps;
            sb.append(member)
                    .append(" operations: ").append(opsOnMember)
                    .append(" percentage: ").append(percentage).append("%\n");
        }
        return sb.toString();
    }

    public static Map<Member, Long> getOperationCount(HazelcastInstance hz) {
        IExecutorService executorService = hz.getExecutorService("operationCountExecutor");

        Map<Member, Future<Long>> futures = new HashMap<Member, Future<Long>>();
        for (Member member : hz.getCluster().getMembers()) {
            Future<Long> future = executorService.submitToMember(new GetOperationCount(), member);
            futures.put(member, future);
        }

        Map<Member, Long> result = new HashMap<Member, Long>();
        for (Map.Entry<Member, Future<Long>> entry : futures.entrySet()) {
            try {
                Member member = entry.getKey();
                Long value = entry.getValue().get();
                if (value == null) {
                    value = 0L;
                }
                result.put(member, value);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        return result;
    }

    public static final class GetOperationCount implements Callable<Long>, HazelcastInstanceAware, Serializable {

        private static final long serialVersionUID = 2875034360565495907L;

        private transient HazelcastInstance hz;

        @Override
        public Long call() throws Exception {
            try {
                Node node = getNode(hz);
                OperationService operationService = node.getNodeEngine().getOperationService();
                return operationService.getExecutedOperationCount();
            } catch (NoSuchMethodError e) {
                LOGGER.warning(e);
                return -1L;
            }
        }

        @Override
        public void setHazelcastInstance(HazelcastInstance hazelcastInstance) {
            this.hz = hazelcastInstance;
        }
    }

    public static void waitClusterSize(ILogger logger, HazelcastInstance hz, int clusterSize) throws InterruptedException {
        for (; ; ) {
            if (hz.getCluster().getMembers().size() >= clusterSize) {
                return;
            }

            logger.info("waiting cluster == " + clusterSize);
            Thread.sleep(1000);
        }
    }

    public static OperationService getOperationService(HazelcastInstance hz) {
        return getNode(hz).getNodeEngine().getOperationService();
    }

    public static Node getNode(HazelcastInstance hz) {
        HazelcastInstanceImpl impl = getHazelcastInstanceImpl(hz);
        return impl != null ? impl.node : null;
    }

    public static HazelcastInstanceImpl getHazelcastInstanceImpl(HazelcastInstance hz) {
        HazelcastInstanceImpl impl = null;
        if (hz instanceof HazelcastInstanceProxy) {
            return getObjectFromField(hz, "original");
        } else if (hz instanceof HazelcastInstanceImpl) {
            impl = (HazelcastInstanceImpl) hz;
        }
        return impl;
    }

    /**
     * Returns the next {@code long} key owned by the given Hazelcast instance.
     *
     * @param instance Hazelcast instance to search next key for
     * @param lastKey  last key to start search from
     * @return next key owned by given Hazelcast instance
     */
    public static long nextKeyOwnedBy(HazelcastInstance instance, long lastKey) {
        Member localMember = instance.getCluster().getLocalMember();
        PartitionService partitionService = instance.getPartitionService();
        while (true) {
            Partition partition = partitionService.getPartition(lastKey);
            if (localMember.equals(partition.getOwner())) {
                return lastKey;
            }
            lastKey++;
        }
    }

    public static boolean isMemberNode(HazelcastInstance instance) {
        return instance instanceof HazelcastInstanceProxy;
    }

    public static boolean isClient(HazelcastInstance instance) {
        return !isMemberNode(instance);
    }
}
