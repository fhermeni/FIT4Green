package org.f4g.power;

import org.f4g.core.IMain;
import java.util.*;
import org.f4g.schema.metamodel.MainboardType;
import java.util.Iterator;
import org.f4g.schema.metamodel.TowerServerType;
import org.f4g.schema.metamodel.BladeServerType;
import org.f4g.schema.metamodel.RackableServerType;
import org.apache.commons.jxpath.JXPathContext;
import org.apache.log4j.Logger;
import org.f4g.com.util.PowerData;
import org.f4g.schema.metamodel.FIT4GreenType;
import org.f4g.schema.metamodel.CPUType;
import org.f4g.schema.metamodel.RAMStickType;
import org.f4g.power.PoweredRamStick;
import org.f4g.power.PoweredFan;
import org.f4g.power.PoweredCore;
import org.f4g.power.PoweredCPU;
import org.f4g.power.PoweredNetworkNode;
import org.f4g.power.PoweredHardDiskDrive;
import org.f4g.power.PoweredNAS;
import org.f4g.schema.metamodel.PowerType;
import org.f4g.schema.metamodel.CPUArchitectureType;
import org.f4g.schema.metamodel.NetworkPortType;
import org.f4g.schema.metamodel.OperatingSystemTypeType;
import org.f4g.schema.metamodel.ServerStatusType;
import org.f4g.schema.metamodel.StorageUnitType;
import org.f4g.schema.metamodel.NICType;
import org.f4g.schema.metamodel.HardwareRAIDType;
import org.f4g.schema.metamodel.SANType;
import org.f4g.schema.metamodel.FanType;
import org.f4g.schema.metamodel.IoRateType;
import org.f4g.schema.metamodel.WaterCoolerType;
import org.f4g.schema.metamodel.PSUType;
import org.f4g.schema.metamodel.RAIDType;
import org.f4g.schema.metamodel.CacheType;
import org.f4g.schema.metamodel.HardDiskType;
import org.f4g.schema.metamodel.SolidStateDiskType;
import org.f4g.schema.metamodel.EnclosureType;
import org.f4g.schema.metamodel.RackType;
import org.f4g.schema.metamodel.PDUType;
import org.f4g.schema.metamodel.DatacenterType;
import org.f4g.schema.metamodel.SiteType;
import org.f4g.schema.metamodel.CoreType;
import org.f4g.schema.metamodel.NativeOperatingSystemType;
import org.f4g.schema.metamodel.ServerType;
import org.f4g.schema.metamodel.BoxRouterType;
import org.f4g.schema.metamodel.BoxSwitchType;
import org.f4g.schema.metamodel.RackableRouterType;
import org.f4g.schema.metamodel.RackableSwitchType;
import org.f4g.schema.metamodel.NetworkNodeType;
import org.f4g.schema.metamodel.MemoryUsageType;
import org.f4g.schema.metamodel.NrOfTransistorsType;
import org.f4g.schema.metamodel.NASType;
import org.f4g.schema.metamodel.ControllerType;
import org.f4g.power.IPowerCalculator;

//Repository copy
/**
 * Class implementing the f4g Power Calculator component
 * 
 * @author basmadji
 *
 */
public class PowerCalculator implements IPowerCalculator {
	
	static Logger log; 
	IMain main = null;
	private boolean simulationFlag = false;

	public PowerCalculator(IMain main) {
		this.main = main;
		log = Logger.getLogger(PowerCalculator.class.getName()); 
	}
	
	public PowerCalculator() {
		log = Logger.getLogger(PowerCalculator.class.getName()); 
	}
	
	public void setSimulationFlag(boolean f){	simulationFlag = f; }
	public boolean getSimulationFlag(){	return simulationFlag; }

	/**
	 * 
	 * Computes the current power consumption of the FIT4Green system 
	 * 
	 * @param model
	 * @return a data structure containing the power consumption in Watts of the FIT4Green system 
	 */
	@Override
	public PowerData computePowerFIT4Green(FIT4GreenType model) {
	
		if (model == null){
			log.debug("FIT4GreeType model is null");
			PowerData powerData = new PowerData();
			powerData.setActualConsumption(0.0);
			return  powerData;
		}
			
		PowerType totalF4GPower = new PowerType();
		totalF4GPower.setValue(0.0);
		JXPathContext context = JXPathContext.newContext(model);
		String F4GQuery = "/";
		Iterator F4GPowerIterator = context.iterate(F4GQuery);
		
		if(F4GPowerIterator.hasNext()){
			FIT4GreenType	myF4G = (FIT4GreenType)F4GPowerIterator.next();
			totalF4GPower = sitePower(myF4G);			
		}		
				
		PowerData powerData = new PowerData();
		powerData.setActualConsumption(totalF4GPower.getValue());

		log.debug("Actual power consumption of the FIT4Green System is: " + powerData.getActualConsumption() + " Watts");

		return powerData; 
	}
	
	/**
	 * 
	 * Computes the current power consumption of a FIT4Green site 
	 * 
	 * @param site
	 * @return a data structure containing the power consumption in Watts of a FIT4Green site 
	 */
	@Override
	public PowerData computePowerSite(SiteType site){
	
		if (site == null){
			log.debug("Site model is null");
			PowerData powerData = new PowerData();
			powerData.setActualConsumption(0.0);
			return  powerData;
		}

		PowerType sitePower=new PowerType();;
		sitePower.setValue(0.0);	
		sitePower = datacenterPower(site);
		site.setComputedPower(sitePower);		
		
		PowerData powerData = new PowerData();
		powerData.setActualConsumption(sitePower.getValue());
		
		log.debug("Actual power consumption of a FIT4Green site is: " + powerData.getActualConsumption() + " Watt/hour");
		return powerData;	
		
	}
	
	/**
	 * 
	 * Computes the current power consumption of a FIT4Green datacenter 
	 * 
	 * @param datacenter
	 * @return a data structure containing the power consumption in Watts of a FIT4Green datacenter 
	 */
	@Override
	public PowerData computePowerDatacenter(DatacenterType datacenter){
		
	double totalRackPower=0.0;
	double totalBoxNetworkPower=0.0;    		
	double totalTowerServerPower=0.0;
	PowerType DCPower = new PowerType();
	DCPower.setValue(0.0);
	
	if (datacenter == null){
		log.debug("Datacenter model is null");
		PowerData powerData = new PowerData();
		powerData.setActualConsumption(0.0);
		return  powerData;
	}
    		
	/**
	 * Power consumption of BoxNetworks
	 */	
	totalBoxNetworkPower = BoxNetworkPower(datacenter);
	DCPower.setValue(DCPower.getValue()+totalBoxNetworkPower);
	log.debug("The power consumption of the Datacenter Box Network devices is "+totalBoxNetworkPower+ " Watts");
	
	/**
	 * Power consumption of Racks
	 */
	totalRackPower = rackPower(datacenter);
	DCPower.setValue(DCPower.getValue()+totalRackPower);	
	log.debug("The power consumption of the Datacenter Racks is "+totalRackPower+ " Watts");
	
	/**
	 * Power consumption of Tower servers
	 */
	totalTowerServerPower = serverPower(datacenter, "towerServer");
	DCPower.setValue(DCPower.getValue()+totalTowerServerPower);
	log.debug("The power consumption of the Datacenter Tower Servers is "+totalTowerServerPower+ " Watts");	
	
	datacenter.setComputedPower(DCPower); 
	
	PowerData powerData = new PowerData();
	powerData.setActualConsumption(DCPower.getValue());
	
	log.debug("Actual power consumption of a FIT4Green datacenter is: " + powerData.getActualConsumption() + " Watt/hour");
	return powerData;	
		
	}//end of computePowerDatacenter method
	
	
	/**
	 * 
	 * Computes the current power consumption of a FIT4Green rack 
	 * 
	 * @param rack
	 * @return a data structure containing the power consumption in Watts of a FIT4Green rack 
	 */
	public PowerData computePowerRack(RackType rack){
		
		PowerType rackPower=new PowerType();
		rackPower.setValue(0.0);
		double totalPDUPower=0.0;
		double totalEnclosurePower=0.0;
		double totalSANPower=0.0;
		double totalNASPower=0.0;
		double totalRackableNetworkPower=0.0;
		double totalCoolingSystemPower=0.0;
		double totalRackableServerPower=0.0;
		
		if (rack == null){
			log.debug("Rack model is null");
			PowerData powerData = new PowerData();
			powerData.setActualConsumption(0.0);
			return  powerData;
		}
		
		/**
		 * Power consumption of power distribution units
		 */
		totalPDUPower = PDUPower(rack);
		rackPower.setValue(rackPower.getValue()+totalPDUPower);
		log.debug("The power consumption of the Rack PDUs is "+totalPDUPower+ " Watts");
		
		/**
		 * Power consumption of enclosures
		 */
		totalEnclosurePower = enclosurePower(rack);
		rackPower.setValue(rackPower.getValue()+totalEnclosurePower);		
		log.debug("The power consumption of the Rack Enclosures is "+totalEnclosurePower+ " Watts");
		
		/**
		 * Power consumption of SAN devices
		 */
		totalSANPower = SANPower(rack, 0);
		rackPower.setValue(rackPower.getValue()+totalSANPower);		
		log.debug("The power consumption of the Rack SAN devices is "+totalSANPower+ " Watts");

		/**
		 * Power consumption of NAS devices
		 */
		totalNASPower = OverallNASPower(rack, 0); //returns the power consumption of all the installed NAS devices
		rackPower.setValue(rackPower.getValue()+totalNASPower);		
		log.debug("The power consumption of the Rack NAS devices is "+totalNASPower+ " Watts");
		
		/**
		 * Power consumption of Rackable Networks
		 */    		
		totalRackableNetworkPower = RackableNetworkPower(rack);
		rackPower.setValue(rackPower.getValue()+totalRackableNetworkPower);		
		log.debug("The power consumption of the Rack Network devices is "+totalRackableNetworkPower+ " Watts");
		
		/**
		 * Power consumption of Cooling systems
		 */
		totalCoolingSystemPower = coolingSystemPower(rack);
		rackPower.setValue(rackPower.getValue()+totalCoolingSystemPower);		
		log.debug("The power consumption of the Rack Cooling Systems is "+totalCoolingSystemPower+ " Watts");
		
		/**
		 * Power consumption of Rackable servers
		 */
		totalRackableServerPower = serverPower(rack, "rackableServer");
		rackPower.setValue(rackPower.getValue()+totalRackableServerPower);		
		log.debug("The power consumption of the Rack Servers is "+totalRackableServerPower+ " Watts");
		
		//Power consumption of the whole rack
		log.debug("The power consumption of the Rack is "+rackPower+ " Watts");	
				
		rack.setComputedPower(rackPower);		
		
		PowerData powerData = new PowerData();
		powerData.setActualConsumption(rackPower.getValue());
		
		log.debug("Actual power consumption of a FIT4Green rack is: " + powerData.getActualConsumption() + " Watt/hour");
		return powerData;		
		
	}
	
	
	
