package edu.kit.ipd.sdq.pcm.simulation.scheduler.exact;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.apache.log4j.Logger;

import de.uka.ipd.sdq.probfunction.math.util.MathTools;
import de.uka.ipd.sdq.scheduler.ISchedulableProcess;
import de.uka.ipd.sdq.scheduler.LoggingWrapper;
import de.uka.ipd.sdq.scheduler.SchedulerModel;
import de.uka.ipd.sdq.scheduler.entities.SchedulerEntity;
import de.uka.ipd.sdq.scheduler.processes.IWaitingProcess;
import de.uka.ipd.sdq.scheduler.resources.active.AbstractActiveResource;
import de.uka.ipd.sdq.scheduler.resources.active.IResourceTableManager;
import de.uka.ipd.sdq.scheduler.sensors.IActiveResourceStateSensor;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.processes.IActiveProcess;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.processes.impl.ActiveProcess;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.processes.impl.ProcessRegistry;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.processes.impl.ProcessWithPriority;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.resources.active.SimResourceInstance;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.resources.passive.WaitingProcess;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.strategy.IScheduler;
import scheduler.configuration.ActiveResourceConfiguration;
import scheduler.configuration.ConfigurationFactory;
import scheduler.configuration.PriorityClass;
import scheduler.configuration.ProcessConfiguration;

public class SimActiveResource extends AbstractActiveResource {

    private IScheduler scheduler;
    private final List<IResourceInstance> instanceList;
    private final ProcessRegistry processRegistry;
    private final IResourceInstance main_instance;
    private final Deque<IWaitingProcess> waiting_queue = new ArrayDeque<IWaitingProcess>();
    private ExactSchedulingFactory exactSchedulingFactory = null;
    private ActiveResourceConfiguration resourceConf = null;

    public static final Logger logger = Logger.getLogger("Scheduler");


    public SimActiveResource(final ExactSchedulingFactory exactSchedulingFactory, final SchedulerModel model, final ActiveResourceConfiguration resourceConf
            , IResourceTableManager resourceTableManager) {
        super(model, resourceConf.getReplicas(), resourceConf.getName(), resourceConf.getId(), resourceTableManager);
        this.resourceConf = resourceConf;
        this.instanceList = new ArrayList<IResourceInstance>();
        this.processRegistry = new ProcessRegistry(this);
        this.exactSchedulingFactory = exactSchedulingFactory;
        for (int i = 0; i < capacity; i++) {
            instanceList.add(exactSchedulingFactory.createResourceInstance(i, this));
        }
        main_instance = instanceList.get(0);
        logger.warn("Note that the used exact scheduler " + this.resourceConf.getName() + "assumes that resource demands are specified in milliseconds.");
    }

    public IScheduler getScheduler() {
        return scheduler;
    }

    public List<IResourceInstance> getInstanceList() {
        return instanceList;
    }

    public IActiveProcess lookUp(final ISchedulableProcess process) {
        IActiveProcess p = processRegistry.lookUp(process);
        if (p == null){
            ISchedulableProcess parent = process;
            IActiveProcess pparent = null;
            int i=0;
            do{
                parent = parent.getRootProcess();
                pparent = processRegistry.lookUp(parent);
                i++;
            } while (pparent == null && parent != null);
            assert pparent != null;
            assert i < 2;
            p = pparent.createNewInstance(process);
            processRegistry.registerProcess(p);
        }
        return p;
    }

    @Override
    public void doProcessing(final ISchedulableProcess sched_process, final int resourceServiceID, final double demand) {
        final IActiveProcess process = lookUp(sched_process);

        LoggingWrapper.log(" Process " + process + " demands "
                + MathTools.round(demand, 0.01));

        process.setCurrentDemand(demand);
        scheduler.scheduleNextEvent(process.getLastInstance());
        sched_process.passivate();
    }

    @Override
    public void start() {
        for (final IResourceInstance instance : this.instanceList) {
            instance.start();
        }
    }

    public boolean isIdle(final IResourceInstance instance) {
        return this.scheduler.isIdle(instance);
    }

