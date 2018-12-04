package edu.kit.ipd.sdq.pcm.simulation.scheduler.exact;

import java.util.Hashtable;
import java.util.Map;

import org.apache.log4j.Logger;

import de.uka.ipd.sdq.scheduler.IActiveResource;
import de.uka.ipd.sdq.scheduler.IRunningProcess;
import de.uka.ipd.sdq.scheduler.ISchedulableProcess;
import de.uka.ipd.sdq.scheduler.SchedulerModel;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.factory.QueueingConfigurationSwitch;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.loaddistribution.IInstanceSelector;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.loaddistribution.ILoadBalancer;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.loaddistribution.IProcessSelector;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.loaddistribution.balancers.OneToIdleBalancer;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.loaddistribution.balancers.ToThresholdBalancer;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.loaddistribution.selectors.instance.IdleSelector;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.loaddistribution.selectors.instance.RoundRobinSelector;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.loaddistribution.selectors.process.NextRunnableProcessSelector;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.loaddistribution.selectors.process.PreferIdealAndLastProcessSelector;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.priority.IPriority;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.priority.IPriorityUpdateStrategy;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.priority.impl.PriorityManagerImpl;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.priority.update.SleepAverageDependentUpdate;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.processes.IActiveProcess;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.processes.impl.ActiveProcess;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.processes.impl.PreemptiveProcess;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.processes.impl.ProcessWithPriority;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.queueing.IProcessQueue;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.queueing.IQueueingStrategy;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.queueing.IRunQueue;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.queueing.basicqueues.PriorityArray;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.queueing.basicqueues.ProcessQueueImpl;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.queueing.runqueues.ActiveExpiredRunQueue;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.queueing.runqueues.SingleRunQueue;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.resources.active.SimResourceInstance;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.strategy.IScheduler;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.strategy.impl.PreemptiveScheduler;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.timeslice.ITimeSlice;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.timeslice.impl.PriorityDependentTimeSlice;
import edu.kit.ipd.sdq.pcm.simulation.scheduler.exact.timeslice.impl.QuantumTimeSlice;
import scheduler.SchedulerLibrary;
import scheduler.configuration.ActiveResourceConfiguration;
import scheduler.configuration.ConfigurationFactory;
import scheduler.configuration.DynamicPriorityBoostConfiguratioin;
import scheduler.configuration.LoadBalancing;
import scheduler.configuration.PreemptionConfiguration;
import scheduler.configuration.PreferredPriority;
import scheduler.configuration.PreferredWaitingTime;
import scheduler.configuration.PriorityBoostConfiguration;
import scheduler.configuration.PriorityClass;
import scheduler.configuration.PriorityConfiguration;
import scheduler.configuration.PriorityDependentTimeSliceConfiguration;
import scheduler.configuration.PriorityRange;
import scheduler.configuration.ProcessConfiguration;
import scheduler.configuration.ProcessSelection;
import scheduler.configuration.QuantumTimeSliceConfiguration;
import scheduler.configuration.QueueingConfiguration;
import scheduler.configuration.SchedulerConfiguration;
import scheduler.configuration.TimeSliceConfiguration;
import scheduler.configuration.util.ConfigurationSwitch;

public class ExactSchedulingFactory {


    private String schedulerConfigurationModel = "pathmap://PCM_EXACT_SCHEDULER_MODELS/Library.scheduler";

    private final SchedulerModel model;


    protected static Logger logger = Logger.getLogger(ExactSchedulingFactory.class.getName());

    public ExactSchedulingFactory(final SchedulerModel model) {
        this.model = model;
    }

    public ExactSchedulingFactory(final SchedulerModel model, final String schedulerConfigurationModel) {
        this.model = model;
        this.schedulerConfigurationModel = schedulerConfigurationModel;
    }

    private final Map<String, IResourceInstance> resource_instance_map = new Hashtable<String, IResourceInstance>();
    private final Map<String, ActiveProcess> process_map = new Hashtable<String, ActiveProcess>();
    private final Map<String, PriorityManagerImpl> manager_map = new Hashtable<String, PriorityManagerImpl>();

