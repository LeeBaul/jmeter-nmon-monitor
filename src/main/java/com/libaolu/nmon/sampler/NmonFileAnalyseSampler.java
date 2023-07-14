package com.libaolu.nmon.sampler;

import com.jcraft.jsch.*;
import com.libaolu.nmon.utils.AvgDataTag;
import org.apache.jmeter.samplers.AbstractSampler;
import org.apache.jmeter.samplers.Entry;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.util.JMeterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

/**
 * <p/>
 * nmon文件计算分析类
 *
 * @author libaolu
 * @version 1.0
 * @dateTime 2020/2/17 13:28
 **/
public class NmonFileAnalyseSampler extends AbstractSampler implements TestStateListener {
    private static final Logger log = LoggerFactory.getLogger(NmonFileAnalyseSampler.class);
    private StringBuffer tempAnalyseResult;
    private String fileLocation;
    private HashMap<String, ArrayList<String>> nmonTxxxxDataMap;
    private HashMap configArea;

    @Override
    public SampleResult sample(Entry entry) {
        return null;
    }

    @Override
    public void testStarted() {

    }

    @Override
    public void testStarted(String s) {
        testStarted();
    }

    @Override
    public void testEnded() {
        String if_remote_start = JMeterUtils.getProperty("IF_SLAVE_START");
        if ("true".equals(if_remote_start)) {
            //当前机器的ip地址，jmeterHostAddress需要提前配置在jmeter.properties配置文件中
            String localHostIP = JMeterUtils.getLocalHostIP();
            String nmonMasterIp = JMeterUtils.getProperty("nmonMasterIp");//获取NmonConfigExecuteSampler 设置的nmonMasterIp
            if ("".equals(nmonMasterIp)) {
                log.error("分布式模式下，必须设置控制机IP，请检查！");
            } else {
                //当前机器IP与要执行NMON采样器的机器IP相同才会执行
                if (localHostIP.equals(nmonMasterIp)) {
                    tempAnalyseResult = new StringBuffer();
                    ftpGetFileAnalyse();
                    log.info("\n{}", tempAnalyseResult.toString());
                    log.info("NMON文件解析结束");
                }
            }
        } else {
            tempAnalyseResult = new StringBuffer();
            ftpGetFileAnalyse();
            log.info("\n{}", tempAnalyseResult.toString());
            log.info("NMON文件解析结束");
        }

    }

    @Override
    public void testEnded(String s) {
        testEnded();
    }

    private void ftpGetFileAnalyse() {
        ChannelSftp sftp = null;
        Session session = null;
        File file = new File("nmonTemp");
        if (file.exists()) {
            if (!(file.isDirectory())) {
                file.mkdirs();
            }
        } else {
            file.mkdir();
            log.info("已成功创建nmonTemp文件夹");
        }
        //以 &@&@ 分割服务器上的nomon文件名
        String[] arrNmonMsg = JMeterUtils.getProperty("nmonFileNameStr").split("&@&@");
        for (String nmonMsgStr : arrNmonMsg) {
            String[] arrTemp = nmonMsgStr.split(",");
            try {
                JSch jsch = new JSch();
                session = jsch.getSession(arrTemp[1], arrTemp[0], 22);
                session.setPassword(arrTemp[2]);
                Properties sshConfig = new Properties();
                sshConfig.put("StrictHostKeyChecking", "no");
                session.setConfig(sshConfig);
                session.connect(60000);
                Channel channel = session.openChannel("sftp");
                channel.connect();
                sftp = (ChannelSftp) channel;
            } catch (JSchException e) {
                e.printStackTrace();
            }
            try {
                fileLocation = file.getPath() + File.separator + arrTemp[4];
                assert sftp != null;
                sftp.get(arrTemp[4], new FileOutputStream(new File(this.fileLocation)));
            } catch (FileNotFoundException e1) {
                log.error("FileNotFoundException[{}]", arrTemp[4]);
                e1.printStackTrace();
            } catch (SftpException e2) {
                log.error("SftpException[{}]", arrTemp[4]);
                e2.printStackTrace();
            } finally {
                if (sftp != null) {
                    if (sftp.isConnected()) {
                        sftp.disconnect();
                    } else if (sftp.isClosed()) {
                        log.info("stfp already closed");
                    }
                    try {
                        if (sftp.getSession() != null) {
                            sftp.getSession().disconnect();
                        }
                    } catch (JSchException e) {
                        e.printStackTrace();
                    }
                }
            }
            analyseLocalNmonFile(arrTemp[4]);
        }
        System.out.println(tempAnalyseResult);
    }