	/**
	 * 
	 * Computes the current power consumption of a FIT4Green server 
	 * 
	 * @param server
	 * @return a data structure containing the power consumption in Watts of a FIT4Green server 
	 */
	@Override
	public PowerData computePowerServer(ServerType server){

        if (server == null){
    		log.debug("Server Type is null");
    		PowerData powerData = new PowerData();
    		powerData.setActualConsumption(0.0);
    		return  powerData;
    	}else if (!simulationFlag && server.getMeasuredPower() != null && server.getMeasuredPower().getValue() > 0){
    		log.debug("Server Measured Power is already given.");
    		PowerData powerData = new PowerData();
    		powerData.setActualConsumption(server.getMeasuredPower().getValue());
    		return  powerData;
    	}

		double totalMainboardPower = 0.0;
		double serverPower = 0.0;		
        double countPSU=0.0;
        boolean measuredPowerPSU = true;
        //boolean loadPSU = false;        
        
        JXPathContext context = JXPathContext.newContext(server);
    	String psuQuery = "PSU";
    	Iterator psuPowerIterator = context.iterate(psuQuery);
    	
		/**
		 * Testing whether all PSUs have measured power provided. We assume that either all of them have the measured power configured or none of them. Also, count the PSUs that have a non zero load 
		 * If count of PSU is zero, then the server has a load of zero
		 *
		 * 
		 */		
    	
    	while(psuPowerIterator.hasNext())
    	{
    		PSUType myPSU = (PSUType)psuPowerIterator.next();
    		if(myPSU.getMeasuredPower() == null ||myPSU.getMeasuredPower().getValue()<=0)
   	    	measuredPowerPSU = false;
    		if(myPSU.getLoad().getValue() > 0.0)
   	    	//loadPSU = true;
    		countPSU++;
    	}
		
		/**
		 * 
		 * If count of PSU is zero, then the server has a load of zero
		 *
		 * 
		 */	
    	if(countPSU==0 && !(server instanceof BladeServerType) )          		
   		serverPower = 0.0;
    	else
    	{
		
		/**
		 * Fetching the operating system
		 */
		OperatingSystemTypeType operatingSystem = getOperatingSystem(server);
		
		/**
		 * Power consumption of the Mainboards
		 */
        totalMainboardPower = mainboardPower(server,operatingSystem);			          
        log.debug("The power consumption of the Mainboards is "+totalMainboardPower+ " Watts");
        serverPower = serverPower + totalMainboardPower; 
        
        /**
         * Power consumption of the cooling system and power supply units for Tower and Rackable Servers
         */
        if(server instanceof TowerServerType || server instanceof RackableServerType){
        
        /**
         * Cooling System part
         */
        double totalCoolingPower = coolingSystemPower(server);            	
        log.debug("The power consumption of the Cooling Systems is "+totalCoolingPower+ " Watts");
        serverPower = serverPower +	totalCoolingPower;
        	
        /**
         * Power supply unit part
         */
        double totalPSUPower=0.0;    	 	
    	context = JXPathContext.newContext(server);
    	psuPowerIterator = context.iterate(psuQuery);
    	
    	while(psuPowerIterator.hasNext())
    	{
    		PowerType psuCurrentPower = new PowerType();
    		psuCurrentPower.setValue(0.0);
    		PSUType myPSU = (PSUType)psuPowerIterator.next();
    		
    		/**
    		 * The monitoring system
    		 * can either provide the measuredPower for every PSU or no information is provided about any PSU inside the same server.
    		 * 
    		 */
    		if(myPSU.getLoad().getValue()>0) //If the PSU has a load of zero, then it can not be considered as a power supplying unit
	    	{		
	    		if(!simulationFlag && measuredPowerPSU)
	    		{
	    			psuCurrentPower.setValue(PSUPower(myPSU.getMeasuredPower().getValue(),myPSU.getEfficiency().getValue()));
	    			myPSU.setComputedPower(psuCurrentPower);
	    			log.debug("The power consumption of the PSU is "+psuCurrentPower.getValue()+ " Watts");
	    			totalPSUPower = totalPSUPower + psuCurrentPower.getValue();	
	    			
	    		}
	    		/**
	    		 * If the measuredPower at the server level is provided by the monitoring system, then this measuredPower is evenly distrbiuted 
	    		 * among all the PSUs inside the same server 
	    		 * 
	    		 */
	    		else if(!simulationFlag && server.getMeasuredPower() != null && server.getMeasuredPower().getValue()>0){
	    			
	    			psuCurrentPower.setValue(PSUPower(server.getMeasuredPower().getValue()/countPSU,myPSU.getEfficiency().getValue()));
	    			log.debug("The power consumption of the PSU is "+psuCurrentPower.getValue()+ " Watts");
	    			myPSU.setComputedPower(psuCurrentPower);
	    			totalPSUPower = totalPSUPower+psuCurrentPower.getValue();
	    			
	    		}else{
	    			double measuredPower=serverPower/countPSU;
	    			
	    			if(myPSU.getEfficiency().getValue()>0)
	    				psuCurrentPower.setValue(Math.round((measuredPower/myPSU.getEfficiency().getValue())*100)-Math.round((serverPower/countPSU)));	    			
	    			log.debug("The power consumption of the PSU is "+psuCurrentPower.getValue()+ " Watts");
	    			myPSU.setComputedPower(psuCurrentPower);
	    			totalPSUPower = totalPSUPower+psuCurrentPower.getValue();	    			
	    		}	    			    			    			
	    	}//end of if ( PSU has a load >0)
	      }	//end of while 
    		
    	log.debug("The power consumption of the PSUs is "+totalPSUPower+ " Watts");
        serverPower = serverPower+totalPSUPower;
        
        } //end of if(myQuery.indexOf('t') == 0 || myQuery.indexOf('r') == 0)          
      } //end of else (any PSU has a load >0)
      
      /* Casting from double to PowerType*/
      PowerType serverPow = new PowerType();
      serverPow.setValue(serverPower);
      server.setComputedPower(serverPow);          
        		
	
	PowerData powerData = new PowerData();
	powerData.setActualConsumption(serverPower);
	
	log.debug("Actual power consumption of a FIT4Green server is: " + powerData.getActualConsumption() + " Watt/hour");
	return powerData;	
		
	}
	
	/**
	 * 
	 * Computes the current power consumption of a server's mainboard 
	 * 
	 * @param mainboard, operatingSystem
	 * @return a data structure containing the power consumption in Watts of a server's mainboard 
	 */
	@Override
	public PowerData computePowerMainboard(MainboardType mainboard,OperatingSystemTypeType operatingSystem){
		
			if (mainboard == null){
	    		log.debug("Mainboard Type is null");
	    		PowerData powerData = new PowerData();
	    		powerData.setActualConsumption(0.0);
	    		return  powerData;
	    	}

			double totalMainboardPower=0.0;
			double totalCPUPower=0.0;
			double totalRAMPower=0.0;
			double totalStoragePower=0.0;

			double totalNICPower=0.0;			
			double totalHWRaidPower=0.0;
		
		    if(mainboard.getPowerMax().getValue()>0)
		    	totalMainboardPower = mainboard.getPowerMax().getValue();
		    else
		    	totalMainboardPower = 40.0;
			/**
			 * Power consumption of the CPUs
			 */
			totalCPUPower = CPUPower(mainboard,operatingSystem);
			log.debug("The power consumption of the CPUs is "+totalCPUPower+ " Watts");
			totalMainboardPower = totalMainboardPower+totalCPUPower;
        
			/**
			 * Power consumption of the RAM sticks
			*/
							
			totalRAMPower = RAMPower(mainboard);			
			log.debug("The power consumption of the RAMs is "+totalRAMPower+ " Watts");
			totalMainboardPower = totalMainboardPower+totalRAMPower;
			
			/**
			 * Power consumption of the Storage Unit
			 */
			totalStoragePower = StoragePower(mainboard);        
			log.debug("The power consumption of the Storage Units is "+totalStoragePower+ " Watts");
			totalMainboardPower = totalMainboardPower+totalStoragePower;
			
			/**
			 * Power consumption of the Network Interface Card           
			 */
			totalNICPower = NICPower(mainboard);		
			log.debug("The power consumption of the Network Interface Cards is "+totalNICPower+ " Watts");  
			totalMainboardPower = totalMainboardPower+totalNICPower;
			
        
			/**
			 * Power consumption of the HardwareRAID
			 */
			JXPathContext context2 = JXPathContext.newContext(mainboard);
			String myQuery = "hardwareRAID";
			Iterator hwRaidPowerIterator = context2.iterate(myQuery);
			
			while(hwRaidPowerIterator.hasNext())
           {
					double hwRaidPower=0.0;
					HardwareRAIDType myRaid = (HardwareRAIDType)hwRaidPowerIterator.next();        	         	  
            
					//Power consumption of the storage unit
					double totalhwRaidStoragePower= StoragePower(myRaid);              
					log.debug("The power consumption of the Hardware Raid Storage Units is "+totalhwRaidStoragePower+ " Watts");              
					hwRaidPower = hwRaidPower+ totalhwRaidStoragePower;          
             
					//Power consumption of the Cache
					double totalhwRaidCachePower= CachePower(myRaid);
					log.debug("The power consumption of the Hardware Raid Caches is "+totalhwRaidCachePower+ " Watts");              
					hwRaidPower = hwRaidPower+totalhwRaidCachePower; 
             
					PowerType hwRaidPow = new PowerType();
					hwRaidPow.setValue(hwRaidPower);
					myRaid.setComputedPower(hwRaidPow);
					log.debug("The power consumption of the Hardware Raid is "+hwRaidPower+ " Watts");
             
					totalHWRaidPower = totalHWRaidPower + hwRaidPower; 
                           
           }
       
			log.debug("The power consumption of the Hardware Raids is "+totalHWRaidPower+ " Watts");         
			totalMainboardPower = totalMainboardPower+totalHWRaidPower;
            
			PowerType totalMainboardPow = new PowerType();
			totalMainboardPow.setValue(totalMainboardPower);
			mainboard.setComputedPower(totalMainboardPow);
			
			PowerData powerData = new PowerData();
			powerData.setActualConsumption(totalMainboardPower);
	
			log.debug("Actual power consumption of a server's mainboard is: " + powerData.getActualConsumption() + " Watt/hour");
			return powerData;	
		
	}
	
	/**
	 * 
	 * Computes the current power consumption of a RAID device 
	 * 
	 * @param raid
	 * @return a data structure containing the power consumption in Watts of a RAID device  
	 */
	@Override
	public PowerData computePowerRAID(RAIDType raid){
	
		if (raid == null){
    		log.debug("Raid Type is null");
    		PowerData powerData = new PowerData();
    		powerData.setActualConsumption(0.0);
    		return  powerData;
    	}

		double raidPower=0.0;
		raidPower = StoragePower(raid);
		
		PowerType raidPow = new PowerType();
		raidPow.setValue(raidPower);
		raid.setComputedPower(raidPow); 			
		
		PowerData powerData = new PowerData();
		powerData.setActualConsumption(raidPower);
		
		log.debug("Actual power consumption of a RAID device is: " + powerData.getActualConsumption() + " Watt/hour");
		return powerData;	
		
	}
	
