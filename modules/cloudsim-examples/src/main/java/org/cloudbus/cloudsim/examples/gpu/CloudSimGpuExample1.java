package org.cloudbus.cloudsim.examples.gpu;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
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
import org.cloudbus.cloudsim.gpu.allocation.VideoCardAllocationPolicy;
import org.cloudbus.cloudsim.gpu.allocation.VideoCardAllocationPolicyDepthFirst;
import org.cloudbus.cloudsim.gpu.hardware_assisted.grid.GridVgpuSchedulerFairShareEx;
import org.cloudbus.cloudsim.gpu.hardware_assisted.grid.GridVgpuTags;
import org.cloudbus.cloudsim.gpu.hardware_assisted.grid.GridVideoCardTags;
import org.cloudbus.cloudsim.gpu.performance.models.PerformanceModelGpuNull;
import org.cloudbus.cloudsim.gpu.provisioners.GpuBwProvisionerShared;
import org.cloudbus.cloudsim.gpu.provisioners.GpuGddramProvisionerSimple;
import org.cloudbus.cloudsim.gpu.provisioners.VideoCardBwProvisioner;
import org.cloudbus.cloudsim.gpu.provisioners.VideoCardBwProvisionerShared;
import org.cloudbus.cloudsim.gpu.selection.PgpuSelectionPolicy;
import org.cloudbus.cloudsim.gpu.selection.PgpuSelectionPolicyDepthFirst;
import org.cloudbus.cloudsim.lists.VmList;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

import de.vandermeer.asciitable.AsciiTable;

/**
 * This example demonstrates the use of gpu package in simulations. <br>
 * GPU virtualization mode: GRID <br>
 * Performance Model: off <br>
 * Interference Model: off <br>
 * Power Model: off
 * 
 * @author Ahmad Siavashi
 * 
 */
public class CloudSimGpuExample1 {
	/** The cloudlet list. */
	private static List<GpuCloudlet> cloudletList;
	/** The vmlist. */
	private static List<GpuVm> vmlist;
	/** number of VMs. */
	private static int numVms = 1;
	/** number of gpuCloudlets */
	private static int numGpuCloudlets = 1;
	/**
	 * The resolution in which progress in evaluated.
	 */
	private static double schedulingInterval = 20;

