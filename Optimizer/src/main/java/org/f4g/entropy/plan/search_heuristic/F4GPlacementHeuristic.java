

package org.f4g.entropy.plan.search_heuristic;

import choco.cp.solver.search.integer.branching.AssignOrForbidIntVarVal;
import choco.cp.solver.search.integer.branching.AssignVar;
import choco.cp.solver.search.integer.valselector.MinVal;
import choco.cp.solver.search.integer.varselector.StaticVarOrder;
import choco.kernel.solver.variables.integer.IntDomainVar;
import entropy.configuration.*;
import entropy.plan.choco.ReconfigurationProblem;
import entropy.plan.choco.actionModel.*;
import entropy.plan.choco.search.HosterVarSelector;
import entropy.plan.choco.search.NodeGroupSelector;
import entropy.plan.choco.search.StayFirstSelector2;
import entropy.plan.choco.search.VMGroupVarSelector;
import gnu.trove.TLongIntHashMap;
import org.apache.log4j.Logger;
import org.f4g.entropy.plan.F4GPlanner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A placement heuristic focused on each VM.
 * First place the VMs, then plan the changes.
 *
 * @author Fabien Hermenier
 */
public class F4GPlacementHeuristic implements F4GCorePlanHeuristic {

	public Logger log;
	
    /**
     * Make a new placement heuristic.
     *
     */
    public F4GPlacementHeuristic(){
    	log = Logger.getLogger(this.getClass().getName());
    }

    /**
     * To compare VMs in a descending order, wrt. their memory consumption.
     */
    private VirtualMachineComparator dsc = new VirtualMachineComparator(false, ResourcePicker.VMRc.memoryConsumption);

    @Override
    public void add(F4GPlanner plan) {
    	ReconfigurationProblem rp = plan.getModel();
        Configuration src = rp.getSourceConfiguration();
        rp.clearGoals();
        //Get the VMs to move
        ManagedElementSet<VirtualMachine> onBadNodes = new SimpleManagedElementSet<VirtualMachine>();
        ManagedElementSet<VirtualMachine> onGoodNodes = src.getRunnings().clone();
        ManagedElementSet<VirtualMachine> vmsToRun = src.getWaitings().clone();

        vmsToRun.removeAll(rp.getFutureWaitings());
                
        for (Node n : Configurations.futureOverloadedNodes(src)) {
            onBadNodes.addAll(src.getRunnings(n));
        }

        onBadNodes.addAll(src.getSleepings());
        onGoodNodes.removeAll(onBadNodes);

        Collections.sort(onGoodNodes, dsc);
        Collections.sort(onBadNodes, dsc);

        List<VirtualMachineActionModel> goodActions = rp.getAssociatedActions(onGoodNodes);
        List<VirtualMachineActionModel> badActions = rp.getAssociatedActions(onBadNodes);
        List<VirtualMachineActionModel> runActions = rp.getAssociatedActions(vmsToRun);

        
        //add heuristic for groups
        //addVMGroup(rp);

        ManagedElementSet<VirtualMachine> relocalisables = plan.getModel().getFutureRunnings();
	    TLongIntHashMap oldLocation = new TLongIntHashMap(relocalisables.size());
	     
        for (VirtualMachine vm : relocalisables) {
            int idx = rp.getVirtualMachine(vm);
            VirtualMachineActionModel a = rp.getAssociatedVirtualMachineAction(idx);
            if (a.getClass() == MigratableActionModel.class || a.getClass() == ResumeActionModel.class || a.getClass() == ReInstantiateActionModel.class) {
                oldLocation.put(a.getDemandingSlice().hoster().getIndex(), rp.getCurrentLocation(idx));
            }
        }
        
        //add heuritic for fixing broken constraints
        addStayFirst(plan, badActions, oldLocation);
        
        //add heuritic for runs
        addStayFirst(plan, runActions, oldLocation);
        
        addEnergyPacking(rp, oldLocation, plan, goodActions);

    
        ///SCHEDULING PROBLEM
        List<ActionModel> actions = new ArrayList<ActionModel>();
        for (VirtualMachineActionModel vma : rp.getVirtualMachineActions()) {
            actions.add(vma);
        }


        rp.addGoal(new AssignOrForbidIntVarVal(new PureIncomingFirst2(plan, rp, actions), new MinVal()));

        
        EmptyNodeVarSelector selectShutdown = new EmptyNodeVarSelector(rp, rp.getSourceConfiguration().getAllNodes());
        rp.addGoal(new AssignVar(selectShutdown, new MinVal()));

        rp.addGoal(new AssignVar(new StaticVarOrder(rp, new IntDomainVar[]{rp.getEnd()}), new MinVal()));

    }

	private void addEnergyPacking(ReconfigurationProblem rp, TLongIntHashMap oldLocation, F4GPlanner plan, List<VirtualMachineActionModel> goodActions) {
		
        //consolidate first: move VMs to low load nodes to high load nodes
        rp.addGoal(new SimpleVMPacking(rp, rp.getSourceConfiguration().getAllNodes()) ); //new AssignVar(selectVM, selectServer));
		
        //add heuritic for remaining VMs
        addStayFirst(plan, goodActions, oldLocation);
	}

	private void addStayFirst(F4GPlanner plan, List<VirtualMachineActionModel> actions, TLongIntHashMap oldLocation) {
		 	        
		HosterVarSelector select = new HosterVarSelector(plan.getModel(), ActionModels.extractDemandingSlices(actions));
		plan.getModel().addGoal(new AssignVar(select, new StayFirstSelector2(plan.getModel(), oldLocation, plan.getPackingConstraintClass(), StayFirstSelector2.Option.bfMem)));
	}

//	private void addInGroupAction(ReconfigurationProblem rp) {
//		for (ManagedElementSet<VirtualMachine> vms : rp.getVMGroups()) {
//		    ManagedElementSet<VirtualMachine> sorted = vms.clone();
//		    Collections.sort(sorted, dsc);
//		    List<VirtualMachineActionModel> inGroupActions = rp.getAssociatedActions(sorted);
//		    addStayFirst(rp, inGroupActions);
//		}
//	}

//	private void addExclusion(ReconfigurationProblem rp) {
//		//Get the VMs to move for exclusion issue
//		ManagedElementSet<VirtualMachine> vmsToExlude = rp.getSourceConfiguration().getAllVirtualMachines().clone();
//		Collections.sort(vmsToExlude, dsc);
//		rp.addGoal(new AssignVar(new ExcludedVirtualMachines(rp, rp.getSourceConfiguration(), vmsToExlude), new StayFirstSelector2(rp, rp.getSatisfyDSlicesHeightConstraint(), StayFirstSelector2.Option.wfMem)));
//	}

	private void addVMGroup(ReconfigurationProblem rp) {
		//Go for the VMgroup variable
        VMGroupVarSelector vmGrp = new VMGroupVarSelector(rp);
        rp.addGoal(new AssignVar(vmGrp, new NodeGroupSelector(rp, NodeGroupSelector.Option.bfMem)));
	}
}
