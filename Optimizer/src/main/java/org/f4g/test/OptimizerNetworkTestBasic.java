package org.f4g.test;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.f4g.power.IPowerCalculator;
import org.f4g.schema.constraints.optimizerconstraints.VMTypeType;
import org.f4g.util.LoadCalculator;
import org.f4g.cost_estimator.NetworkCost;
import static javax.measure.units.NonSI.*;
import static javax.measure.units.SI.*;
import org.jscience.physics.measures.Measure;
import org.jscience.economics.money.*;
import org.jscience.economics.money.Currency;
import javax.measure.quantities.*;

import org.f4g.schema.metamodel.FIT4GreenType;
import org.f4g.schema.metamodel.ServerType;
import org.f4g.schema.metamodel.NetworkNodeType;
import org.f4g.schema.metamodel.NetworkPortType;
import org.f4g.schema.metamodel.VirtualMachineType;
import org.f4g.schema.metamodel.SiteType;
import org.f4g.optimizer.ICostEstimator;
import org.f4g.power.IPowerCalculator;
import org.f4g.power.PoweredNetworkNode;
import org.f4g.optimizer.OptimizationObjective;
import org.f4g.optimizer.utils.Utils;
import org.f4g.schema.metamodel.NetworkPortBufferSizeType;
import org.f4g.schema.metamodel.BitErrorRateType;
import org.f4g.schema.metamodel.PropagationDelayType;
import org.f4g.schema.metamodel.NetworkNodeStatusType;
import org.f4g.schema.metamodel.NetworkTrafficType;
import org.f4g.schema.metamodel.LinkType;
import org.f4g.schema.metamodel.FlowType;
import org.f4g.schema.metamodel.PowerType;
import org.f4g.schema.metamodel.MemoryUsageType;
import org.f4g.schema.metamodel.StorageUsageType;
import org.f4g.schema.metamodel.MainboardType;
import org.f4g.schema.metamodel.NICType;
import org.f4g.schema.metamodel.ServerStatusType;


public class OptimizerNetworkTestBasic extends TestCase {
    
	private Logger log; 
    FIT4GreenType model;
    List<ServerType> allServers;
    List<NetworkNodeType> allSwitches;
        
    
    public OptimizerNetworkTestBasic(String name) { 
        super(name);
        log = Logger.getLogger(OptimizerNetworkTestBasic.class);
    }
    
	protected void setUp() throws Exception {
		super.setUp();

		ModelGenerator modelGenerator = new ModelGenerator();

		// Servers' settings
		modelGenerator.setNB_SERVERS(4); 
		modelGenerator.setCPU(1);
		modelGenerator.setCORE(2); //2 cores
		modelGenerator.setRAM_SIZE(1);
    
        // VMS' settings
		modelGenerator.setNB_VIRTUAL_MACHINES(0);

        // Network settings
        modelGenerator.setNB_SWITCHES(3);
        modelGenerator.setNB_ROUTERS(0);
        ModelGenerator.defaultSwitchPowerIdle = new PowerType( 100.0 );
        ModelGenerator.defaultSwitchPowerMax = new PowerType( 100.0 );
       
        // Populate model
        model = modelGenerator.createPopulatedFIT4GreenType();
        allServers = Utils.getAllServers(model);
        allSwitches = Utils.getAllNetworkDeviceNodes(model.getSite().get(0));

        // Create Topology:
        //            server0 ---\
        //                         switch1 ---\
        //            server1 ---/             \
        //                                       switch0
        //            server2 ---\             /
        //                         switch2 ---/
        //            server3 ---/             

        ModelGenerator.connectNetDevsFullDuplex(allSwitches.get(0), allSwitches.get(1));
        ModelGenerator.connectNetDevsFullDuplex(allSwitches.get(0), allSwitches.get(2));
        ModelGenerator.connectServerToNetDevFullDuplex(allServers.get(0), allSwitches.get(1));
        ModelGenerator.connectServerToNetDevFullDuplex(allServers.get(1), allSwitches.get(1));
        ModelGenerator.connectServerToNetDevFullDuplex(allServers.get(2), allSwitches.get(2));
        ModelGenerator.connectServerToNetDevFullDuplex(allServers.get(3), allSwitches.get(2));
	}
    
	protected void tearDown() throws Exception {
		super.tearDown();
	}
        
    
    public void testIds() {

        // Start tests
        assertEquals(4 ,  allServers.size());   
        assertEquals(3 ,  allSwitches.size());   

        assertEquals("id100000",  allServers.get(0).getFrameworkID());      // server0
        assertEquals("id200000",  allServers.get(1).getFrameworkID());      // server1
        assertEquals("id300000",  allServers.get(2).getFrameworkID());      // server2
        assertEquals("id400000",  allServers.get(3).getFrameworkID());      // server3
        assertEquals("id1000000",  allSwitches.get(0).getFrameworkID());     // switch0
        assertEquals("id2000000",  allSwitches.get(1).getFrameworkID());     // switch1
        assertEquals("id3000000",  allSwitches.get(2).getFrameworkID());     // switch2
    }
        