	/**
	 * 
	 * Computes the current power consumption of a hard disk 
	 * 
	 * @param hardDisk
	 * @return a data structure containing the power consumption in Watts of a hard disk   
	 */
	@Override
	public PowerData computePowerHardDisk(HardDiskType hardDisk){
		
		if (hardDisk == null){
    		log.debug("HardDisk Type is null");
    		PowerData powerData = new PowerData();
    		powerData.setActualConsumption(0.0);
    		return  powerData;
    	}

		double hdPower = 0.0;
		IoRateType IoRate = new IoRateType();
  	    double totalCachePower = CachePower(hardDisk);
  	    log.debug("The power consumption of the Hard Disk Cache is "+totalCachePower+ " Watts");
  	    
  	    if(hardDisk.getMaxReadRate().getValue()+ hardDisk.getMaxWriteRate().getValue()>0)
  	    {
  	    	if(hardDisk.getReadRate() == null)
  	    		{
  	    		 IoRate.setValue(0.0);
  	    		 hardDisk.setReadRate(IoRate);
  	    		}
  	    	if(hardDisk.getWriteRate() == null){
  	    		IoRate.setValue(0.0);
  	    		hardDisk.setWriteRate(IoRate);
  	    	}
  	    	
  	    	PoweredHardDiskDrive myPoweredHardDisk = new PoweredHardDiskDrive(hardDisk.getReadRate(),hardDisk.getMaxReadRate(),hardDisk.getWriteRate(),hardDisk.getMaxWriteRate(),hardDisk.getPowerIdle());	
  	    	hdPower = myPoweredHardDisk.computePower()+ totalCachePower;
  	    }
        
  	    PowerType hdPow = new PowerType();
  	    hdPow.setValue(hdPower);
  	    hardDisk.setComputedPower(hdPow);        		
	
  	    PowerData powerData = new PowerData();
  	    powerData.setActualConsumption(hdPower);
	
  	    log.debug("Actual power consumption of a hard disk is: " + powerData.getActualConsumption() + " Watt/hour");
  	    return powerData;	
		
	}
	
	/**
	 * 
	 * Computes the current power consumption of a solid state disk 
	 * 
	 * @param hardDisk
	 * @return a data structure currently containing a value of zero for the power consumption of a solid state disk   
	 */
	@Override
	public PowerData computePowerSolidStateDisk(SolidStateDiskType ssdisk){		
	     		
		if (ssdisk == null){
    		log.debug("SolidStateDisk Type is null");
    		PowerData powerData = new PowerData();
    		powerData.setActualConsumption(0.0);
    		return  powerData;
    	}

  	    PowerData powerData = new PowerData();
  	    powerData.setActualConsumption(0.0);
	
  	    log.debug("Actual power consumption of a hard disk is: " + powerData.getActualConsumption() + " Watt/hour");
  	    return powerData;	
		
	}
	
	/**
	 * 
	 * Computes the current power consumption of a central processing unit
	 * 
	 * @param cpu, operatingSystem
	 * @return a data structure containing the power consumption in Watts of a central processing unit  
	 */
	@Override	
	
	public PowerData computePowerCPU(CPUType cpu,OperatingSystemTypeType operatingSystem){		
		
		if (cpu == null){
    		log.debug("CPU Type is null");
    		PowerData powerData = new PowerData();
    		powerData.setActualConsumption(0.0);
    		return  powerData;
    	}

		PoweredCPU myPoweredCPU = new PoweredCPU(cpu,operatingSystem);		
		double cpuPower = 0.0;
		
		if (myPoweredCPU.getCpuUsage().getValue() > 0.0)	cpuPower = myPoweredCPU.computePower();
		
		PowerType cpuPow = new PowerType();
		cpuPow.setValue(cpuPower);
		cpu.setComputedPower(cpuPow);			
	
  	    PowerData powerData = new PowerData();
  	    powerData.setActualConsumption(cpuPower);
	
  	    log.debug("Actual power consumption of a central processing unit is: " + powerData.getActualConsumption() + " Watt/hour");
  	    
  	    return powerData;			
	}

	/**
	 * 
	 * Computes the current power consumption of a central processing unit
	 * 
	 * @param cpu, operatingSystem
	 * @return a data structure containing the power consumption in Watts of a central processing unit  
	 */
	@Override	
	
	public PowerData computePowerCore(CoreType myCore, CPUType cpu, OperatingSystemTypeType os){		
		
		if (cpu == null || myCore == null){
    		log.debug("CPU/Core Type is null");
    		PowerData powerData = new PowerData();
    		powerData.setActualConsumption(0.0);
    		return  powerData;
    	}

		PoweredCPU myCPU = new PoweredCPU(cpu,os);
		PoweredCore myPoweredCore = new PoweredCore(myCPU.getNumOfCores(), myCore.getFrequency(), myCore.getFrequencyMin(), myCore.getFrequencyMax(), myCore.getVoltage(), myCPU.getArchitecture(), os, myCore.getCoreLoad(), myCore.getTotalPstates(), myCPU.getTransistorNumber(), myCPU.isDVFS(), myCPU.computeLoadedCore(myCPU));
		
		double corePower = myPoweredCore.computePower();
		
		PowerType corePow = new PowerType();
		corePow.setValue(corePower);
		cpu.setComputedPower(corePow);			
	
  	    PowerData powerData = new PowerData();
  	    powerData.setActualConsumption(corePower);
	
  	    log.debug("Actual power consumption of a central processing unit is: " + powerData.getActualConsumption() + " Watt/hour");
 
  	    return powerData;			
	}


	/**
	 * 
	 * Computes the current power consumption of the RAMs of a server
	 * 
	 * @param mainboard 
	 *         
	 * @return a data structure containing the power consumption in Watts of a core of a specific CPU  
	 */
	@Override	
	
	public PowerData computePowerMainboardRAMs(MainboardType mainboard){		
		
		if (mainboard == null ){
    		log.debug("Mainboard Type is null");
    		PowerData powerData = new PowerData();
    		powerData.setActualConsumption(0.0);
    		return  powerData;
    	}

		double ramPower=0.0;
		
		if(mainboard.getMemoryUsage()== null){
			MemoryUsageType memoryUsageType = new MemoryUsageType();
			memoryUsageType.setValue(0.0);
			mainboard.setMemoryUsage(memoryUsageType);
		}
			
		ramPower = RAMPower(mainboard);
			
  	    PowerData powerData = new PowerData();
  	    powerData.setActualConsumption(ramPower);
	
  	    log.debug("Actual power consumption of the RAMs of a server is: " + powerData.getActualConsumption() + " Watt/hour");
  	    return powerData;	
		
	}
	
	
	
	/**
	 * 
	 * Computes the current power consumption of a fan of a server
	 * 
	 * @param fan 
	 *         
	 * @return a data structure containing the power consumption in Watts of a fan of a server
	 */
	@Override	
	
	public PowerData computePowerFAN(FanType fan){		
		
		if (fan == null ){
    		log.debug("Fan Type is null");
    		PowerData powerData = new PowerData();
    		powerData.setActualConsumption(0.0);
    		return  powerData;
    	}
		
		double fanPower=0.0;
			
		PoweredFan myPoweredFan = new PoweredFan(fan.getActualRPM(),fan.getDepth(), fan.getMaxRPM(), fan.getPowerMax(), fan.getMeasuredPower(), this.simulationFlag);           
		fanPower = myPoweredFan.computePower();
		
		PowerType fanPow = new PowerType();
		fanPow.setValue(fanPower);
		fan.setComputedPower(fanPow);
		
  	    PowerData powerData = new PowerData();
  	    powerData.setActualConsumption(fanPower);
	
  	    log.debug("Actual power consumption of a fan of a server is: " + powerData.getActualConsumption() + " Watt/hour");
  	    return powerData;	
		
	}

/**
 * A function to compute the power consumption of box type routers and switches
 * @param obj of type Datacenter
 * @return double value containing the power consumption of box type routers and switches
 */
	
	private double BoxNetworkPower(DatacenterType obj){
	
		double totalBoxNetworkPower=0.0;
		JXPathContext context = JXPathContext.newContext(obj);
		String boxRouterQuery = "boxRouter";                                   
        Iterator boxRouterPowerIterator = context.iterate(boxRouterQuery);
		
        /**
    	 * Power consumption of box routers
    	 */
        while(boxRouterPowerIterator.hasNext()){
        	
        	double boxRouterPower =0.0;
        	BoxRouterType myBoxRouter = (BoxRouterType)boxRouterPowerIterator.next();
        	PoweredNetworkNode myNetworkNode = new PoweredNetworkNode(myBoxRouter);
        	
        	boxRouterPower = myNetworkNode.computePower();
        	
        	/**
        	 * Cooling system of the box router
        	 * 
        	 */
        	boxRouterPower = boxRouterPower+coolingSystemPower(myBoxRouter);
        	
        	 /**
             * Power supply unit of the box router
             */
            
        	boxRouterPower = boxRouterPower+PSUNetworkNodePower(myBoxRouter, boxRouterPower);
        	
        	PowerType boxRouterPow = new PowerType();
        	boxRouterPow.setValue(boxRouterPower);
        	myBoxRouter.setComputedPower(boxRouterPow);        	
    				
        	totalBoxNetworkPower = totalBoxNetworkPower+ boxRouterPower;
    		
    		log.debug("The power consumption of the box routers is "+boxRouterPower+ " Watts");
    		
        }
		        
        context = JXPathContext.newContext(obj);
		String boxSwitchQuery = "boxSwitch";                                   
        Iterator boxSwitchPowerIterator = context.iterate(boxSwitchQuery);
		
        /**
    	 * Power consumption of box switches
    	 */
        while(boxSwitchPowerIterator.hasNext()){
        	
        	double boxSwitchPower =0.0;
        	BoxSwitchType myBoxSwitch = (BoxSwitchType)boxSwitchPowerIterator.next();
        	PoweredNetworkNode myNetworkNode = new PoweredNetworkNode(myBoxSwitch);
        	boxSwitchPower = myNetworkNode.computePower();
        	
        	/**
        	 * Cooling system of the box router
        	 * 
        	 */
        	
        	boxSwitchPower = boxSwitchPower+coolingSystemPower(myBoxSwitch);
        	
        	/**
             * Power supply unit of the box router
             */
        	
        	boxSwitchPower = boxSwitchPower+PSUNetworkNodePower(myBoxSwitch, boxSwitchPower);
        	
        	PowerType boxSwitchPow = new PowerType();
        	boxSwitchPow.setValue(boxSwitchPower);
        	myBoxSwitch.setComputedPower(boxSwitchPow);        	
    				
        	totalBoxNetworkPower = totalBoxNetworkPower+ boxSwitchPower;
    		
    		log.debug("The power consumption of the box switches is "+boxSwitchPower+ " Watts");  		
        }
		
        return totalBoxNetworkPower;
	}
	
	
	/**
	 * A function to compute the power consumption of rackable type routers and switches
	 * @param obj of type Rack
	 * @return double value containing the power consumption of rackable type routers and switches
	 */
		
