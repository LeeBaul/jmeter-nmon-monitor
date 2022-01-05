package com.libaolu.nmon.control.gui;

import com.libaolu.nmon.sampler.NmonConfigExecuteSampler;
import org.apache.jmeter.gui.util.*;
import org.apache.jmeter.samplers.gui.AbstractSamplerGui;
import org.apache.jmeter.testelement.TestElement;

import javax.swing.*;
import java.awt.*;


/**
 * <p/>
 *
 * @author libaolu
 * @version 1.0
 * @dateTime 2020/1/22 15:04
 **/
public class NmonConfigExecuteSamplerGui extends AbstractSamplerGui {
    /**
     * 采样间隔
     */
    private JTextField interval;
    /**
     * 持续时间
     */
    private JTextField hold;
    /**
     * 是否开启slave机
     */
    private TristateCheckBox isSlaveStart;
    /**
     * 使用说明
     */
    private JSyntaxTextArea note;
    /**
     * 文件名
     */
    private JTextField fileName;
    /**
     * slave机ip
     */
    private JTextField masterIp;

    /**
     * 配置信息
     */
    private JSyntaxTextArea configMsg;

    private static final String ATTENTION = "1.本采样器必须放置setUp Thread Group中，单线执行1次\n"+
            "2.确保被监控服务器上可以正常NMON，Linux操作系统可执行NMON文件，须放置当前登录用户目录\n" +
            "3.采样间隔、持续时间尽量采用整数型，生成文件名禁用中文\n" +
            "4.配置信息中必须使用ip,user,pwd,serverType格式，多台配置信息之间使用回车键分开，其中serverType可填Linux、AIX\n" +
            "5.JMeter压测采用非分布式模式，执行机IP填写本机ip，最终分析结果本机查看\n" +
            "6.JMeter压测采用分布式模式，执行机IP随机填写一台JMeter的slave机ip,最终分析结果执行机IP查看";

    public NmonConfigExecuteSamplerGui(){
        init();
        initFields();
    }


