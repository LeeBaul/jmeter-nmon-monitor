package com.libaolu.nmon.utils;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * <p/>
 *
 * @author libaolu
 * @version 1.0
 * @dateTime 2020/2/17 14:04
 **/
public class AvgDataTag {

    /**
     * @param map         即NmonDataLoader中的nmonTxxxxDataMap
     * @param aaaColArray nmon文件AAA行的数据
     * @return
     */
    public String avgDataCalc(HashMap<String, ArrayList<String>> map, ArrayList aaaColArray) {
        DecimalFormat df = new DecimalFormat("0.000");
        StringBuffer sb = new StringBuffer();
        String systemOs = "";
        //判断当前NMON文件监控的系统:AIX or Linux
        if (aaaColArray != null) {
            for (int i = 0; i < aaaColArray.size(); i++) {
                String aaaLine = (String) aaaColArray.get(i);
                if (aaaLine.startsWith("AAA,OS,Linux")) {
                    systemOs = "linux";
                } else {
                    if (aaaLine.startsWith("AAA,AIX")) {
                        systemOs = "aix";
                    }
                }

            }

        }

        if (map.get("CPU_ALL") != null) {
            List<Double> cpuUser = new ArrayList<Double>();
            List<Double> cpuSys = new ArrayList<Double>();
            List<Double> cpuWait = new ArrayList<Double>();
//			List<Double> cpuIdle = new ArrayList<Double>();
            //>>>>>>>>>>>>>统计CPU消耗情况<<<<<<<<<<<<<<<<
            for (String cpuAll : map.get("CPU_ALL")) {
                String[] cols = cpuAll.split(",");
                cpuUser.add(Double.parseDouble(cols[2]));//User%
                cpuSys.add(Double.parseDouble(cols[3]));//Sys%
                cpuWait.add(Double.parseDouble(cols[4]));//Wait%
//				cpuIdle.add(Double.parseDouble(cols[5]));//Idle%
            }
            //>>>>>>>>>>>>>计算avgcpuUser<<<<<<<<<<<<<<<<
            String avgCpuUser = "0.000";
            double sumCpuUser = 0.0D;
            for (Double double1 : cpuUser) {
                sumCpuUser += double1;
            }
            avgCpuUser = df.format(sumCpuUser / cpuUser.size());

            //>>>>>>>>>>>>>计算avgCpuSys<<<<<<<<<<<<<<<<
            String avgCpuSys = "0.000";
            double sumCpuSys = 0.0D;
            for (Double double2 : cpuSys) {
                sumCpuSys += double2;
            }
            avgCpuSys = df.format(sumCpuSys / cpuSys.size());

            //>>>>>>>>>>>>>计算avgCpuWait<<<<<<<<<<<<<<<<
            String avgCpuWait = "0.000";
            double sumCpuWait = 0.0D;
            for (Double double3 : cpuWait) {
                sumCpuWait += double3;
            }
            avgCpuWait = df.format(sumCpuWait / cpuWait.size());

            //>>>>>>>>>>>>>计算avgCpuIdle<<<<<<<<<<<<<<<<
            String avgCpuIdle = "0.000";
//			原avgCpuIdle计算方法
//			double sumCpuIdle= 0.0D;
//			for (Double double4 : cpuIdle) {
//				sumCpuIdle +=double4.doubleValue();
//			}
//			avgCpuIdle = df.format(sumCpuIdle / cpuIdle.size());

            //新avgCpuIdle计算方法
            avgCpuIdle = df.format(100 - (sumCpuUser / cpuUser.size() + sumCpuSys / cpuSys.size() + sumCpuWait / cpuWait.size()));


            //>>>>>>>>>>>>>计算avgCpuUse 即 Busy<<<<<<<<<<<<<<<<
            double Busy = sumCpuUser / cpuUser.size() + sumCpuSys / cpuSys.size();
            String avgCpuUse = df.format(Busy);

            //>>>>>>>>>>>>>StringBuffer拼接CPU数据<<<<<<<<<<<<<<<<
            sb.append("------------------------------  CPU  ------------------------------\n")
                    .append("CPUUser=").append(avgCpuUser).append("%      CPUSys=").append(avgCpuSys).append("%      CPUWait=").append(avgCpuWait)
                    .append("%\nCPUIdle=").append(avgCpuIdle).append("%      CPUUse=").append(avgCpuUse).append("%\n");
        }

        if (map.get("PROC") != null) {//nmon结果文件中PROC的sheet页 包含nmon内核内部的统计信息，Runnable域是使用的平均时间间隔，单位：比率/秒
            List<Double> runnable = new ArrayList<Double>();
            List<Double> pswitch = new ArrayList<Double>();
            List<Double> syscall = new ArrayList<Double>();
            for (String procAll : map.get("PROC")) {
                String[] cols = procAll.split(",");
                runnable.add(Double.parseDouble(cols[2]));//运行队列中的内核线程平均数
                pswitch.add(Double.parseDouble(cols[4]));//上下文开关个数
                syscall.add(Double.parseDouble(cols[5]));//系统调用总数
            }
            //>>>>>>>>>>>>>计算avgRunnable<<<<<<<<<<<<<<<<
            String avgRunnable = "0.000";
            double sumRunnable = 0.0D;
            for (Double double1 : runnable) {
                sumRunnable += double1;
            }
            avgRunnable = df.format(sumRunnable / runnable.size());

            //>>>>>>>>>>>>>计算avgRunnable<<<<<<<<<<<<<<<<
            String avgPswitch = "0.000";
            double sumPswitch = 0.0D;
            for (Double double2 : pswitch) {
                sumPswitch += double2;
            }
            avgPswitch = df.format(sumPswitch / pswitch.size());

            //>>>>>>>>>>>>>计算avgSyscall<<<<<<<<<<<<<<<<
            String avgSyscall = "0.000";
            double sumSyscall = 0.0D;
            for (Double double3 : syscall) {
                sumSyscall += double3;
            }
            avgSyscall = df.format(sumSyscall / syscall.size());

            //>>>>>>>>>>>>>StringBuffer拼接PROC数据<<<<<<<<<<<<<<<<
            sb.append("------------------------------  PROC  ------------------------------\n")
                    .append("RunQueue=").append(avgRunnable).append("      pswitch=").append(avgPswitch).append("      syscall=").append(avgSyscall).append("\n");
        }

        /**
         * 由于AIX &Linux 的nmon结果的MEM、NET等域   存在差异 所以要分开计算
         */
        if ("aix".equals(systemOs)) {//aix
            //>>>>>>>>>>>>>统计Mem消耗情况<<<<<<<<<<<<<<<<
            if (map.get("MEM") != null) {
                List<Double> realFree = new ArrayList<Double>();
                List<Double> virtualFree = new ArrayList<Double>();
                List<Double> realTotal = new ArrayList<Double>();
                List<Double> virtualTotal = new ArrayList<Double>();
                for (String memAll : map.get("MEM")) {
                    String[] cols = memAll.split(",");
                    realFree.add(Double.parseDouble(cols[4]));//Real free(MB)
                    virtualFree.add(Double.parseDouble(cols[5]));//virtualFree free(MB)
                    realTotal.add(Double.parseDouble(cols[6]));//Real total(MB)
                    virtualTotal.add(Double.parseDouble(cols[7]));//Virtual total(MB)
                }

                //>>>>>>>>>>>>>计算avgRealFree<<<<<<<<<<<<<<<<
                String avgRealFree = "0.000";
                double sumRealFree = 0.0D;
                for (Double double1 : realFree) {
                    sumRealFree += double1;
                }
                avgRealFree = df.format(sumRealFree / realFree.size());

                //>>>>>>>>>>>>>计算avgVirtualFree<<<<<<<<<<<<<<<<
                String avgVirtualFree = "0.000";
                double sumVirtualFree = 0.0D;
                for (Double double2 : virtualFree) {
                    sumVirtualFree += double2;
                }
                avgVirtualFree = df.format(sumVirtualFree / virtualFree.size());

                //>>>>>>>>>>>>>计算avgVirtualFree<<<<<<<<<<<<<<<<
                String avgRealTotal = "0.000";
                double sumRealTotal = 0.0D;
                for (Double double3 : realTotal) {
                    sumRealTotal += double3;
                }
                avgRealTotal = df.format(sumRealTotal / realTotal.size());

                //>>>>>>>>>>>>>计算avgVirtualFree<<<<<<<<<<<<<<<<
                String avgVirtualTotal = "0.000";
                double sumVirtualTotal = 0.0D;
                for (Double double4 : virtualTotal) {
                    sumVirtualTotal += double4;
                }
                avgVirtualTotal = df.format(sumVirtualTotal / virtualTotal.size());

                //>>>>>>>>>>>>>计算avgVirtualUse%<<<<<<<<<<<<<<<<
                String avgVirtualUse = "0.000";
                avgVirtualUse = df.format(100 - (sumVirtualFree / virtualFree.size()) / (sumVirtualTotal / virtualTotal.size()) * 100);

                //>>>>>>>>>>>>>计算avgRealUse%<<<<<<<<<<<<<<<<
                String avgRealUse = "0.000";
                avgRealUse = df.format(100 - (sumRealFree / realFree.size()) / (sumRealTotal / realTotal.size()) * 100);

                //>>>>>>>>>>>>>StringBuffer拼接Mem数据<<<<<<<<<<<<<<<<
                sb.append("------------------------------  Memory  ------------------------------\n")
                        .append("RealUse=").append(avgRealUse).append("%      VirtualUse=").append(avgVirtualUse).append("%      RealFree=").append(avgRealFree)
                        .append("MB\nVirtualFree=").append(avgVirtualFree).append("MB      RealTotal=").append(avgRealTotal).append("MB")
                        .append("      VirtualTotal=").append(avgVirtualTotal).append("MB\n");
            }
            if (map.get("MEMNEW") != null) {
                List<Double> process = new ArrayList<Double>();
                List<Double> fScache = new ArrayList<Double>();
                List<Double> system = new ArrayList<Double>();
                for (String memAll : map.get("MEMNEW")) {
                    String[] cols = memAll.split(",");
                    process.add(Double.parseDouble(cols[2]));
                    fScache.add(Double.parseDouble(cols[3]));
                    system.add(Double.parseDouble(cols[4]));
                }

                //>>>>>>>>>>>>>计算avgProcess<<<<<<<<<<<<<<<<
                String avgProcess = "0.000";
                double sumProcess = 0.0D;
                for (Double double1 : process) {
                    sumProcess += double1;
                }
                avgProcess = df.format(sumProcess / process.size());

                //>>>>>>>>>>>>>计算avgFScache<<<<<<<<<<<<<<<<
                String avgFScache = "0.000";
                double sumFScache = 0.0D;
                for (Double double2 : fScache) {
                    sumFScache += double2;
                }
                avgFScache = df.format(sumFScache / fScache.size());

                //>>>>>>>>>>>>>计算avgFScache<<<<<<<<<<<<<<<<
                String avgSystem = "0.000";
                double sumSystem = 0.0D;
                for (Double double3 : system) {
                    sumSystem += double3;
                }
                avgSystem = df.format(sumSystem / system.size());

                //>>>>>>>>>>>>>StringBuffer拼接Mem数据<<<<<<<<<<<<<<<<
                sb.append("ProcessMem=").append(avgProcess).append("%      FScacheMem=").append(avgFScache).append("%      SystemMem=").append(avgSystem).append("%\n");

            }
            if (map.get("NET") != null) {
                //>>>>>>>>>>>>>StringBuffer拼接NET数据<<<<<<<<<<<<<<<<
                sb.append("------------------------------  NetWork  ------------------------------\n");
                HashMap<String, Integer> netHeaderIndexMap = new HashMap<String, Integer>();
                int m = 0;
                //解析NET_HEADER 获取指定域的位置
                for (String header : map.get("NET_HEADER")) {
                    String[] headerNames = header.split(",");
                    for (int i = 0; i < headerNames.length; i++) {
                        if (headerNames[i].endsWith("read-KB/s") || headerNames[i].endsWith("write-KB/s")) {//保存所要header域的下标
                            netHeaderIndexMap.put(headerNames[i], i);
                        }
                    }

                    for (String key : netHeaderIndexMap.keySet()) {
                        List<Double> tempNetData = new ArrayList<Double>();
                        for (String netAll : map.get("NET")) {
                            String[] cols = netAll.split(",");
                            tempNetData.add(Double.parseDouble(cols[netHeaderIndexMap.get(key)]));
                        }
                        if ("0.000".equals(avgNetCalc(tempNetData))) {

                        } else {
                            sb.append(key).append("=").append(avgNetCalc(tempNetData)).append("      ");
                            if ((++m) % 3 == 0) {
                                sb.append("\n");
                            }
                        }

                    }
                    sb.append("\n");
                }
            }
        } else { //Linux
            if (map.get("MEM") != null) {
                List<Double> memTotal = new ArrayList<Double>();
                List<Double> memFree = new ArrayList<Double>();
                List<Double> cached = new ArrayList<Double>();
                List<Double> buffers = new ArrayList<Double>();

                for (String memAll : map.get("MEM")) {
                    String[] cols = memAll.split(",");
                    if (memTotal.size() == 0) {//memTotal中仅存放一次值即可（所有的memTotal值均是同一个值）
                        memTotal.add(Double.parseDouble(cols[2]));//memtotal(MB)
                    }
                    memFree.add(Double.parseDouble(cols[6]));//memfree(MB)
                    cached.add(Double.parseDouble(cols[11]));//cached(MB)
                    buffers.add(Double.parseDouble(cols[14]));//buffers(MB)
                }

                //>>>>>>>>>>>>>计算totalMemUse<<<<<<<<<<<<<<<<
                String totalMemUse = "0.000";
                double sumMemFree = 0.0D;
                for (Double double1 : memFree) {
                    sumMemFree += double1;
                }
                totalMemUse = df.format(100 - (sumMemFree / memFree.size()) / memTotal.get(0) * 100);//公式：100 - memfree平均值 /总内存*100

                //>>>>>>>>>>>>>计算cached/memtotal<<<<<<<<<<<<<<<<
                String cachedPercent = "0.000";
                double sumMemCached = 0.0D;
                for (Double double2 : cached) {
                    sumMemCached += double2;
                }
                cachedPercent = df.format((sumMemCached / cached.size()) / memTotal.get(0) * 100);//公式：100 - cached平均值 /总内存*100

                //>>>>>>>>>>>>>计算buffers/memtotal<<<<<<<<<<<<<<<<
                String buffersPercent = "0.000";
                double sumMemBuffers = 0.0D;
                for (Double double3 : buffers) {
                    sumMemBuffers += double3;
                }
                buffersPercent = df.format((sumMemBuffers / buffers.size()) / memTotal.get(0) * 100);//公式：100 - buffers平均值 /总内存*100

                //>>>>>>>>>>>>>StringBuffer拼接Mem数据<<<<<<<<<<<<<<<<
                sb.append("------------------------------  Memory  ------------------------------\n")
                        .append("TotalMemUse=").append(totalMemUse).append("%      cached/memtotal=").append(cachedPercent).append("%      buffers/memtotal=").append(buffersPercent)
                        .append("%\n");
            }
            if (map.get("NET") != null) {
                //>>>>>>>>>>>>>StringBuffer拼接NET数据<<<<<<<<<<<<<<<<
                sb.append("------------------------------  NetWork  ------------------------------\n");
                HashMap<String, Integer> netHeaderIndexMap = new HashMap<String, Integer>();
                int m = 0;
                //解析NET_HEADER 获取指定域的位置
                for (String header : map.get("NET_HEADER")) {
                    String[] headerNames = header.split(",");
                    for (int i = 0; i < headerNames.length; i++) {
                        if (headerNames[i].endsWith("read-KB/s") || headerNames[i].endsWith("write-KB/s")) {//保存所要header域的下标
                            netHeaderIndexMap.put(headerNames[i], i);
                        }
                    }

                    for (String key : netHeaderIndexMap.keySet()) {
                        List<Double> tempNetData = new ArrayList<Double>();
                        for (String netAll : map.get("NET")) {
                            String[] cols = netAll.split(",");
                            tempNetData.add(Double.parseDouble(cols[netHeaderIndexMap.get(key)]));
                        }
                        if ("0.000".equals(avgNetCalc(tempNetData))) {

                        } else {
                            sb.append(key).append("=").append(avgNetCalc(tempNetData)).append("      ");
                            if ((++m) % 3 == 0) {
                                sb.append("\n");
                            }
                        }

                    }
                    sb.append("\n");
                }

            }
        }

        //使用双重for循环的方式：避免对系统进行区分 （不用写在上面的针对AIX & Linux的区别判断中）核心是不必关系 DISKREAD的T0001后有哪些域
        if (map.get("DISKREAD") != null) {
            List<Double> diskRead = new ArrayList<Double>();
            /**
             * 遍历DISKREAD每次（Txxxx次）的详细数据 并求和 存在diskRead中
             */
            for (String diskReadAll : map.get("DISKREAD")) {
                String[] cols = diskReadAll.split(",");
                double sumDiskReadTxxxx = 0.0D;
                for (int i = 2; i < cols.length; i++) {// DISKREAD,T0001,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0  把T0001后所有域求和（此时Txxxx为T0001）
                    sumDiskReadTxxxx += Double.parseDouble(cols[i]);
                }
                diskRead.add(sumDiskReadTxxxx);
            }
            /**
             * 遍历diskRead每次（Txxxx次）详细数据的和，再求和 ， 最后除以diskRead.size即为平均值
             */
            double sumDiskRead = 0.0D;
            for (Double double1 : diskRead) {
                sumDiskRead += double1;
            }
            String avgDiskRead = df.format(sumDiskRead / diskRead.size());
            sb.append("------------------------------  Disk  ------------------------------\n")
                    .append("TotalDiskRead=").append(avgDiskRead);
        }
        if (map.get("DISKWRITE") != null) {
            List<Double> diskWrite = new ArrayList<Double>();
            /**
             * 遍历DISKWRITE每次（Txxxx次）的详细数据 并求和 存在diskWrite中
             */
            for (String diskWriteAll : map.get("DISKWRITE")) {
                String[] cols = diskWriteAll.split(",");
                double sumDiskWriteTxxxx = 0.0D;
                for (int i = 2; i < cols.length; i++) {
                    sumDiskWriteTxxxx += Double.parseDouble(cols[i]);
                }
                diskWrite.add(sumDiskWriteTxxxx);
            }
            /**
             * 遍历diskWrite每次（Txxxx次）详细数据的和，再求和，  最后除以diskWrite.size即为平均值
             */
            double sumDiskWrite = 0.0D;
            for (Double double2 : diskWrite) {
                sumDiskWrite += double2;
            }
            String avgDiskWrite = df.format(sumDiskWrite / diskWrite.size());
            sb.append("KB/s      TotalDiskWrite=").append(avgDiskWrite);
        }
        if (map.get("DISKXFER") != null) {//IO
            List<Double> diskXfer = new ArrayList<Double>();
            /**
             * 遍历DISKXFER每次（Txxxx次）的详细数据 并求和 存在diskXfer中
             */
            for (String diskXferAll : map.get("DISKXFER")) {
                String[] cols = diskXferAll.split(",");
                double sumDiskXferTxxxx = 0.0D;
                for (int i = 2; i < cols.length; i++) {
                    sumDiskXferTxxxx += Double.parseDouble(cols[i]);
                }
                diskXfer.add(sumDiskXferTxxxx);
            }
            /**
             * 遍历diskXfer每次（Txxxx次）详细数据的和，再求和，  最后除以diskXfer.size即为平均值
             */
            double sumDiskXfer = 0.0D;
            for (Double double3 : diskXfer) {
                sumDiskXfer += double3;
            }
            String avgIO = df.format(sumDiskXfer / diskXfer.size());
            sb.append("KB/s      TotalDiskIO=").append(avgIO).append("\n");
        }

        if (map.get("DISKBUSY") != null) {
            HashMap<String, Integer> diskBusyHeaderIndexMap = new HashMap<String, Integer>();
            int m = 0;
            //解析NET_HEADER 获取指定域的位置
            for (String header : map.get("DISKBUSY_HEADER")) {
                String[] headerNames = header.split(",");
                for (int i = 2; i < headerNames.length; i++) {
                    diskBusyHeaderIndexMap.put(headerNames[i], i);
                }

                for (String key : diskBusyHeaderIndexMap.keySet()) {
                    List<Double> tempNetData = new ArrayList<Double>();
                    for (String diskBusyAll : map.get("DISKBUSY")) {
                        String[] cols = diskBusyAll.split(",");
                        tempNetData.add(Double.parseDouble(cols[diskBusyHeaderIndexMap.get(key)]));
                    }
                    if ("0.000".equals(avgNetCalc(tempNetData))) {

                    } else {
                        sb.append(key).append("=").append(avgNetCalc(tempNetData)).append("%      ");
                        if ((++m) % 3 == 0) {
                            sb.append("\n");
                        }
                    }
                }
            }
        }
        return sb.toString();
    }

    public String avgNetCalc(List<Double> tempNetData) {
        DecimalFormat df = new DecimalFormat("0.000");
        String avgTemp = "0.000";
        double sumTemp = 0.0D;
        for (Double double1 : tempNetData) {
            sumTemp += double1;
        }
        if (tempNetData.size() == 0) {
            avgTemp = df.format(0);
        } else {
            avgTemp = df.format(sumTemp / tempNetData.size());
        }
        return avgTemp;
    }
}
