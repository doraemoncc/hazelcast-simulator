/*
 * Copyright (c) 2008-2013, Hazelcast, Inc. All Rights Reserved.
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
package com.hazelcast.stabilizer.tests.map;

import com.hazelcast.config.MapStoreConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.stabilizer.tests.TestContext;
import com.hazelcast.stabilizer.tests.TestRunner;
import com.hazelcast.stabilizer.tests.annotations.Run;
import com.hazelcast.stabilizer.tests.annotations.Setup;
import com.hazelcast.stabilizer.tests.annotations.Verify;
import com.hazelcast.stabilizer.tests.annotations.Warmup;
import com.hazelcast.stabilizer.tests.utils.ThreadSpawner;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class MapTTLSaturationTest {

    public String basename = this.getClass().getName();
    public int threadCount = 3;

    private TestContext testContext;
    private HazelcastInstance targetInstance;

    public double aproxHeapUsageFactor = 0.9;
    private long aproxEntryBytesSize = 238;

    private IMap map;

    private long baseLineUsed;

    public MapTTLSaturationTest(){
    }

    @Setup
    public void setup(TestContext testContext) throws Exception {
        this.testContext = testContext;
        targetInstance = testContext.getTargetInstance();

        map = targetInstance.getMap(basename);
    }

    @Warmup(global = false)
    public void warmup() {

        try{
            targetInstance.getConfig().getMapConfig(basename);

            long free = Runtime.getRuntime().freeMemory();
            long total =  Runtime.getRuntime().totalMemory();
            baseLineUsed = total - free;
            long maxBytes =  Runtime.getRuntime().maxMemory();
            double usedOfMax = 100.0 * ( (double) baseLineUsed  / (double) maxBytes);


            System.out.println(basename+" before Init");
            System.out.println(basename+" free = "+humanReadableByteCount(free, true)+" = "+free);
            System.out.println(basename+" used = "+humanReadableByteCount(baseLineUsed, true)+" = "+baseLineUsed);
            System.out.println(basename+" max = "+humanReadableByteCount(maxBytes, true)+" = "+maxBytes);
            System.out.println(basename+" usedOfMax = "+usedOfMax+"%");


            long maxEntries = (long) ( (maxBytes / aproxEntryBytesSize) * aproxHeapUsageFactor) ;

            for(long i=0; i <  maxEntries ; i++){
                map.put(i, i, 24, TimeUnit.HOURS);
            }

            free = Runtime.getRuntime().freeMemory();
            total =  Runtime.getRuntime().totalMemory();
            long nowUsed = total - free;
            maxBytes =  Runtime.getRuntime().maxMemory();
            usedOfMax =  100.0 * ( (double) nowUsed  / (double) maxBytes);

            System.out.println();
            System.out.println(basename+" After Init");
            System.out.println(basename+" map = "+ map.size());
            System.out.println(basename+" free = "+humanReadableByteCount(free, true)+" = "+free);
            System.out.println(basename+" used = "+humanReadableByteCount(nowUsed, true)+" = "+nowUsed);
            System.out.println(basename+" max = "+humanReadableByteCount(maxBytes, true)+" = "+maxBytes);
            System.out.println(basename+" usedOfMax = "+usedOfMax+"%");

            long avgEntryBytes = (nowUsed - baseLineUsed) / maxEntries;

            System.out.println(basename+" avgEntryBytes = "+avgEntryBytes+" vs "+aproxEntryBytesSize+" estimate used");


        }catch(UnsupportedOperationException e){}
    }

    @Run
    public void run() {
        ThreadSpawner spawner = new ThreadSpawner(testContext.getTestId());
        for (int k = 0; k < threadCount; k++) {
            spawner.spawn(new Worker());
        }
        spawner.awaitCompletion();
    }

    private class Worker implements Runnable {
        @Override
        public void run() {
            while (!testContext.isStopped()) {

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Verify(global = false)
    public void globalVerify() throws Exception {
        System.out.println();
        System.out.println(basename+" Verify");

        long free = Runtime.getRuntime().freeMemory();
        long total =  Runtime.getRuntime().totalMemory();
        long used = total - free;
        long maxBytes =  Runtime.getRuntime().maxMemory();
        double usedOfMax = 100.0 * ( (double) used  / (double) maxBytes);


        System.out.println(basename+" map = "+ map.size());
        System.out.println(basename+ "free = "+humanReadableByteCount(free, true)+" = "+free);
        System.out.println(basename+ "used = "+humanReadableByteCount(used, true)+" = "+used);
        System.out.println(basename+ "max = "+humanReadableByteCount(maxBytes, true)+" = "+maxBytes);
        System.out.println(basename+ "usedOfMax = "+usedOfMax+"%");


        long avgEntryBytes = (used - baseLineUsed) / map.size();
        System.out.println(basename+" avgEntryBytes (after Verify and gc ? )= "+avgEntryBytes+" vs "+aproxEntryBytesSize+" estimate used");
    }

    public static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    public static void main(String[] args) throws Throwable {
        new TestRunner(new MapTTLSaturationTest()).run();
    }
}