package org.cloudbus.cloudsim.examples.gpu;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.VmScheduler;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.gpu.BusTags;
import org.cloudbus.cloudsim.gpu.GpuCloudlet;
import org.cloudbus.cloudsim.gpu.GpuCloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.gpu.GpuDatacenter;
import org.cloudbus.cloudsim.gpu.GpuDatacenterBroker;
import org.cloudbus.cloudsim.gpu.GpuHost;
import org.cloudbus.cloudsim.gpu.GpuHostTags;
import org.cloudbus.cloudsim.gpu.GpuTask;
import org.cloudbus.cloudsim.gpu.GpuTaskSchedulerLeftover;
import org.cloudbus.cloudsim.gpu.GpuVm;
import org.cloudbus.cloudsim.gpu.GpuVmAllocationPolicySimple;
import org.cloudbus.cloudsim.gpu.Pgpu;
import org.cloudbus.cloudsim.gpu.Vgpu;
import org.cloudbus.cloudsim.gpu.VgpuScheduler;
import org.cloudbus.cloudsim.gpu.VideoCard;
import org.cloudbus.cloudsim.gpu.VideoCardTags;
import org.cloudbus.cloudsim.gpu.allocation.VideoCardAllocationPolicy;
import org.cloudbus.cloudsim.gpu.allocation.VideoCardAllocationPolicySimple;
import org.cloudbus.cloudsim.gpu.hardware_assisted.grid.GridVgpuSchedulerBestEffort;
import org.cloudbus.cloudsim.gpu.hardware_assisted.grid.GridVgpuSchedulerFairShareEx;
import org.cloudbus.cloudsim.gpu.hardware_assisted.grid.GridVgpuTags;
import org.cloudbus.cloudsim.gpu.performance.models.PerformanceModelGpuNull;
import org.cloudbus.cloudsim.gpu.provisioners.GpuBwProvisionerShared;
import org.cloudbus.cloudsim.gpu.provisioners.GpuGddramProvisionerSimple;
import org.cloudbus.cloudsim.gpu.provisioners.GpuPeProvisionerSimple;
import org.cloudbus.cloudsim.gpu.provisioners.VideoCardBwProvisioner;
import org.cloudbus.cloudsim.gpu.provisioners.VideoCardBwProvisionerShared;
import org.cloudbus.cloudsim.gpu.selection.PgpuSelectionPolicy;
import org.cloudbus.cloudsim.gpu.selection.PgpuSelectionPolicySimple;
import org.cloudbus.cloudsim.lists.VmList;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

import de.vandermeer.asciitable.AsciiTable;

/**
 * Example 2, sharing a pGPU with NVIDIA vGPU. Four VMs with an NVIDIA L40S-16Q
 * vGPU each ask for the L40S of a host. In equal-size mode, the default of
 * NVIDIA vGPU software, an L40S takes at most three 16 GB vGPUs, so the fourth
 * VM is rejected. Two of the placed VMs run a GPU task; the third stays
 * idle. <br>
 * The simulation runs twice to compare the time-sliced vGPU schedulers of
 * NVIDIA vGPU software. Best effort, the default, divides the pGPU among the
 * vGPUs that have work: task 0 gets half of the pGPU while task 1 runs and the
 * whole pGPU afterwards. Equal share divides it among all resident vGPUs, idle
 * or not: task 0 gets a third while three vGPUs are resident and half after
 * VM 1 leaves. Example 3 uses mixed-size mode, in which vGPUs of different sizes
 * share a pGPU. <br>
 * New: vGPU types, placement rules, rejection and time-sliced vGPU
 * schedulers. <br>
 * Next: Example 3 gives a VM more than one vGPU.
 * 
 * @author Ahmad Siavashi
 * 
 */
public class CloudSimGpuExample2 {
	/** The cloudlet list. */
	private static List<GpuCloudlet> cloudletList;
	/** The vmlist. */
	private static List<GpuVm> vmlist;
	/** number of VMs. */
	private static int numVms = 4;
	/** GPU work (seconds on a whole L40S) of the task of each VM; 0 for no task */
	private static int[] gpuTaskSeconds = { 300, 150, 0, 0 };
	/**
	 * The resolution in which progress in evaluated.
	 */
	private static double schedulingInterval = 20;

	/**
	 * Creates main() to run this example.
	 * 
	 * @param args the args
	 */
	public static void main(String[] args) {
		Log.printLine("Starting CloudSimGpuExample2...");
		Log.printLine("== Best effort vGPU scheduler (NVIDIA default) ==");
		run(true);
		Log.printLine("== Equal share vGPU scheduler ==");
		run(false);
		Log.printLine("CloudSimGpuExample2 finished!");
	}

