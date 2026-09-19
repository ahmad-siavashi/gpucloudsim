package org.cloudbus.cloudsim.gpu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.gpu.selection.PgpuSelectionPolicy;

/**
 * VgpuScheduler is an abstract class that represents the policy used by a
 * Virtual Machine Monitor (VMM) to share processing power of a
 * {@link VideoCard} among {@link Vgpu Vgpus} running in a {@link VideoCard}.*
 * 
 * @author Ahmad Siavashi
 */
public abstract class VgpuScheduler {

	/** Type of the video card which this scheduler is associated with. */
	private String videoCardType;

	/**
	 * The {@link Pgpu GPUs} of the video card that the scheduler is associated
	 * with.
	 */
	private List<Pgpu> pgpuList;

	/** Pgpu to Vgpu Mapping */
	private Map<Pgpu, List<Vgpu>> pgpuVgpuMap;

	/**
	 * Vgpus to allocated PEs map.
	 */
	private Map<Vgpu, List<Pe>> vgpuPeMap;

	/**
	 * The map of Vgpus to MIPS
	 */
	private Map<Vgpu, List<Double>> mipsMap;

	/** The policy of allocating video card's pgpus to vgpus */
	private PgpuSelectionPolicy pgpuSelectionPolicy;

	/**
	 * Creates a new VgpuScheduler.
	 * 
	 * @param videoCardType       type of the video card (see
	 *                            {@link VideoCardTags})
	 * @param pgpuList            list of video card pgpus
	 * @param pgpuSelectionPolicy the policy of video card pgpus allocation to vgpus
	 */
	public VgpuScheduler(String videoCardType, List<Pgpu> pgpuList, PgpuSelectionPolicy pgpuSelectionPolicy) {
		setVideoCardType(videoCardType);
		setPgpuList(pgpuList);
		setPgpuVgpuMap(new HashMap<Pgpu, List<Vgpu>>());
		for (Pgpu pgpu : getPgpuList()) {
			getPgpuVgpuMap().put(pgpu, new ArrayList<Vgpu>());
		}
		setVgpuPeMap(new HashMap<Vgpu, List<Pe>>());
		setMipsMap(new HashMap<Vgpu, List<Double>>());
		setPgpuSelectionPolicy(pgpuSelectionPolicy);

	}

	/**
	 * Requests the allocation of PEs for a Vgpu.
	 * 
	 * @param vgpu      the vgpu
	 * @param mipsShare the list of MIPS share to be allocated to a Vgpu
	 * @return $true if this policy allows a new Vgpu in the video card, $false
	 *         otherwise
	 * @pre $none
	 * @post $none
	 */
	public boolean allocatePgpuForVgpu(Vgpu vgpu, List<Double> mipsShare, int gddramShare, long bwShare) {
		final Vm vm = vgpu.getVm();
		List<Pgpu> candidates = (List<Pgpu>) CollectionUtils.select(getPgpuList(), new Predicate() {
			@Override
			public boolean evaluate(Object arg) {
				Pgpu pgpu = (Pgpu) arg;
				return isSuitable(pgpu, vgpu);
			}
		});
		Pgpu selectedPgpu = getPgpuSelectionPolicy().selectPgpu(vgpu, this, candidates);
		// if there is no candidate,
		if (selectedPgpu == null) {
			return false;
		}
		return allocatePgpuForVgpu(selectedPgpu, vgpu, mipsShare, gddramShare, bwShare);
	}

	/**
	 * Requests the allocation of PEs for a Vgpu.
	 * 
	 * @param pgpu      the pgpu to allocate on
	 * @param vgpu      the vgpu
	 * @param mipsShare the list of MIPS share to be allocated to a Vgpu
	 * @return $true if this policy allows a new Vgpu in the video card, $false
	 *         otherwise
	 * @pre $none
	 * @post $none
	 */
	public abstract boolean allocatePgpuForVgpu(Pgpu pgpu, Vgpu vgpu, List<Double> mipsShare, int gddramShare,
			long bwShare);

