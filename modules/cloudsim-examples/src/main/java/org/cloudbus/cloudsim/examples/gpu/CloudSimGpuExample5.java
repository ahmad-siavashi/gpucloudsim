package org.cloudbus.cloudsim.examples.gpu;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.gpu.BusTags;
import org.cloudbus.cloudsim.gpu.GpuCloudlet;
import org.cloudbus.cloudsim.gpu.GpuCloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.gpu.GpuHost;
import org.cloudbus.cloudsim.gpu.GpuHostTags;
import org.cloudbus.cloudsim.gpu.GpuTask;
import org.cloudbus.cloudsim.gpu.GpuTaskSchedulerLeftover;
import org.cloudbus.cloudsim.gpu.GpuVm;
import org.cloudbus.cloudsim.gpu.GpuVmAllocationPolicySimple;
import org.cloudbus.cloudsim.gpu.Pgpu;
import org.cloudbus.cloudsim.gpu.Vgpu;
import org.cloudbus.cloudsim.gpu.VideoCard;
import org.cloudbus.cloudsim.gpu.VideoCardTags;
import org.cloudbus.cloudsim.gpu.allocation.VideoCardAllocationPolicy;
import org.cloudbus.cloudsim.gpu.allocation.VideoCardAllocationPolicySimple;
import org.cloudbus.cloudsim.gpu.hardware_assisted.grid.GridVgpuSchedulerBestEffort;
import org.cloudbus.cloudsim.gpu.hardware_assisted.grid.GridVgpuTags;
import org.cloudbus.cloudsim.gpu.performance.models.PerformanceModelGpuNull;
import org.cloudbus.cloudsim.gpu.power.PowerGpuDatacenter;
import org.cloudbus.cloudsim.gpu.power.PowerGpuDatacenterBroker;
import org.cloudbus.cloudsim.gpu.power.PowerGpuHost;
import org.cloudbus.cloudsim.gpu.power.PowerVideoCard;
import org.cloudbus.cloudsim.gpu.power.PowerVideoCardTags;
import org.cloudbus.cloudsim.gpu.power.models.GpuHostPowerModelLinear;
import org.cloudbus.cloudsim.gpu.power.models.VideoCardPowerModel;
import org.cloudbus.cloudsim.gpu.provisioners.GpuBwProvisionerShared;
import org.cloudbus.cloudsim.gpu.provisioners.GpuGddramProvisionerSimple;
import org.cloudbus.cloudsim.gpu.provisioners.GpuPeProvisionerSimple;
import org.cloudbus.cloudsim.gpu.provisioners.VideoCardBwProvisioner;
import org.cloudbus.cloudsim.gpu.provisioners.VideoCardBwProvisionerShared;
import org.cloudbus.cloudsim.gpu.selection.PgpuSelectionPolicy;
import org.cloudbus.cloudsim.gpu.selection.PgpuSelectionPolicyBreadthFirst;
import org.cloudbus.cloudsim.lists.VmList;
import org.cloudbus.cloudsim.power.models.PowerModel;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

import de.vandermeer.asciitable.AsciiTable;

/**
 * Example 5, energy. A datacenter has three hosts: host 0 and host 2 have no
 * GPU and host 1 has an NVIDIA A16. VM 0 has no vGPU and runs a CPU-only
 * cloudlet on host 0. VMs 1 and 2 have an NVIDIA A16-8Q each, which cannot be
 * placed on host 0, so they go to host 1, where the breadth-first pGPU
 * selection puts them on two of the four pGPUs of the A16. Host 2 stays
 * idle. <br>
 * Hosts and video cards have linear power models; {@link GpuHostTags} and
 * {@link VideoCardTags} give their sources. The results show the energy of the
 * datacenter, of each host (CPUs / whole host) and of each video card. Idle
 * hosts and idle pGPUs still draw power; call
 * {@link PowerVideoCardTags#getA16PowerModel(boolean) getA16PowerModel(true)} to
 * power-gate idle pGPUs. <br>
 * New: power models, energy accounting, and VMs and hosts without GPUs. <br>
 * Next: Example 6 runs GPU tasks on a GPU of another host (GPU remoting).
 * 
 * @author Ahmad Siavashi
 * 
 */