    /* Loads scheduler configuration */
    public IActiveResource getResource(final SchedulerModel model,
            final String schedulerName, final long numReplicas, final String sensorDescription) {

        final SchedulerLibrary lib = (SchedulerLibrary) SchedulerTools.loadFromXMI(schedulerConfigurationModel);
        SchedulerConfiguration selectedConf = null;
        for (final SchedulerConfiguration conf : lib.getSchedulerConfiguration()) {
            if (conf.getName().equals(schedulerName)) {
                selectedConf = conf;
                break;
            }
        }
        if (selectedConf != null) {
            ActiveResourceConfiguration resourceConf = null;
            resourceConf = ConfigurationFactory.eINSTANCE
                    .createActiveResourceConfiguration();
            resourceConf.setName(schedulerName);
            resourceConf.setReplicas((int)numReplicas);
            resourceConf.setSchedulerConfiguration(selectedConf);
            final SimActiveResource resource = new SimActiveResource(this, model, resourceConf);
            final IScheduler scheduler = createScheduler(model, resourceConf.getSchedulerConfiguration(), resource);
            resource.setScheduler(scheduler);
            return resource;
        }
        return null;
    }

    private IScheduler createScheduler(final SchedulerModel model, final SchedulerConfiguration configuration, final IActiveResource scheduled_resource) {
        return createPreemptiveScheduler(model, configuration, scheduled_resource);
    }

    private IScheduler createPreemptiveScheduler(final SchedulerModel model, final SchedulerConfiguration configuration,
            final IActiveResource scheduled_resource) {
        final IProcessQueue process_queue_prototype = createProcessQueue(model, configuration
                .getPriorityConfiguration());
        final IQueueingStrategy queueing_strategy = createQueueingStrategy(model, configuration.getQueueingConfiguration(),
                process_queue_prototype, (SimActiveResource) scheduled_resource);
        final boolean in_front_after_waiting = configuration.isInFrontAfterWaiting();
        final double scheduling_interval = configuration.getInterval();
        return new PreemptiveScheduler(model, (SimActiveResource) scheduled_resource,
                queueing_strategy, in_front_after_waiting, scheduling_interval, configuration.getStarvationBoost());
    }

    private IProcessQueue createProcessQueue(final SchedulerModel model, final PriorityConfiguration configuration) {
        if (configuration == null) {
            return new ProcessQueueImpl(model);
        }
        final PriorityManagerImpl manager = createPriorityManager(configuration
                .getRange());
        return new PriorityArray(model, manager);
    }

    private PriorityManagerImpl createPriorityManager(final PriorityRange range) {
        final String id = getManagerId(range);
        PriorityManagerImpl manager = manager_map.get(id);
        if (manager == null) {
            manager = new PriorityManagerImpl(range.getHighest(), range
                    .getHigh(), range.getAverage(), range.getLow(), range
                    .getLowest(), range.getDefault());
            manager_map.put(id, manager);
        }
        return manager;
    }

    private String getManagerId(final PriorityRange range) {
        return range.getHighest() + "_" + range.getHigh() + "_"
                + range.getAverage() + "_" + range.getLow() + "_"
                + range.getLowest() + "_" + range.getDefault();
    }

    private IQueueingStrategy createQueueingStrategy(final SchedulerModel model, final QueueingConfiguration configuration,
            final IProcessQueue process_queue_prototype, final SimActiveResource resource) {
        final IRunQueue runqueue_prototype = createRunQueue(model, configuration
                .getRunqueueType(), process_queue_prototype);
        final IInstanceSelector instance_selector = createInstanceSelector(
                configuration.getInitialInstanceSelection(), resource);

        final QueueingConfigurationSwitch qSwitch = new QueueingConfigurationSwitch(
                runqueue_prototype, instance_selector, this, resource);
        return qSwitch.doSwitch(configuration);
    }