		private double RackableNetworkPower(RackType obj){
		
			double totalRackableNetworkPower=0.0;
			JXPathContext context = JXPathContext.newContext(obj);
			String rackableRouterQuery = "rackableRouter";                                   
	        Iterator rackableRouterPowerIterator = context.iterate(rackableRouterQuery);
			
	        /**
	    	 * Power consumption of rackable routers
	    	 */
	        while(rackableRouterPowerIterator.hasNext()){
	        	
	        	double rackbaleRouterPower =0.0;
	        	RackableRouterType myRackableRouter = (RackableRouterType)rackableRouterPowerIterator.next();
	        	PoweredNetworkNode myNetworkNode = new PoweredNetworkNode(myRackableRouter);
	        	
	        	rackbaleRouterPower = myNetworkNode.computePower();
	        	
	        	/**
	        	 * Cooling system of the box router
	        	 * 
	        	 */
	        	rackbaleRouterPower = rackbaleRouterPower+coolingSystemPower(myRackableRouter);
	        	
	        	/**
	             * Power supply unit of the box router
	             */
	        	rackbaleRouterPower = rackbaleRouterPower+PSUNetworkNodePower(myRackableRouter, rackbaleRouterPower);
	        	
	        	PowerType rackbaleRouterPow = new PowerType();

	        	rackbaleRouterPow.setValue(rackbaleRouterPower);
	        	myRackableRouter.setComputedPower(rackbaleRouterPow);        	
	    				
	        	totalRackableNetworkPower = totalRackableNetworkPower+ rackbaleRouterPower;
	    		
	    		log.debug("The power consumption of the rackable routers is "+rackbaleRouterPower+ " Watts"); 	    		
	        }
				        
	        context = JXPathContext.newContext(obj);
			String rackableSwitchQuery = "rackableSwitch";                                   
	        Iterator rackableSwitchPowerIterator = context.iterate(rackableSwitchQuery);
			
	        /**
	    	 * Power consumption of rackable switches
	    	 */
	        while(rackableSwitchPowerIterator.hasNext()){
	        	
	        	double rackableSwitchPower =0.0;
	        	RackableSwitchType myRackableSwitch = (RackableSwitchType)rackableSwitchPowerIterator.next();
	        	PoweredNetworkNode myNetworkNode = new PoweredNetworkNode(myRackableSwitch);
	        	
	        	rackableSwitchPower = myNetworkNode.computePower();
	        	
	        	/**
	        	 * Cooling system of the box router
	        	 * 
	        	 */
	        	rackableSwitchPower = rackableSwitchPower+coolingSystemPower(myRackableSwitch);
	        	
	        	/**
	             * Power supply unit of the box router
	             */
	        	rackableSwitchPower = rackableSwitchPower+PSUNetworkNodePower(myRackableSwitch, rackableSwitchPower);
	        	
	        	PowerType rackbaleSwitchPow = new PowerType();
	        	rackbaleSwitchPow.setValue(rackableSwitchPower);
	        	myRackableSwitch.setComputedPower(rackbaleSwitchPow);        	
	    				
	        	totalRackableNetworkPower = totalRackableNetworkPower+ rackableSwitchPower;
	    		
	    		log.debug("The power consumption of the rackable switches is "+rackableSwitchPower+ " Watts");    		  		
	    		
	    		
	        }
			return totalRackableNetworkPower;
		}
	
	
	
	/**	 
	 * Computes the current power consumption of Sites
	 * 
	 * @param obj:input object of type FIT4Green 
	 * @return double value containing information on power consumption of Sites
	 */
	private PowerType sitePower(FIT4GreenType obj){
		
		PowerType totalSitePower = new PowerType();
		totalSitePower.setValue(0.0);
		JXPathContext context = JXPathContext.newContext(obj);
		String siteQuery = "site";                                   
        Iterator sitePowerIterator = context.iterate(siteQuery);
        
        while(sitePowerIterator.hasNext()){
        	
        	SiteType mySite = (SiteType)sitePowerIterator.next();        	
        	PowerType sitePower =new PowerType();
        	sitePower.setValue(0.0); 
        	
        	/**
        	 * Power consumption of Datacenters
        	 */
        	PowerType totalDCPower= datacenterPower(mySite);    		
    		sitePower.setValue(sitePower.getValue()+ totalDCPower.getValue());
    		mySite.setComputedPower(sitePower);
    		log.debug("The power consumption of the Site Datacenters is "+sitePower.getValue()+ " Watts");    		  		
    		
    		totalSitePower.setValue(totalSitePower.getValue()+ sitePower.getValue());
        }
		
        log.debug("The power consumption of the F4G Sites is "+totalSitePower.getValue()+ " Watts");		
		return totalSitePower;
	}
	
	
	/**	 
	 * Computes the current power consumption of Datacenters
	 * 
	 * @param obj:input object of type Site 
	 * @return double value containing information on power consumption of Datacenters
	 */
	private PowerType datacenterPower(SiteType obj){
		
		double totalDCPower=0.0;
		JXPathContext context = JXPathContext.newContext(obj);
		String DCQuery = "datacenter";                                   
        Iterator DCPowerIterator = context.iterate(DCQuery);
        
        while(DCPowerIterator.hasNext()){
        	
        	DatacenterType myDC = (DatacenterType)DCPowerIterator.next();        	
    		
    		double totalRackPower=0.0;
    		double totalBoxNetworkPower=0.0;    		
    		double totalTowerServerPower=0.0;
    		double DCPower=0.0;
    		
    		    		
    		/**
    		 * Power consumption of BoxNetworks
    		 */
    		
    		totalBoxNetworkPower = BoxNetworkPower(myDC);
    		DCPower = DCPower+totalBoxNetworkPower;
    		log.debug("The power consumption of the Datacenter Box Network devices is "+totalBoxNetworkPower+ " Watts");
    		
    		/**
    		 * Power consumption of Racks
    		 */
    		totalRackPower = rackPower(myDC);
    		DCPower = DCPower+totalRackPower;
    		log.debug("The power consumption of the Datacenter Racks is "+totalRackPower+ " Watts");
    		
    		/**
    		 * Power consumption of Tower servers
    		 */
    		totalTowerServerPower = serverPower(myDC, "towerServer");
    		DCPower = DCPower+totalTowerServerPower;
    		log.debug("The power consumption of the Datacenter Tower Servers is "+totalTowerServerPower+ " Watts");
    		
    		//Power consumption of the whole datacenter
    		log.debug("The power consumption of the Datacenter is "+DCPower+ " Watts");
    		
    		PowerType DCPow = new PowerType();
    		DCPow.setValue(DCPower);        	
    	    myDC.setComputedPower(DCPow);    		
    		
    	    totalDCPower = totalDCPower+ DCPower;
        }
		PowerType totalDCPow = new PowerType();
		totalDCPow.setValue(totalDCPower);
        log.debug("The power consumption of the Datacenters is "+totalDCPower+ " Watts");		
		return totalDCPow;
	}
	
	/**	 
	 * Computes the current power consumption of racks
	 * 
	 * @param obj:input object of type Datacenter 
	 * @return double value containing information on power consumption of the racks
	 */
	private double rackPower(DatacenterType obj){
		
		double totalRackPower=0.0;
		JXPathContext context = JXPathContext.newContext(obj);
		String rackQuery = "rack";                                   
        Iterator rackPowerIterator = context.iterate(rackQuery);
        
        while(rackPowerIterator.hasNext()){
        	
        	RackType myRack = (RackType)rackPowerIterator.next();
        	double totalPDUPower=0.0;
    		double totalEnclosurePower=0.0;
    		double totalSANPower=0.0;
		    double totalNASPower=0.0;
    		double totalRackableNetworkPower=0.0;
    		double totalCoolingSystemPower=0.0;
    		double totalRackableServerPower=0.0;
    		double rackPower=0.0;
    		
    		/**
    		 * Power consumption of power distribution units
    		 */
    		totalPDUPower = PDUPower(myRack);
    		rackPower = rackPower+totalPDUPower;
    		log.debug("The power consumption of the Rack PDUs is "+totalPDUPower+ " Watts");
    		
    		/**
    		 * Power consumption of enclosures
    		 */
    		totalEnclosurePower = enclosurePower(myRack);
    		rackPower = rackPower+totalEnclosurePower;
    		log.debug("The power consumption of the Rack Enclosures is "+totalEnclosurePower+ " Watts");
    		
    		/**
    		 * Power consumption of SAN devices
    		 */
    		totalSANPower = SANPower(myRack, 0);
    		rackPower = rackPower+totalSANPower;
    		log.debug("The power consumption of the Rack SAN devices is "+totalSANPower+ " Watts");

		    /**
    		 * Power consumption of NAS devices
    		 */
    		totalNASPower = OverallNASPower(myRack, 0); //returns the power consumption of all the installed NAS devices
    		rackPower = rackPower+totalNASPower;
    		log.debug("The power consumption of the Rack NAS devices is "+totalNASPower+ " Watts");
    		
    		/**
    		 * Power consumption of Rackable Networks
    		 */    		
    		totalRackableNetworkPower = RackableNetworkPower(myRack);
    		rackPower = rackPower+totalRackableNetworkPower;
    		log.debug("The power consumption of the Rack Network devices is "+totalRackableNetworkPower+ " Watts");
    		
    		/**
    		 * Power consumption of Cooling systems
    		 */
    		totalCoolingSystemPower = coolingSystemPower(myRack);
    		rackPower = rackPower+totalCoolingSystemPower;
    		log.debug("The power consumption of the Rack Cooling Systems is "+totalCoolingSystemPower+ " Watts");
    		
    		/**
    		 * Power consumption of Rackable servers
    		 */
    		totalRackableServerPower = serverPower(myRack, "rackableServer");
    		rackPower = rackPower+totalRackableServerPower;
    		log.debug("The power consumption of the Rack Servers is "+totalRackableServerPower+ " Watts");
    		
    		//Power consumption of the whole rack
    		log.debug("The power consumption of the Rack is "+rackPower+ " Watts");
    		
    		PowerType rackPow = new PowerType();
    		rackPow.setValue(rackPower);
    		myRack.setComputedPower(rackPow);
    		
    		totalRackPower = totalRackPower+ rackPower;
        }
		
        log.debug("The power consumption of the Racks is "+totalRackPower+ " Watts");		
		return totalRackPower;
	}
	
