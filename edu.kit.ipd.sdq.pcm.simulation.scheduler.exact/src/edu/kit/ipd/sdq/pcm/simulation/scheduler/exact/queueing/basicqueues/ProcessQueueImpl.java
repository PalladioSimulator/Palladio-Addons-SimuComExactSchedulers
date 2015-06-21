package edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.queueing.basicqueues;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import de.uka.ipd.sdq.scheduler.SchedulerModel;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.IResourceInstance;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.processes.IActiveProcess;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.queueing.IProcessQueue;

public class ProcessQueueImpl implements IProcessQueue {

    private final SchedulerModel model;
    private final ArrayDeque<IActiveProcess> queue;
    private final Hashtable<IActiveProcess, Double> waiting_time_table = new Hashtable<IActiveProcess, Double>();

    public ProcessQueueImpl(final SchedulerModel model) {
        this.model = model;
        this.queue = new ArrayDeque<IActiveProcess>();
    }

    @Override
    public void addLast(final IActiveProcess process) {
        waiting_time_table.put(process, model.getSimulationControl().getCurrentSimulationTime());
        queue.addLast(process);
    }

    @Override
    public void addFirst(final IActiveProcess process) {
        waiting_time_table.put(process, model.getSimulationControl().getCurrentSimulationTime());
        queue.addFirst(process);
    }

    @Override
    public void add(final IActiveProcess process, final boolean inFront){
        if (inFront) {
            addFirst(process);
        } else {
            addLast(process);
        }
    }

    public IActiveProcess peek() {
        return queue.peek();
    }

    public IActiveProcess poll() {
        final IActiveProcess process = queue.poll();
        waiting_time_table.remove(process);
        return process;
    }

    @Override
    public int size() {
        return queue.size();
    }

    @Override
    public boolean remove(final IActiveProcess process) {
        waiting_time_table.remove(process);
        return queue.remove(process);
    }

    @Override
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    @Override
    public Iterable<IActiveProcess> ascending(){
        return new Iterable<IActiveProcess>(){
            @Override
            public Iterator<IActiveProcess> iterator() {
                return queue.iterator();
            }
        };
    }

    @Override
    public Iterable<IActiveProcess> descending(){
        return new Iterable<IActiveProcess>(){
            @Override
            public Iterator<IActiveProcess> iterator() {
                return queue.descendingIterator();
            }
        };
    }

    private boolean containsRunnableFor(final IResourceInstance instance) {
        final Iterator<IActiveProcess> iterator = this.queue.iterator();
        while(iterator.hasNext()){
            final IActiveProcess process = iterator.next();
            if(process.checkAffinity(instance)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean contains(final IActiveProcess process) {
        return queue.contains(process);
    }

    @Override
    public IProcessQueue getBestRunnableQueue(final IResourceInstance instance) {
        if (containsRunnableFor(instance)) {
            return this;
        }
        return null;
    }

    @Override
    public IActiveProcess getNextRunnableProcess(final IResourceInstance instance) {
        for (final IActiveProcess process : ascending()) {
            if (process.checkAffinity(instance)) {
                return process;
            }
        }
        return null;
    }

    @Override
    public IActiveProcess getNextRunnableProcess() {
        return peek();
    }

    @Override
    public void identifyMovableProcesses(
            final IResourceInstance targetInstance, final boolean prio_increasing,
            final boolean queue_ascending, final int processes_needed, final List<IActiveProcess> process_list) {
        final Iterable<IActiveProcess> queue_direction = queue_ascending ? ascending() : descending();
        for (final IActiveProcess process : queue_direction) {
            if (process.isMovable(targetInstance)) {
                process_list.add(process);
                if (process_list.size() >= processes_needed) {
                    break;
                }
            }
        }
    }

    @Override
    public IProcessQueue createNewInstance() {
        return new ProcessQueueImpl(model);
    }


    @Override
    public boolean processStarving(final double threshold) {
        final double now = model.getSimulationControl().getCurrentSimulationTime();
        for (final IActiveProcess process : ascending()){
            final double waiting_time = now - waiting_time_table.get(process);
            if (waiting_time > threshold) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void setWaitingTime(final IActiveProcess process, final double waiting) {
        waiting_time_table.put(process, waiting);
    }

    @Override
    public double getWaitingTime(final IActiveProcess process) {
        return waiting_time_table.get(process);
    }

    @Override
    public List<IActiveProcess> getStarvingProcesses(final double starvationLimit) {
        final double now = model.getSimulationControl().getCurrentSimulationTime();
        final List<IActiveProcess> result = new ArrayList<IActiveProcess>();
        for (final IActiveProcess process : ascending()){
            final Double time = waiting_time_table.get(process);
            final double waiting_time = now - time;
            if (waiting_time > starvationLimit){
                result.add(process);
            }
        }
        return result;
    }
}