    public void setScheduler(final IScheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    protected void dequeue(final ISchedulableProcess process) {
        final ActiveProcess myProcess = (ActiveProcess)lookUp(process);
        final WaitingProcess waiting_process = new WaitingProcess(myProcess,0);
        scheduler.fromRunningToWaiting(waiting_process, waiting_queue, false);
        //		myProcess.setIdealInstance(null);
        //		myProcess.setLastInstance(null);
    }

    @Override
    protected void enqueue(final ISchedulableProcess process) {
        final WaitingProcess waiting_process = lookUpWaitingProcess(process);

        if (waiting_process != null) {
            final IResourceInstance instance = getInstanceFor(waiting_process.getActiveProcess());
            scheduler.fromWaitingToReady(waiting_process, waiting_queue, instance);
        } else {
            final IActiveProcess p = lookUp(process);
            final IResourceInstance instance = getInstanceFor(p);
            scheduler.forkNewProcess(p, instance);
            instance.schedulingInterrupt(0);
        }
    }

    private IResourceInstance getInstanceFor(final IActiveProcess process) {
        IResourceInstance instance = main_instance;
        if (process.hasIdealInstance()) {
            instance = process.getIdealInstance();
        }
        if (process.hasLastInstance()) {
            instance = process.getLastInstance();
        }
        return instance;
    }

    private WaitingProcess lookUpWaitingProcess(final ISchedulableProcess process) {
        for (final IWaitingProcess p : waiting_queue){
            if (((WaitingProcess)p).getActiveProcess().getSchedulableProcess().equals(process)) {
                return (WaitingProcess)p;
            }
        }
        return null;
    }

    @Override
    public void stop() {
        for( final IResourceInstance ri : instanceList) {
            ri.stop();
        }
    }

    @Override
    public double getRemainingDemand(final ISchedulableProcess process) {
        throw new UnsupportedOperationException("getRemainingDemand() not yet supported!");
    }

    @Override
    public void updateDemand(final ISchedulableProcess process, final double demand) {
        throw new UnsupportedOperationException("updateDemand() not yet supported!");
    }

    @Override
    public void registerProcess(final ISchedulableProcess schedulableProcess) {
        final ProcessConfiguration processConf = ConfigurationFactory.eINSTANCE
                .createProcessConfiguration();
        processConf.setName(schedulableProcess.getId());
        processConf.setPriority(PriorityClass.DEFAULT);
        processConf.setReplicas(1);
        final ProcessWithPriority p = (ProcessWithPriority) exactSchedulingFactory.createRunningProcess(schedulableProcess, processConf, resourceConf);

        if (!processRegistry.isRegistered(p)){
            processRegistry.registerProcess(p);
            final IResourceInstance instance = getInstanceFor(p);
            scheduler.registerProcess(p, instance);
            p.getSchedulableProcess().addTerminatedObserver(this);
        }
    }

    public void unregisterProcess(final IActiveProcess process) {
        processRegistry.unregisterProcess(process.getSchedulableProcess());
    }

    @Override
    public void addObserver(final IActiveResourceStateSensor observer) {
        for(final IResourceInstance instance : this.instanceList){
            instance.addObserver(observer);
        }

    }

    public IActiveProcess findProcess(final String processName) {
        return processRegistry.findProcess(processName);
    }

    @Override
    public void notifyTerminated(final ISchedulableProcess simProcess) {
        super.notifyTerminated(simProcess);
        final IActiveProcess activeProcess = lookUp(simProcess);
        final IResourceInstance instance = activeProcess.getLastInstance();
        getScheduler().terminateProcess(activeProcess, instance);
        simProcess.removeTerminatedObserver(this);
    }

    @Override
    public int getQueueLengthFor(final SchedulerEntity schedulerEntity, final int coreId) {
        assert (schedulerEntity instanceof SimResourceInstance);
        // TODO: StB: Fixme: How to use the CoreId here? The following seems to work, but no guarantee
        return this.scheduler.getQueueLengthFor((SimResourceInstance)this.getInstanceList().get(coreId));
    }
}
