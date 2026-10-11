package org.cloudbus.cloudsim.gpu.hardware_assisted.mig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.cloudbus.cloudsim.gpu.Pgpu;
import org.cloudbus.cloudsim.gpu.Vgpu;
import org.cloudbus.cloudsim.gpu.VideoCardTags;

/**
 * Applies the NVIDIA MIG placement rules to the GPUs of a video card:
 * <ul>
 * <li>A vGPU is backed by one GPU instance (GI) of a profile of the video card,
 * e.g. "2g.10gb".</li>
 * <li>A GI of a profile of g memory slices occupies the slices [z, z + g) of
 * its GPU, where z is a multiple of g and at most the last start slice of the
 * profile.</li>
 * <li>The slices a GI occupies must be free, and a GI is never moved once it is
 * placed.</li>
 * </ul>
 * The start slice of a GI is chosen by a {@link MigVgpuPlacementPolicy}, which
 * defaults to the built-in policy of NVIDIA, {@link MigVgpuPlacementPolicyCf}.
 *
 * @author Ahmad Siavashi
 */
public class MigVgpuPlacement {

	private final String videoCardType;
	private final MigGpuTags.Profiles profiles;
	private final MigVgpuPlacementPolicy policy;
	private final Map<Pgpu, Map<Vgpu, Integer>> startSlices = new HashMap<>();

	/**
	 * @param videoCardType the video card type, e.g.
	 *                      {@link MigGpuTags#NVIDIA_A100_40GB_CARD}
	 * @param pgpuList      the GPUs of the video card
	 * @param policy        the policy that chooses the start slice of a GI
	 */
	public MigVgpuPlacement(String videoCardType, List<Pgpu> pgpuList, MigVgpuPlacementPolicy policy) {
		this.videoCardType = videoCardType;
		this.profiles = MigGpuTags.getProfiles(videoCardType);
		if (this.profiles == null) {
			throw new IllegalArgumentException("Not a MIG-enabled video card type: " + videoCardType);
		}
		for (Pgpu pgpu : pgpuList) {
			if (pgpu.getGddramProvisioner().getGddram() != profiles.getGpuGddram()) {
				throw new IllegalArgumentException("A GPU of " + videoCardType + " has " + profiles.getGpuGddram()
						+ " MB, but pGPU #" + pgpu.getId() + " has " + pgpu.getGddramProvisioner().getGddram() + " MB");
			}
		}
		this.policy = policy;
	}

	/**
	 * Instantiates a placement whose GIs are placed by the built-in policy of
	 * NVIDIA.
	 */
	public MigVgpuPlacement(String videoCardType, List<Pgpu> pgpuList) {
		this(videoCardType, pgpuList, new MigVgpuPlacementPolicyCf());
	}

	/**
	 * @return true if a GI of the vGPU can be placed on the GPU
	 */
	public boolean isSuitable(Pgpu pgpu, Vgpu vgpu) {
		return findStartSlice(pgpu, vgpu) >= 0;
	}

	/**
	 * Places a GI of the vGPU on the GPU, at the start slice the policy selects.
	 */
	public void place(Pgpu pgpu, Vgpu vgpu) {
		int z = findStartSlice(pgpu, vgpu);
		if (z < 0) {
			throw new IllegalStateException("No placement for " + vgpu.getType() + " on pGPU #" + pgpu.getId());
		}
		startSlices.computeIfAbsent(pgpu, k -> new HashMap<>()).put(vgpu, z);
	}

	/**
	 * Places a GI of the vGPU at the given start slice of the GPU, bypassing the
	 * placement policy. Used by policies that select the start slice themselves.
	 */
	public void place(Pgpu pgpu, Vgpu vgpu, int startSlice) {
		if (!getStartSlices(pgpu, vgpu).contains(startSlice)) {
			throw new IllegalStateException(
					"Slice " + startSlice + " does not admit " + vgpu.getType() + " on pGPU #" + pgpu.getId());
		}
		startSlices.computeIfAbsent(pgpu, k -> new HashMap<>()).put(vgpu, startSlice);
	}

