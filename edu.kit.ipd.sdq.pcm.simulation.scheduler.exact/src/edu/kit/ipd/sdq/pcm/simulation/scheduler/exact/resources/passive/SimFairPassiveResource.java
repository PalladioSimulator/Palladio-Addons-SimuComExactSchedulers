package edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.resources.passive;

import org.palladiosimulator.pcm.core.composition.AssemblyContext;
import org.palladiosimulator.pcm.repository.PassiveResource;

import de.uka.ipd.sdq.scheduler.IRunningProcess;
import de.uka.ipd.sdq.scheduler.ISchedulableProcess;
import de.uka.ipd.sdq.scheduler.LoggingWrapper;
import de.uka.ipd.sdq.scheduler.SchedulerModel;
import de.uka.ipd.sdq.scheduler.resources.passive.PassiveResourceObservee;
import de.uka.ipd.sdq.scheduler.sensors.IPassiveResourceSensor;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.IResourceInstance;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.SimActiveResource;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.priority.IPriorityBoost;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.processes.IActiveProcess;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.processes.impl.PreemptiveProcess;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.processes.impl.ProcessWithPriority;

public class SimFairPassiveResource extends SimAbstractPassiveResource {

    private final PassiveResourceObservee observee;
    private long available;

    public SimFairPassiveResource(
            final SchedulerModel model,
            final long capacity,
            final PassiveResource resource,
            final IPriorityBoost priority_boost,
            final SimActiveResource managing_resource, final AssemblyContext assemblyContext
            ) {
        super(model, capacity, resource, priority_boost, managing_resource,assemblyContext);
        observee = new PassiveResourceObservee();
        available = capacity;
    }

    private boolean canProceed(final IRunningProcess process, final long num) {
        return (waiting_queue.isEmpty() || ((WaitingProcess)waiting_queue.peek()).getActiveProcess()
                .equals(process))
                && num <= available;
    }

    @Override
    public boolean acquire(final ISchedulableProcess sched_process, final long num,
            final boolean timeout, final double timeoutValue) {

        // AM: Copied from AbstractActiveResource: If simulation is stopped,
        // allow all processes to finish
        if (!getModel().getSimulationControl().isRunning()) {
            // Do nothing, but allows calling process to complete
            return true;
        }

        observee.fireRequest(sched_process, num);
        final PreemptiveProcess process = (PreemptiveProcess) main_resource
                .lookUp(sched_process);
        if (canProceed(process, num)) {
            grantAccess(process, num);
            return true;
        } else {
            LoggingWrapper.log("Process " + process + " is waiting for " + num
                    + " of " + this);
            final WaitingProcess waiting_process = new WaitingProcess(process, num);
            fromRunningToWaiting(waiting_process, false);
            process.getSchedulableProcess().passivate();
            return false;
        }
    }

    @Override
    public void release(final ISchedulableProcess sched_process, final long num) {

        // AM: Copied from AbstractActiveResource: If simulation is stopped,
        // allow all processes to finish
        if (!getModel().getSimulationControl().isRunning()) {
            // Do nothing, but allows calling process to complete
            return;
        }

        final IActiveProcess process = main_resource.lookUp(sched_process);

        LoggingWrapper.log("Process " + process + " releases " + num + " of "
                + this);
        available += num;
        observee.fireRelease(sched_process, num);
        notifyWaitingProcesses(process.getLastInstance());
    }

    private void notifyWaitingProcesses(final IResourceInstance current) {
        final WaitingProcess waitingProcess = (WaitingProcess)waiting_queue.peek();
        if (waitingProcess != null) {
            if (tryToDequeueProcess(waitingProcess)) {
                fromWaitingToReady(waitingProcess, current);
            }
        }
    }

    private void grantAccess(final PreemptiveProcess process, final long num) {
        LoggingWrapper.log("Process " + process + " acquires " + num + " of "
                + this);
        punish(process);
        boostPriority(process);
        available -= num;
        observee.fireAquire(process.getSchedulableProcess(), num);
        assert available >= 0 : "More resource than available have been acquired!";
    }

    /**
     * Tries to remove the given process from the waiting queue and get access
     * of the required number of passive resources.
     *
     * @param waitingProcess
     * @return True if the process was successfully dequeued and activated,
     *         otherwise false.
     */
    private boolean tryToDequeueProcess(final WaitingProcess waitingProcess) {
        if (canProceed(waitingProcess.getActiveProcess(), waitingProcess
                .getNumRequested())) {
            grantAccess((ProcessWithPriority) waitingProcess.getActiveProcess(),
                    waitingProcess.getNumRequested());
            return true;
        }
        return false;
    }

    @Override
    public void addObserver(final IPassiveResourceSensor observer) {
        observee.addObserver(observer);
    }

    @Override
    public void removeObserver(final IPassiveResourceSensor observer) {
        observee.removeObserver(observer);
    }

    @Override
    public long getAvailable() {
        return available;
    }

}