    public boolean analyseLocalNmonFile(String fileName) {
        configArea = new HashMap<>();
        nmonTxxxxDataMap = new HashMap<>();
        tempAnalyseResult.append(">>>>>>>>>>> ").append(fileName).append(" >>>>>>>>>>>\n");
        boolean conSuc = false;
        FileReader fileReader = null;
        BufferedReader bu2ferReader = null;
        String line = null;
        new ArrayList<>();
        nmonTxxxxDataMap = new HashMap<>();

        try {
            fileReader = new FileReader(fileLocation);
        } catch (FileNotFoundException var21) {
            var21.printStackTrace();
        }

        assert fileReader != null;
        bu2ferReader = new BufferedReader(fileReader);

        try {
            while ((line = bu2ferReader.readLine()) != null) {
                if (!"".equals(line)) {
                    String[] cols = line.split(",");
                    ArrayList nmonTxxxxList;
                    if (cols[0].equals("NET") && cols[1].startsWith("Network")) {
                        if (nmonTxxxxDataMap.get("NET_HEADER") != null) {
                            (nmonTxxxxDataMap.get("NET_HEADER")).add(line);
                        } else {
                            nmonTxxxxList = new ArrayList<>();
                            nmonTxxxxList.add(line);
                            nmonTxxxxDataMap.put("NET_HEADER", nmonTxxxxList);
                        }
                    } else if (cols[0].equals("DISKBUSY") && cols[1].startsWith("Disk")) {
                        if (nmonTxxxxDataMap.get("DISKBUSY_HEADER") != null) {
                            (nmonTxxxxDataMap.get("DISKBUSY_HEADER")).add(line);
                        } else {
                            nmonTxxxxList = new ArrayList<>();
                            nmonTxxxxList.add(line);
                            this.nmonTxxxxDataMap.put("DISKBUSY_HEADER", nmonTxxxxList);
                        }
                    }

                    if (cols[1].matches("T\\d+")) {
                        if (!cols[0].equals("ERROR")) {
                            if (cols[0].equals("CPU_ALL")) {
                                if (nmonTxxxxDataMap.get(cols[0]) != null) {
                                    (nmonTxxxxDataMap.get(cols[0])).add(line);
                                } else {
                                    nmonTxxxxList = new ArrayList<>();
                                    nmonTxxxxList.add(line);
                                    nmonTxxxxDataMap.put(cols[0], nmonTxxxxList);
                                }
                            }

                            if (cols[0].equals("MEM")) {
                                if (nmonTxxxxDataMap.get(cols[0]) != null) {
                                    (nmonTxxxxDataMap.get(cols[0])).add(line);
                                } else {
                                    nmonTxxxxList = new ArrayList<>();
                                    nmonTxxxxList.add(line);
                                    nmonTxxxxDataMap.put(cols[0], nmonTxxxxList);
                                }
                            }

                            if (cols[0].equals("MEMNEW")) {
                                if (nmonTxxxxDataMap.get(cols[0]) != null) {
                                    (nmonTxxxxDataMap.get(cols[0])).add(line);
                                } else {
                                    nmonTxxxxList = new ArrayList<>();
                                    nmonTxxxxList.add(line);
                                    nmonTxxxxDataMap.put(cols[0], nmonTxxxxList);
                                }
                            }

                            if (cols[0].equals("PROC")) {
                                if (nmonTxxxxDataMap.get(cols[0]) != null) {
                                    (nmonTxxxxDataMap.get(cols[0])).add(line);
                                } else {
                                    nmonTxxxxList = new ArrayList<>();
                                    nmonTxxxxList.add(line);
                                    this.nmonTxxxxDataMap.put(cols[0], nmonTxxxxList);
                                }
                            }

                            if (cols[0].equals("NET")) {
                                if (nmonTxxxxDataMap.get(cols[0]) != null) {
                                    (nmonTxxxxDataMap.get(cols[0])).add(line);
                                } else {
                                    nmonTxxxxList = new ArrayList<>();
                                    nmonTxxxxList.add(line);
                                    nmonTxxxxDataMap.put(cols[0], nmonTxxxxList);
                                }
                            }

                            if (cols[0].equals("DISKREAD")) {
                                if (nmonTxxxxDataMap.get(cols[0]) != null) {
                                    (nmonTxxxxDataMap.get(cols[0])).add(line);
                                } else {
                                    nmonTxxxxList = new ArrayList<>();
                                    nmonTxxxxList.add(line);
                                    nmonTxxxxDataMap.put(cols[0], nmonTxxxxList);
                                }
                            }

                            if (cols[0].equals("DISKWRITE")) {
                                if (nmonTxxxxDataMap.get(cols[0]) != null) {
                                    (nmonTxxxxDataMap.get(cols[0])).add(line);
                                } else {
                                    nmonTxxxxList = new ArrayList<>();
                                    nmonTxxxxList.add(line);
                                    nmonTxxxxDataMap.put(cols[0], nmonTxxxxList);
                                }
                            }

                            if (cols[0].equals("DISKXFER")) {
                                if (nmonTxxxxDataMap.get(cols[0]) != null) {
                                    (nmonTxxxxDataMap.get(cols[0])).add(line);
                                } else {
                                    nmonTxxxxList = new ArrayList<>();
                                    nmonTxxxxList.add(line);
                                    nmonTxxxxDataMap.put(cols[0], nmonTxxxxList);
                                }
                            }

                            if (cols[0].equals("DISKBUSY")) {
                                if (nmonTxxxxDataMap.get(cols[0]) != null) {
                                    (nmonTxxxxDataMap.get(cols[0])).add(line);
                                } else {
                                    nmonTxxxxList = new ArrayList<>();
                                    nmonTxxxxList.add(line);
                                    nmonTxxxxDataMap.put(cols[0], nmonTxxxxList);
                                }
                            }
                        }
                    } else if (cols[0].equals("TOP")) {
                        if (cols.length < 3) {
                            fillConfigArea(line);
                        } else {
                            fillConfigArea(line);
                        }
                    } else {
                        fillConfigArea(line);
                    }
                }
            }
        } catch (IOException var22) {
            var22.printStackTrace();
        } finally {
            try {
                bu2ferReader.close();
            } catch (IOException var20) {
                var20.printStackTrace();
            }

            try {
                fileReader.close();
            } catch (IOException var19) {
                var19.printStackTrace();
            }

        }

        AvgDataTag avgDataTag = new AvgDataTag();
        String tempResult = avgDataTag.avgDataCalc(nmonTxxxxDataMap, getConfigDataByTag("AAA"));
        tempAnalyseResult.append(tempResult).append("\n");
        end();
        return conSuc;
    }

    protected void fillConfigArea(String line) {
        String[] cols = line.split(",");
        HashMap var4 = configArea;
        synchronized (configArea) {
            ArrayList arrayRef;
            if (!configArea.containsKey(cols[0])) {
                arrayRef = new ArrayList<>();
                arrayRef.add(line);
                configArea.put(cols[0], arrayRef);
            } else {
                arrayRef = (ArrayList) configArea.get(cols[0]);
                arrayRef.add(line);
            }

        }
    }

    private ArrayList getConfigDataByTag(String tag) {
        synchronized (configArea) {
            ArrayList tagData = (ArrayList) configArea.get(tag);
            return tagData;
        }
    }

    private void end() {
        this.fileLocation = null;
        this.nmonTxxxxDataMap = null;
        this.configArea = null;
    }

}
