/*
# Licensed Materials - Property of IBM
# Copyright IBM Corp. 2015  
 */
package com.ibm.streamsx.topology.test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;


import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import org.junit.Before;
import org.junit.BeforeClass;

import com.ibm.streams.operator.version.Product;
import com.ibm.streamsx.topology.TStream;
import com.ibm.streamsx.topology.Topology;
import com.ibm.streamsx.topology.context.ContextProperties;
import com.ibm.streamsx.topology.context.StreamsContext;
import com.ibm.streamsx.topology.context.StreamsContext.Type;
import com.ibm.streamsx.topology.context.StreamsContextFactory;
import com.ibm.streamsx.topology.tester.Condition;
import com.ibm.streamsx.topology.tester.Tester;

/**
 * Root class for topology tests.
 * 
 */
public class TestTopology {

    private static StreamsContext.Type testerType = Type.EMBEDDED_TESTER;

    @BeforeClass
    public static void setTesterType() {
        String testerTypeString = System.getProperty("topology.test.type");

        if (testerTypeString != null) {
            testerType = StreamsContext.Type.valueOf(testerTypeString);
        }
    }
    
    private static File TEST_ROOT;
    
    @BeforeClass
    public static void setTesterRoot() {
        String testerRoot = System.getProperty("topology.test.root");
        
        if (testerRoot != null) {

            TEST_ROOT = new File(testerRoot);

            assertTrue(TEST_ROOT.getPath(), TEST_ROOT.isAbsolute());
            assertTrue(TEST_ROOT.getPath(), TEST_ROOT.exists());
        }
    }
    
    public static File getTestRoot() {
        return TEST_ROOT;
    }
    
    private final Map<String,Object> config = new HashMap<>();
    
    @Before
    public void setupConfig() {
        
        
        List<String> vmArgs = new ArrayList<>();
        config.put(ContextProperties.VMARGS, vmArgs);
        
        if (getTesterType() != Type.EMBEDDED_TESTER) {
           
            File agentJar = new File(System.getProperty("user.home"), ".ant/lib/jacocoagent.jar");
            if (agentJar.exists()) {
                String now = Long.toHexString(System.currentTimeMillis());
                String destFile = "jacoco_" + getTesterType().name() + now + ".exec";
     
                
                String arg = "-javaagent:"
                        + agentJar.getAbsolutePath()
                        + "=destfile="
                        + destFile;
                vmArgs.add(arg);
            }
        }
        // Look for a different compiler
        String differentCompile = System.getProperty(ContextProperties.COMPILE_INSTALL_DIR);
        if (differentCompile != null) {
            config.put(ContextProperties.COMPILE_INSTALL_DIR, differentCompile);
            Topology.STREAMS_LOGGER.setLevel(Level.INFO);
        }
    }
    

    private static final AtomicInteger topoCounter = new AtomicInteger();
    private static final String baseName = UUID.randomUUID().toString().replace('-', '_');
    
    /**
     * Create a new topology with a unique name.
     */
    protected static Topology newTopology() {
        Topology t = new Topology();
        return newTopology(t.getName());
    }
    
    /**
     * Create a new topology with a unique name based upon the passed in name.
     */
    protected static Topology newTopology(String name) {
        return new Topology(name + "_" + topoCounter.getAndIncrement() + "_" + baseName);
    }   

    /**
     * Get the default tester type.
     * 
     * @return
     */
    public StreamsContext.Type getTesterType() {
        return testerType;
    }

    public boolean isEmbedded() {
        return getTesterType() == Type.EMBEDDED_TESTER;
    }

    /**
     * The main run of tests will be with EMBEDDED_TESTER This allows tests to
     * be only run once, with the main run.
     */
    public boolean isMainRun() {
        return getTesterType() == Type.EMBEDDED_TESTER;
    }
    
    public Map<String,Object> getConfig() {
        return config;
    }