    private void init() {
        setLayout(new BorderLayout(0, 5));
        setBorder(makeBorder());
        add(makeTitlePanel(), "North");
        VerticalPanel mainPanel = new VerticalPanel();
        HorizontalPanel optionsPanel = new HorizontalPanel();
        optionsPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder()));
        optionsPanel.add(createIntervalOption());
        optionsPanel.add(createHoldOption());
        optionsPanel.add(createFileNameOption());
        optionsPanel.add(createSlaveIpOption());
        optionsPanel.add(createIsSlaveStartOption());
        mainPanel.add(optionsPanel);
        mainPanel.add(createNotePanel());
        mainPanel.add(createRequestPanel());
        add(mainPanel, BorderLayout.CENTER);
    }

    private JPanel createIntervalOption() {
        JLabel label = new JLabel("采样间隔");
        interval = new JTextField(8);
        interval.setMaximumSize(new Dimension(interval.getPreferredSize()));
        label.setLabelFor(interval);
        JPanel intervalPanel = new JPanel(new FlowLayout());
        intervalPanel.add(label);
        intervalPanel.add(interval);
        return intervalPanel;
    }

    private JPanel createHoldOption() {
        JLabel label = new JLabel("持续时间");
        hold = new JTextField(8);
        hold.setMaximumSize(new Dimension(hold.getPreferredSize()));
        label.setLabelFor(hold);
        JPanel holdPanel = new JPanel(new FlowLayout());
        holdPanel.add(label);
        holdPanel.add(hold);
        return holdPanel;
    }

    private JPanel createFileNameOption() {
        JLabel label = new JLabel("文件名");
        fileName = new JTextField(18);
        fileName.setMaximumSize(new Dimension(fileName.getPreferredSize()));
        label.setLabelFor(fileName);
        JPanel fileNamePanel = new JPanel(new FlowLayout());
        fileNamePanel.add(label);
        fileNamePanel.add(fileName);
        return fileNamePanel;
    }

    private JPanel createSlaveIpOption() {
        JLabel label = new JLabel("执行机IP");
        masterIp = new JTextField(18);
        masterIp.setMaximumSize(new Dimension(masterIp.getPreferredSize()));
        label.setLabelFor(masterIp);
        JPanel slaveIpPanel = new JPanel(new FlowLayout());
        slaveIpPanel.add(label);
        slaveIpPanel.add(masterIp);
        return slaveIpPanel;
    }

    private JPanel createIsSlaveStartOption() {
        JLabel label = new JLabel("分布式模式"); // $NON-NLS-1$
        isSlaveStart = new TristateCheckBox();
        JPanel isSlavePanel = new JPanel(new FlowLayout());
        isSlavePanel.add(label);
        isSlavePanel.add(isSlaveStart);
        return isSlavePanel;
    }

    private JPanel createNotePanel() {
        JLabel reqLabel = new JLabel("注意事项");
        note = JSyntaxTextArea.getInstance(7, 10);
        note.setLanguage("text");
        reqLabel.setLabelFor(note);
        JPanel notePanel = new JPanel(new BorderLayout(5, 0));
        notePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder()));
        notePanel.add(reqLabel, BorderLayout.WEST);
        notePanel.add(JTextScrollPane.getInstance(note), BorderLayout.CENTER);
        return notePanel;
    }

    private JPanel createRequestPanel() {
        JLabel reqLabel = new JLabel("配置信息");
        configMsg = JSyntaxTextArea.getInstance(16, 20);
        configMsg.setLanguage("text");
        reqLabel.setLabelFor(configMsg);
        JPanel reqDataPanel = new JPanel(new BorderLayout(5, 0));
        reqDataPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder()));
        reqDataPanel.add(reqLabel, BorderLayout.WEST);
        reqDataPanel.add(JTextScrollPane.getInstance(configMsg), BorderLayout.CENTER);
        return reqDataPanel;
    }

    public void configure(TestElement element) {
        super.configure(element);
        interval.setText(element.getPropertyAsString(NmonConfigExecuteSampler.INTERVAL));
        hold.setText(element.getPropertyAsString(NmonConfigExecuteSampler.HOLD));
        isSlaveStart.setTristateFromProperty(element,NmonConfigExecuteSampler.IS_SLAVE_START);
        fileName.setText(element.getPropertyAsString(NmonConfigExecuteSampler.FILE_NAME));
        masterIp.setText(element.getPropertyAsString(NmonConfigExecuteSampler.MASTER_IP));
        note.setInitialText(element.getPropertyAsString(NmonConfigExecuteSampler.NOTE));
        note.setCaretPosition(0);
        configMsg.setInitialText(element.getPropertyAsString(NmonConfigExecuteSampler.REQUEST));
        configMsg.setCaretPosition(0);
    }

    public String getStaticLabel(){
        return "BaoluNmonConfigExecute";
    }

    @Override
    public String getLabelResource() {
        return super.getClass().getSimpleName();
    }

    @Override
    public TestElement createTestElement() {
        NmonConfigExecuteSampler sampler = new NmonConfigExecuteSampler();
        modifyTestElement(sampler);
        return sampler;
    }

    @Override
    public void modifyTestElement(TestElement element) {
        super.configureTestElement(element);
        element.setProperty(NmonConfigExecuteSampler.INTERVAL, interval.getText(),"");
        element.setProperty(NmonConfigExecuteSampler.HOLD, hold.getText(),"");
        isSlaveStart.setPropertyFromTristate(element, NmonConfigExecuteSampler.IS_SLAVE_START);
        element.setProperty(NmonConfigExecuteSampler.FILE_NAME, fileName.getText(),"");
        element.setProperty(NmonConfigExecuteSampler.MASTER_IP, masterIp.getText(),"");
        element.setProperty(NmonConfigExecuteSampler.NOTE, note.getText(),"");
        element.setProperty(NmonConfigExecuteSampler.REQUEST, configMsg.getText(),"");
    }

    public void clearGui() {
        super.clearGui();
        initFields();
    }

    private void initFields(){
        interval.setText("");
        hold.setText("");
        fileName.setText("");
        masterIp.setText("");
        isSlaveStart.setSelected(false);
        note.setText(ATTENTION);
        configMsg.setText("hi guys write nmon server config data here");
    }

}
