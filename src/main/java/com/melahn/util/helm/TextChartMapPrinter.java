package com.melahn.util.helm;

import org.apache.commons.collections4.map.MultiKeyMap;

import com.melahn.util.helm.model.HelmChart;


// TODO add some real tests

public class TextChartMapPrinter extends ChartMapPrinter {

    public TextChartMapPrinter(String outputFilename, MultiKeyMap charts, HelmChart chart) {
        super(outputFilename, charts, chart);
    }
}
