package com.libaolu.nmon.control.gui;

import com.libaolu.nmon.sampler.NmonConfigExecuteSampler;
import kg.apc.charting.AbstractGraphRow;
import kg.apc.charting.GraphPanelChart;
import kg.apc.jmeter.JMeterPluginsUtils;
import kg.apc.jmeter.gui.ButtonPanelAddCopyRemove;
import org.apache.jmeter.gui.util.*;
import org.apache.jmeter.samplers.gui.AbstractSamplerGui;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.property.CollectionProperty;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.awt.*;
import java.util.concurrent.ConcurrentHashMap;


/**
 * <p/>
 *
 * @author libaolu
 * @version 1.0
 * @dateTime 2020/1/22 15:04
 **/
public class NmonConfigExecuteSamplerGui extends AbstractSamplerGui implements TableModelListener, CellEditorListener {
    private static final Logger log = LoggerFactory.getLogger(NmonConfigExecuteSamplerGui.class);
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

    protected JTable grid;

    protected ButtonPanelAddCopyRemove buttons;

    protected PowerTableModel tableModel;

    protected ConcurrentHashMap<String, AbstractGraphRow> model;

    protected GraphPanelChart chart;

    private static final String[] defaultValues = new String[] {"192.168.56.129", "root", "root","Linux"};

    protected static final String[] columnIdentifiers = new String[] { "ip", "username", "password" ,"serverType"};

    protected static final Class[] columnClasses = new Class[] { String.class, String.class, String.class ,String.class};

    private static final String ATTENTION = "1.本采样器必须放置setUp Thread Group中，单线执行1次\n" +
            "2.确保被监控服务器上可以正常NMON，Linux操作系统可执行NMON文件，须放置当前登录用户目录\n" +
            "3.采样间隔、持续时间尽量采用整数型，生成文件名禁用中文\n" +
            "4.JMeter压测采用非分布式模式，执行机IP填写本机ip，最终分析结果本机查看\n" +
            "5.JMeter压测采用分布式模式，执行机IP随机填写一台JMeter的slave机ip,最终分析结果执行机IP查看";

    public NmonConfigExecuteSamplerGui() {
        init();
        initFields();
    }

