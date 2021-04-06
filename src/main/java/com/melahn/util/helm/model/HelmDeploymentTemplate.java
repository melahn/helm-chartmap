package com.melahn.util.helm.model;

public class HelmDeploymentTemplate {

    // fileName is not part of the model of a deployment template used in the Helm Chart.
    // Rather it is used to find a template that may be used in a parent Helm Chart and thus should
    // supercede the template used in this Helm Chart
    private String fileName; // fileName is not part of model ... used to find a superceding Deployment Template if one exists

    public String getFileName() {return fileName;}

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    private String apiVersion;
    private String kind;
    private HelmDeploymentSpec spec;

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion=apiVersion;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind=kind;
    }

    public HelmDeploymentSpec getSpec() {return spec;}

    public void setSpec(HelmDeploymentSpec spec) { this.spec = spec;}
}
