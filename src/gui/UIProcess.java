import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class UIProcess extends JFrame implements IPCClient.MessageListener {

    private final Color BG=new Color(10,14,22), PANEL=new Color(17,23,34),
            PANEL2=new Color(21,29,42), BORDER=new Color(45,58,78),
            TEXT=new Color(225,232,242), MUTED=new Color(130,145,165),
            GREEN=new Color(50,220,145), BLUE=new Color(75,155,255),
            ORANGE=new Color(255,175,75), RED=new Color(255,85,95);

    private IPCClient ipc;
    private JLabel connectionStatus,cpuStatus,pcValue,spValue,instructionValue,
            cycleValue,accumulatorValue,r0Value,r1Value,r2Value,r3Value,
            r4Value,r5Value,r6Value,r7Value,carryValue,overflowValue,
            processValue,schedulerValue,queueValue;
    private JTextArea programEditor,executionTrace;
    private DefaultListModel<String> queueModel;
    private JList<String> processQueue;
    private JProgressBar cpuProgress;
    private int cycle=0;

    public static void main(String[] args){
        try{UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());}
        catch(Exception ignored){}
        SwingUtilities.invokeLater(()->new UIProcess().setVisible(true));
    }

    public UIProcess(){
        setTitle("MS51 CONTROL DECK | MS51FB9AE Simulator");
        setSize(1450,900);
        setMinimumSize(new Dimension(1150,700));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setBackground(BG);
        ipc=new IPCClient(this);
        buildUI();
        initializeDemoData();
    }

    private void buildUI(){
        JPanel root=new JPanel(new BorderLayout(12,12));
        root.setBackground(BG);
        root.setBorder(new EmptyBorder(12,15,12,15));
        root.add(createHeader(),BorderLayout.NORTH);

        JPanel center=new JPanel(new BorderLayout(12,12));
        center.setBackground(BG);
        center.add(createLeftPanel(),BorderLayout.WEST);
        center.add(createCenterPanel(),BorderLayout.CENTER);
        center.add(createRightPanel(),BorderLayout.EAST);

        root.add(center,BorderLayout.CENTER);
        root.add(createFooter(),BorderLayout.SOUTH);
        setContentPane(root);
    }

    private JPanel createHeader(){
        JPanel header=new JPanel(new BorderLayout());
        header.setBackground(BG);
        header.setBorder(new EmptyBorder(0,0,12,0));

        JPanel titlePanel=new JPanel(new FlowLayout(FlowLayout.LEFT,12,0));
        titlePanel.setOpaque(false);

        JLabel logo=new JLabel("MS51");
        logo.setFont(new Font("SansSerif",Font.BOLD,28));
        logo.setForeground(BLUE);

        JLabel title=new JLabel("CONTROL DECK");
        title.setFont(new Font("SansSerif",Font.BOLD,24));
        title.setForeground(TEXT);

        JLabel version=new JLabel(" | MS51FB9AE • 8051 CORE");
        version.setFont(new Font("SansSerif",Font.PLAIN,12));
        version.setForeground(MUTED);

        titlePanel.add(logo);
        titlePanel.add(title);
        titlePanel.add(version);

        connectionStatus=new JLabel("● CORE OFFLINE");
        connectionStatus.setFont(new Font("SansSerif",Font.BOLD,12));
        connectionStatus.setForeground(RED);

        JButton connect=createButton("CONNECT CORE",BLUE);
        connect.addActionListener(e->{
            if(!ipc.isConnected()){
                if(!ipc.connect()) addTrace("[IPC] Unable to connect to Core.");
            }else ipc.disconnect();
        });

        JPanel right=new JPanel(new FlowLayout(FlowLayout.RIGHT,10,0));
        right.setOpaque(false);
        right.add(connectionStatus);
        right.add(connect);

        header.add(titlePanel,BorderLayout.WEST);
        header.add(right,BorderLayout.EAST);
        return header;
    }

    private JPanel createLeftPanel(){
        JPanel panel=new JPanel(new BorderLayout(0,12));
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(330,0));

        JPanel editor=createCard("PROGRAM EDITOR");
        programEditor=new JTextArea(
                "MOV A, #10\nMOV 30H, A\nADD A, R1\nINC R1\nHALT"
        );
        programEditor.setFont(new Font("Monospaced",Font.PLAIN,14));
        programEditor.setForeground(TEXT);
        programEditor.setBackground(new Color(9,13,20));
        programEditor.setCaretColor(BLUE);
        programEditor.setBorder(new EmptyBorder(10,10,10,10));

        JScrollPane editorScroll=new JScrollPane(programEditor);
        editorScroll.setBorder(new LineBorder(BORDER));
        editor.add(editorScroll,BorderLayout.CENTER);

        JButton load=createButton("LOAD PROGRAM",BLUE);
        load.addActionListener(e->{
            ipc.loadProgram();
            addTrace("[GUI] LOAD_PROGRAM sent to Core.");
        });
        editor.add(load,BorderLayout.SOUTH);

        JPanel controls=createCard("EXECUTION CONTROL");
        JPanel buttons=new JPanel(new GridLayout(2,2,8,8));
        buttons.setOpaque(false);

        JButton run=createButton("▶ RUN",GREEN);
        run.addActionListener(e->{
            ipc.runCPU();
            cpuStatus.setText("RUNNING");
            addTrace("[GUI] RUN command sent.");
        });

        JButton step=createButton("STEP",BLUE);
        step.addActionListener(e->{
            ipc.stepCPU();
            cycle++;
            cycleValue.setText(String.valueOf(cycle));
            addTrace("[GUI] STEP command sent.");
        });

        JButton stop=createButton("■ STOP",RED);
        stop.addActionListener(e->{
            ipc.stopCPU();
            cpuStatus.setText("STOPPED");
            addTrace("[GUI] STOP command sent.");
        });

        JButton reset=createButton("↻ RESET",ORANGE);
        reset.addActionListener(e->{
            ipc.resetCPU();
            resetDisplay();
            addTrace("[GUI] RESET command sent.");
        });

        buttons.add(run);
        buttons.add(step);
        buttons.add(stop);
        buttons.add(reset);
        controls.add(buttons,BorderLayout.CENTER);

        panel.add(editor,BorderLayout.CENTER);
        panel.add(controls,BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createCenterPanel(){
        JPanel panel=new JPanel(new BorderLayout(0,12));
        panel.setOpaque(false);

        JPanel cpu=createCard("CPU LIVE STATE");
        JPanel grid=new JPanel(new GridLayout(2,3,10,10));
        grid.setOpaque(false);

        pcValue=createValueLabel("0000H");
        spValue=createValueLabel("07H");
        instructionValue=createValueLabel("IDLE");
        cycleValue=createValueLabel("0");
        processValue=createValueLabel("P0");
        schedulerValue=createValueLabel("FCFS");

        grid.add(createMetric("PROGRAM COUNTER",pcValue));
        grid.add(createMetric("STACK POINTER",spValue));
        grid.add(createMetric("INSTRUCTION",instructionValue));
        grid.add(createMetric("CPU CYCLE",cycleValue));
        grid.add(createMetric("ACTIVE PROCESS",processValue));
        grid.add(createMetric("SCHEDULER",schedulerValue));

        cpu.add(grid,BorderLayout.CENTER);

        JPanel progress=new JPanel(new BorderLayout(10,0));
        progress.setOpaque(false);
        JLabel load=new JLabel("CPU LOAD");
        load.setForeground(MUTED);

        cpuProgress=new JProgressBar(0,100);
        cpuProgress.setValue(24);
        cpuProgress.setStringPainted(true);
        cpuProgress.setForeground(BLUE);
        cpuProgress.setBackground(new Color(30,38,52));

        progress.add(load,BorderLayout.WEST);
        progress.add(cpuProgress,BorderLayout.CENTER);
        cpu.add(progress,BorderLayout.SOUTH);

        JPanel registers=createCard("REGISTER BANK");
        JPanel regGrid=new JPanel(new GridLayout(3,4,8,8));
        regGrid.setOpaque(false);

        accumulatorValue=createValueLabel("00H");
        r0Value=createValueLabel("00H");
        r1Value=createValueLabel("01H");
        r2Value=createValueLabel("00H");
        r3Value=createValueLabel("00H");
        r4Value=createValueLabel("00H");
        r5Value=createValueLabel("00H");
        r6Value=createValueLabel("00H");
        r7Value=createValueLabel("00H");
        carryValue=createValueLabel("0");
        overflowValue=createValueLabel("0");

        regGrid.add(createRegister("ACC",accumulatorValue));
        regGrid.add(createRegister("R0",r0Value));
        regGrid.add(createRegister("R1",r1Value));
        regGrid.add(createRegister("R2",r2Value));
        regGrid.add(createRegister("R3",r3Value));
        regGrid.add(createRegister("R4",r4Value));
        regGrid.add(createRegister("R5",r5Value));
        regGrid.add(createRegister("R6",r6Value));
        regGrid.add(createRegister("R7",r7Value));
        regGrid.add(createRegister("CY",carryValue));
        regGrid.add(createRegister("OV",overflowValue));

        registers.add(regGrid,BorderLayout.CENTER);

        JPanel trace=createCard("EXECUTION TRACE");
        executionTrace=new JTextArea();
        executionTrace.setEditable(false);
        executionTrace.setFont(new Font("Monospaced",Font.PLAIN,12));
        executionTrace.setForeground(new Color(170,190,210));
        executionTrace.setBackground(new Color(9,13,20));
        executionTrace.setBorder(new EmptyBorder(10,10,10,10));

        JScrollPane traceScroll=new JScrollPane(executionTrace);
        traceScroll.setBorder(new LineBorder(BORDER));
        trace.add(traceScroll,BorderLayout.CENTER);

        panel.add(cpu,BorderLayout.NORTH);
        panel.add(registers,BorderLayout.CENTER);
        panel.add(trace,BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createRightPanel(){
        JPanel panel=new JPanel(new BorderLayout(0,12));
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(280,0));

        JPanel queue=createCard("READY QUEUE");
        queueModel=new DefaultListModel<>();
        processQueue=new JList<>(queueModel);
        processQueue.setFont(new Font("Monospaced",Font.PLAIN,13));
        processQueue.setForeground(TEXT);
        processQueue.setBackground(new Color(9,13,20));
        processQueue.setBorder(new EmptyBorder(8,8,8,8));

        JScrollPane queueScroll=new JScrollPane(processQueue);
        queueScroll.setBorder(new LineBorder(BORDER));
        queue.add(queueScroll,BorderLayout.CENTER);

        JPanel queueButtons=new JPanel(new GridLayout(1,2,8,0));
        queueButtons.setOpaque(false);

        JButton add=createButton("+ PROCESS",GREEN);
        add.addActionListener(e->{
            queueModel.addElement("P"+(queueModel.size()+1)+" READY");
            queueValue.setText(String.valueOf(queueModel.size()));
            ipc.startProcess();
            addTrace("[GUI] START_PROCESS sent.");
        });

        JButton clear=createButton("CLEAR",RED);
        clear.addActionListener(e->{
            queueModel.clear();
            queueValue.setText("0");
        });

        queueButtons.add(add);
        queueButtons.add(clear);
        queue.add(queueButtons,BorderLayout.SOUTH);

        JPanel scheduler=createCard("SCHEDULER");
        JPanel schedulerGrid=new JPanel(new GridLayout(3,1,6,6));
        schedulerGrid.setOpaque(false);

        ButtonGroup group=new ButtonGroup();
        String[] schedulers={"FCFS","ROUND ROBIN","PRIORITY"};

        for(String name:schedulers){
            JRadioButton radio=new JRadioButton(name);
            radio.setForeground(TEXT);
            radio.setBackground(PANEL);
            radio.setFont(new Font("SansSerif",Font.PLAIN,12));
            group.add(radio);
            schedulerGrid.add(radio);
            if(name.equals("FCFS")) radio.setSelected(true);

            radio.addActionListener(e->{
                schedulerValue.setText(name);
                addTrace("[GUI] Scheduler: "+name);
            });
        }

        scheduler.add(schedulerGrid,BorderLayout.CENTER);

        JPanel status=createCard("SYSTEM STATUS");
        JPanel statusGrid=new JPanel(new GridLayout(4,1,6,6));
        statusGrid.setOpaque(false);

        cpuStatus=createStatusLabel("IDLE",GREEN);
        queueValue=createValueLabel("0");

        statusGrid.add(createStatusRow("CPU",cpuStatus));
        statusGrid.add(createStatusRow("READY PROCESSES",queueValue));
        statusGrid.add(createStatusRow("MEMORY",createValueLabel("256 B")));
        statusGrid.add(createStatusRow("STACK",createValueLabel("ACTIVE")));

        status.add(statusGrid,BorderLayout.CENTER);

        panel.add(queue,BorderLayout.CENTER);
        panel.add(scheduler,BorderLayout.NORTH);
        panel.add(status,BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createFooter(){
        JPanel footer=new JPanel(new BorderLayout());
        footer.setBackground(new Color(8,11,17));
        footer.setBorder(new CompoundBorder(
                new LineBorder(BORDER),
                new EmptyBorder(8,12,8,12)));

        JLabel left=new JLabel(
                "MS51FB9AE • 8051 ARCHITECTURE • SIMULATOR CORE");
        left.setForeground(MUTED);
        left.setFont(new Font("SansSerif",Font.PLAIN,11));

        JLabel right=new JLabel("POSIX IPC • FIFO + MQ + SOCKET");
        right.setForeground(BLUE);
        right.setFont(new Font("Monospaced",Font.BOLD,11));

        footer.add(left,BorderLayout.WEST);
        footer.add(right,BorderLayout.EAST);
        return footer;
    }

    private JPanel createCard(String title){
        JPanel card=new JPanel(new BorderLayout(0,10));
        card.setBackground(PANEL);
        card.setBorder(new CompoundBorder(
                new LineBorder(BORDER),
                new EmptyBorder(12,12,12,12)));

        JLabel label=new JLabel(title);
        label.setFont(new Font("SansSerif",Font.BOLD,12));
        label.setForeground(new Color(155,175,200));
        card.add(label,BorderLayout.NORTH);
        return card;
    }

    private JPanel createMetric(String name,JLabel value){
        JPanel panel=new JPanel(new BorderLayout());
        panel.setBackground(PANEL2);
        panel.setBorder(new EmptyBorder(8,10,8,10));

        JLabel title=new JLabel(name);
        title.setFont(new Font("SansSerif",Font.PLAIN,10));
        title.setForeground(MUTED);

        panel.add(title,BorderLayout.NORTH);
        panel.add(value,BorderLayout.CENTER);
        return panel;
    }

    private JPanel createRegister(String name,JLabel value){
        JPanel panel=new JPanel(new BorderLayout());
        panel.setBackground(new Color(11,17,26));
        panel.setBorder(new LineBorder(BORDER));

        JLabel title=new JLabel(name,SwingConstants.CENTER);
        title.setForeground(MUTED);
        title.setFont(new Font("Monospaced",Font.BOLD,11));

        value.setHorizontalAlignment(SwingConstants.CENTER);

        panel.add(title,BorderLayout.NORTH);
        panel.add(value,BorderLayout.CENTER);
        return panel;
    }

    private JPanel createStatusRow(String name,JLabel value){
        JPanel panel=new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JLabel title=new JLabel(name);
        title.setForeground(MUTED);

        panel.add(title,BorderLayout.WEST);
        panel.add(value,BorderLayout.EAST);
        return panel;
    }

    private JLabel createValueLabel(String value){
        JLabel label=new JLabel(value);
        label.setFont(new Font("Monospaced",Font.BOLD,17));
        label.setForeground(TEXT);
        return label;
    }

    private JLabel createStatusLabel(String text,Color color){
        JLabel label=new JLabel(text);
        label.setForeground(color);
        label.setFont(new Font("Monospaced",Font.BOLD,14));
        return label;
    }

    private JButton createButton(String text,Color color){
        JButton button=new JButton(text);
        button.setFont(new Font("SansSerif",Font.BOLD,11));
        button.setForeground(color);
        button.setBackground(new Color(14,20,30));
        button.setFocusPainted(false);
        button.setBorder(new LineBorder(color));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter(){
            public void mouseEntered(MouseEvent e){
                button.setBackground(new Color(25,34,48));
            }
            public void mouseExited(MouseEvent e){
                button.setBackground(new Color(14,20,30));
            }
        });
        return button;
    }

    private void initializeDemoData(){
        queueModel.addElement("P0 RUNNING");
        queueModel.addElement("P1 READY");
        queueModel.addElement("P2 READY");
        queueValue.setText(String.valueOf(queueModel.size()));

        addTrace("[SYSTEM] MS51FB9AE Simulator initialized.");
        addTrace("[SYSTEM] 8051 CPU architecture loaded.");
        addTrace("[SYSTEM] Program memory ready.");
        addTrace("[SYSTEM] Data memory: 256 bytes.");
        addTrace("[SYSTEM] Scheduler: FCFS.");
        addTrace("[IPC] Waiting for Core connection.");
    }

    private void addTrace(String message){
        String time=new java.text.SimpleDateFormat("HH:mm:ss")
                .format(new Date());
        executionTrace.append("["+time+"] "+message+"\n");
        executionTrace.setCaretPosition(
                executionTrace.getDocument().getLength());
    }

    private void resetDisplay(){
        cycle=0;
        cycleValue.setText("0");
        pcValue.setText("0000H");
        spValue.setText("07H");
        instructionValue.setText("IDLE");
        accumulatorValue.setText("00H");
        r0Value.setText("00H");
        r1Value.setText("01H");
        r2Value.setText("00H");
        r3Value.setText("00H");
        r4Value.setText("00H");
        r5Value.setText("00H");
        r6Value.setText("00H");
        r7Value.setText("00H");
        carryValue.setText("0");
        overflowValue.setText("0");
        cpuProgress.setValue(0);
        cpuStatus.setText("IDLE");
        cpuStatus.setForeground(GREEN);
    }

    @Override
    public void onMessage(String message){
        addTrace("[CORE] "+message);

        if(message.startsWith("PC="))
            pcValue.setText(message.substring(3));
        else if(message.startsWith("SP="))
            spValue.setText(message.substring(3));
        else if(message.startsWith("INSTRUCTION="))
            instructionValue.setText(
                    message.substring("INSTRUCTION=".length()));
        else if(message.startsWith("ACC="))
            accumulatorValue.setText(message.substring(4));
        else if(message.startsWith("R0="))
            r0Value.setText(message.substring(3));
        else if(message.startsWith("R1="))
            r1Value.setText(message.substring(3));
        else if(message.startsWith("R2="))
            r2Value.setText(message.substring(3));
        else if(message.startsWith("R3="))
            r3Value.setText(message.substring(3));
        else if(message.startsWith("R4="))
            r4Value.setText(message.substring(3));
        else if(message.startsWith("R5="))
            r5Value.setText(message.substring(3));
        else if(message.startsWith("R6="))
            r6Value.setText(message.substring(3));
        else if(message.startsWith("R7="))
            r7Value.setText(message.substring(3));
        else if(message.startsWith("CY="))
            carryValue.setText(message.substring(3));
        else if(message.startsWith("OV="))
            overflowValue.setText(message.substring(3));
        else if(message.startsWith("PROCESS="))
            processValue.setText(message.substring(8));
        else if(message.startsWith("CPU=")){
            String state=message.substring(4);
            cpuStatus.setText(state);

            if(state.equalsIgnoreCase("RUNNING"))
                cpuStatus.setForeground(GREEN);
            else if(state.equalsIgnoreCase("STOPPED"))
                cpuStatus.setForeground(RED);
            else
                cpuStatus.setForeground(ORANGE);
        }
    }

    @Override
    public void onConnectionChanged(boolean connected){
        if(connected){
            connectionStatus.setText("● CORE ONLINE");
            connectionStatus.setForeground(GREEN);
            addTrace("[IPC] Connected to Core.");
        }else{
            connectionStatus.setText("● CORE OFFLINE");
            connectionStatus.setForeground(RED);
        }
    }
}
