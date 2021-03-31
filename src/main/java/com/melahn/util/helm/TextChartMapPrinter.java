package com.melahn.util.helm;

import org.apache.commons.collections4.map.MultiKeyMap;

import com.melahn.util.helm.model.HelmChart;
public class TextChartMapPrinter extends ChartMapPrinter {

    public TextChartMapPrinter(ChartMap chartMap, String outputFilename, MultiKeyMap<String,String> charts, HelmChart chart) {
        super(chartMap, outputFilename, charts, chart);
    }
}
