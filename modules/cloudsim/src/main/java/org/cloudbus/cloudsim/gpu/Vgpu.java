package org.cloudbus.cloudsim.gpu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.util.MathUtil;

/**
 * 
 * Vgpu represents the gpu that is associated with a {@link GpuVm}.
 * 
 * @author Ahmad Siavashi
 * 
 */
public class Vgpu {

	/** The virtual GPU unique id. */
	private int id;

	/** The Vm that owns this virtual gpu. */
	private GpuVm vm;

	/** The MIPS capacity of each virtual GPU's PE. */
	private double peMips;

	/** The number of PEs required by the virtual gpu. */
	private int numberOfPes;

	/** The required gddram. */
	private int gddram;

	/** The required memory bw. */
	private long bw;

	/** The required PCIe bw. */
	private int PCIeBw;

	/** The Virtual GPU type. */
	private String type;

	/** The vGPU tenancy */
	private String tenancy;

	/** The video card which the vgpu is assigned to. **/
	private VideoCard videoCard;

	/**
	 * The task scheduler the virtual GPU uses to schedule {@link GpuTask GpuTasks}.
	 */
	private GpuTaskScheduler gpuTaskScheduler;

	/** Indicates if the Vgpu is in migration process. */
	private boolean inMigration;

	/** The current allocated gddram. */
	private int currentAllocatedGDDRam;

	/** The current allocated PCIe bw. */
	private long currentAllocatedBw;

	/** The current allocated mips for each virtual gpu's PE. */
	private List<Double> currentAllocatedMips;

	/** Indicates if the virtual gpu is being instantiated. */
	private boolean beingInstantiated;

	/**
	 * The mips allocation history.
	 * 
	 * @todo Instead of using a list, this attribute would be a map, where the key
	 *       can be the history time and the value the history itself. By this way,
	 *       if one wants to get the history for a given time, he/she doesn't have
	 *       to iterate over the entire list to find the desired entry.
	 */
	private final List<VgpuStateHistoryEntry> stateHistory = new LinkedList<VgpuStateHistoryEntry>();

	public Vgpu(int vgpuId, double mips, int numberOfPes, int gddram, long bw, String type, String tenancy,
			GpuTaskScheduler scheduler, int PCIeBw) {
		
		setInMigration(false);
		setBeingInstantiated(true);
		
		setId(vgpuId);
		// setVm(null);
		setPeMips(mips);
		setNumberOfPes(numberOfPes);
		setGddram(gddram);
		setBw(bw);
		setType(type);
		setTenancy(tenancy);
		setGpuTaskScheduler(scheduler);
		setPCIeBw(PCIeBw);

		setCurrentAllocatedBw(0);
		setCurrentAllocatedMips(null);
		setCurrentAllocatedGddram(0);
	}

	/**
	 * Updates the processing of gpu tasks running on this vgpu.
	 * 
	 * @param currentTime current simulation time
	 * @param mipsShare   list with MIPS share of each Pe available to the scheduler
	 * @return time predicted completion time of the earliest finishing task, or 0
	 *         if there is no next events
	 */
	public double updateGpuTaskProcessing(double currentTime, List<Double> mipsShare) {
		if (mipsShare != null) {
			return getGpuTaskScheduler().updateGpuTaskProcessing(currentTime, mipsShare);
		}
		return 0.0;
	}

	/**
	 * Updates the processing of vgpu memory transfers.
	 * 
	 * @param currentTime current simulation time
	 * @param bw          the available bandwidth to this vgpu
	 * @return time predicted completion time of the earliest memory transfer, or
	 *         {@link Double#MAX_VALUE} if there is no next events
	 */

	/**
	 * Gets the current requested mips.
	 * 
	 * @return the current requested mips
	 */
	public List<Double> getCurrentRequestedMips() {
		List<Double> currentRequestedMips = getGpuTaskScheduler().getCurrentRequestedMips();
		if (isBeingInstantiated()) {
			currentRequestedMips = new ArrayList<Double>(Collections.nCopies(getNumberOfPes(), getPeMips()));
		}
		return currentRequestedMips;
	}

	/**
	 * Gets the current requested total mips.
	 * 
	 * @return the current requested total mips
	 */
	public double getCurrentRequestedTotalMips() {
		return MathUtil.sum(getCurrentRequestedMips());
	}

	/**
	 * Gets the current requested max mips among all virtual PEs.
	 * 
	 * @return the current requested max mips
	 */
	public double getCurrentRequestedMaxMips() {
		return Collections.max(getCurrentRequestedMips());
	}

	/**
	 * Gets the current requested bw.
	 * 
	 * @return the current requested bw
	 */
	public long getCurrentRequestedBw() {
		if (isBeingInstantiated()) {
			return getBw();
		}
		return (long) (getGpuTaskScheduler().getCurrentRequestedUtilizationOfBw() * getBw());
	}

