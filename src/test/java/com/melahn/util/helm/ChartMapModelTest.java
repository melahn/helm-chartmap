package com.melahn.util.helm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import com.melahn.util.helm.model.HelmChart;
import com.melahn.util.helm.model.HelmChartLocalCache;
import com.melahn.util.helm.model.HelmChartRepo;
import com.melahn.util.helm.model.HelmChartRepoLocal;
import com.melahn.util.helm.model.HelmChartReposLocal;
import com.melahn.util.helm.model.HelmDeploymentContainer;
import com.melahn.util.helm.model.HelmDeploymentSpec;
import com.melahn.util.helm.model.HelmDeploymentSpecTemplate;
import com.melahn.util.helm.model.HelmDeploymentSpecTemplateSpec;
import com.melahn.util.helm.model.HelmDeploymentTemplate;
import com.melahn.util.helm.model.HelmMaintainer;
import com.melahn.util.helm.model.HelmRequirement;

import org.junit.jupiter.api.Test;

class ChartMapModelTest {

    static final String EXPECTED = generateRandomString(10);
    static final String NOTEXPECTED = EXPECTED.concat(EXPECTED);
    
    @Test
    void testHelmChart() {
        HelmChart hc = new HelmChart();
        hc.setApiVersion(EXPECTED);
        assertEquals(EXPECTED, hc.getApiVersion());
        hc.setAppVersion(EXPECTED);
        assertEquals(EXPECTED, hc.getAppVersion());
        hc.setCondition(EXPECTED);
        assertFalse(hc.getCondition());
        hc.setCondition(EXPECTED.concat(".enabled"));
        assertTrue(hc.getCondition().booleanValue());
        hc.setCreated(EXPECTED);
        assertEquals(EXPECTED, hc.getCreated());
        HashSet<HelmChart> d = new HashSet<HelmChart>();
        hc.setDependencies(d);
        assertSame(d, hc.getDependencies());
        hc.setDiscoveredDependencies(d);
        assertSame(d, hc.getDiscoveredDependencies());
        HashSet<HelmDeploymentTemplate> t = new HashSet<HelmDeploymentTemplate>();
        HelmDeploymentTemplate hdt = new HelmDeploymentTemplate();
        HelmDeploymentSpec hds = new HelmDeploymentSpec();
        hdt.setSpec(hds);
        HelmDeploymentSpecTemplate hdst = new HelmDeploymentSpecTemplate();
        hds.setTemplate(hdst);
        HelmDeploymentSpecTemplateSpec hdsts = new HelmDeploymentSpecTemplateSpec();
        hdst.setSpec(hdsts);
        HelmDeploymentContainer hdc = new HelmDeploymentContainer();
        hdc.setImage(EXPECTED);
        hdc.setParent(hc);
        HelmDeploymentContainer[] c1 = new HelmDeploymentContainer[1];
        c1[0] = hdc;
        hdsts.setContainers(c1);
        t.add(hdt);
        hc.setDeploymentTemplates(t);
        HashSet<HelmDeploymentContainer> cs = new HashSet<HelmDeploymentContainer>();
        cs.add(hdc);
        assertEquals(cs, hc.getContainers());
        assertSame(t, hc.getDeploymentTemplates());
        HelmDeploymentContainer[] c0 = new HelmDeploymentContainer[0];
        hdsts.setContainers(c0);
        assertTrue(hc.getContainers().isEmpty());
        hdsts.setContainers(null);
        assertTrue(hc.getContainers().isEmpty());
        hc.setDescription(EXPECTED);
        assertEquals(EXPECTED, hc.getDescription());
        hc.setDigest(EXPECTED);
        assertEquals(EXPECTED, hc.getDigest());
        hc.setIcon(EXPECTED);
        assertEquals(EXPECTED, hc.getIcon());
        String[] k = new String[1];
        hc.setKeywords(k);
        assertSame(k, hc.getKeywords());
        HelmMaintainer[] m = new HelmMaintainer[1];
        hc.setMaintainers(m);
        assertSame(m, hc.getMaintainers());
        hc.setName(EXPECTED);
        assertEquals(EXPECTED, hc.getName());
        hc.setVersion(EXPECTED);
        assertEquals(EXPECTED, hc.getVersion());
        assertEquals(EXPECTED.concat(":").concat(EXPECTED), hc.getNameFull());
        hc.setRepoUrl(EXPECTED);
        assertEquals(EXPECTED, hc.getRepoUrl());
        hdsts.setContainers(null);
        HashMap<String, Object> v = new HashMap<String, Object>();
        String k1 = EXPECTED.concat("key");
        String v1 = EXPECTED.concat("value");
        v.put(k1,v1);
        hc.setValues(v);
        assertSame(v, hc.getValues());
        assertEquals(v1, hc.getValue(k1));
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }


