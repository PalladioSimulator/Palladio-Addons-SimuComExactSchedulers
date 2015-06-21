package edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.queueing.basicqueues;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import de.uka.ipd.sdq.scheduler.SchedulerModel;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.IResourceInstance;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.priority.IPriority;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.priority.IPriorityManager;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.processes.IActiveProcess;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.processes.impl.ProcessWithPriority;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.queueing.IProcessQueue;

public class PriorityArray implements IProcessQueue {

    private final SchedulerModel model;
    private final Hashtable<IPriority, IProcessQueue> priorityTable;
    private final IPriorityManager priority_manager;

    public PriorityArray(final SchedulerModel model, final IPriorityManager priority_manager) {
        this.model = model;
        this.priority_manager = priority_manager;
        this.priorityTable = new Hashtable<IPriority, IProcessQueue>();
        for (final IPriority prio : priority_manager.decreasing()) {
            priorityTable.put(prio, new ProcessQueueImpl(model));
        }
    }

    /**
     * Returns the number of processes in the priority array.
     */
    @Override
    public int size() {
        int num = 0;
        for (final IProcessQueue queue : priorityTable.values()) {
            num += queue.size();
        }
        return num;
    }

    /**
     * @return True, if the priority array is empty. False, otherwise.
     */
    @Override
    public boolean isEmpty() {
        for (final IProcessQueue queue : priorityTable.values()) {
            if (!queue.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Removes the given process from the priorityarray.
     *
     * @param process
     * @return True, if the process has been successfully removed. False,
     *         otherwise.
     */
    @Override
    public boolean remove(final IActiveProcess process) {
        return getQueueFor(process).remove(process);
    }

    /**
     * Adds a new process at the END of the process' priority's queue.
     *
     * @param process
     */
    @Override
    public void addLast(final IActiveProcess process) {
        add(process,false);
    }

    /**
     * Adds a new process at the BEGINNING of the process' priority's queue.
     *
     * @param process
     */
    @Override
    public void addFirst(final IActiveProcess process) {
        add(process,true);
    }

    @Override
    public void add(final IActiveProcess process, final boolean in_front) {
        getQueueFor(process).add(process,in_front);
    }

    /**
     * Returns the queue of the process' priority.
     *
     * @param process
     * @return Queue for the given process.
     */
    private IProcessQueue getQueueFor(final IActiveProcess process) {
        assert process instanceof ProcessWithPriority;
        return getQueue( ((ProcessWithPriority)process).getDynamicPriority());
    }


    /**
     * @return Returns the queue with the highest priority which is not empty.
     *         Null if all queues are empty.
     */
    private IProcessQueue getNonEmptyQueueWithHighestPriority() {
        for (final IPriority prio : priority_manager.decreasing()) {
            if (!getQueue(prio).isEmpty()) {
                return getQueue(prio);
            }
        }
        return null;
    }

    private IProcessQueue getQueue(final IPriority prio) {
        return priorityTable.get(prio);
    }

    /**
     * Returns the queue with the highest priority of which at least one process
     * can be executed on instance.
     *
     * @param instance
     * @return
     */
    @Override
    public IProcessQueue getBestRunnableQueue(
            final IResourceInstance instance) {
        IProcessQueue queue = null;
        for (final IPriority prio : priority_manager.decreasing()) {
            queue = getQueue(prio).getBestRunnableQueue(instance);
            if (queue != null) {
                break;
            }
        }
        return queue;
    }

    /**
     * Returns the process with the highest priority runnable on the given instance.
     * @param instance
     * @return
     */
    @Override
    public IActiveProcess getNextRunnableProcess(final IResourceInstance instance) {
        IActiveProcess process = null;
        for (final IPriority prio : priority_manager.decreasing()) {
            process = getQueue(prio).getNextRunnableProcess();
            if (process != null) {
                break;
            }
        }
        return process;
    }

    @Override
    public boolean contains(final IActiveProcess process) {
        for (final IProcessQueue queue : priorityTable.values()) {
            if (queue.contains(process)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public IActiveProcess getNextRunnableProcess() {
        final IProcessQueue queue = getNonEmptyQueueWithHighestPriority();
        if (queue != null){
            return queue.getNextRunnableProcess();
        }
        return null;
    }

    /**
     * Adds processes that do not violate the affinity constraint to the list
     * in the specified direction.
     *
     * @param target_instance
     */
    @Override
    public void identifyMovableProcesses(
            final IResourceInstance target_instance, final boolean prio_increasing,
            final boolean queue_ascending, final int processes_needed, final List<IActiveProcess> process_list){
        final Iterable<IPriority> prio_direction = prio_increasing ? priority_manager.increasing() : priority_manager.decreasing();
        for (final IPriority prio : prio_direction) {
            getQueue(prio).identifyMovableProcesses(target_instance, prio_increasing, queue_ascending, processes_needed, process_list);
            if (process_list.size() >= processes_needed) {
                break;
            }
        }
    }

    @Override
    public Iterable<IActiveProcess> ascending() {
        return new Iterable<IActiveProcess>(){
            @Override
            public Iterator<IActiveProcess> iterator() {
                return new Iterator<IActiveProcess>(){

                    Iterator<IPriority> prio_iterator = priority_manager.increasing().iterator();
                    Iterator<IActiveProcess> queue_iterator = null;

                    @Override
                    public boolean hasNext() {
                        while ((queue_iterator == null || !queue_iterator.hasNext()) && prio_iterator.hasNext()){
                            queue_iterator = getQueue(prio_iterator.next()).ascending().iterator();
                        }
                        return (queue_iterator != null && queue_iterator.hasNext());
                    }

                    @Override
                    public IActiveProcess next() {
                        while ((queue_iterator == null || !queue_iterator.hasNext()) && prio_iterator.hasNext()){
                            queue_iterator = getQueue(prio_iterator.next()).ascending().iterator();
                        }
                        return queue_iterator == null ? null : queue_iterator.next();
                    }

                    @Override
                    public void remove() {
                    }

                };
            }

        };
    }

    @Override
    public Iterable<IActiveProcess> descending() {
        return new Iterable<IActiveProcess>(){
            @Override
            public Iterator<IActiveProcess> iterator() {
                return new Iterator<IActiveProcess>(){

                    Iterator<IPriority> prio_iterator = priority_manager.decreasing().iterator();
                    Iterator<IActiveProcess> queue_iterator = null;

                    @Override
                    public boolean hasNext() {
                        while ((queue_iterator == null || !queue_iterator.hasNext()) && prio_iterator.hasNext()){
                            queue_iterator = getQueue(prio_iterator.next()).descending().iterator();
                        }
                        return (queue_iterator != null && queue_iterator.hasNext());
                    }

                    @Override
                    public IActiveProcess next() {
                        while ((queue_iterator == null || !queue_iterator.hasNext()) && prio_iterator.hasNext()){
                            queue_iterator = getQueue(prio_iterator.next()).descending().iterator();
                        }
                        return queue_iterator == null ? null : queue_iterator.next();
                    }

                    @Override
                    public void remove() {
                    }

                };
            }

        };
    }

    @Override
    public IProcessQueue createNewInstance() {
        return new PriorityArray(model, priority_manager);
    }

    @Override
    public boolean processStarving(final double threshold) {
        for(final IProcessQueue q : priorityTable.values()){
            if (q.processStarving(threshold)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public double getWaitingTime(final IActiveProcess process) {
        return getQueueFor(process).getWaitingTime(process);
    }

    @Override
    public void setWaitingTime(final IActiveProcess process, final double waiting) {
        getQueueFor(process).setWaitingTime(process, waiting);
    }

    @Override
    public List<IActiveProcess> getStarvingProcesses(final double starvationLimit) {
        final List<IActiveProcess> result = new ArrayList<IActiveProcess>();
        for(final IProcessQueue q : priorityTable.values()){
            result.addAll(q.getStarvingProcesses(starvationLimit));
        }
        return result;
    }
}
