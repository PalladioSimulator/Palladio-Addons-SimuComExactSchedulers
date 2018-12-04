package edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.resources.passive;

import de.uka.ipd.sdq.scheduler.ISchedulableProcess;
import de.uka.ipd.sdq.scheduler.processes.IWaitingProcess;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.processes.IActiveProcess;

public class WaitingProcess implements IWaitingProcess {

    private final IActiveProcess process;
    private final long num_requested;

    public WaitingProcess(final IActiveProcess process, final long num_requested) {
        super();
        this.process = process;
        this.num_requested = num_requested;
    }

    public IActiveProcess getActiveProcess() {
        return process;
    }

    @Override
    public long getNumRequested() {
        return num_requested;
    }

    @Override
    public ISchedulableProcess getProcess() {
        return process.getSchedulableProcess();
    }
}
