# Palladio-Addons-SimuComExactSchedulers
The abstract simulation engine provides generic simulation concepts
that are common to most discrete-event simulation libraries.
Concrete simulation libraries are used to implement the abstract
concepts. In this way, a simulator relying on the abstract simulation
engine is decoupled from a specific simulation engine. Moreover,
the simulation engine can be replaced without having to change
the simulation code.

## Documentation
Based on [happe2008](http://sdqweb.ipd.kit.edu/publications/theses_happe_bib.html#happe2008b), simulated schedulers for different operating systems have been implemented in SimuCom. These scheduler are available as a PCM AddOn.

Please note: the exact scheduler assumes one time unit to be one ms.

## Installing the Exact Schedulers AddOn
This AddOn requires Palladio Core to be installed in Eclipse beforehand.

The following table gives an overview on the available AddOn releases and the corresponding required PCM Core version:

| PCM Core Release | AddOn Release |
|---|---|
| [Nightly](https://updatesite.palladio-simulator.com/palladio-core-pcm/nightly/) |	[Nightly](http://sdqweb.ipd.kit.edu/eclipse/palladio/addons/exactschedulers/nightly/) |
| [Latest](https://updatesite.palladio-simulator.com/palladio-core-pcm/releases/latest/)	| [Latest](https://updatesite.palladio-simulator.com/palladio-addons-simucomexactschedulers/releases/latest/) |

First, install Palladio Core build from the specified update site. Afterwards, install the Exact Schedulers AddOn feature from the corresponding update site.

## Use the exact schedulers in SimuCom
Once the AddOn is installed, exact scheduling policies can be specified for a CPU processing resource specification. To do so, load the resource

```
pathmap://PCM_EXACT_SCHEDULER_MODELS/ExactSchedulers.resourcetype
```

in a PCM resourceenvironment model. Afterwards, the exact scheduling policies can be selected and are supported by the simulation.

The following exact scheduling policies are available:

* Windows Server 2003, Windows XP, Windows Vista, Windows 7
* Linux 2.6 O(1)
* Linux 2.6 CFS (approximated by ProcessorSharing)
Note that the schedulers assume that the resource demands are defined in milliseconds.

## Demo project and launch configuration
Is available with the [Minimum_ExactScheduler_Project Example](https://anonymous:anonymous@svnserver.informatik.kit.edu/i43/svn/code/trunk/Palladio/Incubation/SimuLizar/trunk/ExampleModels/Minimum_ExactScheduler_Project) in SVN. Please note that it only demonstrates the use of exact schedulers but is not a meaningful prediction model.

## Support
For support
* visit our [issue tracking system](https://palladio-simulator.com/jira)
* contact us via our [mailing list](https://lists.ira.uni-karlsruhe.de/mailman/listinfo/palladio-dev)

For professional support, please fill in our [contact form](http://www.palladio-simulator.com/about_palladio/support/).
