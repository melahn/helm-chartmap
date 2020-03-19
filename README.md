# Chart Map

See some examples &#8594; [https://melahn.github.io/helm-chartmap/](https://melahn.github.io/helm-chartmap/) 

## Overview

This project generates a file that shows the recursive dependencies of a Helm Chart.  

The generated file can be in JSON format, PlantUML text format or plain text format.  

The JSON file can be used with [helm-inspector](https://github.com/melahn/helm-inspector) to 
create an interaction visualization of the Helm Chart based using the tree view of [Data Driven Documents](https://d3js.org/).  You can see an example of 
such a JSON file here &#8594; https://melahn.github.io/helm-chartmap/alfresco-dbp/alfresco-dbp-1.5.0.json and you can see
an example of how it can be visualized with helm inspector here &#8594; https://melahn.github.io/helm-inspector/src/?chart=./examples/alfresco-dbp-1.5.0

The PlantUML file can be turned into an image.  You can see an example of that here &#8594; 
https://melahn.github.io/helm-chartmap/alfresco-dbp/alfresco-dbp-1.5.0.png.
For more information about PlantUML, see http://plantuml.com/.  

The text file provides a simple text summary of the charts and images used, and the dependencies.  It also
detects anomolies such as a stable chart depending on an incubator chart.
You can see an example of a text file generated from Chart Map here &#8594; 
https://melahn.github.io/helm-chartmap/alfresco-dbp/alfresco-dbp-1.5.0.txt.

## Prerequisites

Java 8 or later.  

Helm Client 2.9.1 or later.  The Helm Client is required since the chart map is based on the dependencies discovered by the Kubernetes Helm client. I have tested it with version 2.9.1 of the Helm Client though other versions may also work. 

For instructions on installing the Helm Client, see https://docs.helm.sh/using_helm/#installing-helm

The junit test cases rely on the environment variable *HELM_HOME* being set.


## Using Chart Map

### Setup

1. Download the executable jar from the [resource directory](./resource/jar), or build it yourself from source (see below).

2. Run the command line, or write a Java program using the API, to generate a chart.  See Syntax and Examples below.

### Command Line Syntax

```
                                    
java -jar ---<filename>---+---  -a <apprspec>----+---  -o <filename>---  -d <directoryname>----+---------------------+---+------------+---+------------+---+------------+---+------------+
                          |                      |                                             |                     |   |            |   |            |   |            |   |            |
                          +---  -c <chartname>---+                                             +---  -e <filename ---+   +---  -g  ---+   +---  -r  ---+   +---  -v  ---+   +---  -h  ---+
                          |                      |                                                                       
                          +---  -f <filename>----+                                                                       
                          |                      |                                                                       
                          +---  -u <url>---------+        
                   
```

#### Parameters

* **Required**
   * \<filename\>
     * The name of the jar file (e.g. target/chartmap-1.0-SNAPSHOT.jar)
   * To specify the Helm Chart, one of the following input formats must be specified
     * **-a** \<apprspec\>
          *  The name and version of the chart as an appr specification \<host\>/\<org\>/\<chart-name\>@\<chart-version\>   
     * **-c** \<chartname\>
          *  The name and version of the chart in the format \<chart-name\:chart-version\>
     * **-f** \<filename\>
          *  The location in the file system for a Helm Chart package (a tgz file)
     * **-u** \<url\>
          *  A url for the Helm Chart
   * **-d** \<directoryname\>
      * The file system location of HELM_HOME 
   * **-o** \<filename\>
      * The name of the file to be generated.  
      If a file extension of 'puml' is specifed the format of the generated file will be PlantUML. 
      If a file extension of 'json' is specifed the format of the generated file will be in JSON format.
      Otherwise it will be plain text. 
* **Optional**
   * **-e** \<filename\>
      *  The location of an Environment Specification which is a yaml file containing a list of environment variables to set before rendering helm templates.  See the example environment specification provided in resource/example-env-spec.yaml to understand the format. 
   * **-g**
      * Generate image.  Whenever specified, an image file is generated from the PlantUML file (if any).
   * **-r**
      * Refresh.  If specified, the Helm command *helm update dependencies* will be run before generating the chart map
   * **-v**
      * Verbose.  If specified, some extra command line output is shown
   * **-h**
      * Help.  Whenever specified, any other parameters are ignored.  When no parameters are specified, **-h** is assumed.
 
#### Example Commands

##### Generating a Chartmap using a chart reference 
```
java -jar chartmap-1.0-SNAPSHOT.jar -c "wordpress:0.8.17" -r -v -o "wordpress.puml" -d "/Users/melahn/.helm"
```
##### Generating a Chartmap using a file specification
```
java -jar chartmap-1.0-SNAPSHOT.jar -f "/Users/melahn/helm/alfresco-content-services-2.1.3.tgz" " -d "/Users/melahn/.helm" -o  alfresco-dbp.puml -v -g

```
##### Generating a Chartmap using a url specification
```
java -jar chartmap-1.0-SNAPSHOT.jar -u "http://kubernetes-charts.alfresco.com/stable/alfresco-content-services-2.1.3.tgz" " -d "/Users/melahn/.helm" -o  alfresco-dbp.puml -v

```
##### Generating a Chartmap using an appr specification
```
java -jar chartmap-1.0-SNAPSHOT.jar -a "quay.io/alfresco/alfresco-dbp@1.5.0" -d "/Users/melahn/.helm" -o  alfresco-dbp.puml -v

```

### Java Methods

In addition to the command line interface, a Java API is provided.


#### Constructor
```
    public ChartMap(ChartOption option,
                    String chart,
                    String outputFilename,
                    String helmHome,
                    String envFilename,
                    boolean generate,
                    boolean refresh,
                    boolean verbose)
                    
```                  
##### Description
Constructs a new instance of the *com.melahn.util.helm.ChartMap* class

##### Parameters
* *option*            
  * The format of the Helm Chart 
* *chart*             
  * The name of the Helm Chart in one of the formats specified by the option parameter
* *outputFilename*     
  * The name of the file to which to write the generated Chart Map.  Note the file is overwritten if it exists.
* *helmHome*          
  * The location of Helm Home
* *envSpecFilename*          
    * The location of an Environment Specification which is a yaml file containing a list of environment variables to set before rendering helm templates, or <null>.  See the example environment specification provided in resource/example-env-spec.yaml to understand the format. 
* *generate*            
  * When *true*, an image file is generated from the PlantUML file (if any). (default *false*)
* *refresh*            
    * When *true*, refresh the local Helm repo (default *false*)
* *verbose*           
  * When *true*, provides a little more information as the Chart Map is generated (default *false*)
                                          

##### Throws
* *java.lang.Exception*

#### print

##### Description
Prints a *ChartMap* 

```
    public void print ()
                    
```    

##### Throws
* *java.io.Exception*

#### Java Example
```
import com.melahn.util.helm.ChartMap;
import ChartOption;

public class ChartMapExample {
    public static void printExampleChartMap(String[] args) {
        try {
            ChartMap testMap = new ChartMap(
                    ChartOption.FILENAME,
                    "src/test/resource/testChartFile.tgz",
                    "my-chartmap.puml",
                    System.getenv("HELM_HOME"),
                    "resource/example/example-env-spec.yaml",
                    false,
                    true);
            testMap.print();
        } catch (Exception e) {
            System.out.println("Exception generating chart map: " + e.getMessage());
        }
    }
}
```
More examples illustrating the use of the Java interface can be found in [ChartMapTest.java](./src/test/java/org/com.melahn.util.helm/ChartMapTest.java).
 
### Examples of Generated Files

#### Example Image generated from a PlantUML file generated by Chartmap

![Example Image](./docs/alfresco-dbp/alfresco-dbp-1.5.0.png)


Note that the colors chosen for a chart are randomly selected from a standard set of PlantUML
colors (see [PlantUML Colors](http://plantuml.com/color)) using a method that will depict
Helm Charts or Docker Files that differ only by their version using the same color.   For example 'postgresql:0.8.5'
and 'postgresql:0.8.7' will be depicted with the same color.  This will make it easier to spot
cases you may want to optimize a deployment to use a common Helm Chart or Docker Image instead.

Helm Charts are depicted as rectangular objects.   Docker Images are depicted as ovals.

Dependencies of Helm Charts on other Helm Charts are shown as green lines.   Dependencies of Helm Charts on Docker Images are shown as orange lines.

#### Example PlantUML File generated by Chartmap

[Example PlantUML File](./docs/alfresco-dbp/alfresco-dbp-1.5.0.puml)

#### Example JSON File generated by Chartmap

[Example JSON File](./docs/alfresco-dbp/alfresco-dbp-1.5.0.json)

#### Example Text File generated by Chartmap

[Example Text File](./docs/alfresco-dbp/alfresco-dbp-1.5.0.txt)


### Architecture Overview

![Architecture](./resource/documentation/architecture.png)

A illustrated, there is a *Chartmap* component, implemented as a Java class, that reads
in a Helm Chart from a Helm Chart source. It then relies on the use 
of the [helm template command](https://helm.sh/docs/helm/helm_template)
to recursively generate a template representation of a Helm Chart and its dependencies. 
The resulting templates are parsed and the information saved in an in-memory representation
of the Helm Chart and its dependencies, using a model of each of the main elements of Helm,
such as *HelmChart* and *HelmDeploymentContainer*.  

The result is then used to generate a file
representation of the Helm Chart using one of several *ChartMapPrinter* classes,
such as the *PlantUMLChartMapPrinter*. The end-user can then enjoy the result using an image
viewer, a text viewer or [helm-inspector](https://github.com/melahn/helm-inspector).

### Maven Commands

#### Building the jar from source and running tests

1.  git clone this repository
2.  Run Maven
```
mvn clean install 

```
Note: The [prebuilt jar](resource/jar/chartmap-1.0-SNAPSHOT.jar) that is included in the ./resources directory targets Java 8 for the widest compatibiity. You can target a different
version of Java by modifying the configuration in the maven-compiler-plugin to use a different target like in the example below.
```
<target>11</target>
```

#### Building Image files from PUML source
1.  Git clone this repository
2.  Copy any PUML files into the source directory
3.  Run Maven
```
mvn com.github.jeluard:plantuml-maven-plugin:generate
```

### Generating Images from PlantUML files outside of Maven

Having generated some PlantUML files, if you want to generate image files from the PlantUML files outside of Maven, there are several options. 
*  Use the online [PlantUML Service](http://www.plantuml.com/plantuml/uml/SyfFKj2rKt3CoKnELR1Io4ZDoSa70000).
Just copy/paste the generated PlantUML text and click 'Submit'.  Then you can view the resulting image as PNG, SVG or Ascii Art. 
*  Download the [PlantUML jar](http://plantuml.com/download) and use 
the command line like this ...
 ```
java -DPLANTUML_LIMIT_SIZE=8192 -jar ~/Downloads/plantuml.jar alfresco-dbp-1.5.0.puml
 ```
*  Build PlantUML from [source](https://github.com/plantuml/plantuml) and then use the command line like this ...
```
java -DPLANTUML_LIMIT_SIZE=8192 -jar ~/IdeaProjects/plantuml/target/plantuml-1.2018.11-SNAPSHOT.jar -tsvg alfresco-dbp-1.5.0.puml
```
**Notes about a local deployment of PlantUML:**
 * Setting the optional property *PLANTUML_LIMIT_SIZE=8192* as illustrated in the above examples is useful when creating large images to avoid image truncation.
 * [Graphviz](https://www.graphviz.org/) is a prerequisite

### Issues
If you find any problems please open an [issue](https://github.com/melahn/helm-chartmap/issues).

### License
Apache 2.0