    protected void init() {
        setLayout(new BorderLayout());
        setBorder(makeBorder());
        VerticalPanel verticalPanel = new VerticalPanel();
        HorizontalPanel optionsPanel = new HorizontalPanel();
        optionsPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder()));
        optionsPanel.add(createIntervalOption());
        optionsPanel.add(createHoldOption());
        optionsPanel.add(createFileNameOption());
        optionsPanel.add(createSlaveIpOption());
        optionsPanel.add(createIsSlaveStartOption());
        verticalPanel.add(optionsPanel);
        verticalPanel.add(createNotePanel());
        verticalPanel.add(createParamsPanel());
        add((Component)verticalPanel, BorderLayout.CENTER);
    }

    private JPanel createParamsPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("server config message"));
        panel.setPreferredSize(new Dimension(280, 280));
        JScrollPane scroll = new JScrollPane(createGrid());
        scroll.setPreferredSize(scroll.getMinimumSize());
        panel.add(scroll, BorderLayout.CENTER);
        this.buttons = new ButtonPanelAddCopyRemove(this.grid, this.tableModel, (Object[])defaultValues);
        panel.add((Component)this.buttons, BorderLayout.SOUTH);
        return panel;
    }

    private JTable createGrid() {
        this.grid = new JTable();
        this.grid.getDefaultEditor(String.class).addCellEditorListener(this);
        createTableModel();
        this.grid.setSelectionMode(0);
        this.grid.setMinimumSize(new Dimension(50, 50));
        return this.grid;
    }

    private void createTableModel() {
        this.tableModel = new PowerTableModel(NmonConfigExecuteSamplerGui.columnIdentifiers, NmonConfigExecuteSamplerGui.columnClasses);
        this.tableModel.addTableModelListener(this);
        this.grid.setModel((TableModel)this.tableModel);
    }

    private JPanel createIntervalOption() {
        JLabel label = new JLabel("Interval");
        interval = new JTextField(4);
        interval.setMaximumSize(new Dimension(interval.getPreferredSize()));
        label.setLabelFor(interval);
        JPanel intervalPanel = new JPanel(new FlowLayout());
        intervalPanel.add(label);
        intervalPanel.add(interval);
        return intervalPanel;
    }

    private JPanel createHoldOption() {
        JLabel label = new JLabel("During");
        hold = new JTextField(4);
        hold.setMaximumSize(new Dimension(hold.getPreferredSize()));
        label.setLabelFor(hold);
        JPanel holdPanel = new JPanel(new FlowLayout());
        holdPanel.add(label);
        holdPanel.add(hold);
        return holdPanel;
    }

    private JPanel createFileNameOption() {
        JLabel label = new JLabel("Filename");
        fileName = new JTextField(10);
        fileName.setMaximumSize(new Dimension(fileName.getPreferredSize()));
        label.setLabelFor(fileName);
        JPanel fileNamePanel = new JPanel(new FlowLayout());
        fileNamePanel.add(label);
        fileNamePanel.add(fileName);
        return fileNamePanel;
    }

    private JPanel createSlaveIpOption() {
        JLabel label = new JLabel("Run IP");
        masterIp = new JTextField(10);
        masterIp.setMaximumSize(new Dimension(masterIp.getPreferredSize()));
        label.setLabelFor(masterIp);
        JPanel slaveIpPanel = new JPanel(new FlowLayout());
        slaveIpPanel.add(label);
        slaveIpPanel.add(masterIp);
        return slaveIpPanel;
    }

    private JPanel createIsSlaveStartOption() {
        JLabel label = new JLabel("Slave Mode"); // $NON-NLS-1$
        isSlaveStart = new TristateCheckBox();
        JPanel isSlavePanel = new JPanel(new FlowLayout());
        isSlavePanel.add(label);
        isSlavePanel.add(isSlaveStart);
        return isSlavePanel;
    }

    private JPanel createNotePanel() {
        JLabel reqLabel = new JLabel("Note");
        note = JSyntaxTextArea.getInstance(6, 10);
        note.setLanguage("text");
        reqLabel.setLabelFor(note);
        JPanel notePanel = new JPanel(new BorderLayout(5, 0));
        notePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder()));
        notePanel.add(reqLabel, BorderLayout.WEST);
        notePanel.add(JTextScrollPane.getInstance(note), BorderLayout.CENTER);
        return notePanel;
    }

    public void configure(TestElement element) {
        super.configure(element);
        interval.setText(element.getPropertyAsString(NmonConfigExecuteSampler.INTERVAL));
        hold.setText(element.getPropertyAsString(NmonConfigExecuteSampler.HOLD));
        isSlaveStart.setTristateFromProperty(element, NmonConfigExecuteSampler.IS_SLAVE_START);
        fileName.setText(element.getPropertyAsString(NmonConfigExecuteSampler.FILE_NAME));
        masterIp.setText(element.getPropertyAsString(NmonConfigExecuteSampler.MASTER_IP));
        note.setInitialText(element.getPropertyAsString(NmonConfigExecuteSampler.NOTE));
        note.setCaretPosition(0);
        NmonConfigExecuteSampler nes = (NmonConfigExecuteSampler)element;
        JMeterProperty threadValues = nes.getData();
        if (threadValues instanceof org.apache.jmeter.testelement.property.NullProperty) {
            log.warn("Received null property instead of collection");
            return;
        }
        CollectionProperty columns = (CollectionProperty)threadValues;
        this.tableModel.removeTableModelListener(this);
        JMeterPluginsUtils.collectionPropertyToTableModelRows(columns, this.tableModel);
        this.tableModel.addTableModelListener(this);
        this.buttons.checkDeleteButtonStatus();
        updateUI();
    }

    public String getStaticLabel() {
        return "BaoLu NmonConfigExecute";
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
        element.setProperty(NmonConfigExecuteSampler.INTERVAL, interval.getText(), "");
        element.setProperty(NmonConfigExecuteSampler.HOLD, hold.getText(), "");
        isSlaveStart.setPropertyFromTristate(element, NmonConfigExecuteSampler.IS_SLAVE_START);
        element.setProperty(NmonConfigExecuteSampler.FILE_NAME, fileName.getText(), "");
        element.setProperty(NmonConfigExecuteSampler.MASTER_IP, masterIp.getText(), "");
        element.setProperty(NmonConfigExecuteSampler.NOTE, note.getText(), "");
        if (element instanceof NmonConfigExecuteSampler){
            NmonConfigExecuteSampler nes = (NmonConfigExecuteSampler)element;
            CollectionProperty rows = JMeterPluginsUtils.tableModelRowsToCollectionProperty(this.tableModel, NmonConfigExecuteSampler.DATA_PROPERTY);
            nes.setData(rows);
        }
    }

    public void clearGui() {
        super.clearGui();
        initFields();
        this.tableModel.clearData();
        this.tableModel.fireTableDataChanged();
    }

    private void initFields() {
        interval.setText("");
        hold.setText("");
        fileName.setText("");
        masterIp.setText("${__machineIP()}");
        isSlaveStart.setSelected(false);
        note.setText(ATTENTION);
    }

    @Override
    public void editingStopped(ChangeEvent e) {
        updateUI();
    }

    @Override
    public void editingCanceled(ChangeEvent e) {

    }

    @Override
    public void tableChanged(TableModelEvent e) {
        updateUI();
    }

    public void updateUI() {
        super.updateUI();
        if (this.tableModel != null) {
            NmonConfigExecuteSampler utgForPreview = new NmonConfigExecuteSampler();
            utgForPreview.setData(JMeterPluginsUtils.tableModelRowsToCollectionPropertyEval(this.tableModel, NmonConfigExecuteSampler.DATA_PROPERTY));
        }
    }
}
