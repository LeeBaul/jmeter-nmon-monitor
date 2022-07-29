package com.libaolu.nmon.control.gui;

import com.libaolu.nmon.sampler.NmonFileAnalyseSampler;
import org.apache.jmeter.gui.util.JSyntaxTextArea;
import org.apache.jmeter.gui.util.JTextScrollPane;
import org.apache.jmeter.gui.util.VerticalPanel;
import org.apache.jmeter.samplers.gui.AbstractSamplerGui;
import org.apache.jmeter.testelement.TestElement;

import javax.swing.*;
import java.awt.*;

/**
 * <p/>
 *
 * @author libaolu
 * @version 1.0
 * @dateTime 2020/2/17 14:14
 **/
public class NmonFileAnalyseSamplerGui extends AbstractSamplerGui {

    /**
     * 注意事项
     */
    private JSyntaxTextArea noteMsg;
    private static final String ATTENTION_1 = "本采样器必须放置tearDown Thread Group中，单线执行1次\n" + "NMON解析结果可在console/jmeter.log/jmeter-server.log中查看\n" + "原始NMON结果文件保存在JMeter/bin/nmonTemp目录下";

    public NmonFileAnalyseSamplerGui() {
        init();
        initFields();
    }

    private void init() {
        setLayout(new BorderLayout(0, 5));
        setBorder(makeBorder());
        add(makeTitlePanel(), "North");
        VerticalPanel mainPanel = new VerticalPanel();
        mainPanel.add(createActionPanel());
        add(mainPanel, BorderLayout.CENTER);
    }

    private JPanel createActionPanel() {
        JLabel reqLabel = new JLabel("注意事项");
        noteMsg = JSyntaxTextArea.getInstance(4, 20);
        noteMsg.setLanguage("text");
        reqLabel.setLabelFor(noteMsg);
        JPanel noteMsgPanel = new JPanel(new BorderLayout(5, 0));
        noteMsgPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder()));
        noteMsgPanel.add(reqLabel, BorderLayout.WEST);
        noteMsgPanel.add(JTextScrollPane.getInstance(noteMsg), BorderLayout.CENTER);
        return noteMsgPanel;
    }

    @Override
    public String getLabelResource() {
        return super.getClass().getSimpleName();
    }

    public String getStaticLabel() {
        return "Baolu NmonFileAnalyse";
    }

    @Override
    public TestElement createTestElement() {
        NmonFileAnalyseSampler sampler = new NmonFileAnalyseSampler();
        modifyTestElement(sampler);
        return sampler;
    }

    @Override
    public void modifyTestElement(TestElement testElement) {
        super.configureTestElement(testElement);
    }

    @Override
    public void clearGui() {
        super.clearGui();
        initFields();
    }

    private void initFields() {
        noteMsg.setText(ATTENTION_1);
    }
}