    public void testSwitchConnections() {

        // verify who is connected to switch0 
        assertEquals(2, allSwitches.get(0).getNetworkPort().size());

        String n0 = (String) allSwitches.get(0).getNetworkPort().get(0).getNetworkPortRef();    // node connected to switch0's port 0
        String n1 = (String) allSwitches.get(0).getNetworkPort().get(1).getNetworkPortRef();    // node connected to switch0's port 1
        assertEquals("id2000000", n0);        // should be switch1
        assertEquals("id3000000", n1);        // should be switch2

        // verify who is connected to switch1
        assertEquals(3, allSwitches.get(1).getNetworkPort().size());
        String n2 = (String) allSwitches.get(1).getNetworkPort().get(0).getNetworkPortRef();    // node connected to switch1's port 0
        String n3 = (String) allSwitches.get(1).getNetworkPort().get(1).getNetworkPortRef();    // node connected to switch1's port 1
        String n4 = (String) allSwitches.get(1).getNetworkPort().get(2).getNetworkPortRef();    // node connected to switch1's port 2
        assertEquals("id1000000", n2);        // should be switch0
        assertEquals("id100000", n3);        // should be server0
        assertEquals("id200000", n4);   // should be server1
        
        // verify who is connected to switch2
        assertEquals(3, allSwitches.get(2).getNetworkPort().size());
        String n5 = (String) allSwitches.get(2).getNetworkPort().get(0).getNetworkPortRef();    // node connected to switch2's port 0
        String n6 = (String) allSwitches.get(2).getNetworkPort().get(1).getNetworkPortRef();    // node connected to switch2's port 1
        String n7 = (String) allSwitches.get(2).getNetworkPort().get(2).getNetworkPortRef();    // node connected to switch2's port 2
        assertEquals("id1000000", n5);        // should be connected to switch0
        assertEquals("id300000", n6);   // should be connected to server2
        assertEquals("id400000", n7);   // should be connected to server3
         
    }

    public void testSwitchPower() {

        // test power
        double power_switch0 = (new PoweredNetworkNode(allSwitches.get(0))).computePower();
        double power_switch1 = (new PoweredNetworkNode(allSwitches.get(1))).computePower();
        double power_switch2 = (new PoweredNetworkNode(allSwitches.get(2))).computePower();    
                 
        assertTrue( power_switch0 >= 100.0 );
        assertTrue( power_switch1 >= 100.0 );
        assertTrue( power_switch2 >= 100.0 );
        assertTrue( power_switch0 < 100.1 );    // due to loss of precision
        assertTrue( power_switch1 < 100.1 );
        assertTrue( power_switch2 < 100.1 );
        
        // switch off server0
        allServers.get(0).setStatus( ServerStatusType.OFF );
        power_switch0 = new PoweredNetworkNode(allSwitches.get(0)).computePower();
        power_switch1 = new PoweredNetworkNode(allSwitches.get(1)).computePower();
        power_switch2 = new PoweredNetworkNode(allSwitches.get(2)).computePower();
        assertTrue( power_switch0 >= 100.0 );
        assertTrue( power_switch0  < 100.1 );    // due to loss of precision
        assertTrue( power_switch1 >= 100.0 );
        assertTrue( power_switch1  < 100.1 );
        assertTrue( power_switch2 >= 100.0 );
        assertTrue( power_switch2  < 100.1 );

        // switch off server1
        allServers.get(1).setStatus( ServerStatusType.OFF );
        power_switch0 = new PoweredNetworkNode(allSwitches.get(0)).computePower();
        power_switch1 = new PoweredNetworkNode(allSwitches.get(1)).computePower();
        power_switch2 = new PoweredNetworkNode(allSwitches.get(2)).computePower();
        assertTrue( power_switch0 >= 100.0 );
        assertTrue( power_switch0  < 100.1 );    // due to loss of precision
        assertTrue( power_switch1 >= 100.0 );
        assertTrue( power_switch1  < 100.1 );
        assertTrue( power_switch2 >= 100.0 );
        assertTrue( power_switch2  < 100.1 );
        
        // switch off server2
        allServers.get(2).setStatus( ServerStatusType.OFF );
        power_switch0 = new PoweredNetworkNode(allSwitches.get(0)).computePower();
        power_switch1 = new PoweredNetworkNode(allSwitches.get(1)).computePower();
        power_switch2 = new PoweredNetworkNode(allSwitches.get(2)).computePower();
        assertTrue( power_switch0 >= 100.0 );
        assertTrue( power_switch0  < 100.1 );    // due to loss of precision
        assertTrue( power_switch1 >= 100.0 );
        assertTrue( power_switch1  < 100.1 );
        assertTrue( power_switch2 >= 100.0 );
        assertTrue( power_switch2  < 100.1 );

        // switch off server3
        allServers.get(3).setStatus( ServerStatusType.OFF );        
        power_switch0 = new PoweredNetworkNode(allSwitches.get(0)).computePower();
        power_switch1 = new PoweredNetworkNode(allSwitches.get(1)).computePower();
        power_switch2 = new PoweredNetworkNode(allSwitches.get(2)).computePower();
        assertTrue( power_switch0 >= 100.0 );
        assertTrue( power_switch0  < 100.1 );    // due to loss of precision
        assertTrue( power_switch1 >= 100.0 );
        assertTrue( power_switch1  < 100.1 );
        assertTrue( power_switch2 >= 100.0 );
        assertTrue( power_switch2  < 100.1 );

        // switch off switch1
        allSwitches.get(1).setStatus( NetworkNodeStatusType.OFF );
        power_switch0 = new PoweredNetworkNode(allSwitches.get(0)).computePower();
        power_switch1 = new PoweredNetworkNode(allSwitches.get(1)).computePower();
        power_switch2 = new PoweredNetworkNode(allSwitches.get(2)).computePower();

        // switch on everything
        for(int i=0; i<allServers.size(); ++i) 
                allServers.get(i).setStatus( ServerStatusType.ON );        
        for(int i=0; i<allSwitches.size(); ++i) 
                allSwitches.get(i).setStatus( NetworkNodeStatusType.ON );
	}
    
    
}


