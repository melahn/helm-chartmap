package com.melahn.util.helm;

import com.melahn.util.helm.model.HelmChart;
public class TextChartMapPrinter extends ChartMapPrinter {

    public TextChartMapPrinter(ChartMap chartMap, String outputFilename, ChartKeyMap charts, HelmChart chart) throws ChartMapException {
        super(chartMap, outputFilename, charts, chart);
    }
}