public class CloudSimGpuExample5 {
	/** The cloudlet list. */
	private static List<GpuCloudlet> cloudletList;
	/** The vmlist. */
	private static List<GpuVm> vmlist;
	/** The datacenter list. */
	private static List<PowerGpuDatacenter> datacenterList;
	/** frame buffer (GB) of the vGPU of each VM; 0 for no vGPU */
	private static int[] vgpuGddrams = { 0, 8, 8 };
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
		Log.printLine("Starting CloudSimGpuExample5...");
		try {
			// number of cloud users
			int num_user = 1;
			Calendar calendar = Calendar.getInstance();
			// trace events
			boolean trace_flag = true;

			// CloudSim initialization
			CloudSim.init(num_user, calendar, trace_flag);

			// Create a list to hold created datacenters
			datacenterList = new ArrayList<PowerGpuDatacenter>();
			// Create one Datacenter
			PowerGpuDatacenter datacenter = createDatacenter("Datacenter");
			// add the datacenter to the datacenterList
			datacenterList.add(datacenter);

			// Create one Broker
			PowerGpuDatacenterBroker broker = createBroker("Broker");
			int brokerId = broker.getId();

			// Create a list to hold created VMs
			vmlist = new ArrayList<GpuVm>();
			// Create a list to hold issued Cloudlets
			cloudletList = new ArrayList<GpuCloudlet>();

			// Create VMs and their cloudlets
			for (int i = 0; i < vgpuGddrams.length; i++) {
				int vmId = i;
				int vgpuId = i;
				GpuVm vm = createGpuVm(vmId, vgpuId, vgpuGddrams[i], brokerId);
				vmlist.add(vm);
				GpuCloudlet cloudlet = createGpuCloudlet(i, i, brokerId, vm.hasVgpu());
				cloudlet.setVmId(vmId);
				cloudletList.add(cloudlet);
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
			printCloudletList(newList);
			printEnergy();

			Log.printLine("CloudSimGpuExample5 finished!");
		} catch (Exception e) {
			e.printStackTrace();
			Log.printLine("Unwanted errors happen");
		}
	}

	/**
	 * Create a GpuCloudlet.
	 * 
	 * @param gpuCloudletId gpuCloudlet id
	 * @param gpuTaskId     gpuCloudlet's gpuTask id
	 * @param brokerId      the broker to which the gpuCloudlet belongs
	 * @param hasGpuTask    whether the gpuCloudlet has a gpuTask
	 * @return the gpuCloudlet
	 */
	private static GpuCloudlet createGpuCloudlet(int gpuCloudletId, int gpuTaskId, int brokerId,
			boolean hasGpuTask) {
		// Cloudlet properties; 100 seconds on a CPU core, or 300 without a gpuTask
		long length = (long) ((hasGpuTask ? 100 : 300) * GpuHostTags.DUAL_INTEL_XEON_PLATINUM_8380_PE_MIPS);
		long fileSize = 300;
		long outputSize = 300;
		int pesNumber = 1;
		UtilizationModel cpuUtilizationModel = new UtilizationModelFull();
		UtilizationModel ramUtilizationModel = new UtilizationModelFull();
		UtilizationModel bwUtilizationModel = new UtilizationModelFull();

		GpuTask gpuTask = null;
		if (hasGpuTask) {
			// GpuTask properties; one block per SM, each 300 seconds on an SM
			long taskLength = (long) (VideoCardTags.NVIDIA_A16_CARD_PE_MIPS * 300);
			long taskInputSize = 1024;
			long taskOutputSize = 1024;
			long requestedGddramSize = 4 * 1024;
			int numberOfBlocks = VideoCardTags.NVIDIA_A16_CARD_GPU_PES;
			UtilizationModel gpuUtilizationModel = new UtilizationModelFull();
			UtilizationModel gddramUtilizationModel = new UtilizationModelFull();
			UtilizationModel gddramBwUtilizationModel = new UtilizationModelFull();

			gpuTask = new GpuTask(gpuTaskId, taskLength, numberOfBlocks, taskInputSize, taskOutputSize,
					requestedGddramSize, gpuUtilizationModel, gddramUtilizationModel, gddramBwUtilizationModel);
		}

		GpuCloudlet gpuCloudlet = new GpuCloudlet(gpuCloudletId, length, pesNumber, fileSize, outputSize,
				cpuUtilizationModel, ramUtilizationModel, bwUtilizationModel, gpuTask, false);

		gpuCloudlet.setUserId(brokerId);
		return gpuCloudlet;
	}

	/**
	 * Create a VM.
	 * 
	 * @param vmId       vm id
	 * @param vgpuId     vm's vgpu id
	 * @param vgpuGddram frame buffer (GB) of the vgpu; 0 for no vgpu
	 * @param brokerId   the broker to which this vm belongs
	 * @return the GpuVm
	 */
	private static GpuVm createGpuVm(int vmId, int vgpuId, int vgpuGddram, int brokerId) {
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
		String vmm = "Xen";

		// Create VM
		GpuVm vm = new GpuVm(vmId, brokerId, mips, pesNumber, ram, bw, size, vmm, "Custom",
				new GpuCloudletSchedulerTimeShared());
		if (vgpuGddram > 0) {
			// Create GpuTask Scheduler
			GpuTaskSchedulerLeftover gpuTaskScheduler = new GpuTaskSchedulerLeftover();
			// Create a Q-series vGPU of the A16
			Vgpu vgpu = GridVgpuTags.getA16Q(vgpuId, vgpuGddram * 1024, gpuTaskScheduler);
			vm.setVgpu(vgpu);
		}
		return vm;
	}

