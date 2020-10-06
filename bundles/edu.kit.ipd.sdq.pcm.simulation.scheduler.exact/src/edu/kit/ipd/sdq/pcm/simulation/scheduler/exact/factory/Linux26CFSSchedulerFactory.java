package edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.factory;

import de.uka.ipd.sdq.scheduler.IActiveResource;
import de.uka.ipd.sdq.scheduler.SchedulerModel;
import de.uka.ipd.sdq.scheduler.factory.SchedulerExtensionFactory;
import de.uka.ipd.sdq.scheduler.resources.active.IResourceTableManager;
import de.uka.ipd.sdq.scheduler.resources.active.SimProcessorSharingResource;

public class Linux26CFSSchedulerFactory implements SchedulerExtensionFactory {

    @Override
    public IActiveResource getExtensionScheduler(final SchedulerModel model, final String resourceName,
            final String resourceId, final long numberOfCores, IResourceTableManager resourceTableManager) {
        return new SimProcessorSharingResource(model, resourceName, resourceId, numberOfCores, resourceTableManager);
        // return new Linux26CFSResource(model, resourceName, resourceId);
    }

}