	/**
	 * Runs the simulation.
	 * 
	 * @param bestEffort whether pGPUs use the best effort vGPU scheduler, or the
	 *                   equal share one otherwise
	 */
	@SuppressWarnings("unused")
	private static void run(boolean bestEffort) {
		try {
			// number of cloud users
			int num_user = 1;
			Calendar calendar = Calendar.getInstance();
			// trace events
			boolean trace_flag = true;

			// CloudSim initialization
			CloudSim.init(num_user, calendar, trace_flag);

			// Create one Datacenter
			GpuDatacenter datacenter = createDatacenter("Datacenter", bestEffort);

			// Create one Broker
			GpuDatacenterBroker broker = createBroker("Broker");
			int brokerId = broker.getId();

			// Create a list to hold created VMs
			vmlist = new ArrayList<GpuVm>();
			// Create a list to hold issued Cloudlets
			cloudletList = new ArrayList<GpuCloudlet>();

			// Create VMs and their cloudlets
			for (int i = 0; i < numVms; i++) {
				int vmId = i;
				int vgpuId = i;
				GpuVm vm = createGpuVm(vmId, vgpuId, brokerId);
				vmlist.add(vm);
				if (gpuTaskSeconds[i] > 0) {
					GpuCloudlet gpuCloudlet = createGpuCloudlet(i, i, brokerId, gpuTaskSeconds[i]);
					gpuCloudlet.setVmId(vmId);
					cloudletList.add(gpuCloudlet);
				}
			}

			// submit vm list to the broker
			broker.submitGuestList(vmlist);

			// submit cloudlet list to the broker
			broker.submitCloudletList(cloudletList);

			// Disable Logs
			Log.disable();
			// Starts the simulation
			CloudSim.startSimulation();

			CloudSim.stopSimulation();
			Log.enable();

			// Print results when simulation is over
			List<Cloudlet> newList = broker.getCloudletReceivedList();
			printVmList();
			printGpuTaskList(newList);
		} catch (Exception e) {
			e.printStackTrace();
			Log.printLine("Unwanted errors happen");
		}
	}

	/**
	 * Create a GpuCloudlet
	 * 
	 * @param gpuCloudletId gpuCloudlet id
	 * @param gpuTaskId     gpuCloudlet's gpuTask id
	 * @param brokerId      the broker to which the gpuCloudlet belongs
	 * @param gpuSeconds    the time the gpuTask takes on a whole L40S
	 * @return the gpuCloudlet
	 */
	private static GpuCloudlet createGpuCloudlet(int gpuCloudletId, int gpuTaskId, int brokerId, int gpuSeconds) {
		// Cloudlet properties; 100 seconds on a CPU core
		long length = (long) (100 * GpuHostTags.DUAL_INTEL_XEON_PLATINUM_8380_PE_MIPS);
		long fileSize = 300;
		long outputSize = 300;
		int pesNumber = 1;
		UtilizationModel cpuUtilizationModel = new UtilizationModelFull();
		UtilizationModel ramUtilizationModel = new UtilizationModelFull();
		UtilizationModel bwUtilizationModel = new UtilizationModelFull();

		// GpuTask properties; one block per SM
		long taskLength = (long) (VideoCardTags.NVIDIA_L40S_CARD_PE_MIPS * gpuSeconds);
		long taskInputSize = 4 * 1024;
		long taskOutputSize = 4 * 1024;
		long requestedGddramSize = 8 * 1024;
		int numberOfBlocks = VideoCardTags.NVIDIA_L40S_CARD_GPU_PES;
		UtilizationModel gpuUtilizationModel = new UtilizationModelFull();
		UtilizationModel gddramUtilizationModel = new UtilizationModelFull();
		UtilizationModel gddramBwUtilizationModel = new UtilizationModelFull();

		GpuTask gpuTask = new GpuTask(gpuTaskId, taskLength, numberOfBlocks, taskInputSize, taskOutputSize,
				requestedGddramSize, gpuUtilizationModel, gddramUtilizationModel, gddramBwUtilizationModel);

		GpuCloudlet gpuCloudlet = new GpuCloudlet(gpuCloudletId, length, pesNumber, fileSize, outputSize,
				cpuUtilizationModel, ramUtilizationModel, bwUtilizationModel, gpuTask, false);

		gpuCloudlet.setUserId(brokerId);
		return gpuCloudlet;
	}

