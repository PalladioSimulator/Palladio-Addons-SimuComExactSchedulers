package edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.resources.active;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import de.uka.ipd.sdq.scheduler.IActiveResource;
import de.uka.ipd.sdq.scheduler.IRunningProcess;
import de.uka.ipd.sdq.scheduler.SchedulerModel;
import de.uka.ipd.sdq.scheduler.entities.SchedulerEntity;
import de.uka.ipd.sdq.scheduler.sensors.IActiveResourceStateSensor;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.IResourceInstance;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.SimActiveResource;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.events.SchedulingEvent;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.events.SchedulingInterruptEvent;

public class SimResourceInstance extends SchedulerEntity implements IResourceInstance {
	/** logger for this class. */
	private static final Logger log = Logger.getLogger(SimResourceInstance.class.getCanonicalName());

    private final int number;
    private final IActiveResource containing_resource;
    private IRunningProcess running_process;

    /**
     * The variable last_running_process is necessary to fake (!!!) changes in
     * the association of light weight processes (LWPs) and threads, since the
     * current scheduler simulator does not reflect this association (it just
     * models 'processes'). For a solid model, this distinction is mandatory.
     *
     *
     * The last_running_process is used to fake the different treatment of
     * threads in load balancing.
     *
     * So, what's the difficulty there?
     *
     * As soon as a thread is put to sleep, its LWP looks for a new thread (of
     * the same heavyweight process) to execute for its remaining timeslice. It
     * prefers the last thread in the busiest run queue. This leads to a new
     * LWP-to-thread mapping. Basically, the LWPs of both threads are switched.
     *
     * This behavior differs significantly from process load balancing, where
     * the LWP is moved to a new processor!
     *
     */
    private IRunningProcess last_running_process;

    private SchedulingEvent scheduling_event;
    private boolean isScheduling;
    private final List<IActiveResourceStateSensor> resourceObserverList = new ArrayList<IActiveResourceStateSensor>();

    public SimResourceInstance(final SchedulerModel model, final int number, final IActiveResource containing_resource) {
        super(model, SimResourceInstance.class.getName());
        this.number = number;
        this.containing_resource = containing_resource;
        // Initialise this at start instead of container for multiple Simulation
        // runs with different simulator instances...
        // this.scheduling_event = new
        // SchedulingEvent((SimActiveResource)containing_resource,this);
        this.running_process = null;
        this.isScheduling = false;
    }

    @Override
    public IRunningProcess getRunningProcess() {
        return running_process;
    }

    @Override
    public void release() {
        this.last_running_process = running_process;
        this.running_process = null;
        updateObservers();
    }

    private void updateObservers() {
        for (final IActiveResourceStateSensor observer : resourceObserverList) {
            observer.update(getQueueLength(), getId());
        }
    }

    @Override
    public void addObserver(final IActiveResourceStateSensor observer) {
        resourceObserverList.add(observer);
    }

    @Override
    public void removeObserver(final IActiveResourceStateSensor observer) {
        resourceObserverList.remove(observer);
    }

    @Override
    public boolean processAssigned() {
        return running_process != null;
    }

    @Override
    public void assign(final IRunningProcess process) {
        assert !this.processAssigned() : "There is already a process executing on resource instance "
                + this;
        running_process = process;
        updateObservers();
    }

    @Override
    public String getName() {
        return containing_resource.getName() + "_" + number;
    }

    @Override
    public void scheduleSchedulingEvent(final double time) {
        cancelSchedulingEvent();
    	if (scheduling_event != null)
            scheduling_event.schedule(this, time);
    	else {
    		log.log(Level.ERROR, "Tried to reschedule event that did not exists. Error in simulation may cause subsequent failures.");
    	}
    }

    @Override
    public void schedulingInterrupt(final double time) {
        new SchedulingInterruptEvent(getModel(), (SimActiveResource) containing_resource).schedule(this, time);
    }

    @Override
    public void cancelSchedulingEvent() {
    	if (scheduling_event != null)
            scheduling_event.removeEvent();
    	else {
    		log.log(Level.ERROR, "Tried to cancel event that did not exists. Error in simulation may cause subsequent failures.");
    	}
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof SimResourceInstance) {
            final SimResourceInstance instance = (SimResourceInstance) obj;
            return this.getId() == instance.getId();
        }
        return false;
    }

    public int getId() {
        return number;
    }

    @Override
    public int hashCode() {
        return (getName() + getId()).hashCode();
    }

    @Override
    public double getNextSchedEventTime() {
        final double simTime = getModel().getSimulationControl().getCurrentSimulationTime();
        final double eventTime = scheduling_event.scheduledAtTime();
        return eventTime - simTime;
    }

    @Override
    public void start() {
        this.scheduling_event = new SchedulingEvent(getModel(),
                (SimActiveResource) containing_resource);
        scheduling_event.schedule(this, 0);
    }

    @Override
    public void stop() {
    	if (scheduling_event != null)
    		scheduling_event.removeEvent();
    	else {
    		log.log(Level.ERROR, "Tried to remove event that did not exists. Error in simulation may cause subsequent failures.");
    	}
    }

    @Override
    public void setIsScheduling(final boolean b) {
        isScheduling = b;
    }

    @Override
    public boolean isScheduling() {
        return isScheduling;
    }

    @Override
    public boolean isIdle() {
        return running_process == null;
    }

    @Override
    public IRunningProcess getLastRunningProcess() {
        return last_running_process;
    }

    public int getQueueLength() {
        return containing_resource.getQueueLengthFor(this,0);
    }

}
