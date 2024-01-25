package com.libaolu.nmon.control;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import com.jcraft.jsch.*;
import com.libaolu.nmon.utils.AvgDataTag;
import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.testelement.AbstractTestElement;
import org.apache.jmeter.testelement.property.CollectionProperty;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.testelement.property.PropertyIterator;
import org.apache.jmeter.util.JMeterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author libaolu
 * @since 2024/1/25
 */
public class NmonControl extends AbstractTestElement {
    private static final Logger log = LoggerFactory.getLogger(NmonControl.class);
    public static final String INTERVAL = "NmonControl.interval";
    public static final String HOLD = "NmonControl.hold";
    public static final String FILE_NAME = "NmonControl.fileName";
    public static final String MASTER_IP = "NmonControl.masterIp";
    public static final String IS_SLAVE_START = "NmonControl.isSlaveStart";
    public StringBuffer nmonFileStr;
    private boolean isFist = true;
    private CollectionProperty overrideProp;
    public static final String DATA_PROPERTY = "nmonControl_load_nmon_profile";
    public static final String DELIMITER = ",";
    private StringBuffer tempAnalyseResult;
    private String fileLocation;
    private HashMap<String, ArrayList<String>> nmonTxxxxDataMap;
    private HashMap configArea;

    public void setInterval(String interval) {
        this.setProperty(INTERVAL, interval);
    }
    public String getInterval() {
        return getPropertyAsString(INTERVAL);
    }
    public void setHold(String hold) {
        setProperty(HOLD, hold);
    }

    public String getHold() {
        return getPropertyAsString(HOLD);
    }
    public void setFileName(String fileName) {
        setProperty(FILE_NAME, fileName);
    }

    public String getFileName() {
        return getPropertyAsString(FILE_NAME);
    }
    public void setMasterIp(String fileName) {
        setProperty(MASTER_IP, fileName);
    }

    public String getMasterIp() {
        return getPropertyAsString(MASTER_IP);
    }

    public void setIsSlaveStart(boolean selected) {
        setProperty(IS_SLAVE_START, selected);
    }

    public boolean getIsSlaveStart() {
        return getPropertyAsBoolean(IS_SLAVE_START);
    }

    public void setData(CollectionProperty rows) {
        setProperty((JMeterProperty)rows);
    }

    public JMeterProperty getData() {
        if (this.overrideProp != null){
            return this.overrideProp;
        }
        return getProperty(DATA_PROPERTY);
    }

    public void startNmon() throws IOException {
        String pluginsShow = JMeterUtils.getProperty("baolu-jmeter-plugins");
        if (StringUtils.isEmpty(pluginsShow)) {
            log.info("{}", displayAsciiArt());
            JMeterUtils.setProperty("baolu-jmeter-plugins", "show");
        }
        if (getIsSlaveStart()) {
            JMeterUtils.setProperty("NMON_IF_SLAVE_START", "true");
            String localHostIP = JMeterUtils.getLocalHostIP();
            String masterIp = getMasterIp();
            JMeterUtils.setProperty("nmonMasterIp", masterIp);//将masterIp 设置成属性值，供NmonFileAnalyseSampler使用
            if (localHostIP.equals(masterIp)) {//判断GUI页面填写的MasterIp是否为本机ip
                finalRunCommand();
            }
        } else {
            JMeterUtils.setProperty("NMON_IF_SLAVE_START", "false");
            finalRunCommand();
        }
    }

    /**
     * 执行nmon命令
     */
    @SuppressWarnings("unchecked")
    public void finalRunCommand() throws IOException {
        JMeterProperty data = getData();
        CollectionProperty rows = (CollectionProperty)data;
        PropertyIterator scheduleIT = rows.iterator();
        List<String> list = new ArrayList<>();
        nmonFileStr = new StringBuffer();
        while (scheduleIT.hasNext()) {
            List<Object> curProp = (List<Object>)scheduleIT.next().getObjectValue();
            String serverConfig = getStringValue(curProp, 0) + DELIMITER + getStringValue(curProp, 1) + DELIMITER + getStringValue(curProp, 2)
                    + DELIMITER + getStringValue(curProp, 3);
            list.add(serverConfig);
        }
        for (String serverConfig : list) {
            loginExecuteCommand(serverConfig);
        }
        log.info("current make nmon file [{}]", nmonFileStr.toString());
        JMeterUtils.setProperty("nmon_server_Config_Msg", nmonFileStr.toString());
    }