	/**
	 * Gets the current requested gddram.
	 * 
	 * @return the current requested gddram
	 */
	public int getCurrentRequestedGddram() {
		if (isBeingInstantiated()) {
			return getGddram();
		}
		return (int) (getGpuTaskScheduler().getCurrentRequestedUtilizationOfGddram() * getGddram());
	}

	/**
	 * Gets total PE utilization percentage of all workloads running on this virtual
	 * gpu at the given time
	 * 
	 * @param time the time
	 * @return total utilization percentage
	 */
	public double getTotalUtilizationOfVgpu(double time) {
		return getGpuTaskScheduler().getTotalUtilizationOfGpu(time);
	}

	/**
	 * Gets current total GDDRAM utilization percentage of all workloads running on
	 * this virtual gpu at current time
	 * 
	 * @return total utilization percentage
	 */
	public List<Double> getCurrentUtilizationOfGddram() {
		List<Double> gddramUtilization = new ArrayList<Double>();
		for (ResGpuTask rcl : getGpuTaskScheduler().getTaskExecList()) {
			gddramUtilization.add(rcl.getGpuTask().getUtilizationOfGddram(CloudSim.clock()));
		}
		return gddramUtilization;
	}

	/**
	 * Get total virtual gpu utilization (in MIPS) of all tasks running on this
	 * virtual gpu at the given time.
	 * 
	 * @param time the time
	 * @return total virtual gpu utilization in MIPS
	 * @see #getTotalUtilizationOfVgpu(double)
	 */
	public double getTotalUtilizationOfVgpuMips(double time) {
		return getTotalUtilizationOfVgpu(time) * getPeMips() * getNumberOfPes();
	}

	/**
	 * Gets the virtual gpu id.
	 * 
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * Gets the unique id of the virtual gpu, which keys its MIPS in GPU PE
	 * provisioners. It differs from the uid of its VM, so vgpus of the same VM on
	 * the same pgpu are tracked separately.
	 * 
	 * @return the uid
	 */
	public String getUid() {
		return (getVm() == null ? "" : getVm().getUid()) + "-vgpu-" + getId();
	}

	/**
	 * Sets the Vgpu id.
	 * 
	 * @param id the new Vgpu id
	 */
	protected void setId(int id) {
		this.id = id;
	}

	/**
	 * Gets the mips.
	 * 
	 * @return the mips
	 */
	public double getPeMips() {
		return peMips;
	}

	/**
	 * Sets the mips.
	 * 
	 * @param mips the new mips
	 */
	public void setPeMips(double mips) {
		this.peMips = mips;
	}

	/**
	 * Gets the number of pes.
	 * 
	 * @return the number of pes
	 */
	public int getNumberOfPes() {
		return numberOfPes;
	}

	/**
	 * Sets the number of pes.
	 * 
	 * @param numberOfPes the new number of pes
	 */
	public void setNumberOfPes(int numberOfPes) {
		this.numberOfPes = numberOfPes;
	}

	/**
	 * Gets the amount of gddram.
	 * 
	 * @return amount of gddram
	 * @pre $none
	 * @post $none
	 */
	public int getGddram() {
		return gddram;
	}

	/**
	 * Sets the amount of gddram.
	 * 
	 * @param gddram new amount of gddram
	 * @pre gddram > 0
	 * @post $none
	 */
	public void setGddram(int gddram) {
		if (gddram < 0) {
			throw new IllegalArgumentException("gddram cannot be negative");
		} else if (!isBeingInstantiated()) {
			throw new UnsupportedOperationException("cannot resize an allocated vgpu");
		}
		this.gddram = gddram;
	}

	/**
	 * Gets the amount of PCIe bandwidth.
	 * 
	 * @return amount of PCIe bandwidth
	 * @pre $none
	 * @post $none
	 */
	public long getBw() {
		return bw;
	}

	/**
	 * Sets the amount of PCIe bandwidth.
	 * 
	 * @param bw new amount of PCIe bandwidth
	 * @pre bw > 0
	 * @post $none
	 */
	public void setBw(long bw) {
		this.bw = bw;
	}

	/**
	 * Gets the Vgpu type.
	 * 
	 * @return VMM
	 * @pre $none
	 * @post $none
	 */
	public String getType() {
		return type;
	}

	/**
	 * Sets the virtual gpu type.
	 * 
	 * @param type the new virtual gpu type
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * Sets the video card that runs this virtual gpu.
	 * 
	 * @param videoCard video card running the virtual gpu
	 * @pre vm != $null
	 * @post $none
	 */
	public void setGpuVm(GpuVm vm) {
		this.vm = vm;
		if (!vm.getVgpuList().contains(this)) {
			vm.addVgpu(this);
		}
	}