	/**	 
	 * Computes the current power consumption of enclosures
	 * 
	 * @param obj:input object of type Rack 
	 * @return double value containing information on power consumption of the enclosures
	 */
	private double enclosurePower(RackType obj){
		
		double totalEnclosurePower=0.0;
		JXPathContext context = JXPathContext.newContext(obj);
		String enclosureQuery = "enclosure";                                   
        Iterator enclosurePowerIterator = context.iterate(enclosureQuery);
        
        while(enclosurePowerIterator.hasNext()){
        	
        	EnclosureType myEnclosure = (EnclosureType)enclosurePowerIterator.next();
    		double enclosurePower=0.0;
    		
        	if (!simulationFlag && myEnclosure.getMeasuredPower() != null && myEnclosure.getMeasuredPower().getValue() > 0.0) 
        		enclosurePower = myEnclosure.getMeasuredPower().getValue();
        	else{
            	double totalCoolingSystemPower=0.0;    		
        		double totalNICPower=0.0;
        		double totalBladeServerPower=0.0;

	    						
	            double countPSU=0.0;
	            boolean measuredPowerPSU = true;
	            //boolean loadPSU = false;
	            JXPathContext contextPSU = JXPathContext.newContext(myEnclosure);
	        	String psuQuery = "PSU";
	        	Iterator psuPowerIterator = contextPSU.iterate(psuQuery);
	
	        	/**
				* Testing whether all PSUs have measured power provided. We assume that either all of them have the measured power configured or none of them. Also, count the PSUs that have a non zero load 
				* If count of PSU is zero, then the server has a load of zero
				*
				* 
				*/	
	        	while(psuPowerIterator.hasNext())
	        	{
	        		PSUType myPSU = (PSUType)psuPowerIterator.next();
	        	    if(myPSU.getMeasuredPower() == null ||myPSU.getMeasuredPower().getValue()<=0)
	        	    	measuredPowerPSU = false;
	        	    if(myPSU.getLoad().getValue() > 0.0)
	        	    	//loadPSU = true;
	        	    	countPSU++;
	        	}
				
	        	/**
				* 
				* If count of PSU is zero, then the server has a load of zero
				*
				* 
				*/	
	        	if(countPSU==0)        		
	        		enclosurePower = 0.0;
	        	else{
	    		    		
	    		/**
	    		 * Power consumption of network interface cards
	    		 */
	    		totalNICPower = NICPower(myEnclosure);
	    		enclosurePower = enclosurePower+totalNICPower;
	    		log.debug("The power consumption of the Enclosure NICs is "+totalNICPower+ " Watts");
	    		
	    		/**
	    		 * Power consumption of cooling systems
	    		 */
	    		totalCoolingSystemPower = coolingSystemPower(myEnclosure);
	    		enclosurePower = enclosurePower+totalCoolingSystemPower;
	    		log.debug("The power consumption of the Enclosure Cooling Systems is "+totalCoolingSystemPower+ " Watts");
	    		
	    		/**
	    		 * Power consumption of Blade servers
	    		 */
	    		totalBladeServerPower = serverPower(myEnclosure, "bladeServer");
	    		enclosurePower = enclosurePower+totalBladeServerPower;
	    		log.debug("The power consumption of the Enclosure Servers is "+totalBladeServerPower+ " Watts");    		
	    		
	    		/**
	    		 * Power consumption of power supply units
	    		 */
	    		double totalPSUPower=0.0;        	
	        	contextPSU = JXPathContext.newContext(myEnclosure);
	        	psuPowerIterator = contextPSU.iterate(psuQuery);
	        	while(psuPowerIterator.hasNext())
	        	{
	        		double psuCurrentPower = 0.0;
	        		PSUType myPSU = (PSUType)psuPowerIterator.next();
	        		
	        		/**
						* The monitoring system
						* can either provide the measuredPower for every PSU or no information is provided about any PSU inside the same server.
						* 
					*/
	        		if(myPSU.getLoad().getValue()>0) //If PSU has a load of zero, then it can't be considered as power supplying unit.
		        	{	
		        		if(!simulationFlag && measuredPowerPSU)
		        		{
		        			psuCurrentPower = PSUPower(myPSU.getMeasuredPower().getValue(),myPSU.getEfficiency().getValue());
		        			log.debug("The power consumption of the PSU is "+psuCurrentPower+ " Watts");
		        			PowerType psuCurrentPow = new PowerType();
		        			psuCurrentPow.setValue(psuCurrentPower);
		        			myPSU.setComputedPower(psuCurrentPow);
		        			
		        			totalPSUPower = totalPSUPower+psuCurrentPower;
		        			
		        		}
		        		/**
		        		 * If the measuredPower at the server level is provided by the monitoring system, then this measuredPower is evenly distrbiuted 
		        		 * among all the PSUs inside the same server 
		        		 * 
		        		 */
		        		else if(!simulationFlag && myEnclosure.getMeasuredPower() != null && myEnclosure.getMeasuredPower().getValue()>0){
		        			
		        			
		        			psuCurrentPower= PSUPower(myEnclosure.getMeasuredPower().getValue()/countPSU,myPSU.getEfficiency().getValue());
		        			
		        			PowerType psuCurrentPow = new PowerType();
		        			psuCurrentPow.setValue(psuCurrentPower);
		        			log.debug("The power consumption of the PSU is "+psuCurrentPow.getValue()+ " Watts");
		        			myPSU.setComputedPower(psuCurrentPow);
		        			totalPSUPower = totalPSUPower+psuCurrentPower;    			
		        		}
		        		else{
		        			double measuredPower = enclosurePower/countPSU;
		        			if(myPSU.getEfficiency().getValue()>0)
		        				psuCurrentPower = Math.round((measuredPower/myPSU.getEfficiency().getValue())*100)-Math.round(enclosurePower/countPSU);
		        			
		        			PowerType psuCurrentPow = new PowerType();
		        			psuCurrentPow.setValue(psuCurrentPower);
		        			log.debug("The power consumption of the PSU is "+psuCurrentPow.getValue()+ " Watts");
		        			myPSU.setComputedPower(psuCurrentPow);
		        			totalPSUPower = totalPSUPower+psuCurrentPower;	        			
		        		}	        		
		        	}// end of if(PSU load >0)
		        }
	        		
	        	log.debug("The power consumption of the PSUs is "+totalPSUPower+ " Watts");	        		        	
	    		enclosurePower = enclosurePower+totalPSUPower;
	    		log.debug("The power consumption of the Enclosure PSUs is "+totalPSUPower+ " Watts");
	        	}//end of else (any PSU has a load >0)
        	}//end of else with if (measurePower>0)
        	
    		//Power consumption of the whole enclosure
    		log.debug("The power consumption of the Enclosure is "+enclosurePower+ " Watts");
    		
    		PowerType enclosurePow = new PowerType();
    		enclosurePow.setValue(enclosurePower);
    		myEnclosure.setComputedPower(enclosurePow);
    		
    		totalEnclosurePower = totalEnclosurePower+ enclosurePower;    		
        }
		
        log.debug("The power consumption of the Enclosures is "+totalEnclosurePower+ " Watts");		
		return totalEnclosurePower;
	}
	
	 
	/**	 
	 * Computes the current power consumption of servers
	 * 
	 * @param obj:input object of type Enclosure, Rack or Datacenter
	 * @param myQuery: the required type of the server ("bladeServer", "rackableServer", or "towerServer")
	 * @return double value containing information on power consumption of the servers
	 */
	private double serverPower(Object obj, String myQuery){
	
		JXPathContext context = JXPathContext.newContext(obj);		
		Iterator serverPowerIterator = context.iterate(myQuery);
		
		double totalServersPower=0.0;
		TowerServerType myTSObj = null;
		RackableServerType myRSObj = null;
		BladeServerType myBSObj = null;
		
		while(serverPowerIterator.hasNext())
		{				
			double serverPower=0.0;
			double totalMainboardPower=0.0;			
			ServerType serverObj = (ServerType)serverPowerIterator.next();			
			
			/**
			 * C.Dupont: I'm adding that as a server off should consume 0
			 * to check
			 */
			if(serverObj.getStatus() == ServerStatusType.ON){
				
			
			//Casting to Tower Server
			if(myQuery.indexOf('t') == 0)
				myTSObj = (TowerServerType)serverObj;				 
			
			//Casting to Rackable Server
			else if (myQuery.indexOf('r') == 0)
				myRSObj = (RackableServerType)serverObj;
			
			//Casting to Blade Server
			else if	(myQuery.indexOf('b') == 0)
				myBSObj = (BladeServerType)serverObj;
			
						
			
            double countPSU=0.0;
            boolean measuredPowerPSU = true;
            //boolean loadPSU = false;
            JXPathContext contextPSU = JXPathContext.newContext(serverObj);
        	String psuQuery = "PSU";
        	Iterator psuPowerIterator = contextPSU.iterate(psuQuery);

        	/**
				* Testing whether all PSUs have measured power provided. We assume that either all of them have the measured power configured or none of them. Also, count the PSUs that have a non zero load 
				* If count of PSU is zero, then the server has a load of zero
				*
				* 
				*/	
        	while(psuPowerIterator.hasNext())
        	{
        		PSUType myPSU = (PSUType)psuPowerIterator.next();
        	    if(myPSU.getMeasuredPower() == null ||myPSU.getMeasuredPower().getValue()<=0)
        	    	measuredPowerPSU = false;
        	    if(myPSU.getLoad().getValue() > 0.0)
        	    	//loadPSU = true;
        	    	countPSU++;
        	}
        	
        	if((countPSU==0 && myTSObj != null) ||(countPSU==0 && myRSObj != null) )         		
        		serverPower = 0.0;
        	else{
			/**
			 * Fetching the operating system
			 */
			OperatingSystemTypeType operatingSystem = getOperatingSystem(serverObj);
			
			/**
			 * Power consumption of the Mainboards
			 */
            totalMainboardPower = mainboardPower(serverObj,operatingSystem);			          
            log.debug("The power consumption of the Mainboards is "+totalMainboardPower+ " Watts");
            serverPower = serverPower + totalMainboardPower; 
            
            /**
             * Power consumption of the cooling system and power supply units for Tower and Rackable Servers
             */
            if(myQuery.indexOf('t') == 0 || myQuery.indexOf('r') == 0){
            
            /**
             * Cooling System part
             */
            double totalCoolingPower = coolingSystemPower(serverObj);            	
            log.debug("The power consumption of the Cooling Systems is "+totalCoolingPower+ " Watts");
            serverPower = serverPower +	totalCoolingPower;
            	
            /**
             * Power supply unit part
             */
            /**
    		 * Power consumption of power supply units
    		 */
    		double totalPSUPower=0.0;            
        	        		
    			contextPSU = JXPathContext.newContext(serverObj);
        		psuPowerIterator = contextPSU.iterate(psuQuery);
        		while(psuPowerIterator.hasNext())
        		{
        			double psuCurrentPower = 0.0;
        			PSUType myPSU = (PSUType)psuPowerIterator.next();
        		
        			/**
        			 * The monitoring system
        			 * can either provide the measuredPower for every PSU or no information is provided about any PSU inside the same server.
        			 * 
        			 */
        		
        		if(myPSU.getLoad().getValue()>0) //If PSU has a load of zero, then it can not be considered as a power supplying unit
        		{	
        			if(!simulationFlag && measuredPowerPSU)
        			{
        				psuCurrentPower = PSUPower(myPSU.getMeasuredPower().getValue(),myPSU.getEfficiency().getValue());
        				
        				PowerType psuCurrentPow = new PowerType();
        				psuCurrentPow.setValue(psuCurrentPower);
        				log.debug("The power consumption of the PSU is "+psuCurrentPow.getValue()+ " Watts");
        				myPSU.setComputedPower(psuCurrentPow);
        				totalPSUPower = totalPSUPower+psuCurrentPower;
        			
        			}
        			/**
        			 * If the measuredPower at the server level is provided by the monitoring system, then this measuredPower is evenly distrbiuted 
        			 * among all the PSUs inside the same server 
        			 * 
        			 */
        			else if(!simulationFlag && serverObj.getMeasuredPower() != null && serverObj.getMeasuredPower().getValue()>0){        			
        			
        			psuCurrentPower= PSUPower(serverObj.getMeasuredPower().getValue()/countPSU,myPSU.getEfficiency().getValue());
        			
        			PowerType psuCurrentPow = new PowerType();
    				psuCurrentPow.setValue(psuCurrentPower);
    				log.debug("The power consumption of the PSU is "+psuCurrentPow.getValue()+ " Watts");
        			myPSU.setComputedPower(psuCurrentPow);
        			totalPSUPower = totalPSUPower+psuCurrentPower;    			
        			}
        			else{        			
        				
        				double measuredPower = serverPower/countPSU;
        				
        				if(myPSU.getEfficiency().getValue()>0)
        					psuCurrentPower = Math.round(((measuredPower/myPSU.getEfficiency().getValue())*100))-Math.round((serverPower/countPSU));
        				
        				PowerType psuCurrentPow = new PowerType();
        				psuCurrentPow.setValue(psuCurrentPower);
        				log.debug("The power consumption of the PSU is "+psuCurrentPow.getValue()+ " Watts");
        				myPSU.setComputedPower(psuCurrentPow);
        				totalPSUPower = totalPSUPower+psuCurrentPower;
        			
        			}        		
        			log.debug("The power consumption of the PSU is "+psuCurrentPower+ " Watts");  			
        		}//end of else (PSU has a load >0)
        	} 
        	       	
        			
            log.debug("The power consumption of the PSUs is "+totalPSUPower+ " Watts");
            serverPower = serverPower+totalPSUPower;
           
            } //end of if(myQuery.indexOf('t') == 0 || myQuery.indexOf('r') == 0)          
         } //end of else (any PSU has a load >0)
        	PowerType serverPow = new PowerType();
        	serverPow.setValue(serverPower);
            if(myQuery.indexOf('t') == 0)           	 
              myTSObj.setComputedPower(serverPow);                      
			else if (myQuery.indexOf('r') == 0)					
				myRSObj.setComputedPower(serverPow);			       		
			else if	(myQuery.indexOf('b') == 0)
				myBSObj.setComputedPower(serverPow);				
            
            log.debug("The power consumption of the Server [" + serverObj.getClass().getName() +"] is "+serverPower+ " Watts with ID....."+serverObj.getFrameworkID());
            totalServersPower= totalServersPower+serverPower;            
            
			} //end of IF (ON servers) 
		}// end of while (loop over the servers)
		
	return totalServersPower;	
		
	}
    
