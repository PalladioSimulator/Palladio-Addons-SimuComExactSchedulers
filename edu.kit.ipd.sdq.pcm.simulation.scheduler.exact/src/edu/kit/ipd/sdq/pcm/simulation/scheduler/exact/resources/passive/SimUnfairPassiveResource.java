package edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.resources.passive;

import de.uka.ipd.sdq.pcm.core.composition.AssemblyContext;
import de.uka.ipd.sdq.pcm.repository.PassiveResource;
import de.uka.ipd.sdq.scheduler.ISchedulableProcess;
import de.uka.ipd.sdq.scheduler.LoggingWrapper;
import de.uka.ipd.sdq.scheduler.SchedulerModel;
import de.uka.ipd.sdq.scheduler.resources.passive.PassiveResourceObservee;
import de.uka.ipd.sdq.scheduler.sensors.IPassiveResourceSensor;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.SimActiveResource;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.events.IDelayedAction;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.priority.IPriorityBoost;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.processes.IActiveProcess;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.processes.impl.ProcessWithPriority;

public class SimUnfairPassiveResource extends SimAbstractPassiveResource {

    private final double acquisition_demand;
    private final boolean isFifo;
    private long available;
    private final PassiveResourceObservee observee;

    public SimUnfairPassiveResource(final SchedulerModel model,
            final long capacity,
            final PassiveResource passiveResource,
            final IPriorityBoost priority_boost, final SimActiveResource managing_resource,
            final double acquisition_demand, final boolean isFifo, final AssemblyContext assemblyContext) {
        super(model, capacity, passiveResource, priority_boost, managing_resource, assemblyContext);
        this.acquisition_demand = acquisition_demand;
        this.isFifo = isFifo;
        available = capacity;
        observee = new PassiveResourceObservee();
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
        final ProcessWithPriority process = (ProcessWithPriority) main_resource
                .lookUp(sched_process);
        if (num <= available) {
            grantAccess(num, process);
            return true;
        } else {
            LoggingWrapper.log("Process " + process + " is waiting for " + num
                    + " of " + this);
            final WaitingProcess waiting_process = new WaitingProcess(process, num);
            fromRunningToWaiting(waiting_process, !isFifo);
            process.getSchedulableProcess().passivate();
            return false;
        }
    }

    private void grantAccess(final long num, final ProcessWithPriority process) {
        LoggingWrapper.log("Process " + process + " acquires " + num + " of "
                + this);
        punish(process);
        boostPriority(process);
        available -= num;
        observee.fireAquire(process.getSchedulableProcess(), num);
        assert available >= 0 : "More resource than available have been acquired!";
    }

    @Override
    public void release(final ISchedulableProcess sched_process, final long num) {

        // AM: Copied from AbstractActiveResource: If simulation is stopped,
        // allow all processes to finish
        if (!getModel().getSimulationControl().isRunning()) {
            // Do nothing, but allows calling process to complete
            return;
        }

        LoggingWrapper.log("Process " + sched_process + " releases " + num
                + " of " + this);
        available += num;
        observee.fireRelease(sched_process, num);
        notifyNextWaitingProcess();
    }

    private void notifyNextWaitingProcess() {
        final WaitingProcess waiting_process = (WaitingProcess)waiting_queue.peek();
        if (waiting_process != null) {
            final IActiveProcess process = waiting_process.getActiveProcess();
            process.setCurrentDemand(acquisition_demand);
            process.setDelayedAction(new UnfairAccessAction(waiting_process));
            fromWaitingToReady(waiting_process, process.getLastInstance());
        }
    }

    /**
     * Tries to remove the given process from the waiting queue and get access
     * of the required number of passive resources.
     *
     * @param waitingProcess
     * @return True if the process was successfully dequeued and activated,
     *         otherwise false.
     */
    protected boolean tryToDequeueProcess(final WaitingProcess waitingProcess) {
        if (waitingProcess.getNumRequested() <= available) {
            grantAccess(waitingProcess.getNumRequested(),
                    (ProcessWithPriority) waitingProcess.getActiveProcess());
            if (available > 0) {
                notifyNextWaitingProcess();
            }
            return true;
        } else {
            return false;
        }
    }

    private class UnfairAccessAction implements IDelayedAction {
        private final WaitingProcess waiting_process;

        public UnfairAccessAction(final WaitingProcess waiting_process) {
            super();
            this.waiting_process = waiting_process;
        }

        @Override
        public boolean perform() {
            if (!tryToDequeueProcess(waiting_process)) {
                fromRunningToWaiting(waiting_process, true);
                return false;
            } else {
                waiting_process.getActiveProcess().getSchedulableProcess().activate();
                return true;
            }
        }
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
