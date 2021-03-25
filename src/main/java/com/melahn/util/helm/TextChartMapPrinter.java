package com.melahn.util.helm;

import org.apache.commons.collections4.map.MultiKeyMap;

import com.melahn.util.helm.model.HelmChart;


// TODO add some real tests

public class TextChartMapPrinter extends ChartMapPrinter {

    public TextChartMapPrinter(ChartMap chartMap, String outputFilename, MultiKeyMap charts, HelmChart chart) {
        super(chartMap, outputFilename, charts, chart);
    }
}
