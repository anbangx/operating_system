package plot;

import java.awt.BorderLayout;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import mm.core.MMManager.Strategy;
import mm.core.Result;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;


public class XYLineChartExample extends JFrame {
 
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public static int deviation = 0;
	public static ArrayList<Integer> means = new ArrayList<Integer>();
	public static HashMap<Strategy, ArrayList<Result>> stats = new HashMap<Strategy, ArrayList<Result>>(); 
	
	public XYLineChartExample() {
        super("XY Line Chart Example with JFreechart");
 
        JPanel chartPanel = createChartPanel();
        add(chartPanel, BorderLayout.CENTER);
 
        setSize(640, 480);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }
 
    private JPanel createChartPanel() {
    	String chartTitle = "Objects Movement Chart";
        String xAxisLabel = "request_size_mean";
        String yAxisLabel = "deviation";
     
        XYDataset dataset = createDataset();
     
        JFreeChart chart = ChartFactory.createXYLineChart(chartTitle,
                xAxisLabel, yAxisLabel, dataset);
        
        // save to file
        File imageFile = new File("MemoryUtilization.png");
        int width = 640;
        int height = 480;
         
        try {
            ChartUtilities.saveChartAsPNG(imageFile, chart, width, height);
        } catch (IOException ex) {
            System.err.println(ex);
        }
        
        return new ChartPanel(chart);
    }
    
    private JPanel createMemoryUtilChartPanel() {
    	String chartTitle = "Memory Utilization deviation=" + deviation;
        String xAxisLabel = "request_size_mean";
        String yAxisLabel = "memory utilization";
     
        XYDataset dataset = createMemoryUtilDataset();
     
        JFreeChart chart = ChartFactory.createXYLineChart(chartTitle,
                xAxisLabel, yAxisLabel, dataset);
        
        // save to file
        File imageFile = new File("MemoryUtilization.png");
        int width = 640;
        int height = 480;
         
        try {
            ChartUtilities.saveChartAsPNG(imageFile, chart, width, height);
        } catch (IOException ex) {
            System.err.println(ex);
        }
        
        return new ChartPanel(chart);
    }
    
    private XYDataset createMemoryUtilDataset() {
    	XYSeriesCollection dataset = new XYSeriesCollection();
        XYSeries series1 = new XYSeries("First-Fit");
        XYSeries series2 = new XYSeries("Best-Fit");
        
        int size = means.size();
        for(int i = 0; i < size; i++){
	        series1.add((double)means.get(i), stats.get(Strategy.FIRST_FIT).get(i).getMemoryUtilization());
        }
     
        dataset.addSeries(series1);
        dataset.addSeries(series2);
     
        return dataset;
    }
    
    private XYDataset createDataset() {
    	XYSeriesCollection dataset = new XYSeriesCollection();
        XYSeries series1 = new XYSeries("First-Fit");
        XYSeries series2 = new XYSeries("Best-Fit");
     
        series1.add(1.0, 2.0);
        series1.add(2.0, 3.0);
        series1.add(3.0, 2.5);
        series1.add(3.5, 2.8);
        series1.add(4.2, 6.0);
     
        series2.add(2.0, 1.0);
        series2.add(2.5, 2.4);
        series2.add(3.2, 1.2);
        series2.add(3.9, 2.8);
        series2.add(4.6, 3.0);
     
        dataset.addSeries(series1);
        dataset.addSeries(series2);
     
        return dataset;
    }
 
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new XYLineChartExample().setVisible(true);
            }
        });
    }
}