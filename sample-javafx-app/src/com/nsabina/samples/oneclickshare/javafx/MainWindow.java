package com.nsabina.samples.oneclickshare.javafx; 
 
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javafx.application.Platform;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
  
public class MainWindow extends JFrame {

    private static final long serialVersionUID = 1L;
    private WorkflowManager manager = new WorkflowManager(this);
 
    private final JPanel panel = new JPanel(null); 

    private final JButton shareButton = new JButton("Share");
    
    public MainWindow() {
        super();
        initComponents();
    }

    
    private void initComponents() {
        
    	manager.setStatusWidgetUrl("http://localhost:8000/check-progress.html");
        //shareCaseButton.setEnabled(false);
        shareButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	manager.startWorkflow("http://localhost:8000/share-widgets-step1.html", "{\"visitorId\":\"visitor00012345\", \"visitorName\":\"Paul Smith\"}");

                
            }});
        
        shareButton.setBounds(50, 50, 100, 40);
        panel.add(shareButton);
        
        getContentPane().add(panel);
        
        setPreferredSize(new Dimension(1024, 600));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();

    }
 
   public static void main(String[] args) {
        Platform.setImplicitExit(false);
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                MainWindow browser = new MainWindow();
                browser.setVisible(true);
           }     
       });
    }
}