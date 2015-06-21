package edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.strategy.impl;

import de.uka.ipd.sdq.scheduler.ISchedulableProcess;
import de.uka.ipd.sdq.scheduler.SchedulerModel;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.IResourceInstance;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.SimActiveResource;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.priority.IPriorityUpdateStrategy;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.priority.update.SetToBaseUpdate;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.processes.IActiveProcess;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.processes.impl.ProcessWithPriority;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.queueing.IQueueingStrategy;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.resources.active.SimResourceInstance;
import scheduler.configuration.StarvationBoost;

public class PreemptiveScheduler extends AbstractScheduler {

    public PreemptiveScheduler(final SchedulerModel model, final SimActiveResource resource,
            final IQueueingStrategy queueingStrategy, final boolean in_front_after_waiting,
            final double scheduling_interval, final StarvationBoost starvationBoost) {
        super(resource, queueingStrategy, in_front_after_waiting, starvationBoost);
        this.scheduling_interval = scheduling_interval;
    }


    @Override
    public void schedule(final IResourceInstance instance) {
        if (instance.isScheduling()) {
            return;
        }

        // Balance the runqueue of this instance with the runqueues of other
        // instances. This might change the state of the instance's runqueue.
        // So, the next runnable process can only be determined after the
        // balancing was finished.
        queueing_strategy.activelyBalance(instance);

        // get the currently scheduled process for the instance.
        final ProcessWithPriority running_process = (ProcessWithPriority) instance
                .getRunningProcess();

        // Update the timing variables and priority of the process. Possibly
        // pending events of the process are canceled.
        toNow(running_process);

        if (running_process == null){
        } else if ( running_process.getTimeslice().isFinished()) {
            unschedule(running_process, false, instance);
        } else {
            unschedule(running_process, true, instance);
        }
        scheduleNextProcess(instance);
        scheduleNextEvent(instance);
    }


    private void scheduleNextProcess(final ProcessWithPriority next_process, final IResourceInstance instance) {
        if (next_process != null) {
            next_process.toNow();
            next_process.update();
            fromReadyToRunningOn(next_process, instance);
        }
    }

    private void scheduleNextProcess(final IResourceInstance instance) {
        lookForStarvingProcessesAndApplyStarvationBoost(instance);

        final ProcessWithPriority next_process = (ProcessWithPriority) queueing_strategy.getNextProcessFor(instance);
        scheduleNextProcess(next_process, instance);
    }

    private void lookForStarvingProcessesAndApplyStarvationBoost(final IResourceInstance instance) {
        if(starvationBoost != null){
            for (final IActiveProcess p : queueing_strategy.getStarvingProcesses(instance, starvationBoost.getStarvationLimit())){
                applyStarvationBoost((ProcessWithPriority)p);
            }

        }

    }

    private void applyStarvationBoost(final ProcessWithPriority p) {
        p.setToStaticPriorityWithBonus(starvationBoost.getBoost());
        final IPriorityUpdateStrategy priorityUpdateStrategy = new SetToBaseUpdate(starvationBoost.getDurationInTimeslices());
        p.setPriorityUpdateStrategy(priorityUpdateStrategy);
    }


    private void toNow(final ProcessWithPriority process) {
        if (process != null){
            process.toNow();
        }
    }

    private void unschedule(final ProcessWithPriority running_process,
            final boolean next_has_higher_priority, final IResourceInstance current) {
        if (running_process != null) {
            if (running_process.getTimeslice().isFinished()){
                running_process.update();
                fromRunningToReady(running_process, current, false);
                running_process.getTimeslice().fullReset();
            } else {
                fromRunningToReady(running_process, current, next_has_higher_priority);
            }
            if (running_process.getRunQueue().getCurrentLoad() == 1){
                running_process.getRunQueue().resetStarvationInfo();
            }
        }
    }

    //    /**
    //     * pOne > pTwo ?
    //     *
    //     * @param pTwo
    //     * @return
    //     */
    //    private boolean hasHigherPriority(final ProcessWithPriority pOne,
    //            final ProcessWithPriority pTwo) {
    //        if (pOne == null) {
    //            return false;
    //        }
    //        if (pTwo == null) {
    //            return true;
    //        }
    //        final IPriority prio_one = pOne.getDynamicPriority();
    //        final IPriority prio_two = pTwo.getDynamicPriority();
    //        return prio_one.greaterThan(prio_two);
    //    }

    @Override
    public void scheduleNextEvent(final IResourceInstance instance) {
        final ProcessWithPriority running = (ProcessWithPriority) instance
                .getRunningProcess();
        if (running != null) {
            running.toNow();
            final double remainingTime = running.getTimeslice().getRemainingTime();
            final double currentDemand = running.getCurrentDemand();
            if ( currentDemand < remainingTime ) {
                running.scheduleProceedEvent(this);
            } else {
                instance.scheduleSchedulingEvent(remainingTime);
            }
        } else {
            if (!queueing_strategy.isIdle(instance)) {
                instance.scheduleSchedulingEvent(0);
            } else {
                instance.scheduleSchedulingEvent(scheduling_interval);
            }
        }
    }


    @Override
    public boolean isIdle(final IResourceInstance instance) {
        return queueing_strategy.isIdle(instance);
    }


    @Override
    public double getInterval() {
        return scheduling_interval;
    }

    @Override
    public void terminateProcess(final IActiveProcess process, final IResourceInstance current) {
        super.terminateProcess(process, current);
        final ISchedulableProcess sProcess = process.getSchedulableProcess();
        if (sProcess.isFinished()
                // do NOT remove the originally defined processes as they
                // serve as prototypes for all spawned processes.
                && sProcess.getRootProcess() != sProcess){
            this.resource.unregisterProcess(process);
        }
    }


    @Override
    public int getQueueLengthFor(final SimResourceInstance simResourceInstance) {
        return this.queueing_strategy.getQueueLengthFor(simResourceInstance);
    }


}