	/**	 
	 * A function to return the operating system running on the server
	 * 
	 * @param obj:input object of type Server
	 * @return OperatingSystemTypeType value containing information on the operating system
	 */
	private OperatingSystemTypeType getOperatingSystem(ServerType obj){

		if(obj.getNativeHypervisor() != null){
			if(obj.getNativeHypervisor().getName() == null){
				return OperatingSystemTypeType.LINUX;
			}
			return obj.getNativeHypervisor().getName();
		} else {
			if(obj.getNativeOperatingSystem().getName() == null){
				return OperatingSystemTypeType.LINUX;
			}
			return obj.getNativeOperatingSystem().getName();
		}

		
	}
	
	/**	 
	 * Computes the current power consumption of cooling systems
	 * 
	 * @param obj:input object of type Enclosure, Network Node, Rack, PSU, RackableServer and TowerServer 
	 * @return double value containing information on power consumption of the cooling systems
	 */
	private double coolingSystemPower(Object obj){
	
		double totalCoolingPower = 0.0;
		double fanPower=0.0;
		double waterCoolerPower=0.0;
		JXPathContext context = JXPathContext.newContext(obj);
		
		/**
		 * Fan power consumption
		 */
		String fanQuery = "fan";
		Iterator fanPowerIterator = context.iterate(fanQuery);
		
		while(fanPowerIterator.hasNext())
        {		   	
           PowerType fanCurrentPow = new PowerType();
           fanCurrentPow.setValue(computePowerFAN((FanType)fanPowerIterator.next()).getActualConsumption());           
           fanPower = fanPower + fanCurrentPow.getValue();
           log.debug("The power consumption of the Fan is "+fanCurrentPow.getValue()+ " Watts");
           
        }
		log.debug("The power consumption of the Fans is "+fanPower+ " Watts");
		totalCoolingPower = totalCoolingPower+fanPower;
		
		/**
		 * Water cooler power consumption
		 */
		String waterCoolerQuery = "waterCooler";
		Iterator waterCoolerPowerIterator = context.iterate(waterCoolerQuery);
		
		while(waterCoolerPowerIterator.hasNext())
        {		   	
		   WaterCoolerType myWaterCooler = (WaterCoolerType)waterCoolerPowerIterator.next();	
           double waterCoolerCurrentPower = 0.0; 
           //myWaterCooler.getCurrentPower();
           
           PowerType waterCoolerCurrentPow = new PowerType();
           waterCoolerCurrentPow.setValue(waterCoolerCurrentPower);
           myWaterCooler.setComputedPower(waterCoolerCurrentPow);
           waterCoolerPower = waterCoolerPower + waterCoolerCurrentPower; 
           log.debug("The power consumption of the Water Cooler is "+waterCoolerCurrentPower+ " Watts");           
        }
		log.debug("The power consumption of the Water Coolers is "+waterCoolerPower+ " Watts");
		totalCoolingPower = totalCoolingPower+waterCoolerPower;
		
		return totalCoolingPower;
	}
	
	/**	 
	 * Computes the current power consumption of mainboards
	 * 
	 * @param obj:input object of type Server
	 * @return double value containing information on power consumption of the mainboards
	 */
	private double mainboardPower(ServerType obj,OperatingSystemTypeType operatingSystem){
		
		JXPathContext context = JXPathContext.newContext(obj);
		String mainboardQuery = "mainboard";                                   
        Iterator mainboardPowerIterator = context.iterate(mainboardQuery);
        double mainboardsPower=0.0; // The overall power consumed by all the attached mainboards
        
        while(mainboardPowerIterator.hasNext())
        {
        	MainboardType myMainboard = (MainboardType)mainboardPowerIterator.next();
           	JXPathContext context2 = JXPathContext.newContext(myMainboard);
           	
           	double totalMainboardPower=0.0;
		
           	/**
           	 * In case, the mainboard power of different servers is not given,
           	 * the powercalculator will use the following constant number of the mainboard power.
           	 */
			if(obj instanceof BladeServerType)
		           	{
		           		if(myMainboard.getPowerMax().getValue()>0)
		           			totalMainboardPower = myMainboard.getPowerMax().getValue();
		           		else
		           			totalMainboardPower = 80.0;
		           		
		           	}
		           	else if(obj instanceof TowerServerType)
		           	{
		           		if(myMainboard.getPowerMax().getValue()>0)
		           			totalMainboardPower = myMainboard.getPowerMax().getValue();
		           		else
		           			totalMainboardPower= 65.0;
		           		
		           	}
		           	else if(obj instanceof RackableServerType)
		           	{
		           		if(myMainboard.getPowerMax().getValue()>0)
		           			totalMainboardPower = myMainboard.getPowerMax().getValue();
		           		else
		           			totalMainboardPower=85.0;
		           	}
					
   			double totalCPUPower=0.0;
   			double totalRAMPower=0.0;
   			double totalStoragePower=0.0;
   			double totalNICPower=0.0;   			
   			double totalHWRaidPower=0.0;
   		
   			/**
   			 * Power consumption of the CPUs
   			 */
   			totalCPUPower = CPUPower(myMainboard,operatingSystem);
            log.debug("The power consumption of the CPUs is "+totalCPUPower+ " Watts");
            totalMainboardPower = totalMainboardPower+totalCPUPower;
            
           	/**
           	 * Power consumption of the RAM sticks
           	 */
            
            totalRAMPower = RAMPower(myMainboard);   			
   			log.debug("The power consumption of the RAMs is "+totalRAMPower+ " Watts");
   			totalMainboardPower = totalMainboardPower+totalRAMPower;
   			
   			/**
   			 * Power consumption of the Storage Unit
   			 */
   			totalStoragePower = StoragePower(myMainboard);        
   			log.debug("The power consumption of the Storage Units is "+totalStoragePower+ " Watts");
   			totalMainboardPower = totalMainboardPower+totalStoragePower;
   			
   			/**
   			 * Power consumption of the Network Interface Card           
   			 */
   			totalNICPower = NICPower(myMainboard);		
   			log.debug("The power consumption of the Network Interface Cards is "+totalNICPower+ " Watts");  
   			totalMainboardPower = totalMainboardPower+totalNICPower;
   			
   			
           	/**
           	 * Power consumption of the HardwareRAID
           	 */
           	String myQuery = "hardwareRAID";
   			Iterator hwRaidPowerIterator = context2.iterate(myQuery);
   			
   			while(hwRaidPowerIterator.hasNext())
               {
   					double hwRaidPower=0.0;
   					HardwareRAIDType myRaid = (HardwareRAIDType)hwRaidPowerIterator.next();        	         	  
                
   					//Power consumption of the storage unit
   					double totalhwRaidStoragePower= StoragePower(myRaid);              
   					log.debug("The power consumption of the Hardware Raid Storage Units is "+totalhwRaidStoragePower+ " Watts");              
   					hwRaidPower = hwRaidPower+ totalhwRaidStoragePower;          
                 
   					//Power consumption of the Cache
   					double totalhwRaidCachePower= CachePower(myRaid);
   					log.debug("The power consumption of the Hardware Raid Caches is "+totalhwRaidCachePower+ " Watts");              
   					hwRaidPower = hwRaidPower+totalhwRaidCachePower; 
                 
   					PowerType hwRaidPow = new PowerType();
   					hwRaidPow.setValue(hwRaidPower);
   					myRaid.setComputedPower(hwRaidPow);
   					log.debug("The power consumption of the Hardware Raid is "+hwRaidPower+ " Watts");
                 
   					totalHWRaidPower = totalHWRaidPower + hwRaidPower; 
                               
               }
           log.debug("The power consumption of the Hardware Raids is "+totalHWRaidPower+ " Watts");         
           totalMainboardPower = totalMainboardPower+totalHWRaidPower;
          
           PowerType totalMainboardPow = new PowerType();
           totalMainboardPow.setValue(totalMainboardPower);
           myMainboard.setComputedPower(totalMainboardPow);         
           
           mainboardsPower = mainboardsPower+ totalMainboardPower;
           
           log.debug("The power consumption of the Mainboard is "+totalMainboardPower+ " Watts");               
        } 		
               
		 		
        return mainboardsPower;		
		
	}
	
	
	/**	 
	 * Computes the current power consumption of the RAMSticks based on a probabilistic approach due to the
	 * fact that the Monitoring System provides only information regarding the total available free RAM  
	 * 
	 * @param obj:input object of type Mainboard 
	 * @return double value containing information on power consumption of the RAMSticks
	 */	
	private double RAMPower(MainboardType myMainboard){
		
		double totalRAMPower=0.0;		
		int totalRAMNumber=0;
		double totalRAMSize=0.0;
		int counter=0;
		double factor=0.0;
		double firstRAMFactor=0.0;
		double remainingRAMFactor =0.0;
		double memoryUsage = 0.0;
		
		if(myMainboard.getMemoryUsage()!= null)			
			memoryUsage = myMainboard.getMemoryUsage().getValue();
		
		JXPathContext context = JXPathContext.newContext(myMainboard);
		String ramQuery = "RAMStick";
		Iterator ramPowerIterator = context.iterate(ramQuery);
		
		/**
		 * Compute the total size of the RAM and number of RAMSticks
		 */
		while(ramPowerIterator.hasNext())
		{
			RAMStickType myRAM = (RAMStickType)ramPowerIterator.next();			
			totalRAMSize = totalRAMSize + myRAM.getSize().getValue();
			totalRAMNumber = totalRAMNumber+1;
		} 
		
		
		if(totalRAMSize !=0 )
		{
			/** check whether the CPU is in idle mode or not			 
			 */
								
			Iterator<CPUType> cpuIterator = myMainboard.getCPU().iterator();
			boolean CPUIdle = true;
			
			while(cpuIterator.hasNext())
			{
				
				CPUType myCPU = (CPUType)cpuIterator.next();
				if(myCPU.getCpuUsage()!= null && myCPU.getCpuUsage().getValue()> 0)
					CPUIdle = false;
				else if(myCPU.getCpuUsage()== null || myCPU.getCpuUsage().getValue()== 0){
					Iterator<CoreType> coreIterator = myCPU.getCore().iterator();
					
					while(coreIterator.hasNext()){
					CoreType myCore = (CoreType)coreIterator.next();
					if(myCore.getCoreLoad()!= null && myCore.getCoreLoad().getValue() >0)					
						CPUIdle = false;
						
						
					}// end of while(coreIterator.hasNext()) 
					
					
				}// end of while(cpuIterator.hasNext()							  
				
			}
			
			
			/**
			 * Probabilistic approach based on the total available RAM size and its current memory usage
			 */
			if(memoryUsage == 0.0 || CPUIdle)
				factor = 0.0;			
			else
				factor = memoryUsage/totalRAMSize;
		
		//Give the higher probability of performing R/W operations to the first RAM due to the fact that OS resides there 
		firstRAMFactor = factor/2;
		
		//Distribute uniformly the other probabilities among the other RAMs 
		remainingRAMFactor = firstRAMFactor/(totalRAMNumber-1);
		}
		else 
		return 0.0;
	    	
		/**
		 *  Iterate again the RAM objects to compute the power consumption
		 */
		context = JXPathContext.newContext(myMainboard);
		ramQuery = "RAMStick";
		ramPowerIterator = context.iterate(ramQuery);			
		
		while(ramPowerIterator.hasNext())
		{
			RAMStickType myRAM = (RAMStickType)ramPowerIterator.next();
			PoweredRamStick myLoadedRAM = null;
			
			if(counter == 0)
			myLoadedRAM = new PoweredRamStick(myRAM.getFrequency(),myRAM.getBufferType(),myRAM.getSize(),myRAM.getVendor(),myRAM.getType(),firstRAMFactor, myRAM.getVoltage());
			
			else
			myLoadedRAM = new PoweredRamStick(myRAM.getFrequency(),myRAM.getBufferType(),myRAM.getSize(),myRAM.getVendor(),myRAM.getType(),remainingRAMFactor, myRAM.getVoltage());	
			
			double ramPower = myLoadedRAM.computePower();
			
			PowerType ramPow = new PowerType();
			ramPow.setValue(ramPower);
			myRAM.setComputedPower(ramPow);
			totalRAMPower = totalRAMPower + ramPower; 
		    log.debug("The power consumption of the RAM is "+ramPower+ " Watts"); 
		    counter++;
		} 
		return totalRAMPower;
	}
	
	
	/**	 
	 * Computes the current power consumption of the central processing units
	 * 
	 * @param obj:input object of type Mainboard
	 * @return double value containing information on power consumption of the central processing units
	 */
    private double CPUPower(MainboardType obj, OperatingSystemTypeType operatingSystem){
		
		JXPathContext context = JXPathContext.newContext(obj);
		double totalCPUPower= 0.0;
		String myQuery = "CPU";
		
		Iterator cpuPowerIterator = context.iterate(myQuery);		
		
			while(cpuPowerIterator.hasNext())
			{
				
				CPUType myCPU = (CPUType)cpuPowerIterator.next();
				PoweredCPU myPoweredCPU = new PoweredCPU(myCPU,operatingSystem);		
				double cpuPower = myPoweredCPU.computePower();
				
				PowerType cpuPow = new PowerType();
				cpuPow.setValue(cpuPower);
				myCPU.setComputedPower(cpuPow);
				log.debug("The power consumption of the CPU is "+cpuPower+ " Watts");
				
				totalCPUPower = totalCPUPower+cpuPower;					  
				
			}
		
		return totalCPUPower;
	}

	
    /**	 
	 * Computes the current power consumption of the storage units
	 * 
	 * @param obj:input object of type Mainboard, RAID, SAN
	 * @return double value containing information on power consumption of the storage units
	 */
	private double StoragePower(Object obj){
		
		double totalStoragePower=0.0;
        JXPathContext context = JXPathContext.newContext(obj);              
		String storageQuery = "hardDisk";
	    Iterator storagePowerIterator = context.iterate(storageQuery);
	    
        while(storagePowerIterator.hasNext())
        {
        	HardDiskType myHardDisk = (HardDiskType)storagePowerIterator.next();
        	double storageCurrentPower = 0.0;
      	    double totalCachePower = CachePower(myHardDisk);
      	    log.debug("The power consumption of the Hard Disk Cache is "+totalCachePower+ " Watts");
      	    
      	    if(myHardDisk.getMaxReadRate().getValue()+ myHardDisk.getMaxWriteRate().getValue()>0)
      	    {
      	    	
      	    	if(myHardDisk.getReadRate() == null){
      	    		IoRateType ioRate = new IoRateType();
      	    		ioRate.setValue(0.0);      	    		
      	    		myHardDisk.setReadRate(ioRate);
      	    	}
      	    	if(myHardDisk.getWriteRate() == null){
      	    		IoRateType ioRate = new IoRateType();
      	    		ioRate.setValue(0.0);
      	    		myHardDisk.setWriteRate(ioRate);
      	    	}
      	    	PoweredHardDiskDrive myPoweredHardDisk = new PoweredHardDiskDrive(myHardDisk.getReadRate(),myHardDisk.getMaxReadRate(),myHardDisk.getWriteRate(),myHardDisk.getMaxWriteRate(),myHardDisk.getPowerIdle());	
      	    	storageCurrentPower = myPoweredHardDisk.computePower()+ totalCachePower;
      	    }
            
      	    PowerType storageCurrentPow = new PowerType();
      	    storageCurrentPow.setValue(storageCurrentPower);
            myHardDisk.setComputedPower(storageCurrentPow);
            totalStoragePower = totalStoragePower + storageCurrentPower; 
            log.debug("The power consumption of the Hard Disk Unit is "+storageCurrentPower+ " Watts");               
         }	
		
        storageQuery = "solidStateDisk";
	    storagePowerIterator = context.iterate(storageQuery);	      		  
        while(storagePowerIterator.hasNext())
        {
        	SolidStateDiskType mySolidStateDisk = (SolidStateDiskType)storagePowerIterator.next();
      	    double totalCachePower = CachePower(mySolidStateDisk);
      	    log.debug("The power consumption of the Solid State Disk Cache is "+totalCachePower+ " Watts"); 
            double storageCurrentPower = mySolidStateDisk.getPowerMax().getValue()+ totalCachePower;
            
            PowerType storageCurrentPow = new PowerType();
      	    storageCurrentPow.setValue(storageCurrentPower);
            mySolidStateDisk.setComputedPower(storageCurrentPow);
            totalStoragePower = totalStoragePower + storageCurrentPower; 
            log.debug("The power consumption of the Solid State Disk is "+storageCurrentPower+ " Watts");               
         }	
		return totalStoragePower;
		
		
	}