	/**
	 * Creates main() to run this example.
	 * 
	 * @param args the args
	 */
	@SuppressWarnings("unused")
	public static void main(String[] args) {
		Log.printLine("Starting CloudSimGpuExample1...");

		try {
			// number of cloud users
			int num_user = 1;
			Calendar calendar = Calendar.getInstance();
			// trace events
			boolean trace_flag = true;

			// CloudSim initialization
			CloudSim.init(num_user, calendar, trace_flag);

			// Create one Datacenter
			GpuDatacenter datacenter = createDatacenter("Datacenter");

			// Create one Broker
			GpuDatacenterBroker broker = createBroker("Broker");
			int brokerId = broker.getId();

			// Create a list to hold created VMs
			vmlist = new ArrayList<GpuVm>();
			// Create a list to hold issued Cloudlets
			cloudletList = new ArrayList<GpuCloudlet>();

			// Create VMs
			for (int i = 0; i < numVms; i++) {
				int vmId = i;
				int vgpuId = i;
				// Create a VM
				GpuVm vm = createGpuVm(vmId, vgpuId, brokerId);
				// add the VM to the vmList
				vmlist.add(vm);
			}

			// Create gpuCloudlets
			for (int i = 0; i < numGpuCloudlets; i++) {
				int gpuCloudletId = i;
				int gpuTaskId = i;
				// Create Cloudlet
				GpuCloudlet gpuCloudlet = createGpuCloudlet(gpuCloudletId, gpuTaskId, brokerId);
				// add the cloudlet to the list
				cloudletList.add(gpuCloudlet);
			}

			// Cloudlet-VM assignment
			for (int i = 0; i < numGpuCloudlets; i++) {
				GpuCloudlet cloudlet = cloudletList.get(i);
				cloudlet.setVmId(i % numVms);
			}

			// submit vm list to the broker
			broker.submitVmList(vmlist);

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
			printCloudletList(newList);

			Log.printLine("CloudSimGpuExample1 finished!");
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
	 * @return the gpuCloudlet
	 */
	private static GpuCloudlet createGpuCloudlet(int gpuCloudletId, int gpuTaskId, int brokerId) {
		// Cloudlet properties
		long length = (long) (400 * GpuHostTags.DUAL_INTEL_XEON_E5_2620_V3_PE_MIPS);
		long fileSize = 300;
		long outputSize = 300;
		int pesNumber = 1;
		UtilizationModel cpuUtilizationModel = new UtilizationModelFull();
		UtilizationModel ramUtilizationModel = new UtilizationModelFull();
		UtilizationModel bwUtilizationModel = new UtilizationModelFull();

		// GpuTask properties
		long taskLength = (long) (GridVideoCardTags.NVIDIA_K1_CARD_PE_MIPS * 150);
		long taskInputSize = 128;
		long taskOutputSize = 128;
		long requestedGddramSize = 4 * 1024;
		int numberOfBlocks = 2;
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
		double mips = GpuHostTags.DUAL_INTEL_XEON_E5_2620_V3_PE_MIPS;
		// image size (GB)
		int size = 10;
		// vm memory (GB)
		int ram = 2;
		long bw = 100;
		// number of cpus
		int pesNumber = 4;
		// VMM name
		String vmm = "vSphere";

		// Create a VM
		GpuVm vm = new GpuVm(vmId, brokerId, mips, pesNumber, ram, bw, size, vmm, "Custom",
				new GpuCloudletSchedulerTimeShared());
		// Create GpuTask Scheduler
		GpuTaskSchedulerLeftover gpuTaskScheduler = new GpuTaskSchedulerLeftover();
		// Create a Vgpu
		Vgpu vgpu = GridVgpuTags.getK180Q(vgpuId, gpuTaskScheduler);
		vm.setVgpu(vgpu);
		return vm;
	}

	/**
	 * Create a datacenter.
	 * 
	 * @param name the name of the datacenter
	 * 
	 * @return the datacenter
	 */
	private static GpuDatacenter createDatacenter(String name) {
		// We need to create a list to store our machines
		List<GpuHost> hostList = new ArrayList<GpuHost>();
		// Number of host's video cards
		int numVideoCards = GpuHostTags.DUAL_INTEL_XEON_E5_2620_V3_NUM_VIDEO_CARDS;
		// To hold video cards
		List<VideoCard> videoCards = new ArrayList<VideoCard>(numVideoCards);
		for (int videoCardId = 0; videoCardId < numVideoCards; videoCardId++) {
			List<Pgpu> pgpus = new ArrayList<Pgpu>();
			// Adding an NVIDIA K1 Card
			double mips = GridVideoCardTags.NVIDIA_K1_CARD_PE_MIPS;
			int gddram = GridVideoCardTags.NVIDIA_K1_CARD_GPU_MEM;
			long bw = GridVideoCardTags.NVIDIA_K1_CARD_BW_PER_BUS;
			for (int pgpuId = 0; pgpuId < GridVideoCardTags.NVIDIA_K1_CARD_GPUS; pgpuId++) {
				List<Pe> pes = new ArrayList<Pe>();
				for (int peId = 0; peId < GridVideoCardTags.NVIDIA_K1_CARD_GPU_PES; peId++) {
					pes.add(new Pe(peId, new PeProvisionerSimple(mips)));
				}
				pgpus.add(new Pgpu(pgpuId, GridVideoCardTags.NVIDIA_K1_GPU_TYPE, pes,
						new GpuGddramProvisionerSimple(gddram), new GpuBwProvisionerShared(bw)));
			}
			// Pgpu selection policy
			PgpuSelectionPolicy pgpuSelectionPolicy = new PgpuSelectionPolicyDepthFirst();
			// Vgpu Scheduler
			VgpuScheduler vgpuScheduler = new GridVgpuSchedulerFairShareEx(GridVideoCardTags.NVIDIA_K1_CARD, pgpus,
					pgpuSelectionPolicy, new PerformanceModelGpuNull(), GridVideoCardTags.K1_VGPUS);
			// PCI Express Bus Bw Provisioner
			VideoCardBwProvisioner videoCardBwProvisioner = new VideoCardBwProvisionerShared(BusTags.PCI_E_3_X16_BW);
			// Create a video card
			VideoCard videoCard = new VideoCard(videoCardId, GridVideoCardTags.NVIDIA_K1_CARD, vgpuScheduler,
					videoCardBwProvisioner);
			videoCards.add(videoCard);
		}

		// Create a host
		int hostId = 0;

		// A Machine contains one or more PEs or CPUs/Cores.
		List<Pe> peList = new ArrayList<Pe>();

		// PE's MIPS power
		double mips = GpuHostTags.DUAL_INTEL_XEON_E5_2620_V3_PE_MIPS;

		for (int peId = 0; peId < GpuHostTags.DUAL_INTEL_XEON_E5_2620_V3_NUM_PES; peId++) {
			// Create PEs and add these into a list.
			peList.add(new Pe(0, new PeProvisionerSimple(mips)));
		}

		// Create Host with its id and list of PEs and add them to the list of machines
		// host memory (MB)
		int ram = GpuHostTags.DUAL_INTEL_XEON_E5_2620_V3_RAM;
		// host storage
		long storage = GpuHostTags.DUAL_INTEL_XEON_E5_2620_V3_STORAGE;
		// host BW
		int bw = GpuHostTags.DUAL_INTEL_XEON_E5_2620_V3_BW;
		// Set VM Scheduler
		VmScheduler vmScheduler = new VmSchedulerTimeShared(peList);
		// Video Card Selection Policy
		VideoCardAllocationPolicy videoCardAllocationPolicy = new VideoCardAllocationPolicyDepthFirst(videoCards);
		GpuHost newHost = new GpuHost(hostId, GpuHostTags.DUAL_INTEL_XEON_E5_2620_V3, new RamProvisionerSimple(ram),
				new BwProvisionerSimple(bw), storage, peList, vmScheduler, videoCardAllocationPolicy);
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
		String vmm = "Horizen";
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
	 * Prints the GpuCloudlet objects.
	 * 
	 * @param list list of GpuCloudlets
	 */
	private static void printCloudletList(List<Cloudlet> gpuCloudlets) {
		Log.printLine(String.join("", Collections.nCopies(100, "-")));
		DecimalFormat dft = new DecimalFormat("###.##");
		for (GpuCloudlet gpuCloudlet : (List<GpuCloudlet>) (List<?>) gpuCloudlets) {
			// Cloudlet
			AsciiTable at = new AsciiTable();
			at.addRule();
			at.addRow("Cloudlet ID", "Status", "Datacenter ID", "VM ID", "Time", "Start Time", "Finish Time");
			at.addRule();
			if (gpuCloudlet.getStatus() == Cloudlet.SUCCESS) {
				at.addRow(gpuCloudlet.getCloudletId(), "SUCCESS", gpuCloudlet.getResourceId(), gpuCloudlet.getVmId(),
						dft.format(gpuCloudlet.getActualCPUTime()).toString(),
						dft.format(gpuCloudlet.getExecStartTime()).toString(),
						dft.format(gpuCloudlet.getFinishTime()).toString());
				at.addRule();
			}
			GpuTask gpuTask = gpuCloudlet.getGpuTask();
			// Gpu Task
			at.addRow("Task ID", "Cloudlet ID", "Status", "vGPU Profile", "Time", "Start Time", "Finish Time");
			at.addRule();
			if (gpuTask.getTaskStatus() == GpuTask.SUCCESS) {
				at.addRow(gpuTask.getTaskId(), gpuTask.getCloudlet().getCloudletId(), "SUCCESS",
						((GpuVm) VmList.getById(vmlist, gpuTask.getCloudlet().getVmId())).getVgpu().getType(),
						dft.format(gpuTask.getActualGPUTime()).toString(),
						dft.format(gpuTask.getExecStartTime()).toString(),
						dft.format(gpuTask.getFinishTime()).toString());
				at.addRule();
			}
			Log.printLine(at.render());
			Log.printLine(String.join("", Collections.nCopies(100, "-")));
		}
	}
}