    private String getStringValue(List<Object> prop, int colID) {
        JMeterProperty val = (JMeterProperty)prop.get(colID);
        return val.getStringValue().trim();
    }

    public String displayAsciiArt() {
        String wel = "";
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("banner/banner.txt")) {
            if (inputStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                String key = "";
                try {
                    while ((line = reader.readLine()) != null) {
                        if (line.indexOf("___") > 0) {
                            key += System.lineSeparator() + line + System.lineSeparator();
                        } else if (line.indexOf("JMeter") > 0) {
                            key += line;
                        } else {
                            key += line + System.lineSeparator();
                        }
                    }
                    wel = key;
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        reader.close();
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return wel;
    }

    public void loginExecuteCommand(String str) throws IOException{
        boolean conSuc = false;
        Connection conn = null;
        Session session = null;
        String[] cols = str.split(",");
        conn = new Connection(cols[0]);
        conn.connect();
        conSuc = conn.authenticateWithPassword(cols[1], cols[2]);
        if (conSuc) {
            session = conn.openSession();
            StringBuilder sb;
            InputStream stdOut;
            InputStream stdErr;
            String outStr;
            String outErr;
            Integer count;
            String fileName;
            if ("Linux".equalsIgnoreCase(cols[3].replaceAll("\r", ""))) {
                sb = new StringBuilder();
                stdOut = null;
                stdErr = null;
                outStr = "";
                outErr = "";
                try {
                    count = Integer.parseInt(getHold()) / Integer.parseInt(getInterval());
                    fileName = getFileName() + "_" + getTimeStr() + "_" + cols[0] + ".nmon";
                    sb.append("./nmon -s ").append(getInterval()).append(" -c ").append(count).append(" -F ");
                    sb.append(fileName);
                    assert session != null;
                    session.execCommand(sb.toString());
                    if ("".equals(outStr) && "".equals(outErr)) {
                        if (isFist) {
                            isFist = false;
                        } else {
                            nmonFileStr.append("&@&@");
                        }

                        nmonFileStr.append(str).append(",").append(fileName);
                    } else {
                        log.error(cols[0] + " nmon command execution failed " + outErr);
                    }
                } finally {
                    if (session != null) {
                        session.close();
                    }
                    conn.close();
                }

            }

            if ("Aix".equalsIgnoreCase(cols[3].replaceAll("\r", ""))) {
                sb = new StringBuilder();
                stdOut = null;
                stdErr = null;
                outStr = "";
                outErr = "";
                try {
                    count = Integer.parseInt(getHold()) / Integer.parseInt(getInterval());
                    fileName = getFileName() + "_" + getTimeStr() + "_" + cols[0] + ".nmon";
                    sb.append("nmon  -s ").append(getInterval()).append(" -c ").append(count).append(" -F ");
                    sb.append(fileName);
                    assert session != null;
                    session.execCommand(sb.toString());
                    if ("".equals(outStr) && "".equals(outErr)) {
                        if (isFist) {
                            isFist = false;
                        } else {
                            nmonFileStr.append("&@&@");
                        }
                        nmonFileStr.append(str).append(",").append(fileName);
                    } else {
                        log.error(cols[0] + " nmon command execution failed " + outErr);
                    }
                } finally {
                    if (session != null) {
                        session.close();
                    }
                    conn.close();
                }
            }
        } else {
            log.error("server login failed [{}]", cols[0]);
            throw new IOException("server login failed " + cols[0]);
        }
    }

    private static String getTimeStr() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        return sdf.format(new Date());
    }

    public void stopNmon() {
        String if_remote_start = JMeterUtils.getProperty("NMON_IF_SLAVE_START");
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

    private void ftpGetFileAnalyse() {
        ChannelSftp sftp = null;
        com.jcraft.jsch.Session session = null;
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
        String[] arrNmonMsg = JMeterUtils.getProperty("nmon_server_Config_Msg").split("&@&@");
        for (String nmonMsgStr : arrNmonMsg) {
            String[] arrTemp = nmonMsgStr.split(",");
            log.error(Arrays.toString(arrTemp));
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
        fileLocation = null;
        nmonTxxxxDataMap = null;
        configArea = null;
        isFist = true;
    }

}