	/**
	 * Checks the possibility of resource allocation for the given vgpu.
	 * 
	 * @param vgpu the vgpu
	 * @return $true if this policy allows a new Vgpu in the video card, $false
	 *         otherwise
	 */
	public boolean isSuitable(Vgpu vgpu) {
		for (Pgpu pgpu : getPgpuList()) {
			if (isSuitable(pgpu, vgpu)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks the possibility of resource allocation for the given vgpu.
	 * 
	 * @param pgpu the pgpu to check
	 * @param vgpu the vgpu
	 * @return $true if this policy allows a new Vgpu in the video card, $false
	 *         otherwise
	 */
	public abstract boolean isSuitable(Pgpu pgpu, Vgpu vgpu);

	/**
	 * Releases PEs allocated to a {@link Vgpu}. After that, the PEs may be used on
	 * demand by other Vgpus.
	 * 
	 * @param vgpu the vgpu
	 */
	public abstract void deallocatePgpuForVgpu(Vgpu vgpu);

	/**
	 * Releases PEs allocated to all the Vgpus of the video card the VgpuScheduler
	 * is associated to. After that, all PEs will be available to be used on demand
	 * for requesting Vgpus.
	 */
	public void deallocatePgpusForAllVgpus() {
		getMipsMap().clear();
		getVgpuPeMap().clear();
		for (Pgpu pgpu : getPgpuList()) {
			for (Pe pe : pgpu.getPeList()) {
				pe.getPeProvisioner().deallocateMipsForAllVms();
			}
		}
		for (List<Vgpu> vgpus : getPgpuVgpuMap().values()) {
			vgpus.clear();
		}
	}

	/**
	 * Gets the pes allocated for a vgpu.
	 * 
	 * @param vgpu the vgpu
	 * @return the pes allocated for the given vgpu
	 */
	public List<Pe> getPesAllocatedForVgpu(Vgpu vgpu) {
		return getVgpuPeMap().get(vgpu);
	}

	/**
	 * Returns the MIPS share of each host's Pe that is allocated to a given Vgpu.
	 * 
	 * @param vgpu the vgpu
	 * @return an array containing the amount of MIPS of each pe that is available
	 *         to the Vgpu
	 */
	public List<Double> getAllocatedMipsForVgpu(Vgpu vgpu) {
		return getMipsMap().get(vgpu);
	}

	/**
	 * Returns the available memory for all on-board pGPUs.
	 */
	public Map<Pgpu, Integer> getPgpusAvailableMemory() {
		Map<Pgpu, Integer> pgpusAvailableMemory = new HashMap<>();
		for (Pgpu pgpu : getPgpuList()) {
			Integer availableMemory = pgpu.getGddramProvisioner().getAvailableGddram();
			pgpusAvailableMemory.put(pgpu, availableMemory);
		}
		return pgpusAvailableMemory;
	}

	/**
	 * Returns minimum free memory over all GPUs.
	 */
	public int getMinAvailableMemory() {
		Integer minAvailableMemory = getPgpusAvailableMemory().values().stream().mapToInt(v -> v).min()
				.orElseThrow(NoSuchElementException::new);
		return minAvailableMemory;
	}

	/**
	 * Returns maximum free memory over all GPUs.
	 */
	public int getMaxAvailableMemory() {
		Integer maxAvailableMemory = getPgpusAvailableMemory().values().stream().mapToInt(v -> v).max()
				.orElseThrow(NoSuchElementException::new);
		return maxAvailableMemory;
	}

	/**
	 * Gets the total allocated MIPS for a Vgpu along all its allocated PEs.
	 * 
	 * @param vgpu the vgpu
	 * @return the total allocated mips for the vgpu
	 */
	public double getTotalAllocatedMipsForVgpu(Vgpu vgpu) {
		double allocated = 0;
		List<Double> mipsMap = getAllocatedMipsForVgpu(vgpu);
		if (mipsMap != null) {
			for (double mips : mipsMap) {
				allocated += mips;
			}
		}
		return allocated;
	}

	/**
	 * Returns the Pgpu allocated to the given Vgpu.
	 * 
	 * @param vgpu the vgpu
	 * @return the pgpu allocated to the given vgpu
	 */
	public Pgpu getPgpuForVgpu(Vgpu vgpu) {
		Pgpu pgpu = null;
		for (Entry<Pgpu, List<Vgpu>> entry : getPgpuVgpuMap().entrySet()) {
			if (entry.getValue().contains(vgpu)) {
				pgpu = entry.getKey();
				break;
			}
		}
		return pgpu;
	}

	/**
	 * @return the videoCardType
	 */
	public String getVideoCardType() {
		return videoCardType;
	}

	/**
	 * @param videoCardType the videoCardType to set
	 */
	protected void setVideoCardType(String videoCardType) {
		this.videoCardType = videoCardType;
	}

	/**
	 * @return the pgpuList
	 */
	public final List<Pgpu> getPgpuList() {
		return pgpuList;
	}

	/**
	 * @param pgpuList the pgpuList to set
	 */
	protected void setPgpuList(List<Pgpu> pgpuList) {
		this.pgpuList = pgpuList;
	}

	/**
	 * Returns the number of pgpus in the video card associated with this scheduler.
	 */
	public int getNumberOfPgpus() {
		return getPgpuList().size();
	}

	/**
	 * @return the pgpuVgpuMap
	 */
	public Map<Pgpu, List<Vgpu>> getPgpuVgpuMap() {
		return pgpuVgpuMap;
	}

	/**
	 * @param pgpuVgpuMap the pgpuVgpuMap to set
	 */
	protected void setPgpuVgpuMap(Map<Pgpu, List<Vgpu>> pgpuVgpuMap) {
		this.pgpuVgpuMap = pgpuVgpuMap;
	}

	/**
	 * @return the vgpuPeMap
	 */
	public Map<Vgpu, List<Pe>> getVgpuPeMap() {
		return vgpuPeMap;
	}

	/**
	 * @param vgpuPeMap the vgpuPeMap to set
	 */
	protected void setVgpuPeMap(Map<Vgpu, List<Pe>> vgpuPeMap) {
		this.vgpuPeMap = vgpuPeMap;
	}

	/**
	 * @param mipsMap the mipsMap to set
	 */
	protected void setMipsMap(Map<Vgpu, List<Double>> mipsMap) {
		this.mipsMap = mipsMap;
	}

	/**
	 * @return the mipsMap
	 */
	public Map<Vgpu, List<Double>> getMipsMap() {
		return mipsMap;
	}

	/**
	 * @return the pgpuSelectionPolicy
	 */
	public PgpuSelectionPolicy getPgpuSelectionPolicy() {
		return pgpuSelectionPolicy;
	}

	/**
	 * @param pgpuSelectionPolicy the pgpuSelectionPolicy to set
	 */
	protected void setPgpuSelectionPolicy(PgpuSelectionPolicy pgpuSelectionPolicy) {
		this.pgpuSelectionPolicy = pgpuSelectionPolicy;
	}

}
