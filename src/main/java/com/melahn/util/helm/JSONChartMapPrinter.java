package com.melahn.util.helm;

import com.melahn.util.helm.model.HelmChart;
import com.melahn.util.helm.model.HelmDeploymentContainer;
import org.json.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;

/**
 * A class that generates a JSON file describing a Kubernetes
 * Helm Chart and its dependencies.
 *
 */
public class JSONChartMapPrinter extends ChartMapPrinter {

    /**
     * Constructor
     *
     * @param   chartMap        the instance of chartMap using this printer
     * @param   outputFilename  the name of the PlantUML file to be created.
     * @param   charts          a multi-key map of all the Helm Charts that
     *                          might be referenced.  The map is keyed by
     *                          Chart Name and Chart Version.
     * @param   chart           a Helm Chart to be printed in PlantUML format
     */
    public JSONChartMapPrinter(ChartMap chartMap, String outputFilename, ChartKeyMap charts, HelmChart chart)  throws ChartMapException {
        super(chartMap, outputFilename, charts, chart);
     }

    /**
     * For JSON, there is no header
     *
     * @throws IOException      IOException
     */
    @Override
    public void printHeader() throws ChartMapException {
        /* For JSON there is no header */
    }

    /**
     * For JSON, there is no footer
     *
     * @throws IOException      IOException
     */
    @Override
    public void printFooter() throws ChartMapException {
        /* For JSON there is no footer */
    }

    /**
     * Prints a JSON representation of the chart including
     * all dependent charts and images
     *
     * @param   c   a Helm chart
     * @throws  IOException     IOException
     */
    @Override
     public void printTree(HelmChart c) throws ChartMapException {
        // create a root JSON object to get started
        JSONObject j = new JSONObject();  // root object
        addChartToObject(c, null, j); // recursively fill out the  rest of the tree
        printObject(j);  // print it all
    }

    /**
     * A recursive method to adds a chart or image to the tree
     *
     * @param   h   a Helm chart or Docker image to add to the tree
     * @param   p   the parent helm chart of h
     * @param   j   a JSONObject to which the properties will
     *              be written
     */
    public void addChartToObject(HelmChart h, HelmChart p, JSONObject j) {
        addProperties(h, j);
        JSONArray a = new JSONArray(); // array of children
        addContainers(h, p, a);
        Iterator<HelmChart> itr = h.getDiscoveredDependencies().iterator();
        while(itr.hasNext()){
            JSONObject c = new JSONObject(); // new child object
            addChartToObject(itr.next(),h, c);
            a.put(c);  // add new child to array
        }
        j.put("children", a);  // add the array to the object
    }

    /**
     * Adds the properties of a chart or image to a JSON Object
     *
     * @param   h   a Helm Chart
     * @param   j   a JSONObject to which the properties will
     *              be written
     * @throws  IOException     IOException
     */
    private void addProperties(HelmChart h, JSONObject j) {
        j.put("name",h.getNameFull());
        j.put("type","chart");
        j.put("shortName", h.getName());
        j.put("version", h.getVersion());
        j.put("description", h.getDescription());
        j.put("maintainers", h.getMaintainers());
        j.put("keywords", h.getKeywords());
    }

    /**
     * Adds the dependent containers to an array.
     *
     * Checks if the parent of the helm chart was the same
     * parent that caused the dependent chart to be added.  This
     * check is necessary because there are cases where a chart
     * is common between two charts in a dependency tree but in each
     * usage the image that is used may be different (because of
     * the use of the imageTag for example).
     *
     *
     * @param   h   a Helm Chart
     * @param   p   the parent of h
     * @param   a   a JSONArray to which the container
     *              will be added
     * @throws  IOException     IOException
     */
    private void addContainers(HelmChart h, HelmChart p, JSONArray a) {
        for (HelmDeploymentContainer c : h.getContainers()) {
            String ignf = "";
            String pgnf = "";
            if (c.getParent() != null) {
                ignf = c.getParent().getNameFull();
            }
            if (p != null) {
                pgnf = p.getNameFull();
            }
            // If the parent chart matches the parent that was found
            // when the image was collected, then add it to the returned
            // containers so it can be printed with this chart.  Otherwise
            // ignore because it will be printed elsewhere in the tree.
            if (ignf.equals(pgnf)) {
                addContainer(c.getImage(), a);
            }
        }
    }

    /**
     * Adds a container to a JSONArray
     *
     * @param   s   the name of the container
     * @param   a   a JSONArray to which the container will
     *              be added, if not already present in the
     *              array
     * @throws  IOException     IOException
     */
    private void addContainer(String s, JSONArray a) {
        JSONObject c = new JSONObject(); // new child object for the container
        c.put("type", "image");
        addImageDetails(s, c);
        c.put("children", new JSONArray());  // add an empty child array (containers have no children)
        a.put(c); // add the container to the parent children array
    }

    /**
     * Writes a JSON object to a file
     *
     * @param   j   a JSONObject to which the properties will
     *              be written
     * @throws  IOException     IOException
     */
    private void printObject(JSONObject j) throws ChartMapException {
        String s = j.toString(indent);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilename))) {
            writer.write(s);
        }
        catch (IOException e) {
            throw new ChartMapException(e.getMessage());
        }
    }

    /**
     * @param   i   a string containing the image name which can be in a vaiety
     *           of formats
     * @param   j   a JSONObject to which the properties will
     *              be written
     */
    public void addImageDetails(String i, JSONObject j) {
        //     image: "quay.io/alfresco/service-sync:2.2-SNAPSHOT"
        String repoHost="Docker Hub";
        String imageName;
        String version="not specified";
        int count = i.length() - i.replace("/", "").length();
        if (count == 0) { // e.g. postgres:9.6.2
            imageName = i.substring(0,i.indexOf(':'));
        }
        else if (count == 1) { // e.g. : alfresco/process-services:1.8.0
            imageName = i.substring(0,i.indexOf(':'));
        } else { // e.g. quay.io/alfresco/service:1.0.0
            repoHost = i.substring(0,i.indexOf('/'));
            imageName = i.substring(i.indexOf('/')+1, i.indexOf(':'));
        }
        if (i.contains(":")) {
            version = i.substring(i.indexOf(':')+1,i.length());
        }
        j.put("name", imageName);
        j.put("repoHost", repoHost);
        j.put("version", version);
    }

    /**
     * Writes a section header.  Not relevant for JSON
     *
     * @param   header the header to be written
     * @throws  IOException     IOException
     */
    @Override
     public void printSectionHeader(String header) throws ChartMapException {
       /* For JSON there is no section header */
    }

}
