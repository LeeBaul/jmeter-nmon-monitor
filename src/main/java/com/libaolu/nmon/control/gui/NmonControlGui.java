package com.libaolu.nmon.control.gui;

import com.libaolu.nmon.control.NmonControl;
import com.libaolu.nmon.sampler.NmonConfigExecuteSampler;
import kg.apc.jmeter.JMeterPluginsUtils;
import kg.apc.jmeter.gui.ButtonPanelAddCopyRemove;
import org.apache.jmeter.control.gui.LogicControllerGui;
import org.apache.jmeter.gui.JMeterGUIComponent;
import org.apache.jmeter.gui.UnsharedComponent;
import org.apache.jmeter.gui.util.*;
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.*;


/**
 * @author libaolu
 * @since 2024/1/25
 */
public class NmonControlGui extends LogicControllerGui
        implements JMeterGUIComponent, ActionListener, UnsharedComponent, TableModelListener, CellEditorListener, Runnable {
    private static final Logger log = LoggerFactory.getLogger(NmonControlGui.class);

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
    private JButton stopNmon;
    private JButton startNmon;
//    private JButton startAnalyse;
    private NmonControl nmonControl;

    private static final String ACTION_STOP = "stopNmon"; // $NON-NLS-1$

    private static final String ACTION_START = "startNmon"; // $NON-NLS-1$
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> timerHandle;
    private static final String[] defaultValues = new String[] {"192.168.56.129", "root", "root","Linux"};

    protected static final String[] columnIdentifiers = new String[] { "ip", "username", "password" ,"serverType"};

    protected static final Class[] columnClasses = new Class[] { String.class, String.class, String.class ,String.class};

    public NmonControlGui() {
        super();
        log.debug("Creating NmonControlGui");
        init();
    }
    private void init() {
        setLayout(new BorderLayout(0, 5));
        setBorder(makeBorder());
        add(makeTitlePanel(), BorderLayout.NORTH);
        VerticalPanel verticalPanel = new VerticalPanel();
        HorizontalPanel optionsPanel = new HorizontalPanel();
        optionsPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder()));
        optionsPanel.add(createIntervalOption());
        optionsPanel.add(createHoldOption());
        optionsPanel.add(createFileNameOption());
        optionsPanel.add(createSlaveIpOption());
        optionsPanel.add(createIsSlaveStartOption());
        verticalPanel.add(optionsPanel);
        verticalPanel.add(createParamsPanel());
        verticalPanel.add(createControls());
        add(verticalPanel, BorderLayout.CENTER);
    }

    private JPanel createControls() {
        startNmon = new JButton(ACTION_START); // $NON-NLS-1$
        startNmon.addActionListener(this);
        startNmon.setActionCommand(ACTION_START);
        startNmon.setEnabled(true);

        stopNmon = new JButton(ACTION_STOP); // $NON-NLS-1$
        stopNmon.addActionListener(this);
        stopNmon.setActionCommand(ACTION_STOP);
        stopNmon.setEnabled(false);

        JPanel panel = new JPanel();
        panel.add(startNmon);
        panel.add(stopNmon);
        return panel;
    }

    private JPanel createParamsPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("server config message"));
        panel.setPreferredSize(new Dimension(280, 280));
        JScrollPane scroll = new JScrollPane(createGrid());
        scroll.setPreferredSize(scroll.getMinimumSize());
        panel.add(scroll, BorderLayout.CENTER);
        this.buttons = new ButtonPanelAddCopyRemove(this.grid, this.tableModel, (Object[])defaultValues);
        panel.add(this.buttons, BorderLayout.SOUTH);
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
        this.tableModel = new PowerTableModel(columnIdentifiers, columnClasses);
        this.tableModel.addTableModelListener(this);
        this.grid.setModel(this.tableModel);
    }
    private JPanel createIntervalOption() {
        JLabel label = new JLabel("Interval(sec)");
        interval = new JTextField(4);
        interval.setMaximumSize(new Dimension(interval.getPreferredSize()));
        label.setLabelFor(interval);
        JPanel intervalPanel = new JPanel(new FlowLayout());
        intervalPanel.add(label);
        intervalPanel.add(interval);
        return intervalPanel;
    }

    private JPanel createHoldOption() {
        JLabel label = new JLabel("During(sec)");
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
        fileName = new JTextField(20);
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

    @Override
    public String getStaticLabel() {
        return "Baolu NmonControl";
    }

    @Override
    public String getLabelResource() {
        return super.getClass().getSimpleName();
    }

    @Override
    public Collection<String> getMenuCategories() {
        return Arrays.asList(MenuFactory.NON_TEST_ELEMENTS);
    }

    @Override
    public void configure(TestElement element) {
        super.configure(element);
        nmonControl = (NmonControl) element;
        interval.setText(nmonControl.getInterval());
        hold.setText(nmonControl.getHold());
        fileName.setText(nmonControl.getFileName());
        masterIp.setText(nmonControl.getMasterIp());
        isSlaveStart.setSelected(nmonControl.getIsSlaveStart());
        JMeterProperty threadValues = nmonControl.getData();
        if (threadValues instanceof org.apache.jmeter.testelement.property.NullProperty) {
            log.warn("Received null property instead of collection");
            return;
        }
        CollectionProperty columns = (CollectionProperty)threadValues;
        tableModel.removeTableModelListener(this);
        JMeterPluginsUtils.collectionPropertyToTableModelRows(columns, tableModel);
        tableModel.addTableModelListener(this);
        buttons.checkDeleteButtonStatus();
        updateUI();
    }

    @Override
    public TestElement createTestElement() {
        nmonControl = new NmonControl();
        modifyTestElement(nmonControl);
        return nmonControl;
    }

    /**
     * Modifies a given TestElement to mirror the data in the gui components.
     *
     * @see org.apache.jmeter.gui.JMeterGUIComponent#modifyTestElement(TestElement)
     */
    @Override
    public void modifyTestElement(TestElement el) {
        configureTestElement(el);
        if (el instanceof NmonControl) {
            nmonControl =(NmonControl) el;
            nmonControl.setInterval(interval.getText());
            nmonControl.setHold(hold.getText());
            nmonControl.setFileName(fileName.getText());
            nmonControl.setMasterIp(masterIp.getText());
            nmonControl.setIsSlaveStart(isSlaveStart.isSelected());
            CollectionProperty rows = JMeterPluginsUtils.tableModelRowsToCollectionProperty(this.tableModel, NmonControl.DATA_PROPERTY);
            nmonControl.setData(rows);
        }
    }

    @Override
    public void actionPerformed(ActionEvent action) {
        String command = action.getActionCommand();

        if (command.equals(ACTION_STOP)) {
            nmonControl.stopNmon();
            stopNmon.setEnabled(false);
            startNmon.setEnabled(true);
            shutdownScheduler();
        } else if (command.equals(ACTION_START)) {
            modifyTestElement(nmonControl);
            scheduler = Executors.newScheduledThreadPool(1);
            timerHandle = scheduler.scheduleAtFixedRate(this,Integer.parseInt(nmonControl.getHold()),60, TimeUnit.SECONDS);
            try {
                nmonControl.startNmon();
            } catch (IOException e) {
                startNmon.setEnabled(true);
                stopNmon.setEnabled(false);
                throw new RuntimeException(e);
            }
            startNmon.setEnabled(false);
            stopNmon.setEnabled(true);
        }
    }

    public void shutdownScheduler() {
        scheduler.shutdown();
        boolean cancelState = timerHandle.cancel(false);
        if (log.isDebugEnabled()){
            log.debug("Canceled state: {}", cancelState);
        }
        try {
            scheduler.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("Error waiting for end of scheduler {}", e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void editingStopped(ChangeEvent e) {
        updateUI();
    }

    @Override
    public void editingCanceled(ChangeEvent e) {
//        updateUI();
    }

    @Override
    public void tableChanged(TableModelEvent e) {
        updateUI();
    }

    @Override
    public void updateUI() {
        super.updateUI();
        if (this.tableModel != null) {
            NmonConfigExecuteSampler utgForPreview = new NmonConfigExecuteSampler();
            utgForPreview.setData(JMeterPluginsUtils.tableModelRowsToCollectionPropertyEval(this.tableModel, NmonConfigExecuteSampler.DATA_PROPERTY));
        }
    }

    @Override
    public void clearGui(){
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
    }

    /**
     * Redefined to remove change parent and insert parent menu
     * @see org.apache.jmeter.control.gui.AbstractControllerGui#createPopupMenu()
     */
    @Override
    public JPopupMenu createPopupMenu() {
        JPopupMenu pop = new JPopupMenu();
        MenuFactory.addEditMenu(pop, true);
        MenuFactory.addFileMenu(pop);
        return pop;
    }

    @Override
    public void run() {
        nmonControl.stopNmon();
        stopNmon.setEnabled(false);
        startNmon.setEnabled(true);
        shutdownScheduler();
    }
}