	/**	 
	 * Computes the current power consumption of the caches
	 * 
	 * @param obj:input object of type HardwareRAID, Storage Unit, Core and CPU
	 * @return double value containing information on power consumption of the caches
	 */
	private double CachePower(Object obj){
		
        double totalCachePower=0;
        JXPathContext context = JXPathContext.newContext(obj);              
		String cacheQuery = "cache";
		Iterator cachePowerIterator = context.iterate(cacheQuery);
		//log.debug("Inside the cache"); 
        while(cachePowerIterator.hasNext())
        {
      	  CacheType myCache = (CacheType)cachePowerIterator.next();
      	  //TODO: In the future, implement the power consumption function for the Cache
          double cacheCurrentPower = 0.0;
          
          PowerType cacheCurrentPow = new PowerType();
          cacheCurrentPow.setValue(cacheCurrentPower);
          myCache.setComputedPower(cacheCurrentPow);
          totalCachePower = totalCachePower + cacheCurrentPower; 
            log.debug("The power consumption of the Cache is "+cacheCurrentPower+ " Watts");               
         }
        
        return totalCachePower;
		
		
	}
	
	public PowerData computePowerSAN(RackType obj){
		
		double power = SANPower(obj, 0);
		PowerData powerData = new PowerData();
  	    powerData.setActualConsumption(power);
  		return powerData;

	}
	
	public PowerData computePowerIdleSAN(RackType obj){

		double power = SANPower(obj, 1);
		PowerData powerData = new PowerData();
  	    powerData.setActualConsumption(power);
  		return powerData;
	}//end of computePowerIdleSAN method
	
	public PowerData computePowerNAS(NASType obj){
		
		double powerNAS = NASPower(obj, 0);
		
		//Set the computed power to the NAS
		PowerType nasPower=new PowerType();
		nasPower.setValue(powerNAS);
		obj.setComputedPower(nasPower);
				
		PowerData powerData = new PowerData();
  	    powerData.setActualConsumption(powerNAS);
  		return powerData;
	}//end of computePowerNAS method

	public PowerData computePowerIdleNAS(NASType obj){
		
		double powerNAS = NASPower(obj, 1);
		
		//Set the computed power to the NAS
		PowerType nasPower=new PowerType();
		nasPower.setValue(powerNAS);
		obj.setComputedPower(nasPower);
				
		PowerData powerData = new PowerData();
  	    powerData.setActualConsumption(powerNAS);
  		return powerData;
	}//end of computePowerIdleNAS method

	/**	 
	 * Computes the current power consumption of the storage area network devices
	 * 
	 * @param obj:input object of type Rack
	 * @return double value containing information on power consumption of the storage area network devices
	 */
	private double SANPower(RackType obj, int idleFlag){

		JXPathContext contextSAN = JXPathContext.newContext(obj);
		String myQuery = "SAN";
		Iterator SANPowerIterator = contextSAN.iterate(myQuery);
		JXPathContext contextRAID = null;
		double totalSANPower=0.0;
		
		while(SANPowerIterator.hasNext())
		{	

			SANType san = (SANType)SANPowerIterator.next();
			PoweredSAN mySAN = new PoweredSAN(san, simulationFlag);
			double sanCurrenPower = 0.0;

			if (mySAN.getMeasuredPower()!=null && mySAN.getMeasuredPower().getValue() > 0) sanCurrenPower = mySAN.getMeasuredPower().getValue();
			else	sanCurrenPower = (idleFlag == 1) ? mySAN.computePowerIdle() : mySAN.computePower();

      		PowerType sanCurrentPow = new PowerType();
      		sanCurrentPow.setValue(sanCurrenPower);
      		mySAN.setComputedPower(sanCurrentPow);
			totalSANPower= totalSANPower + sanCurrenPower;
		}
	
		return totalSANPower;
		
	}//end of SANPower method

	 /**	 
	 * Computes the current power consumption of the network-attached storage devices
	 * 
	 * @param obj:input object of type NAS
	 * @param idleFlag: takes a value of 1 if we want to compute the idle power, otherwise it has a value of 1
	 * @return double value containing information on power consumption of the network-attached storage devices
	 */
	private double NASPower(NASType nasObj, int idleFlag){		   	
			
		double totalNASPower=0.0;
		
		if (nasObj.getMeasuredPower()!=null && nasObj.getMeasuredPower().getValue() > 0) totalNASPower = nasObj.getMeasuredPower().getValue();
		else
		{
			PoweredNAS myNAS = new PoweredNAS(nasObj, simulationFlag);
			totalNASPower =(idleFlag == 1) ? myNAS.computePowerIdle() : myNAS.computePower();
			
			
		}
		      		
	
		return totalNASPower;
		
	}//end of NASPower method	
	
	/**	 
	 * Computes the current power consumption of the all NAS devices
	 * 
	 * @param obj:input object of type NAS
	 * @param idleFlag: takes a value of 1 if we want to compute the idle power, otherwise it has a value of 1
	 * @return double value containing information on power consumption of the network-attached storage devices
	 */
	private double OverallNASPower(RackType rack, int idleFlag){
		
		double totalNASPower=0.0;  
		JXPathContext context = JXPathContext.newContext(rack);
		String nasQuery = "NAS";
	      
	      Iterator nasPowerIterator = context.iterate(nasQuery);		 
	      while(nasPowerIterator.hasNext())
	      	{
	    	    double nasCurrentPower = 0.0;
	      		NASType myNAS = (NASType)nasPowerIterator.next();	      		
	      		nasCurrentPower = NASPower(myNAS,idleFlag); 
	      		
	      		PowerType nasCurrentPow = new PowerType();
	      		nasCurrentPow.setValue(nasCurrentPower);
	      		myNAS.setComputedPower(nasCurrentPow);
	      		totalNASPower = totalNASPower + nasCurrentPower; 
	      		log.debug("The power consumption of NAS is "+nasCurrentPower+ " Watts");               
	      	}	
		
		return totalNASPower;
	}

