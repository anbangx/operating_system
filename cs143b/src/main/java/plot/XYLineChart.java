package plot;

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

public class XYLineChart extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static int deviation = 0;
	public static ArrayList<Integer> means = new ArrayList<Integer>();
	public static HashMap<Strategy, ArrayList<Result>> stats = new HashMap<Strategy, ArrayList<Result>>();

	public static JPanel createMemoryUtilChartPanel() {
		String chartTitle = "Memory Utilization(d=" + deviation + ")";
		String xAxisLabel = "request_size_mean";
		String yAxisLabel = "memory utilization";

		XYDataset dataset = createMemoryUtilDataset();

		JFreeChart chart = ChartFactory.createXYLineChart(chartTitle,
				xAxisLabel, yAxisLabel, dataset);

		// save to file
		File imageFile = new File("graph/MemoryUtilization/MemoryUtilization_d=" + deviation + ".png");
		int width = 640;
		int height = 480;

		try {
			ChartUtilities.saveChartAsPNG(imageFile, chart, width, height);
		} catch (IOException ex) {
			System.err.println(ex);
		}

		return new ChartPanel(chart);
	}

	private static XYDataset createMemoryUtilDataset() {
		XYSeriesCollection dataset = new XYSeriesCollection();
		XYSeries series1 = new XYSeries("First-Fit");
		XYSeries series2 = new XYSeries("Next-Fit");
		XYSeries series3 = new XYSeries("Best-Fit");

		int size = means.size();
		for (int i = 0; i < size; i++) {
			series1.add((double) means.get(i), stats.get(Strategy.FIRST_FIT)
					.get(i).getMemoryUtilization());
		}
//		for (int i = 0; i < size; i++) {
//			series2.add((double) means.get(i), stats.get(Strategy.NEXT_FIT)
//					.get(i).getMemoryUtilization());
//		}
		for (int i = 0; i < size; i++) {
			series3.add((double) means.get(i), stats.get(Strategy.BEST_FIT)
					.get(i).getMemoryUtilization());
		}

		dataset.addSeries(series1);
//		dataset.addSeries(series2);
		dataset.addSeries(series3);
		
		return dataset;
	}

	public static JPanel createSearchRatioChartPanel() {
		String chartTitle = "Search Ratio(d=" + deviation + ")";
		String xAxisLabel = "mean of request size";
		String yAxisLabel = "search ratio";

		XYDataset dataset = createSearchRatioDataset();

		JFreeChart chart = ChartFactory.createXYLineChart(chartTitle,
				xAxisLabel, yAxisLabel, dataset);

		// save to file
		File imageFile = new File("graph/SearchRatio/SearchRatio_d=" + deviation + ".png");
		int width = 640;
		int height = 480;

		try {
			ChartUtilities.saveChartAsPNG(imageFile, chart, width, height);
		} catch (IOException ex) {
			System.err.println(ex);
		}

		return new ChartPanel(chart);
	}

	private static XYDataset createSearchRatioDataset() {
		XYSeriesCollection dataset = new XYSeriesCollection();
		XYSeries series1 = new XYSeries("First-Fit");
		XYSeries series2 = new XYSeries("Next-Fit");
		XYSeries series3 = new XYSeries("Best-Fit");

		int size = means.size();
		for (int i = 0; i < size; i++) {
			series1.add((double) means.get(i), stats.get(Strategy.FIRST_FIT)
					.get(i).getSearchRatio());
		}
//		for (int i = 0; i < size; i++) {
//			series2.add((double) means.get(i), stats.get(Strategy.NEXT_FIT)
//					.get(i).getSearchRatio());
//		}
		for (int i = 0; i < size; i++) {
			series3.add((double) means.get(i), stats.get(Strategy.BEST_FIT)
					.get(i).getSearchRatio());
		}

		dataset.addSeries(series1);
//		dataset.addSeries(series2);
		dataset.addSeries(series3);

		return dataset;
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				new XYLineChart().setVisible(true);
			}
		});
	}
}