    @Test
    void testHelmChartLocalCache() {
        HelmChartLocalCache hclc = new HelmChartLocalCache();
        hclc.setApiVersion(EXPECTED);
        assertEquals(EXPECTED, hclc.getApiVersion());
        HashMap<String,HelmChart[]> e = new HashMap<String,HelmChart[]>();
        hclc.setEntries(e);
        assertSame(e, hclc.getEntries());
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    @Test
    void testHelmChartRepo() {
        HelmChartRepo hcr = new HelmChartRepo();
        hcr.setApiVersion(EXPECTED);
        assertEquals(EXPECTED, hcr.getApiVersion());
        hcr.setGenerated(EXPECTED);
        assertEquals(EXPECTED, hcr.getGenerated());
        HashMap<String,HelmChart[]> e = new HashMap<String,HelmChart[]>();
        hcr.setEntries(e);
        assertSame(e, hcr.getEntries());
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    @Test
    void testHelmChartReposLocal() {
        HelmChartReposLocal hcrl = new HelmChartReposLocal();
        hcrl.setApiVersion(EXPECTED);
        assertEquals(EXPECTED, hcrl.getApiVersion());
        hcrl.setGenerated(EXPECTED);
        assertEquals(EXPECTED, hcrl.getGenerated()); 
        HelmChartRepoLocal[] r = new HelmChartRepoLocal[1]; 
        hcrl.setRepos(r);
        assertSame(r, hcrl.getRepositories());     
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    @Test
    void testHelmDeploymentContainer() {
        HelmDeploymentContainer hdc = new HelmDeploymentContainer();
        hdc.setImage(EXPECTED);
        assertEquals(EXPECTED, hdc.getImage());
        hdc.setImagePullPolicy(EXPECTED);
        assertEquals(EXPECTED, hdc.getImagePullPolicy());
        hdc.setName(EXPECTED);
        assertEquals(EXPECTED, hdc.getName());
        HelmDeploymentContainer hdc2 = new HelmDeploymentContainer();
        hdc2.setImage(EXPECTED);
        hdc2.setImagePullPolicy(EXPECTED);
        hdc2.setName(EXPECTED);
        boolean e = hdc.equals(hdc2);
        assertTrue(e);
        e = hdc.equals(hdc);
        assertTrue(e);
        hdc2.setImage(NOTEXPECTED);
        e = hdc.equals(hdc2);
        assertFalse(e);
        hdc2.setImage(EXPECTED);
        hdc2.setImagePullPolicy(NOTEXPECTED);
        e = hdc.equals(hdc2);
        assertFalse(e);
        hdc2.setImagePullPolicy(EXPECTED);
        hdc2.setName(NOTEXPECTED);
        e = hdc.equals(hdc2);
        assertFalse(e);
        hdc.equals(null);
        e = hdc.equals(hdc2);
        assertFalse(e);
        e = hdc.equals(new Object());
        assertFalse(e);
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    @Test
    void testHelmDeploymentSpec() {
        HelmDeploymentSpec hds = new HelmDeploymentSpec();
        hds.setReplicas(EXPECTED);
        assertSame(EXPECTED, hds.getReplicas());
        HelmDeploymentSpecTemplate hdst = new HelmDeploymentSpecTemplate();
        hds.setTemplate(hdst);
        assertSame(hdst, hds.getTemplate());
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    @Test
    void testHelmDeploymentSpecTemplateSpec() {
        HelmDeploymentSpecTemplateSpec hdsts = new HelmDeploymentSpecTemplateSpec();
        HelmDeploymentContainer[] c = new HelmDeploymentContainer[1];
        hdsts.setContainers(c);
        assertSame(c, hdsts.getContainers());
        hdsts.setHostNetwork(EXPECTED);
        assertEquals(EXPECTED, hdsts.getHostNetwork());
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    @Test
    void testHelmDeploymentTemplate() {
        HelmDeploymentTemplate hdt = new HelmDeploymentTemplate();
        hdt.setApiVersion(EXPECTED);
        assertEquals(EXPECTED, hdt.getApiVersion());
        hdt.setFileName(EXPECTED);
        assertEquals(EXPECTED, hdt.getFileName());
        hdt.setKind(EXPECTED);
        assertEquals(EXPECTED, hdt.getKind());
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    @Test
    void testHelmRequirement() {
        HelmRequirement hr = new HelmRequirement();
        hr.setCondition(EXPECTED);
        assertEquals(EXPECTED, hr.getCondition());
        hr.setName(EXPECTED);
        assertEquals(EXPECTED, hr.getName());
        hr.setRepository(EXPECTED);
        assertEquals(EXPECTED, hr.getRepository());
        hr.setVersion(EXPECTED);
        assertEquals(EXPECTED, hr.getVersion());
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }
        
    /**
     * Returns a randomly generated string. 
     * @param c the number of characters in the string you want. If
     *          invalid (less than or equal to 0) 10 is used
     * @return the generated String
     */
    private static String generateRandomString(int c) {
        byte[] array = new byte[c <= 0 ? 10 : c];
        new Random().nextBytes(array);
        return new String(array, Charset.forName("UTF-8"));
    }

}