	/**
	 * Gets the video card.
	 * 
	 * @return the video card
	 */
	public GpuVm getVm() {
		return vm;
	}

	/**
	 * Gets the virtual gpu scheduler.
	 * 
	 * @return the virtual gpu scheduler
	 */
	public GpuTaskScheduler getGpuTaskScheduler() {
		return gpuTaskScheduler;
	}

	/**
	 * Sets the virtual gpu scheduler.
	 * 
	 * @param gpuTaskScheduler the new virtual gpu scheduler
	 */
	public void setGpuTaskScheduler(GpuTaskScheduler gpuTaskScheduler) {
		this.gpuTaskScheduler = gpuTaskScheduler;
	}

	/**
	 * Checks if is in migration.
	 * 
	 * @return true, if is in migration
	 */
	public boolean isInMigration() {
		return inMigration;
	}

	/**
	 * Sets the in migration.
	 * 
	 * @param inMigration the new in migration
	 */
	public void setInMigration(boolean inMigration) {
		this.inMigration = inMigration;
	}

	/**
	 * Gets the current allocated gddram.
	 * 
	 * @return the current allocated gddram
	 */
	public int getCurrentAllocatedGDDRam() {
		return currentAllocatedGDDRam;
	}

	/**
	 * Sets the current allocated gddram.
	 * 
	 * @param currentAllocatedRam the new current allocated gddram
	 */
	public void setCurrentAllocatedGddram(int currentAllocatedGDDRam) {
		this.currentAllocatedGDDRam = currentAllocatedGDDRam;
	}

	/**
	 * Gets the current allocated bw.
	 * 
	 * @return the current allocated bw
	 */
	public long getCurrentAllocatedBw() {
		return currentAllocatedBw;
	}

	/**
	 * Sets the current allocated PCIe bw.
	 * 
	 * @param currentAllocatedBw the new current allocated PCIe bw
	 */
	public void setCurrentAllocatedBw(long currentAllocatedBw) {
		this.currentAllocatedBw = currentAllocatedBw;
	}

	/**
	 * Gets the current allocated mips.
	 * 
	 * @return the current allocated mips
	 * @TODO replace returning the field by a call to
	 *       getThreadScheduler().getCurrentMipsShare()
	 */
	public List<Double> getCurrentAllocatedMips() {
		return currentAllocatedMips;
	}

	/**
	 * Sets the current allocated mips.
	 * 
	 * @param currentAllocatedMips the new current allocated mips
	 */
	public void setCurrentAllocatedMips(List<Double> currentAllocatedMips) {
		this.currentAllocatedMips = currentAllocatedMips;
	}

	/**
	 * Checks if is being instantiated.
	 * 
	 * @return true, if is being instantiated
	 */
	public boolean isBeingInstantiated() {
		return beingInstantiated;
	}

	/**
	 * Sets the being instantiated.
	 * 
	 * @param beingInstantiated the new being instantiated
	 */
	public void setBeingInstantiated(boolean beingInstantiated) {
		this.beingInstantiated = beingInstantiated;
	}

	/**
	 * Gets the state history.
	 * 
	 * @return the state history
	 */
	public List<VgpuStateHistoryEntry> getStateHistory() {
		return stateHistory;
	}

	/**
	 * Adds a virtual gpu state history entry.
	 * 
	 * @param time          the time
	 * @param allocatedMips the allocated mips
	 * @param requestedMips the requested mips
	 * @param isInMigration the virtual gpu is in migration
	 */
	public void addStateHistoryEntry(double time, double allocatedMips, double requestedMips, boolean isInMigration) {
		VgpuStateHistoryEntry newState = new VgpuStateHistoryEntry(time, allocatedMips, requestedMips, isInMigration);
		if (!getStateHistory().isEmpty()) {
			VgpuStateHistoryEntry previousState = getStateHistory().get(getStateHistory().size() - 1);
			if (previousState.getTime() == time) {
				getStateHistory().set(getStateHistory().size() - 1, newState);
				return;
			}
		}
		getStateHistory().add(newState);
	}

	/**
	 * @return the PCIeBw
	 */
	public int getPCIeBw() {
		return PCIeBw;
	}

	/**
	 * @param PCIeBw the PCIeBw to set
	 */
	protected void setPCIeBw(int PCIeBw) {
		this.PCIeBw = PCIeBw;
	}

	public VideoCard getVideoCard() {
		return videoCard;
	}

	public void setVideoCard(VideoCard videoCard) {
		this.videoCard = videoCard;
	}

	public String getTenancy() {
		return tenancy;
	}

	protected void setTenancy(String tenancy) {
		this.tenancy = tenancy;
	}
}