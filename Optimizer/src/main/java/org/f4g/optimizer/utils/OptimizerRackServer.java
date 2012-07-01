/**
* ============================== Header ============================== 
* file:          Server.java
* project:       FIT4Green/Optimizer
* created:       26 nov. 2010 by cdupont
* last modified: $LastChangedDate: 2011-10-21 14:40:57 +0200 (vie, 21 oct 2011) $ by $LastChangedBy: f4g.cnit $
* revision:      $LastChangedRevision: 923 $
* 
* short description:
*   optimizer's type to represent a rackable server. 
*   
* ============================= /Header ==============================
*/

package org.f4g.optimizer.utils;


import java.util.List;

import org.apache.log4j.Logger;
import org.f4g.com.util.PowerData;
import org.f4g.optimizer.CloudTraditional.OptimizerEngineCloudTraditional.AlgoType;
import org.f4g.power.IPowerCalculator;
import org.f4g.schema.constraints.optimizerconstraints.VMTypeType;
import org.f4g.schema.metamodel.*;
import org.jvnet.jaxb2_commons.lang.CopyStrategy;
import org.jvnet.jaxb2_commons.lang.JAXBCopyStrategy;
import org.jvnet.jaxb2_commons.locator.ObjectLocator;

import org.f4g.optimizer.utils.OptimizerServer.CandidateState;
import org.f4g.optimizer.utils.OptimizerServer.CreationImpossible;

/** 
 *  This class is the internal representation of a rackable server used by the optimizer.
 */	
public class OptimizerRackServer extends RackableServerType implements IOptimizerServer {
		
	public Logger log;  	
	OptimizerServer optimizerServer;
	
	public void InitOptimizerRackServer(RackableServerType modelServer){
		log = Logger.getLogger(this.getClass().getName());		
		getPSU().addAll(modelServer.getPSU());
		getFan().addAll(modelServer.getFan());
		getWaterCooler().addAll(modelServer.getWaterCooler());
	}
	/**
	 * Server constructor for Cloud
	 */
	public OptimizerRackServer(RackableServerType modelServer, VMTypeType myVMTypes) throws CreationImpossible{
		
		InitOptimizerRackServer(modelServer);
		optimizerServer = new OptimizerServer(modelServer, myVMTypes, (ServerType)this);
		
	}
	
	/**
	 * Server constructor for traditional
	 * if the first parameter is not null, it take these workloads. Otherwise it gets them from the model server.
	 */
	public OptimizerRackServer(List<OptimizerWorkload> WLs, RackableServerType modelServer) throws CreationImpossible{

		InitOptimizerRackServer(modelServer);	
		optimizerServer = new OptimizerServer(WLs, modelServer, (ServerType)this);
	}
	
	/**
	 * Default constructor
	 */
	public OptimizerRackServer() {}
	
	//Forward calls to OptimizerServer.
	@Override public List<OptimizerWorkload> getWorkloads()                                   { return optimizerServer.getWorkloads(     );  }
	@Override public void                    setWorkloads(List<OptimizerWorkload> workloads)  {        optimizerServer.setWorkloads(     workloads);}
	@Override public int                     getNbCores()                                     { return optimizerServer.getNbCores(       this);}
	@Override public int                     getNbCPU()                                       { return optimizerServer.getNbCPU(         this);  }	
	@Override public long                    getMemory()                                      { return optimizerServer.getMemory(        this); }	
	@Override public double                  getStorage()   	                              { return optimizerServer.getStorage(       this);}
	@Override public double                  getNICBandwidth() 	                              { return optimizerServer.getNICBandwidth(  this);}
	@Override public PowerData               getPower(IPowerCalculator powerCalculator)       { return optimizerServer.getPower(         powerCalculator, this);}		
	@Override public void                    setCandidateState(CandidateState candState)      {        optimizerServer.setCandidateState(candState);}
	@Override public String                  getCandidateState()                              { return optimizerServer.getCandidateState();}
	@Override public double                  getLoadRate(AggregatedUsage ref, AlgoType at)    { return optimizerServer.getLoadRate(      ref, at);}
	@Override public void                    addVM(OptimizerWorkload WL, AlgoType at)         {        optimizerServer.addVM(            WL, at, this);}
	@Override public ServerStatusType        getServerStatus()                                { return this           .getStatus();}
	@Override public void                    setServerStatus(ServerStatusType value)          {        this           .setStatus(        value);}
	@Override public List<MainboardType>     getServerMainboard()                             { return this           .getMainboard();}
//	          public double                  getNICMaxPower()                                 { return optimizerServer.getNICMaxPower(   this);}
//              public double                  getNICIdlePower()                                {	return optimizerServer.getNICIdlePower(this);}
//              public double                  getIdlePower()                                   {	return optimizerServer.getIdlePower(this);}
//              public int                     getMaxPower()                                    {	return optimizerServer.getMaxPower(this);}
	
    @Override
    public Object clone() {
        return copyTo(createNewInstance());
    }

	@Override
    public Object copyTo(Object target) {
        return copyTo(null, target, JAXBCopyStrategy.INSTANCE);
    }

	@Override
    public Object copyTo(ObjectLocator locator, Object target, CopyStrategy strategy) {
        final Object draftCopy = ((target == null)?createNewInstance():target);
        super.copyTo(locator, draftCopy, strategy);
        if (draftCopy instanceof OptimizerRackServer) {
        	final OptimizerRackServer copy = ((OptimizerRackServer) draftCopy);
        	           
            copy.log = this.log;
            copy.optimizerServer = (OptimizerServer)optimizerServer.clone();
    		
        }
        return draftCopy;
    }

	@Override
    public Object createNewInstance() {
        return new OptimizerRackServer();
    }


}
