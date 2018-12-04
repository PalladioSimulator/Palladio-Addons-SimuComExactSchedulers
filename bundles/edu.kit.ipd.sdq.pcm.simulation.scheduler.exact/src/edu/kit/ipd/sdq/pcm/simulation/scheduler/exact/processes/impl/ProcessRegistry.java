package edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.processes.impl;

import java.util.Hashtable;

import de.uka.ipd.sdq.scheduler.ISchedulableProcess;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.SimActiveResource;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.processes.IActiveProcess;


public class ProcessRegistry {

    private final Hashtable<ISchedulableProcess, IActiveProcess> registry;
    //	private SimActiveResource resource;

    public ProcessRegistry(final SimActiveResource resource) {
        super();
        this.registry = new Hashtable<ISchedulableProcess, IActiveProcess>();
        //		this.resource = resource;
    }

    public void registerProcess(final IActiveProcess process){
        registry.put(process.getSchedulableProcess(), process);
        //		process.getSchedulableProcess().setMainResource(resource);
    }

    public IActiveProcess lookUp(final ISchedulableProcess process){
        return registry.get(process);
    }

    public void unregisterProcess(final ISchedulableProcess process){
        registry.remove(process);
    }

    public IActiveProcess findProcess(final String processName) {
        for (final IActiveProcess p: registry.values()){
            if (p.getName().startsWith(processName)) {
                return p;
            }
        }
        assert false : "Cannot find process: " + processName;
        return null;
    }

    public boolean isRegistered(final IActiveProcess p) {
        return registry.containsKey(p.getSchedulableProcess());
    }

}
