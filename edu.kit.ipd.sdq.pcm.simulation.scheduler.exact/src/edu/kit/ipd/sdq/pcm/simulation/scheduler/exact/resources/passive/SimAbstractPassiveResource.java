package edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.resources.passive;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Queue;

import de.uka.ipd.sdq.pcm.core.composition.AssemblyContext;
import de.uka.ipd.sdq.pcm.repository.PassiveResource;
import de.uka.ipd.sdq.scheduler.IPassiveResource;
import de.uka.ipd.sdq.scheduler.IRunningProcess;
import de.uka.ipd.sdq.scheduler.SchedulerModel;
import de.uka.ipd.sdq.scheduler.processes.IWaitingProcess;
import de.uka.ipd.sdq.scheduler.resources.AbstractSimResource;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.IResourceInstance;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.SimActiveResource;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.priority.IPriorityBoost;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.processes.impl.ProcessWithPriority;

public abstract class SimAbstractPassiveResource extends AbstractSimResource
implements IPassiveResource {

    private final IPriorityBoost priority_boost;
    protected Deque<IWaitingProcess> waiting_queue;
    protected SimActiveResource main_resource;
    private final AssemblyContext assCtx;
    private final PassiveResource passiveResouce;

    public SimAbstractPassiveResource(
            final SchedulerModel model,
            final long capacity,
            final PassiveResource passiveResource,
            final IPriorityBoost priority_boost,
            final SimActiveResource managing_resource,
            final AssemblyContext assCtx) {
        super(model, capacity, passiveResource.getEntityName(), passiveResource.getId());
        this.priority_boost = priority_boost;
        this.main_resource = managing_resource;
        this.waiting_queue = new ArrayDeque<IWaitingProcess>();
        this.assCtx = assCtx;
        this.passiveResouce = passiveResource;
    }

    protected void fromWaitingToReady(final WaitingProcess waiting_process,
            final IResourceInstance current) {
        if (main_resource != null){
            main_resource.getScheduler().fromWaitingToReady(waiting_process,
                    waiting_queue, current);
        }
    }

    protected void fromRunningToWaiting(final WaitingProcess waiting_process,
            final boolean inFront) {
        if (main_resource != null) {
            main_resource.getScheduler().fromRunningToWaiting(waiting_process,
                    waiting_queue, inFront);
        }
    }

    protected void boostPriority(final IRunningProcess process) {
        if (priority_boost != null) {
            assert process instanceof ProcessWithPriority : "If priority boosts are used only ProcessWithPriorities can be used!";
        priority_boost.boost((ProcessWithPriority) process);
        }
    }

    protected void punish(final IRunningProcess process) {
        if (priority_boost != null) {
            assert process instanceof ProcessWithPriority : "If priority boosts are used only ProcessWithPriorities can be used!";
        priority_boost.punish((ProcessWithPriority) process);
        }
    }

    @Override
    public Queue<IWaitingProcess> getWaitingProcesses() {
        return waiting_queue;
    }

    @Override
    public String toString() {
        return super.getName()+"_"+super.getId();
    }

    /* (non-Javadoc)
     * @see de.uka.ipd.sdq.scheduler.IPassiveResource#getResource()
     */
    @Override
    public PassiveResource getResource() {
        return passiveResouce;
    }

    @Override
    public AssemblyContext getAssemblyContext() {
        return assCtx;
    }

}
