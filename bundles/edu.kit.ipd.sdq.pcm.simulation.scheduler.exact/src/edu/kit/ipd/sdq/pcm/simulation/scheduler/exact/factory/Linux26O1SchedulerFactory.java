package edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.factory;

import de.uka.ipd.sdq.scheduler.IActiveResource;
import de.uka.ipd.sdq.scheduler.SchedulerModel;
import de.uka.ipd.sdq.scheduler.factory.SchedulerExtensionFactory;
import de.uka.ipd.sdq.scheduler.resources.active.IResourceTableManager;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.ExactSchedulingFactory;


public class Linux26O1SchedulerFactory implements SchedulerExtensionFactory {

    @Override
    public IActiveResource getExtensionScheduler(final SchedulerModel model, final String resourceName, final String resourceId, final long numberOfCores
            , IResourceTableManager resourceTableManager) {
        final ExactSchedulingFactory factory = new ExactSchedulingFactory(model);
        return factory.getResource(model, "Linux 2.6.22", numberOfCores, "Utilisation of " + resourceName + " " + resourceId, resourceTableManager);
    }

}
