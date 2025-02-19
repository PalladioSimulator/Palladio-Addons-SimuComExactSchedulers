package edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.jobs;

import java.util.Map;

import de.uka.ipd.sdq.codegen.simucontroller.runconfig.SimuComExtensionConfigurationBuilder;
import de.uka.ipd.sdq.simucomframework.core.SimuComConfigExtension;
import de.uka.ipd.sdq.workflow.extension.AbstractExtensionJobConfiguration;

public class ExactSchedulerSimuComExtensionConfigurationBuilder  extends SimuComExtensionConfigurationBuilder {

    @Override
    public AbstractExtensionJobConfiguration buildConfiguration(final Map<String, Object> attributes) {
        return null;
    }

    @Override
    public SimuComConfigExtension deriveSimuComConfigExtension(final Map<String,Object> attributes) {
        return null;
    }


}