	/**
	 * Create a GpuVM
	 * 
	 * @param vmId     vm id
	 * @param vgpuId   vm's vgpu id
	 * @param brokerId the broker to which this vm belongs
	 * @return the GpuVm
	 */
	private static GpuVm createGpuVm(int vmId, int vgpuId, int brokerId) {
		// VM description
		double mips = GpuHostTags.DUAL_INTEL_XEON_PLATINUM_8380_PE_MIPS;
		// image size (MB)
		int size = 100 * 1024;
		// vm memory (MB)
		int ram = 64 * 1024;
		// 1 Gbit/s in MB/s
		long bw = 125;
		// number of cpus
		int pesNumber = 16;
		// VMM name
		String vmm = "vSphere";

		// Create a VM
		GpuVm vm = new GpuVm(vmId, brokerId, mips, pesNumber, ram, bw, size, vmm, "Custom",
				new GpuCloudletSchedulerTimeShared());
		// Create GpuTask Scheduler
		GpuTaskSchedulerLeftover gpuTaskScheduler = new GpuTaskSchedulerLeftover();
		// Create a 16 GB Q-series vGPU of the L40S
		Vgpu vgpu = GridVgpuTags.getL40SQ(vgpuId, 16 * 1024, gpuTaskScheduler);
		vm.setVgpu(vgpu);
		return vm;
	}

