package org.cloudbus.cloudsim.gpu;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.VmScheduler;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.gpu.allocation.VideoCardAllocationPolicy;
import org.cloudbus.cloudsim.provisioners.BwProvisioner;
import org.cloudbus.cloudsim.provisioners.RamProvisioner;

/**
 * 
 * {@link GpuHost} extends {@link Host} and supports {@link VideoCard}s through
 * a {@link VideoCardAllocationPolicy}.
 * 
 * @author Ahmad Siavashi
 * 
 */
public class GpuHost extends Host {

	/**
	 * type of the host
	 */
	public String type;

	/** video card allocation policy */
	private VideoCardAllocationPolicy videoCardAllocationPolicy;

	/**
	 * 
	 * See {@link Host#Host}
	 * 
	 * @param type                      type of the host which is specified in
	 *                                  {@link GpuHostTags}.
	 * @param videoCardAllocationPolicy the policy in which the host allocates video
	 *                                  cards to vms
	 */
	public GpuHost(int id, String type, RamProvisioner ramProvisioner, BwProvisioner bwProvisioner, long storage,
			List<? extends Pe> peList, VmScheduler vmScheduler, VideoCardAllocationPolicy videoCardAllocationPolicy) {
		super(id, ramProvisioner, bwProvisioner, storage, peList, vmScheduler);
		setType(type);
		setVideoCardAllocationPolicy(videoCardAllocationPolicy);
	}

	/**
	 * 
	 * See {@link Host#Host}
	 * 
	 * @param type type of the host which is specified in {@link GpuHostTags}.
	 */
	public GpuHost(int id, String type, RamProvisioner ramProvisioner, BwProvisioner bwProvisioner, long storage,
			List<? extends Pe> peList, VmScheduler vmScheduler) {
		super(id, ramProvisioner, bwProvisioner, storage, peList, vmScheduler);
		setType(type);
		setVideoCardAllocationPolicy(null);
	}

	public double updateVgpusProcessing(double currentTime) {
		double smallerTime = Double.MAX_VALUE;
		if (isGpuEquipped()) {
			List<Vgpu> runningVgpus = getRunningVgpus();
			smallerTime = updateVgpusProcessing(currentTime, new ArrayList<Vgpu>(runningVgpus));
			// Tasks have started or finished, which may change the MIPS of vGPUs
			if (!runningVgpus.equals(getRunningVgpus())) {
				smallerTime = updateVgpusProcessing(currentTime, getRunningVgpus());
			}
		}
		return smallerTime;
	}

	/**
	 * Updates resident vGPUs. The MIPS of all vGPUs are read before any of them is
	 * updated, as they belong to the time that has passed.
	 */
	protected double updateVgpusProcessing(double currentTime, List<Vgpu> runningVgpus) {
		double smallerTime = Double.MAX_VALUE;
		Map<Vgpu, List<Double>> vgpuMips = new LinkedHashMap<Vgpu, List<Double>>();
		for (Vgpu vgpu : getVideoCardAllocationPolicy().getVgpuVideoCardMap().keySet()) {
			vgpuMips.put(vgpu, getVgpuMips(vgpu, runningVgpus));
		}
		for (Entry<Vgpu, List<Double>> entry : vgpuMips.entrySet()) {
			double time = entry.getKey().updateGpuTaskProcessing(currentTime, entry.getValue());
			if (time > 0.0 && time < smallerTime) {
				smallerTime = time;
			}
		}
		return smallerTime;
	}

	/**
	 * @return the MIPS that are available to the vGPU
	 */
	protected List<Double> getVgpuMips(Vgpu vgpu, List<Vgpu> runningVgpus) {
		return getVideoCardAllocationPolicy().getVgpuVideoCardMap().get(vgpu).getVgpuScheduler()
				.getAllocatedMipsForVgpu(vgpu);
	}

	/**
	 * @return resident vGPUs that have running tasks
	 */
	protected List<Vgpu> getRunningVgpus() {
		List<Vgpu> runningVgpus = new ArrayList<Vgpu>();
		for (Vgpu vgpu : getVideoCardAllocationPolicy().getVgpuVideoCardMap().keySet()) {
			if (vgpu.getGpuTaskScheduler().runningTasks() > 0) {
				runningVgpus.add(vgpu);
			}
		}
		return runningVgpus;
	}

	@Override
	public boolean isSuitableForGuest(GuestEntity guest) {
		boolean result = guestCreate(guest);
		if (result) {
			guestDestroy(guest);
		}
		return result;
	}

	/**
	 * @return the videoCardAllocationPolicy
	 */
	public VideoCardAllocationPolicy getVideoCardAllocationPolicy() {
		return videoCardAllocationPolicy;
	}

	/**
	 * @param videoCardAllocationPolicy the videoCardAllocationPolicy to set
	 */
	public void setVideoCardAllocationPolicy(VideoCardAllocationPolicy videoCardAllocationPolicy) {
		this.videoCardAllocationPolicy = videoCardAllocationPolicy;
	}

	/**
	 * Checks the existence of a given video card id in the host
	 * 
	 * @param videoCardId id of the video card
	 * @return
	 */
	public boolean hasVideoCard(int videoCardId) {
		if (!isGpuEquipped()) {
			return false;
		}
		for (VideoCard videoCard : getVideoCardAllocationPolicy().getVideoCards()) {
			if (videoCard.getId() == videoCardId) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks the existence of a given pgpu id in the host
	 * 
	 * @param pgpuId id of the video card
	 * @return
	 */
	public boolean hasPgpu(int pgpuId) {
		if (!isGpuEquipped()) {
			return false;
		}
		for (VideoCard videoCard : getVideoCardAllocationPolicy().getVideoCards()) {
			for (Pgpu pgpu : videoCard.getVgpuScheduler().getPgpuList()) {
				if (pgpu.getId() == pgpuId) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean isGpuEquipped() {
		return getVideoCardAllocationPolicy() != null && !getVideoCardAllocationPolicy().getVideoCards().isEmpty();
	}

	public void vgpuDestroy(Vgpu vgpu) {
		if (vgpu != null) {
			getVideoCardAllocationPolicy().deallocate(vgpu);
		}
	}

	public boolean vgpuCreate(Vgpu vgpu) {
		return getVideoCardAllocationPolicy().allocate(vgpu, vgpu.getPCIeBw());
	}

	public boolean vgpuCreate(Vgpu vgpu, Pgpu pgpu) {
		return getVideoCardAllocationPolicy().allocate(pgpu, vgpu, vgpu.getPCIeBw());
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param type the type to set
	 */
	protected void setType(String type) {
		this.type = type;
	}

	public Set<Vgpu> getVgpuSet() {
		if (!isGpuEquipped()) {
			return null;
		}
		return getVideoCardAllocationPolicy().getVgpuVideoCardMap().keySet();
	}

	public boolean isIdle() {

		if (!getVmList().isEmpty()) {
			return false;
		} else if (getVgpuSet() != null && !getVgpuSet().isEmpty()) {
			return false;
		}

		return true;
	}
}