	/**
	 * Create a datacenter.
	 * 
	 * @param name the name of the datacenter
	 * 
	 * @return the datacenter
	 */
	private static PowerGpuDatacenter createDatacenter(String name) {
		// We need to create a list to store our machine
		List<GpuHost> hostList = new ArrayList<GpuHost>();

		/* Create 3 hosts, the second one is GPU-equipped */
		hostList.add(createHost(0, null));
		hostList.add(createHost(1, createA16VideoCards()));
		hostList.add(createHost(2, null));

		// Create a DatacenterCharacteristics object that stores the
		// properties of a data center: architecture, OS, list of
		// Machines, allocation policy: time- or space-shared, time zone
		// and its price (G$/Pe time unit).
		// system architecture
		String arch = "x86";
		// operating system
		String os = "Linux";
		// VM Manager
		String vmm = "Xen";
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
		PowerGpuDatacenter datacenter = null;
		try {
			datacenter = new PowerGpuDatacenter(name, characteristics, new GpuVmAllocationPolicySimple(hostList),
					storageList, schedulingInterval);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return datacenter;
	}

	/**
	 * Create the video cards of a host with an NVIDIA A16.
	 * 
	 * @return the video cards
	 */
	private static List<VideoCard> createA16VideoCards() {
		// Number of host's video cards
		int numVideoCards = GpuHostTags.DUAL_INTEL_XEON_PLATINUM_8380_A16_NUM_VIDEO_CARDS;
		// To hold video cards
		List<VideoCard> videoCards = new ArrayList<VideoCard>(numVideoCards);
		for (int videoCardId = 0; videoCardId < numVideoCards; videoCardId++) {
			List<Pgpu> pgpus = new ArrayList<Pgpu>();
			// Adding an NVIDIA A16 Card
			double mips = VideoCardTags.NVIDIA_A16_CARD_PE_MIPS;
			int gddram = VideoCardTags.NVIDIA_A16_CARD_GPU_MEM;
			long bw = VideoCardTags.NVIDIA_A16_CARD_BW_PER_BUS;
			for (int pgpuId = 0; pgpuId < VideoCardTags.NVIDIA_A16_CARD_GPUS; pgpuId++) {
				List<Pe> pes = new ArrayList<Pe>();
				for (int peId = 0; peId < VideoCardTags.NVIDIA_A16_CARD_GPU_PES; peId++) {
					pes.add(new Pe(peId, new GpuPeProvisionerSimple(mips)));
				}
				pgpus.add(new Pgpu(pgpuId, VideoCardTags.NVIDIA_A16_GPU_TYPE, pes,
						new GpuGddramProvisionerSimple(gddram), new GpuBwProvisionerShared(bw)));
			}
			// Pgpu selection policy; spreads vGPUs over pGPUs
			PgpuSelectionPolicy pgpuSelectionPolicy = new PgpuSelectionPolicyBreadthFirst();
			// Scheduler
			GridVgpuSchedulerBestEffort vgpuScheduler = new GridVgpuSchedulerBestEffort(VideoCardTags.NVIDIA_A16_CARD,
					pgpus, pgpuSelectionPolicy, new PerformanceModelGpuNull());
			// PCI Express Bus Bw Provisioner
			VideoCardBwProvisioner videoCardBwProvisioner = new VideoCardBwProvisionerShared(BusTags.PCI_E_4_X16_BW);
			// Video Card Power Model; idle pGPUs are not power-gated
			VideoCardPowerModel videoCardPowerModel = PowerVideoCardTags.getA16PowerModel(false);
			// Create a video card
			PowerVideoCard videoCard = new PowerVideoCard(videoCardId, VideoCardTags.NVIDIA_A16_CARD, vgpuScheduler,
					videoCardBwProvisioner, videoCardPowerModel);
			videoCards.add(videoCard);
		}
		return videoCards;
	}

	/**
	 * Create a dual Intel Xeon Platinum 8380 host.
	 * 
	 * @param hostId     the host id
	 * @param videoCards the video cards of the host, or null for none
	 * @return the host
	 */
	private static PowerGpuHost createHost(int hostId, List<VideoCard> videoCards) {
		// A Machine contains one or more PEs or CPUs/Cores.
		List<Pe> peList = new ArrayList<Pe>();
		for (int peId = 0; peId < GpuHostTags.DUAL_INTEL_XEON_PLATINUM_8380_NUM_PES; peId++) {
			// Create PEs and add these into a list.
			peList.add(new Pe(peId, new PeProvisionerSimple(GpuHostTags.DUAL_INTEL_XEON_PLATINUM_8380_PE_MIPS)));
		}
		// Host Power Model; the video cards have their own power models
		PowerModel powerModel = new GpuHostPowerModelLinear(GpuHostTags.DUAL_INTEL_XEON_PLATINUM_8380_MAX_POWER,
				GpuHostTags.DUAL_INTEL_XEON_PLATINUM_8380_STATIC_POWER_PERCENT);
		// Video Card Selection Policy
		VideoCardAllocationPolicy videoCardAllocationPolicy = videoCards == null ? null
				: new VideoCardAllocationPolicySimple(videoCards);
		return new PowerGpuHost(hostId,
				videoCards == null ? GpuHostTags.DUAL_INTEL_XEON_PLATINUM_8380
						: GpuHostTags.DUAL_INTEL_XEON_PLATINUM_8380_A16,
				new RamProvisionerSimple(GpuHostTags.DUAL_INTEL_XEON_PLATINUM_8380_RAM),
				new BwProvisionerSimple(GpuHostTags.DUAL_INTEL_XEON_PLATINUM_8380_BW),
				GpuHostTags.DUAL_INTEL_XEON_PLATINUM_8380_STORAGE, peList, new VmSchedulerTimeShared(peList),
				videoCardAllocationPolicy, powerModel);
	}

	/**
	 * Creates the broker.
	 * 
	 * * @param name the name
	 * 
	 * @return the datacenter broker
	 */
	private static PowerGpuDatacenterBroker createBroker(String name) {
		PowerGpuDatacenterBroker broker = null;
		try {
			broker = new PowerGpuDatacenterBroker(name);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return broker;
	}

	/**
	 * Prints the GpuCloudlets.
	 * 
	 * @param list list of GpuCloudlets
	 */
	private static void printCloudletList(List<Cloudlet> gpuCloudlets) {
		DecimalFormat dft = new DecimalFormat("###.##");
		AsciiTable at = new AsciiTable();
		at.addRule();
		at.addRow("Cloudlet ID", "VM ID", "vGPU Type", "GPU Time", "Start Time", "Finish Time");
		at.addRule();
		for (GpuCloudlet gpuCloudlet : (List<GpuCloudlet>) (List<?>) gpuCloudlets) {
			GpuVm vm = (GpuVm) VmList.getById(vmlist, gpuCloudlet.getVmId());
			GpuTask gpuTask = gpuCloudlet.getGpuTask();
			at.addRow(gpuCloudlet.getCloudletId(), vm.getId(), vm.hasVgpu() ? vm.getVgpu().getType() : "-",
					gpuTask == null ? "-" : dft.format(gpuTask.getActualGPUTime()),
					dft.format(gpuCloudlet.getExecStartTime()), dft.format(gpuCloudlet.getFinishTime()));
			at.addRule();
		}
		Log.printLine(at.render());
	}

	/**
	 * Prints the energy consumed by datacenters, hosts and video cards.
	 */
	private static void printEnergy() {
		DecimalFormat dft = new DecimalFormat("###.##");
		AsciiTable at = new AsciiTable();
		at.addRule();
		at.addRow("Entity", "Energy Consumed (Joules)");
		at.addRule();
		for (PowerGpuDatacenter datacenter : datacenterList) {
			at.addRow("Datacenter #" + datacenter.getId(), dft.format(datacenter.getConsumedEnergy()));
			at.addRule();
			for (Entry<PowerGpuHost, Double> entry : datacenter.getHostEnergyMap().entrySet()) {
				PowerGpuHost host = entry.getKey();
				at.addRow("Host #" + host.getId() + " (CPUs / whole host)",
						dft.format(datacenter.getHostCpuEnergyMap().get(host)) + " / "
								+ dft.format(datacenter.getHostEnergyMap().get(host)));
				at.addRule();
				if (host.isGpuEquipped()) {
					for (PowerVideoCard videoCard : (List<PowerVideoCard>) host.getVideoCardAllocationPolicy()
							.getVideoCards()) {
						at.addRow("Video Card #" + videoCard.getId() + " of Host #" + host.getId(),
								dft.format(datacenter.getHostVideoCardEnergyMap().get(host).get(videoCard)));
						at.addRule();
					}
				}
			}
		}
		Log.printLine(at.render());
	}
}
