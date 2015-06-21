package edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.factory;

import de.uka.ipd.sdq.scheduler.IActiveResource;
import de.uka.ipd.sdq.scheduler.SchedulerModel;
import de.uka.ipd.sdq.scheduler.factory.SchedulerExtensionFactory;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.ExactSchedulingFactory;


public class Windows7SchedulerFactory implements SchedulerExtensionFactory {

    @Override
    public IActiveResource getExtensionScheduler(final SchedulerModel model, final String resourceName, final String resourceId, final long numberOfCores) {
        final ExactSchedulingFactory factory = new ExactSchedulingFactory(model);
        return factory.getResource(model, "Windows 2003", numberOfCores, "Utilisation of " + resourceName + " " + resourceId);
    }
}