    private IInstanceSelector createInstanceSelector(
            final scheduler.configuration.ResourceInstanceSelection initialInstanceSelection,
            final SimActiveResource resource) {
        switch (initialInstanceSelection) {
        case PREFER_IDLE:
            return new IdleSelector(resource);
        case ROUND_ROBIN:
            return new RoundRobinSelector(resource);
        default:
            assert false : "Unknown InstanceSelector!";
        break;
        }
        return null;
    }

    private IRunQueue createRunQueue(final SchedulerModel model, final scheduler.configuration.RunQueueType type,
            final IProcessQueue process_queue_prototype) {
        IRunQueue runqueue = null;
        switch (type) {
        case SINGLE:
            runqueue = new SingleRunQueue(process_queue_prototype);
            break;
        case ACTIVE_AND_EXPIRED:
            runqueue = new ActiveExpiredRunQueue(model, process_queue_prototype);
            break;
        default:
            assert false : "Unknown RunqueueType";
        break;
        }
        return runqueue;
    }


    public IResourceInstance createResourceInstance(final int index,
            final IActiveResource containing_resource) {

        final String id = containing_resource.getId() + index;

        IResourceInstance instance = resource_instance_map.get(id);
        if (instance == null) {
            instance = new SimResourceInstance(model, index, containing_resource);
            resource_instance_map.put(id, instance);
        }
        return instance;
    }

    /*
     * (non-Javadoc)
     *
     * @see de.uka.ipd.sdq.scheduler.builder.ISchedulingFactory#createRunningProcess(de.uka.ipd.sdq.scheduler.ISchedulableProcess,
     *      scheduler.configuration.ProcessConfiguration,
     *      scheduler.configuration.ActiveResourceConfiguration)
     */
    public IRunningProcess createRunningProcess(final ISchedulableProcess process,
            final ProcessConfiguration configuration, final ActiveResourceConfiguration resourceConfiguration) {
        final String id = process.getId() + resourceConfiguration.getId();

        ActiveProcess active_process = process_map.get(id);

        if (active_process == null) {
            if (resourceConfiguration.getReplicas() > 0) {

                if (resourceConfiguration.getSchedulerConfiguration()
                        .getPriorityConfiguration() != null) {

                    // set the priority to the right level
                    final IPriority prio = getPriority(configuration.getPriority(),resourceConfiguration.getSchedulerConfiguration().getPriorityConfiguration().getRange());
                    prio.setValue(process.getPriority());

                    active_process = new ProcessWithPriority(model, process, prio);
                    final IPriorityUpdateStrategy updateStrategy = createPriorityUpdadateStrategy(
                            resourceConfiguration.getSchedulerConfiguration()
                            .getPriorityConfiguration()
                            .getBoostConfiguration(), active_process);
                    ((ProcessWithPriority) active_process)
                    .setPriorityUpdateStrategy(updateStrategy);

                    //System.out.println("Creating new process "+ active_process.getName() + " from Closed/Open Workload " + process.getClass().getName() + " with priority " + ((ProcessWithPriority)active_process).getStaticPriority());

                }
                if (resourceConfiguration.getSchedulerConfiguration()
                        .getPreemptionConfiguration() != null) {
                    if (active_process == null) {
                        active_process = new PreemptiveProcess(model, process);
                    }
                    final ITimeSlice timeslice = createTimeSlice(
                            resourceConfiguration.getSchedulerConfiguration()
                            .getPreemptionConfiguration(),
                            active_process);
                    timeslice.fullReset();
                    ((PreemptiveProcess) active_process)
                    .setTimeSlice(timeslice);
                } else {
                    active_process = new ActiveProcess(model, process);
                }

                process_map.put(id, active_process);
            }
        }
        return active_process;
    }