	/**
	 * Create a datacenter.
	 * 
	 * @param name       the name of the datacenter
	 * @param bestEffort whether pGPUs use the best effort vGPU scheduler
	 * 
	 * @return the datacenter
	 */
	private static GpuDatacenter createDatacenter(String name, boolean bestEffort) {
		// We need to create a list to store our machines
		List<GpuHost> hostList = new ArrayList<GpuHost>();
		// Number of host's video cards
		int numVideoCards = GpuHostTags.DUAL_INTEL_XEON_PLATINUM_8380_L40S_NUM_VIDEO_CARDS;
		// To hold video cards
		List<VideoCard> videoCards = new ArrayList<VideoCard>(numVideoCards);
		for (int videoCardId = 0; videoCardId < numVideoCards; videoCardId++) {
			List<Pgpu> pgpus = new ArrayList<Pgpu>();
			// Adding an NVIDIA L40S Card
			double mips = VideoCardTags.NVIDIA_L40S_CARD_PE_MIPS;
			int gddram = VideoCardTags.NVIDIA_L40S_CARD_GPU_MEM;
			long bw = VideoCardTags.NVIDIA_L40S_CARD_BW_PER_BUS;
			for (int pgpuId = 0; pgpuId < VideoCardTags.NVIDIA_L40S_CARD_GPUS; pgpuId++) {
				List<Pe> pes = new ArrayList<Pe>();
				for (int peId = 0; peId < VideoCardTags.NVIDIA_L40S_CARD_GPU_PES; peId++) {
					pes.add(new Pe(peId, new GpuPeProvisionerSimple(mips)));
				}
				pgpus.add(new Pgpu(pgpuId, VideoCardTags.NVIDIA_L40S_GPU_TYPE, pes,
						new GpuGddramProvisionerSimple(gddram), new GpuBwProvisionerShared(bw)));
			}
			// Pgpu selection policy
			PgpuSelectionPolicy pgpuSelectionPolicy = new PgpuSelectionPolicySimple();
			// Vgpu Scheduler; pGPUs are in equal-size mode by default
			VgpuScheduler vgpuScheduler = bestEffort
					? new GridVgpuSchedulerBestEffort(VideoCardTags.NVIDIA_L40S_CARD, pgpus, pgpuSelectionPolicy,
							new PerformanceModelGpuNull())
					: new GridVgpuSchedulerFairShareEx(VideoCardTags.NVIDIA_L40S_CARD, pgpus, pgpuSelectionPolicy,
							new PerformanceModelGpuNull());
			// PCI Express Bus Bw Provisioner
			VideoCardBwProvisioner videoCardBwProvisioner = new VideoCardBwProvisionerShared(BusTags.PCI_E_4_X16_BW);
			// Create a video card
			VideoCard videoCard = new VideoCard(videoCardId, VideoCardTags.NVIDIA_L40S_CARD, vgpuScheduler,
					videoCardBwProvisioner);
			videoCards.add(videoCard);
		}

		// Create a host
		int hostId = 0;

		// A Machine contains one or more PEs or CPUs/Cores.
		List<Pe> peList = new ArrayList<Pe>();

		// PE's MIPS power
		double mips = GpuHostTags.DUAL_INTEL_XEON_PLATINUM_8380_PE_MIPS;

		for (int peId = 0; peId < GpuHostTags.DUAL_INTEL_XEON_PLATINUM_8380_NUM_PES; peId++) {
			// Create PEs and add these into a list.
			peList.add(new Pe(peId, new PeProvisionerSimple(mips)));
		}

		// Create Host with its id and list of PEs and add them to the list of machines
		// host memory (MB)
		int ram = GpuHostTags.DUAL_INTEL_XEON_PLATINUM_8380_RAM;
		// host storage
		long storage = GpuHostTags.DUAL_INTEL_XEON_PLATINUM_8380_STORAGE;
		// host BW
		int bw = GpuHostTags.DUAL_INTEL_XEON_PLATINUM_8380_BW;
		// Set VM Scheduler
		VmScheduler vmScheduler = new VmSchedulerTimeShared(peList);
		// Video Card Selection Policy
		VideoCardAllocationPolicy videoCardAllocationPolicy = new VideoCardAllocationPolicySimple(videoCards);
		GpuHost newHost = new GpuHost(hostId, GpuHostTags.DUAL_INTEL_XEON_PLATINUM_8380_L40S,
				new RamProvisionerSimple(ram), new BwProvisionerSimple(bw), storage, peList, vmScheduler,
				videoCardAllocationPolicy);
		hostList.add(newHost);

		// Create a DatacenterCharacteristics object that stores the
		// properties of a data center: architecture, OS, list of
		// Machines, allocation policy: time- or space-shared, time zone
		// and its price (G$/Pe time unit).
		// system architecture
		String arch = "x86";
		// operating system
		String os = "Linux";
		// VM Manager
		String vmm = "vSphere";
		// time zone this resource located (Tehran)
		double time_zone = +3.5;
		// the cost of using processing in this resource
		double cost = 0.0;
		// the cost of using memory in this resource
		double costPerMem = 0.00;
		// the cost of using storage in this resource
		double costPerStorage = 0.000;
		// the cost of using bw in this resource
		double costPerBw = 0.0;
		// we are not adding SAN devices by now
		LinkedList<Storage> storageList = new LinkedList<Storage>();

		DatacenterCharacteristics characteristics = new DatacenterCharacteristics(arch, os, vmm, hostList, time_zone,
				cost, costPerMem, costPerStorage, costPerBw);

		// We need to create a Datacenter object.
		GpuDatacenter datacenter = null;
		try {
			datacenter = new GpuDatacenter(name, characteristics, new GpuVmAllocationPolicySimple(hostList),
					storageList, schedulingInterval);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return datacenter;
	}

	/**
	 * Create a broker.
	 * 
	 * * @param name the name of the broker
	 * 
	 * @return the datacenter broker
	 */
	private static GpuDatacenterBroker createBroker(String name) {
		GpuDatacenterBroker broker = null;
		try {
			broker = new GpuDatacenterBroker(name);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return broker;
	}

	/**
	 * Prints whether each VM was placed. A VM that could not be placed is still
	 * being instantiated when the simulation ends.
	 */
	private static void printVmList() {
		AsciiTable at = new AsciiTable();
		at.addRule();
		at.addRow("VM ID", "vGPU Type", "Placed");
		at.addRule();
		for (GpuVm vm : vmlist) {
			at.addRow(vm.getId(), vm.getVgpu().getType(), vm.isBeingInstantiated() ? "No (rejected)" : "Yes");
			at.addRule();
		}
		Log.printLine(at.render());
	}

	/**
	 * Prints the GpuTasks of the GpuCloudlets.
	 * 
	 * @param list list of GpuCloudlets
	 */
	private static void printGpuTaskList(List<Cloudlet> gpuCloudlets) {
		DecimalFormat dft = new DecimalFormat("###.##");
		AsciiTable at = new AsciiTable();
		at.addRule();
		at.addRow("Task ID", "VM ID", "vGPU Type", "Status", "GPU Time", "Start Time", "Finish Time");
		at.addRule();
		for (GpuCloudlet gpuCloudlet : (List<GpuCloudlet>) (List<?>) gpuCloudlets) {
			GpuTask gpuTask = gpuCloudlet.getGpuTask();
			GpuVm vm = (GpuVm) VmList.getById(vmlist, gpuCloudlet.getVmId());
			at.addRow(gpuTask.getTaskId(), vm.getId(), vm.getVgpu(gpuTask.getVgpuId()).getType(),
					gpuTask.getTaskStatus() == GpuTask.SUCCESS ? "SUCCESS" : "FAILED",
					dft.format(gpuTask.getActualGPUTime()), dft.format(gpuTask.getExecStartTime()),
					dft.format(gpuTask.getFinishTime()));
			at.addRule();
		}
		Log.printLine(at.render());
	}
}