	public void remove(Pgpu pgpu, Vgpu vgpu) {
		Map<Vgpu, Integer> residents = startSlices.get(pgpu);
		if (residents != null) {
			residents.remove(vgpu);
		}
	}

	/**
	 * @return the start slice of the GI of the vGPU on the GPU, or null if the
	 *         vGPU is not placed on it
	 */
	public Integer getStartSlice(Pgpu pgpu, Vgpu vgpu) {
		return startSlices.getOrDefault(pgpu, Collections.emptyMap()).get(vgpu);
	}

	/**
	 * S(F, p): the start slices of the GPU that admit a GI of the vGPU. Empty if
	 * the vGPU requests no profile of the video card.
	 */
	public List<Integer> getStartSlices(Pgpu pgpu, Vgpu vgpu) {
		MigGpuTags.Profile profile = getProfile(vgpu);
		if (profile == null) {
			return Collections.emptyList();
		}
		return profiles.getStartSlices(profile, getFreeSlices(pgpu));
	}

	/**
	 * CF(G): the configuration flexibility of the GPU in its current
	 * configuration.
	 */
	public int getConfigurationFlexibility(Pgpu pgpu) {
		return profiles.getConfigurationFlexibility(getFreeSlices(pgpu));
	}

	/**
	 * CF(G + {z:g_p}): the configuration flexibility the GPU would have if a GI of
	 * the vGPU were placed at the given start slice.
	 */
	public int getConfigurationFlexibility(Pgpu pgpu, Vgpu vgpu, int startSlice) {
		MigGpuTags.Profile profile = getProfile(vgpu);
		boolean[] free = getFreeSlices(pgpu);
		for (int u = startSlice; u < startSlice + profile.getSlices(); u++) {
			free[u] = false;
		}
		return profiles.getConfigurationFlexibility(free);
	}

	/**
	 * @return whether each memory slice of the GPU is free, i.e. the slices F left
	 *         free by the GIs of its configuration G
	 */
	public boolean[] getFreeSlices(Pgpu pgpu) {
		boolean[] free = new boolean[profiles.getMemorySlices()];
		Arrays.fill(free, true);
		for (Entry<Vgpu, Integer> resident : startSlices.getOrDefault(pgpu, Collections.emptyMap()).entrySet()) {
			MigGpuTags.Profile profile = getProfile(resident.getKey());
			for (int u = resident.getValue(); u < resident.getValue() + profile.getSlices(); u++) {
				free[u] = false;
			}
		}
		return free;
	}

	/**
	 * @return the GIs of the GPU, as a map of each vGPU to its start slice
	 */
	public Map<Vgpu, Integer> getGis(Pgpu pgpu) {
		return Collections.unmodifiableMap(startSlices.getOrDefault(pgpu, Collections.emptyMap()));
	}

	/**
	 * @return the profile of the vGPU, or null if the video card has no such
	 *         profile
	 */
	public MigGpuTags.Profile getProfile(Vgpu vgpu) {
		return profiles.getProfile(MigVgpuTags.getProfileName(vgpu));
	}

	public MigGpuTags.Profiles getProfiles() {
		return profiles;
	}

	public String getVideoCardType() {
		return videoCardType;
	}

	public MigVgpuPlacementPolicy getPolicy() {
		return policy;
	}

	/**
	 * @return the start slice the policy selects for a GI of the vGPU on the GPU,
	 *         or -1 if the GPU admits none
	 */
	protected int findStartSlice(Pgpu pgpu, Vgpu vgpu) {
		if (!MigVgpuTags.getVideoCardType(vgpu).equals(videoCardType)) {
			return -1;
		}
		List<Integer> candidates = new ArrayList<>(getStartSlices(pgpu, vgpu));
		if (candidates.isEmpty()) {
			return -1;
		}
		return policy.selectStartSlice(this, pgpu, vgpu, candidates);
	}

}
