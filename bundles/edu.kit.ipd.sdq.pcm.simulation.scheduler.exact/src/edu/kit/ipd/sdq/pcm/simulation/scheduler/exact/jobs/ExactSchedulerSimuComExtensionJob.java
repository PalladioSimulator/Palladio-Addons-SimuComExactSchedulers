package edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.jobs;

import org.eclipse.core.runtime.IProgressMonitor;

import de.uka.ipd.sdq.codegen.simucontroller.workflow.jobs.AbstractSimuComExtensionJob;
import de.uka.ipd.sdq.simucomframework.core.model.SimuComModel;
import de.uka.ipd.sdq.simucomframework.core.resources.AbstractScheduledResource;
import de.uka.ipd.sdq.simucomframework.core.resources.AbstractSimulatedResourceContainer;
import de.uka.ipd.sdq.simucomframework.core.resources.SimulatedResourceContainer;
import de.uka.ipd.sdq.workflow.jobs.JobFailedException;
import de.uka.ipd.sdq.workflow.jobs.UserCanceledException;

public class ExactSchedulerSimuComExtensionJob extends AbstractSimuComExtensionJob {

    @Override
    public void execute(final IProgressMonitor monitor) throws JobFailedException,
    UserCanceledException {
        final SimuComModel simuComModel = getSimuComModel();
        for (final SimulatedResourceContainer simulatedResourceContainer : simuComModel.getResourceRegistry().getSimulatedResourceContainers()) {
            for (final AbstractScheduledResource resource : simulatedResourceContainer.getActiveResources()) {
                if (resource.getSchedulingStrategyID().startsWith("edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.")) {
                    final AbstractSimulatedResourceContainer container = simuComModel.getResourceRegistry().removeResourceContainerFromRegistry(simulatedResourceContainer.getResourceContainerID());
                    final ResourceContainerWrapper resourceContainerWrapper = new ResourceContainerWrapper(simuComModel, container.getResourceContainerID(), container, resource.getName(), resource.getSchedulingStrategyID());
                    simuComModel.getResourceRegistry().addResourceContainer(resourceContainerWrapper);
                }
            }
        }
    }

    @Override
    public String getName() {
        return "Simucom Exact Scheduler Extension Job";
    }

}
