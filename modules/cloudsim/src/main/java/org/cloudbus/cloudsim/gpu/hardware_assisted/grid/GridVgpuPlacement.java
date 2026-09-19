package org.cloudbus.cloudsim.gpu.hardware_assisted.grid;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.cloudbus.cloudsim.gpu.GpuVm;
import org.cloudbus.cloudsim.gpu.Pgpu;
import org.cloudbus.cloudsim.gpu.Vgpu;
import org.cloudbus.cloudsim.gpu.VideoCardTags;

/**
 * Applies NVIDIA vGPU placement rules to the GPUs of a video card:
 * <ul>
 * <li>In equal-size mode, all vGPUs on a GPU have the same frame buffer. In
 * mixed-size mode, they may differ.</li>
 * <li>A vGPU is a Q-series type of the video card, named after its frame
 * buffer, e.g. "NVIDIA A16-4Q".</li>
 * <li>A vGPU of s GB occupies the placements [id, id + s) of its GPU, where id
 * is one of the placement IDs of its type in the mode of the GPU.</li>
 * <li>A VM has at most {@link GridVideoCardTags#MAX_VGPUS_PER_VM} vGPUs, all of
 * the same video card type and series, and on Ampere boards of the same
 * type. vGPUs of a VM may share a GPU.</li>
 * </ul>
 * 
 * @author Ahmad Siavashi
 */
public class GridVgpuPlacement {

	private final String videoCardType;
	private final GridVideoCardTags.Placements placements;
	private final boolean mixedSize;
	private final Map<Pgpu, Map<Vgpu, Integer>> placementIds = new HashMap<>();

	/**
	 * @param videoCardType the video card type, e.g.
	 *                      {@link VideoCardTags#NVIDIA_A16_CARD}
	 * @param pgpuList      the GPUs of the video card
	 * @param mixedSize     whether the GPUs are in mixed-size mode
	 */
	public GridVgpuPlacement(String videoCardType, List<Pgpu> pgpuList, boolean mixedSize) {
		this.videoCardType = videoCardType;
		this.placements = GridVideoCardTags.getPlacements(videoCardType);
		if (this.placements == null) {
			throw new IllegalArgumentException("Unknown video card type: " + videoCardType);
		}
		for (Pgpu pgpu : pgpuList) {
			if (pgpu.getGddramProvisioner().getGddram() != placements.getGpuGddram()) {
				throw new IllegalArgumentException("A GPU of " + videoCardType + " has " + placements.getGpuGddram()
						+ " MB, but pGPU #" + pgpu.getId() + " has " + pgpu.getGddramProvisioner().getGddram() + " MB");
			}
		}
		this.mixedSize = mixedSize;
	}

	/**
	 * @return true if the vGPU can be placed on the GPU
	 */
	public boolean isSuitable(Pgpu pgpu, Vgpu vgpu) {
		return findPlacementId(pgpu, vgpu) >= 0 && isValidForVm(vgpu);
	}

	/**
	 * Places the vGPU at its lowest free placement ID on the GPU.
	 */
	public void place(Pgpu pgpu, Vgpu vgpu) {
		int id = findPlacementId(pgpu, vgpu);
		if (id < 0) {
			throw new IllegalStateException("No placement for " + vgpu.getType() + " on pGPU #" + pgpu.getId());
		}
		placementIds.computeIfAbsent(pgpu, k -> new HashMap<>()).put(vgpu, id);
	}

	public void remove(Pgpu pgpu, Vgpu vgpu) {
		Map<Vgpu, Integer> residents = placementIds.get(pgpu);
		if (residents != null) {
			residents.remove(vgpu);
		}
	}

	/**
	 * @return the placement ID of the vGPU on the GPU, or null if not placed
	 */
	public Integer getPlacementId(Pgpu pgpu, Vgpu vgpu) {
		return placementIds.getOrDefault(pgpu, Collections.emptyMap()).get(vgpu);
	}

	public boolean isMixedSize() {
		return mixedSize;
	}

	protected int findPlacementId(Pgpu pgpu, Vgpu vgpu) {
		if (vgpu.getGddram() % 1024 != 0
				|| !GridVgpuTags.getQType(videoCardType, vgpu.getGddram()).equals(vgpu.getType())) {
			return -1;
		}
		int size = vgpu.getGddram() / 1024;
		int[] ids = placements.getIds(size, mixedSize);
		Map<Vgpu, Integer> residents = placementIds.getOrDefault(pgpu, Collections.emptyMap());
		if (ids == null || residents.size() >= placements.maxVgpusPerGpu) {
			return -1;
		}
		if (!mixedSize && residents.keySet().stream().anyMatch(v -> v.getGddram() != vgpu.getGddram())) {
			return -1;
		}
		for (int id : ids) {
			boolean free = true;
			for (Entry<Vgpu, Integer> resident : residents.entrySet()) {
				int start = resident.getValue();
				int end = start + resident.getKey().getGddram() / 1024;
				if (id < end && start < id + size) {
					free = false;
					break;
				}
			}
			if (free) {
				return id;
			}
		}
		return -1;
	}

	protected boolean isValidForVm(Vgpu vgpu) {
		GpuVm vm = vgpu.getVm();
		if (vm == null) {
			return true;
		}
		List<Vgpu> vgpus = vm.getVgpuList();
		if (vgpus.size() > GridVideoCardTags.MAX_VGPUS_PER_VM) {
			return false;
		}
		for (Vgpu other : vgpus) {
			if (!GridVgpuTags.getVideoCardType(other).equals(GridVgpuTags.getVideoCardType(vgpu))
					|| GridVgpuTags.getSeries(other) != GridVgpuTags.getSeries(vgpu)
					|| placements.sameTypePerVm && !other.getType().equals(vgpu.getType())) {
				return false;
			}
		}
		return true;
	}
}
