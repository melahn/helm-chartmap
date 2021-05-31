package com.melahn.util.helm;

import com.melahn.util.helm.model.HelmChart;

import org.apache.commons.collections4.map.MultiKeyMap;
/**
 * A class to map a key of Chart Name and Chart Version to a Helm Chart,  
 * 
 * Defined as a subclass to simplify the code references to MultiKeyMap
 */
public class ChartKeyMap extends MultiKeyMap<String, HelmChart> {

}