    /**
     * Return the default context for tests. Using this allows the tests to be
     * run against different contexts, assuming the results/asserts are expected
     * to be the same.
     */
    public StreamsContext<?> getTesterContext() {
        return StreamsContextFactory.getStreamsContext(getTesterType());
    }

    public void complete(Tester tester) throws Exception {
        tester.complete(getTesterContext());
    }
    
    /**
     * Test a topology that may run forever.
     * If endCondition is null then:
     *   
     * In a distributed environment the 
     */
    public boolean complete(Tester tester, Condition<?> endCondition, long timeout, TimeUnit unit) throws Exception {
        
        return tester.complete(getTesterContext(), getConfig(), endCondition, timeout, unit);
    }

    /**
     * Once Junit has been upgraded, by default any sub-class with tests will
     * only be run by the EMBEDDED_TESTER by default. A sub-class overrides this
     * to run in multiple modes (to be tested to see if this idea will actually
     * work!).
     */
    @Before
    public void runOnce() {
        // assumeTrue(isMainRun());
    }

    /**
     * Assume check field for compiling bundles with sc
     */
    public static final boolean SC_OK = Boolean
            .getBoolean("topology.test.sc_ok");

    /**
     * Assume check field for performance tests.
     */
    public static final boolean PERF_OK = Boolean
            .getBoolean("topology.test.perf_ok");
    
    protected void assumeSPLOk() {       
        assumeTrue(getTesterType() != StreamsContext.Type.EMBEDDED_TESTER);
        assumeTrue(SC_OK);
    }
        
    /**
     * Only run a test at a specific minimum version or higher.
     */
    protected void checkMinimumVersion(String reason, int ...vrmf) {
        switch (vrmf.length) {
        case 4:
            assumeTrue(Product.getVersion().getFix() >= vrmf[3]);
        case 3:
            assumeTrue(Product.getVersion().getMod() >= vrmf[2]);
        case 2:
            assumeTrue(Product.getVersion().getRelease() >= vrmf[1]);
        case 1:
            assumeTrue(Product.getVersion().getVersion() >= vrmf[0]);
            break;
        default:
            fail("Invalid version supplied!");
        }    }
    
    /**
     * Allow a test to be skipped for a specific version.
     */
    protected void skipVersion(String reason, int ...vrmf) {
        
        switch (vrmf.length) {
        case 4:
            assumeTrue(Product.getVersion().getFix() != vrmf[3]);
        case 3:
            assumeTrue(Product.getVersion().getMod() != vrmf[2]);
        case 2:
            assumeTrue(Product.getVersion().getRelease() != vrmf[1]);
        case 1:
            assumeTrue(Product.getVersion().getVersion() != vrmf[0]);
            break;
        default:
            fail("Invalid version supplied!");
        }
    }
    
    public void completeAndValidate(TStream<?> output, int seconds, String...contents) throws Exception {
        completeAndValidate(getConfig(), output, seconds, contents);
    }
    
    public void completeAndValidate(Map<String,Object> config,
            TStream<?> output, int seconds, String...contents) throws Exception {
        
        Tester tester = output.topology().getTester();
        
        Condition<List<String>> expectedContents = tester.completeAndTestStringOutput(
                getTesterContext(),
                config,
                output,
                seconds, TimeUnit.SECONDS,
                contents);

        assertTrue(expectedContents.toString(), expectedContents.valid());
    }
    
    /**
     * Return a condition that is true if all conditions are valid.
     * The result is a Boolean that indicates if the condition is valid.
     * @param conditions
     * @return
     */
    public static Condition<Boolean> allConditions(final Condition<?> ...conditions) {
        return new Condition<Boolean>() {

            @Override
            public boolean valid() {
                for (Condition<?> condition : conditions) {
                    if (!condition.valid())
                        return false;
                }
                return true;
            }

            @Override
            public Boolean getResult() {
                return valid();
            }};
    }
}