    private IPriorityUpdateStrategy createPriorityUpdadateStrategy(
            final PriorityBoostConfiguration boostConfiguration, final IActiveProcess process) {
        if (boostConfiguration instanceof DynamicPriorityBoostConfiguratioin) {
            final DynamicPriorityBoostConfiguratioin dynamic = (DynamicPriorityBoostConfiguratioin) boostConfiguration;
            return new SleepAverageDependentUpdate(model, process, dynamic
                    .getMaxSleepAverage(), dynamic.getMaxBonus());
        }
        return null;
    }

    private IPriority getPriority(final PriorityClass priority, final PriorityRange range) {
        final PriorityManagerImpl manager = createPriorityManager(range);
        IPriority prio = manager.getDefaultPriority();
        switch (priority) {
        case LOWEST:
            prio = manager.getLowestPriority();
            break;
        case LOW:
            prio = manager.getLowPriority();
            break;
        case AVERAGE:
            prio = manager.getAveragePriority();
            break;
        case HIGH:
            prio = manager.getHighPriority();
            break;
        case HIGHEST:
            prio = manager.getHighestPriority();
            break;
        default:
            prio = manager.getDefaultPriority();
            break;
        }
        return prio;
    }

    private ITimeSlice createTimeSlice(
            final PreemptionConfiguration preemptionConfiguration,
            final ActiveProcess process) {

        final ConfigurationSwitch<ITimeSlice> timesliceSwitch = new ConfigurationSwitch<ITimeSlice>() {

            @Override
            public ITimeSlice caseQuantumTimeSliceConfiguration(
                    final QuantumTimeSliceConfiguration configuration) {
                final double timeslice = configuration.getTimeslice();
                final int quanta = configuration.getQuanta();
                final int min_quanta = configuration.getMinQuanta();
                return new QuantumTimeSlice(timeslice,quanta,min_quanta);
            }

            @Override
            public ITimeSlice casePriorityDependentTimeSliceConfiguration(
                    final PriorityDependentTimeSliceConfiguration configuration) {
                final double timeslice = configuration.getTimeslice();
                final double min_timeslice = configuration.getMinTimeslice();
                final double min_time_to_be_scheduled = configuration.getMinTimeToBeScheduled();
                return new PriorityDependentTimeSlice(
                        (ProcessWithPriority) process, timeslice,
                        min_timeslice, min_time_to_be_scheduled);
            }
        };

        if (preemptionConfiguration != null) {
            final TimeSliceConfiguration timesliceConf = preemptionConfiguration
                    .getTimesliceConfiguration();
            return timesliceSwitch.doSwitch(timesliceConf);
        }
        return null;
    }

    public ILoadBalancer createLoadBalancer(final LoadBalancing load_balancing) {
        final double balance_interval = load_balancing.getBalancingInterval();
        final double threshold = load_balancing.getThreshold();
        final boolean prio_increasing = load_balancing.getPreferredPriority() == PreferredPriority.HIGHER;
        final boolean queue_ascending = load_balancing.getPreferredWaitingTime() == PreferredWaitingTime.SHORT;

        switch (load_balancing.getBalancingType()) {
        case ANY_TO_THRESHOLD:
            return new ToThresholdBalancer(model, balance_interval,
                    prio_increasing, queue_ascending, (int)threshold);
        case IDLE_TO_THRESHOLD:
            //			return new IdleToThresholdBalancer(balance_interval,
            //					global_balance, prio_increasing, queue_ascending,
            //					max_iterations, threshold);
        case IDLE_TO_ONE:
            return new OneToIdleBalancer(balance_interval,
                    prio_increasing, queue_ascending);
        default:
            assert false : "Unknown LoadBalancing Type.";
        break;
        }
        return null;
    }


    public IProcessSelector createProcessSelector(
            final ProcessSelection processSelection) {
        switch (processSelection) {
        case NEXT_RUNNABLE:
            return new NextRunnableProcessSelector();
        case PREFER_IDEAL_AND_LAST:
            return new PreferIdealAndLastProcessSelector();
        default:
            assert false : "Unknown ProcessSelection";
        break;
        }
        return null;
    }



}
