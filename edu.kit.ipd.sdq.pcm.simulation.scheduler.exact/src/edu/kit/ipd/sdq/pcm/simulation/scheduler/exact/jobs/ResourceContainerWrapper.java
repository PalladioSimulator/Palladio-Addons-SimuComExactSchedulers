package edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.jobs;

import de.uka.ipd.sdq.pcm.core.composition.AssemblyContext;
import de.uka.ipd.sdq.pcm.repository.PassiveResource;
import de.uka.ipd.sdq.scheduler.IActiveResource;
import de.uka.ipd.sdq.scheduler.IPassiveResource;
import de.uka.ipd.sdq.simucomframework.SimuComSimProcess;
import de.uka.ipd.sdq.simucomframework.model.SimuComModel;
import de.uka.ipd.sdq.simucomframework.resources.AbstractSimulatedResourceContainer;
import de.uka.ipd.sdq.simucomframework.resources.CalculatorHelper;
import de.uka.ipd.sdq.simucomframework.resources.SimulatedResourceContainer;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.SimActiveResource;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.priority.IPriorityBoost;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.priority.IPriorityUpdateStrategy;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.priority.boost.StaticPriorityBoost;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.priority.update.DecayToBaseUpdate;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.resources.passive.SimFairPassiveResource;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.resources.passive.SimUnfairPassiveResource;

public class ResourceContainerWrapper extends SimulatedResourceContainer {


    public static final String SCHEDULING_STRATEGY_EXACT_WINXP = "edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.winxp";
    public static final String SCHEDULING_STRATEGY_EXACT_WIN7 = "edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.win7";
    public static final String SCHEDULING_STRATEGY_EXACT_WINVISTA = "edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.winvista";
    public static final String SCHEDULING_STRATEGY_EXACT_WINSERVER2003 = "edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.winserver2003";
    public static final String SCHEDULING_STRATEGY_EXACT_LINUX26O1 = "edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.linux26o1";
    public static final String SCHEDULING_STRATEGY_EXACT_LINUXCFS= "edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.linux26cfs";

    AbstractSimulatedResourceContainer originalResourceContainer = null;

    private String cpuSchedulingStrategy = null;
    private String cpuResourceId = null;

    public ResourceContainerWrapper(final SimuComModel myModel, final String myContainerID, final AbstractSimulatedResourceContainer originalResourceContainer, final String cpuResourceId, final String cpuSchedulingStrategy) {
        super(myModel, myContainerID,
                ((SimulatedResourceContainer)originalResourceContainer).getNestedResourceContainers(),
                ((SimulatedResourceContainer)originalResourceContainer).getParentResourceContainer());
        this.cpuSchedulingStrategy = cpuSchedulingStrategy;
        this.cpuResourceId = cpuResourceId;
        this.originalResourceContainer = originalResourceContainer;
        this.activeResources = originalResourceContainer.getAllActiveResources();
        /*for (Entry<String,AbstractScheduledResource> r : this.activeResources.entrySet()) {
			r.getValue().addDemandListener(new IDemandListener() {
				public void demand(double demand) {
		            	// Do nothing.
		        }

				public void demandCompleted(ISchedulableProcess thread) {
					consumeResourceCompleted(thread);
				}
			});
		}*/
    }

    @Override
    public void loadActiveResource(final SimuComSimProcess requestingProcess, final String typeID, final double demand) {
        originalResourceContainer.loadActiveResource(requestingProcess, typeID, demand);
    }

    @Override
    public void loadActiveResource(final SimuComSimProcess requestingProcess, final String providedInterfaceID, final int resourceServiceID, final double demand) {
        originalResourceContainer.loadActiveResource(requestingProcess, providedInterfaceID, resourceServiceID, demand);
    }

    @Override
    public IPassiveResource createPassiveResource(final PassiveResource resource,
            final AssemblyContext assemblyContext, final long capacity) {
        IPassiveResource r = null;
        if (cpuSchedulingStrategy.equals(SCHEDULING_STRATEGY_EXACT_WINXP) ||
                cpuSchedulingStrategy.equals(SCHEDULING_STRATEGY_EXACT_WIN7) ||
                cpuSchedulingStrategy.equals(SCHEDULING_STRATEGY_EXACT_WINVISTA) ||
                cpuSchedulingStrategy.equals(SCHEDULING_STRATEGY_EXACT_WINSERVER2003)) {
            r = getPassiveResourceWindows(
                    resource,
                    capacity, 1, true, true,
                    activeResources.get(cpuResourceId).getScheduledResource(),
                    assemblyContext);
            // setup calculators
            // FIXME! CalculatorHelper.setupStateCalculator(r, this.myModel);
            CalculatorHelper.setupWaitingTimeCalculator(r, this.myModel);
            CalculatorHelper.setupHoldTimeCalculator(r, this.myModel);
            return r;
        } else if (cpuSchedulingStrategy.equals(SCHEDULING_STRATEGY_EXACT_LINUX26O1)) {
            r = getPassiveResourceLinux(resource, capacity,
                    true, activeResources.get(cpuResourceId).getScheduledResource(),assemblyContext);
            // setup calculators
            // FIXME! CalculatorHelper.setupStateCalculator(r, this.myModel);
            CalculatorHelper.setupWaitingTimeCalculator(r, this.myModel);
            CalculatorHelper.setupHoldTimeCalculator(r, this.myModel);
            return r;
        } else {
            return super.createPassiveResource(resource, assemblyContext, capacity);
        }

    }

    private IPassiveResource getPassiveResourceWindows(final PassiveResource resource,
            final long capacity, final int bonus, final boolean resetTimeSlice, final boolean isFair,
            final IActiveResource managingResource, final AssemblyContext assemblyContext) {
        final IPriorityUpdateStrategy update = new DecayToBaseUpdate();
        final IPriorityBoost boost = new StaticPriorityBoost(update, bonus, 0,
                resetTimeSlice);

        if (isFair) {
            return new SimFairPassiveResource(myModel, capacity, resource, boost,
                    (SimActiveResource) managingResource, assemblyContext);
        } else {
            return new SimUnfairPassiveResource(myModel, capacity, resource, boost,
                    (SimActiveResource) managingResource, 0.1, true,assemblyContext);
        }
    }

    private IPassiveResource getPassiveResourceLinux(final PassiveResource resource,
            final long capacity, final boolean isFair, final IActiveResource managingResource, final AssemblyContext assemblyContext) {
        if (isFair) {
            return new SimFairPassiveResource(myModel, capacity, resource, null,
                    (SimActiveResource) managingResource, assemblyContext);
        } else {
            return new SimUnfairPassiveResource(myModel, capacity, resource, null,
                    (SimActiveResource) managingResource, 0.1, true, assemblyContext);
        }
    }

}
