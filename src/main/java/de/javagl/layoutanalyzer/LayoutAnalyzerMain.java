/*
 * LayoutAnalyzer  
 *
 * Copyright (c) 2015-2015 Marco Hutter - http://www.javagl.de
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package de.javagl.layoutanalyzer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import de.javagl.swing.tasks.runner.TaskRunner;
import de.javagl.swing.tasks.runner.TaskRunnerControlPanel;

/**
 * The main class of the LayoutAnalyzer
 */
public class LayoutAnalyzerMain 
{
    /**
     * The entry point of this application
     * 
     * @param args Not used
     */
    public static void main(String[] args)
    {
        LoggerUtil.initLogging();
        SwingUtilities.invokeLater(
            () -> createAndShowGUI());
    }
    
    /**
     * Create and show the GUI, to be called on the Event Dispatch Thread
     */
    private static void createAndShowGUI()
    {
        // Create the main frame
        JFrame f = new JFrame("LayoutAnalyzer");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.getContentPane().setLayout(new BorderLayout());

        // Create the Layout and initialize it with some test data
        Layout layout = new Layout();
        Layouts.initTestData(layout);
        
        // Create the panel that shows the layout
        int WIDTH = 1000;
        int HEIGHT = 1000;
        LayoutPanel layoutPanel = new LayoutPanel(layout);
        layoutPanel.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        f.getContentPane().add(layoutPanel, BorderLayout.CENTER);

        // Create the Aspects that should determine the layout
        List<Aspect> aspects = new ArrayList<Aspect>();
        aspects.add(new TargetPositionForce(layout.getLayoutObjects()));
        aspects.add(new PairwiseRepulsionForce(300.0));
        aspects.add(new ShapeBoundsRepulsionForce());
        aspects.add(new ShapeBoundsBorderRepulsionForce(
            new Rectangle2D.Double(200, 200, WIDTH-400,HEIGHT-400)));
        
        // Create the control panel, containing one AspectPanel for
        // each aspect, offering a slider for the weight, and a 
        // panel for plotting the quality measures that are collected
        // the the QualityDataRecorders
        JPanel controlPanel = new JPanel();
        BoxLayout boxLayout = new BoxLayout(controlPanel, BoxLayout.Y_AXIS);
        controlPanel.setLayout(boxLayout);
        Map<Aspect, QualityDataRecorder> qualityDataRecorders = 
            new LinkedHashMap<Aspect, QualityDataRecorder>();
        for (Aspect aspect : aspects)
        {
            int queueSize = 500;
            QualityDataRecorder qualityDataRecorder = 
                new QualityDataRecorder(queueSize);
            AspectPanel aspectPanel = 
                new AspectPanel(aspect, qualityDataRecorder);
            aspectPanel.setPreferredSize(new Dimension(400,250));
            controlPanel.add(aspectPanel);
            qualityDataRecorders.put(aspect, qualityDataRecorder);
        }
        f.getContentPane().add(controlPanel, BorderLayout.EAST);
        
        // Create the Layouter and add all aspects to it
        Layouter layouter = new Layouter(layout);
        for (Aspect aspect : aspects)
        {
            layouter.addAspect(aspect);
        }
        
        // Attach a listener to the Layouter that will receive the
        // LayouterData after each step, pass it to the LayoutPanel
        // so that the LayoutData (i.e. the force arrows) may be
        // painted, and and pass the contained QualityData to the 
        // control panel for plotting the quality measures
        LayouterDataListener layouterDataListener = new LayouterDataListener()
        {
            @Override
            public void layouterDataComputed(LayouterData layouterData)
            {
                layoutPanel.setLayouterData(layouterData);
                Set<Aspect> aspects = layouterData.getAspects();
                for (Aspect aspect : aspects)
                {
                    QualityData qualityData = 
                        layouterData.getQualityData(aspect);
                    QualityDataRecorder qualityDataRecorder =
                        qualityDataRecorders.get(aspect);
                    qualityDataRecorder.record(qualityData);
                }
                controlPanel.repaint();
            }
        };
        layouter.addLayouterDataListener(layouterDataListener);

        // Create the task that will run the Layouter in an own
        // thread, and add a control panel for the task runner
        LayouterTask layouterTask = new LayouterTask(layouter, 
            () -> layoutPanel.repaint());
        TaskRunner taskRunner = new TaskRunner(layouterTask);
        TaskRunnerControlPanel taskRunnerControlPanel = 
            new TaskRunnerControlPanel();
        taskRunnerControlPanel.setTaskRunner(taskRunner);
        f.getContentPane().add(taskRunnerControlPanel, BorderLayout.NORTH);
        
        f.pack();
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }
    
}
