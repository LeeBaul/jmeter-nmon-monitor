package com.libaolu.nmon.sampler;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import com.libaolu.nmon.utils.LeeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.samplers.AbstractSampler;
import org.apache.jmeter.samplers.Entry;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.testelement.property.CollectionProperty;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.testelement.property.PropertyIterator;
import org.apache.jmeter.util.JMeterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * <p/>
 *
 * @author libaolu
 * @version 1.0
 * @dateTime 2020/1/22 15:52
 **/
public class NmonConfigExecuteSampler extends AbstractSampler implements TestStateListener {
    private static final Logger log = LoggerFactory.getLogger(NmonConfigExecuteSampler.class);
    public static final String INTERVAL = "NmonConfigExecuteSampler.interval";
    public static final String HOLD = "NmonConfigExecuteSampler.hold";
    public static final String FILE_NAME = "NmonConfigExecuteSampler.fileName";
    public static final String MASTER_IP = "NmonConfigExecuteSampler.masterIp";
    public static final String IS_SLAVE_START = "NmonConfigExecuteSampler.isSlaveStart";
    public static final String NOTE = "NmonConfigExecuteSampler.note";
    public static final String REQUEST = "NmonConfigExecuteSampler.requestData";
    public StringBuffer nmonFileStr;
    private boolean isFist = true;
    private CollectionProperty overrideProp;
    public static final String DATA_PROPERTY = "load_nmon_profile";
    public static final String DELIMITER = ",";

    @Override
    public SampleResult sample(Entry entry) {
        LeeUtils agr = new LeeUtils();
        String pluginsShow = JMeterUtils.getProperty("baolu-jmeter-plugins");
        if (StringUtils.isEmpty(pluginsShow)) {
            log.info("{}", agr.displayAsciiArt());
            JMeterUtils.setProperty("baolu-jmeter-plugins", "show");
        }
        if (getIsSlaveStart()) {
            JMeterUtils.setProperty("IF_SLAVE_START", "true");
            String localHostIP = JMeterUtils.getLocalHostIP();
            String masterIp = getMasterIp();
            JMeterUtils.setProperty("nmonMasterIp", masterIp);//将masterIp 设置成属性值，供NmonFileAnalyseSampler使用
            if (localHostIP.equals(masterIp)) {//判断GUI页面填写的MasterIp是否为本机ip
                finalRunCommand();
            }
        } else {
            JMeterUtils.setProperty("IF_SLAVE_START", "false");
            finalRunCommand();
        }
        return null;// This means no sample is saved
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
        log.info("NMON命令执行结束");
    }

    @Override
    public void testEnded(String s) {
        testEnded();
    }

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

    public void setNote(String note) {
        setProperty(NOTE, note);
    }

    public String getNote() {
        return getPropertyAsString(NOTE);
    }

    public void setRequest(String request) {
        setProperty(REQUEST, request);
    }

    public String getREQUEST() {
        return getPropertyAsString(REQUEST);
    }

    public boolean loginExecuteCommand(String str) {
        boolean conSuc = false;
        Connection conn = null;
        Session session = null;
        String[] cols = str.split(",");
        try {
            conn = new Connection(cols[0]);
            conn.connect();
            conSuc = conn.authenticateWithPassword(cols[1], cols[2]);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (conSuc) {
            try {
                session = conn.openSession();
            } catch (IOException e) {
                e.printStackTrace();
            }
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
                        log.error(cols[0] + " NMON 命令执行失败 " + outErr);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (session != null) {
                        session.close();
                    }
                    if (conn != null) {
                        conn.close();
                    }
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
                        log.error(cols[0] + " NMON命令执行失败 " + outErr);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (session != null) {
                        session.close();
                    }
                    if (conn != null) {
                        conn.close();
                    }
                }
            }
        } else {
            log.error("服务器登录失败[{}]", cols[0]);
        }
        return conSuc;
    }

    private static String getTimeStr() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        return sdf.format(new Date());
    }

    /**
     * 执行nmon命令
     */
    public void finalRunCommand() {
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
        log.info("当前产生的NMON文件[{}]", nmonFileStr.toString());
        JMeterUtils.setProperty("nmonFileNameStr", nmonFileStr.toString());
    }

    /**
     * 对于master来说要先判断GUI页面配置的slave机ip
     * 是否在remote_hosts中，存在此地址设为属性值，
     * 不存在默认取remote_hosts首个ip
     *
     * @param slave_ip 要执行NMON采样器的salve机ip
     * @return
     */
    private static String retExistSavle(String slave_ip) {
        String remote_hosts = JMeterUtils.getProperty("remote_hosts");//获取slave机列表
        String[] ip_port = remote_hosts.split(",");
        String firstIp = ip_port[0].split(":")[0];
        if (!"".equals(slave_ip) && slave_ip != null) {
            for (String temp : ip_port) {
                String ip = temp.split(":")[0];
                if (ip.equals(slave_ip)) {
                    JMeterUtils.setProperty("NmonSlaveIp", slave_ip);
                    return slave_ip;
                }
            }
        }
        JMeterUtils.setProperty("NmonSlaveIp", firstIp);
        return firstIp;
    }

    public void setData(CollectionProperty rows) {
        setProperty((JMeterProperty)rows);
    }

    public JMeterProperty getData() {
        if (this.overrideProp != null)
            return (JMeterProperty)this.overrideProp;
        return getProperty(DATA_PROPERTY);
    }

    private String getStringValue(List<Object> prop, int colID) {
        JMeterProperty val = (JMeterProperty)prop.get(colID);
        return val.getStringValue().trim();
    }
}