	/**	 
	 * Computes the current power consumption of the network interface cards
	 * 
	 * @param obj:input object of type Mainboard, SAN, and Enclosure
	 * @return double value containing information on power consumption of the network interface cards
	 */	
	private double NICPower(Object obj){
	  
	  double totalNICPower=0.0;  
	  JXPathContext context = JXPathContext.newContext(obj);
	  String nicQuery = "NIC";
      
      Iterator nicPowerIterator = context.iterate(nicQuery);		 
      while(nicPowerIterator.hasNext())
      	{
    	  double nicCurrentPower = 0.0;
      		NICType myNIC = (NICType)nicPowerIterator.next();
      		PoweredNetworkNode myNetworkNode = new PoweredNetworkNode(myNIC);
      		nicCurrentPower = myNetworkNode.computePower(); 
      		
      		PowerType nicCurrentPow = new PowerType();
      		nicCurrentPow.setValue(nicCurrentPower);
      		myNIC.setComputedPower(nicCurrentPow);
      		totalNICPower = totalNICPower + nicCurrentPower; 
      		log.debug("The power consumption of the Network Interface Card is "+nicCurrentPower+ " Watts");               
      	}
    //Ethernet NIC  power
      nicQuery = "EthernetNIC";
     
     nicPowerIterator = context.iterate(nicQuery);		 
     while(nicPowerIterator.hasNext())
     	{
   	    	double nicCurrentPower = 0.0;
     		NICType myNIC = (NICType)nicPowerIterator.next();
     		PoweredNetworkNode myNetworkNode = new PoweredNetworkNode(myNIC);
      		nicCurrentPower = myNetworkNode.computePower(); 
     		      		
     		PowerType nicCurrentPow = new PowerType();
     		nicCurrentPow.setValue(nicCurrentPower);
     		myNIC.setComputedPower(nicCurrentPow);
     		totalNICPower = totalNICPower + nicCurrentPower; 
     		log.debug("The power consumption of the Ethernet Network Interface Card is "+nicCurrentPower+ " Watts");               
     	}
     
   //Fiber channel NIC  power
     nicQuery = "FiberchannelNIC";
    
    nicPowerIterator = context.iterate(nicQuery);		 
    while(nicPowerIterator.hasNext())
    	{
  	    	double nicCurrentPower = 0.0;
    		NICType myNIC = (NICType)nicPowerIterator.next();
    		PoweredNetworkNode myNetworkNode = new PoweredNetworkNode(myNIC);
      		nicCurrentPower = myNetworkNode.computePower(); 
    		      		
    		PowerType nicCurrentPow = new PowerType();
    		nicCurrentPow.setValue(nicCurrentPower);
    		myNIC.setComputedPower(nicCurrentPow);
    		totalNICPower = totalNICPower + nicCurrentPower; 
    		log.debug("The power consumption of the Fiber Channel Network Interface Card is "+nicCurrentPower+ " Watts");               
    	}
	  return totalNICPower;
  }
	
	/**  
	* Computes the current power consumption of the network interface cards
	* 
	* @param obj:input object of type Mainboard, SAN, and Enclosure
	* @return double value containing information on power consumption of the network interface cards
	*/
	private double NICIdlePower(Object obj){
		
		double totalNICPower=0.0;  
		JXPathContext context = JXPathContext.newContext(obj);
		String nicQuery = "NIC";
		
		Iterator nicPowerIterator = context.iterate(nicQuery);  
	    
		while(nicPowerIterator.hasNext()) {
			double nicCurrentPower = 0.0;
			NICType myNIC = (NICType)nicPowerIterator.next();
			nicCurrentPower=myNIC.getPowerIdle().getValue();
	      
			PowerType nicCurrentPow = new PowerType();
			nicCurrentPow.setValue(nicCurrentPower);
			myNIC.setComputedPower(nicCurrentPow);
			totalNICPower = totalNICPower + nicCurrentPower; 
	      
			log.debug("The idle power consumption of the Network Interface Card is "+nicCurrentPower+ " Watts");
		}
	      
	      
		//Ethernet NIC idle power
		nicQuery = "EthernetNIC"; 
		nicPowerIterator = context.iterate(nicQuery);  
		while(nicPowerIterator.hasNext()) {
	       
			double nicCurrentPower = 0.0;
			NICType myNIC = (NICType)nicPowerIterator.next();
			nicCurrentPower=myNIC.getPowerIdle().getValue();
			
			PowerType nicCurrentPow = new PowerType();
			nicCurrentPow.setValue(nicCurrentPower);
			myNIC.setComputedPower(nicCurrentPow);
			totalNICPower = totalNICPower + nicCurrentPower; 
	      
			log.debug("The idle power consumption of the Ethernet Network Interface Card is "+nicCurrentPower+ " Watts");               
	      
		}
	      
	    //Fiber channel NIC idle power
		nicQuery = "FiberchannelNIC";
		nicPowerIterator = context.iterate(nicQuery);  
		while(nicPowerIterator.hasNext()) {
	       
			double nicCurrentPower = 0.0;
			NICType myNIC = (NICType)nicPowerIterator.next();
			nicCurrentPower=myNIC.getPowerIdle().getValue();
			
			PowerType nicCurrentPow = new PowerType();
			nicCurrentPow.setValue(nicCurrentPower);	      
			myNIC.setComputedPower(nicCurrentPow);	      
			totalNICPower = totalNICPower + nicCurrentPower; 
			
			log.debug("The idle power consumption of the Fiber Channel Network Interface Card is "+nicCurrentPower+ " Watts");               
		}
		return totalNICPower;  
	}
	
	/**	 
	 * Computes the current power consumption of the power supply units
	 * The efficiency of the PSU should be given in percentage form with respect to the corresponding load
	 * @param obj:input object of type SAN, NetworkNode, Enclosure, RackableServer and TowerServer 
	 * @return double value containing information on power consumption of the power supply units
	 */	
	private double PSUPower(double measuredPower, double efficiency){		
			
			double PSUPower =0.0;
					
			PSUPower=(measuredPower*(100-efficiency))/100;			
				
			return PSUPower;		
		
	}
	
	
	/**	 
	 * Computes the current power consumption of the power distribution units
	 * 
	 * @param obj:input object of type Rack 
	 * @return double value containing information on power consumption of the power distribution units
	 */	
	private double PDUPower(RackType obj){
		
		double totalPDUPower=0;	
		JXPathContext context = JXPathContext.newContext(obj);
		String pduQuery = "PDU";
		Iterator pduPowerIterator = context.iterate(pduQuery);
	
		while(pduPowerIterator.hasNext())
		{
			PDUType myPDU = (PDUType)pduPowerIterator.next();
			//TODO: implementation of the formula for the PDUs
			double pduCurrentPower = myPDU.getMeasuredPower().getValue();
			
			PowerType pduCurrentPow = new PowerType();
			pduCurrentPow.setValue(pduCurrentPower);
			myPDU.setComputedPower(pduCurrentPow);			
			totalPDUPower = totalPDUPower+pduCurrentPower;       
			log.debug("The power consumption of the PDU is "+pduCurrentPower+ " Watts");       
		} 
					
		return totalPDUPower;
	}
	
	private double PSUNetworkNodePower(NetworkNodeType networkNode, double countedPower){
		
		double totalPSUPower=0.0;
        double countPSU=0.0;
        boolean measuredPowerPSU = true;
        JXPathContext context = JXPathContext.newContext(networkNode);
    	String psuQuery = "PSU";
    	Iterator psuPowerIterator = context.iterate(psuQuery);

    	/**
		* Testing whether all PSUs have measured power provided. We assume that either all of them have the measured power configured or none of them. Also, count the PSUs that have a non zero load 
		* 
		*
		* 
		*/	
    	while(psuPowerIterator.hasNext())
    	{
    		PSUType myPSU = (PSUType)psuPowerIterator.next();
    	    if(myPSU.getMeasuredPower() == null ||myPSU.getMeasuredPower().getValue()<=0)
    	    	measuredPowerPSU = false;
    	    if(myPSU.getLoad().getValue() > 0.0)
    	    	//loadPSU = true;
    	    	countPSU++;
    	    
    	}
    	
    	context = JXPathContext.newContext(networkNode);
    	psuPowerIterator = context.iterate(psuQuery);
    	while(psuPowerIterator.hasNext())
    	{
    		double psuCurrentPower = 0.0;
    		PSUType myPSU = (PSUType)psuPowerIterator.next();
    		
    		/**
    		 * The monitoring system
    		 * can either provide the measuredPower for every PSU or no information is provided about any PSU inside the same server.
    		 * 
    		 */
    	if(myPSU.getLoad().getValue()>0)
    	{	
    		if(!simulationFlag && measuredPowerPSU)
    		{
    			psuCurrentPower = PSUPower(myPSU.getMeasuredPower().getValue(),myPSU.getEfficiency().getValue());
    			
    			PowerType psuCurrentPow = new PowerType();
    			psuCurrentPow.setValue(psuCurrentPower);
    			log.debug("The power consumption of the PSU is "+psuCurrentPow.getValue()+ " Watts");
    			myPSU.setComputedPower(psuCurrentPow);
    			totalPSUPower = totalPSUPower+psuCurrentPower;
    			
    		}
    		/**
    		 * If the measuredPower at the server level is provided by the monitoring system, then this measuredPower is evenly distrbiuted 
    		 * among all the PSUs inside the same server 
    		 * 
    		 */
    		else if(!simulationFlag && networkNode.getMeasuredPower() != null && networkNode.getMeasuredPower().getValue()>0){    			
    			
    			psuCurrentPower= PSUPower(networkNode.getMeasuredPower().getValue()/countPSU,myPSU.getEfficiency().getValue());
    			
    			PowerType psuCurrentPow = new PowerType();
    			psuCurrentPow.setValue(psuCurrentPower);
    			log.debug("The power consumption of the PSU is "+psuCurrentPow.getValue()+ " Watts");
    			myPSU.setComputedPower(psuCurrentPow);
    			totalPSUPower = totalPSUPower+psuCurrentPower;    			
    		}
    		else{
    			double measuredPower = countedPower/countPSU;
    			
    			if(myPSU.getEfficiency().getValue()>0)
    			psuCurrentPower = Math.round((measuredPower/myPSU.getEfficiency().getValue())*100)-Math.round(countedPower/countPSU);
    			
    			
    			PowerType psuCurrentPow = new PowerType();
    			psuCurrentPow.setValue(psuCurrentPower);
    			log.debug("The power consumption of the PSU is "+psuCurrentPow.getValue()+ " Watts");
    			myPSU.setComputedPower(psuCurrentPow);
    			totalPSUPower = totalPSUPower+psuCurrentPower;
    			
    		}
    		
    	}// end of PSU load >0
    			
    	}
    		
    	log.debug("The power consumption of the PSUs is "+totalPSUPower+ " Watts");
    	return totalPSUPower;		
		
		
	}

	/* (non-Javadoc)
	 * @see org.f4g.power.IPowerCalculator#dispose()
	 */
	@Override
	public boolean dispose() {
		// TODO Auto-generated method stub
		return false;
	}
	
}
