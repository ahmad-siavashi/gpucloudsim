package org.cloudbus.cloudsim.gpu.hardware_assisted.mig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.gpu.VideoCardTags;

/**
 * NVIDIA MIG GPU instance (GI) profiles of MIG-enabled {@link VideoCardTags
 * video cards}. Values follow the NVIDIA Multi-Instance GPU User Guide.
 * <p>
 * A GI profile is named after its compute slices and its memory, e.g.
 * "1g.5gb". A GI of a profile occupies {@code g} contiguous memory slices
 * starting at one of the start slices of the profile, and the last start slice
 * {@code s} already accounts for the compute slices of the GPU, so no
 * arrangement on the memory slices exceeds them.
 *
 * @author Ahmad Siavashi
 */
public class MigGpuTags {

	/** A MIG GPU instance profile. */
	public static class Profile {
		/** Profile name, e.g. "2g.10gb" */
		private final String name;
		/** Memory slices the GI occupies */
		private final int slices;
		/** Last start slice permitted by the compute slices of the GPU */
		private final int lastStartSlice;

		Profile(String name, int slices, int lastStartSlice) {
			this.name = name;
			this.slices = slices;
			this.lastStartSlice = lastStartSlice;
		}

		public String getName() {
			return name;
		}

		public int getSlices() {
			return slices;
		}

		public int getLastStartSlice() {
			return lastStartSlice;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	/** The GI profiles of a MIG-enabled GPU. */
	public static class Profiles {
		/** Memory slices of a GPU */
		private final int memorySlices;
		/** Memory of a memory slice (MB) */
		private final int sliceGddram;
		/** Compute slices (SMs are divided among them) */
		private final int computeSlices;
		private final List<Profile> profiles = new ArrayList<>();

		Profiles(int memorySlices, int sliceGddram, int computeSlices) {
			this.memorySlices = memorySlices;
			this.sliceGddram = sliceGddram;
			this.computeSlices = computeSlices;
		}

		/**
		 * @param name    profile name, e.g. "2g.10gb"
		 * @param slices  memory slices the GI occupies
		 * @param maxGis  maximum GIs of the profile on a GPU, which bounds the start
		 *                slices by the compute slices of the GPU
		 */
		Profiles add(String name, int slices, int maxGis) {
			profiles.add(new Profile(name, slices, (maxGis - 1) * slices));
			return this;
		}

		public List<Profile> getProfiles() {
			return Collections.unmodifiableList(profiles);
		}

		/**
		 * @return the profile of the given name, or null if the GPU has no such
		 *         profile
		 */
		public Profile getProfile(String name) {
			for (Profile profile : profiles) {
				if (profile.getName().equals(name)) {
					return profile;
				}
			}
			return null;
		}

		public int getMemorySlices() {
			return memorySlices;
		}

		public int getSliceGddram() {
			return sliceGddram;
		}

		public int getComputeSlices() {
			return computeSlices;
		}

		/** @return the frame buffer (MB) of a GPU of the video card */
		public int getGpuGddram() {
			return memorySlices * sliceGddram;
		}

		/**
		 * S(F, p): the start slices of the profile that are permissible in the given
		 * free memory slices. A start slice is a multiple of the size of the profile,
		 * at most its last start slice, and the slices the GI occupies are all free.
		 *
		 * @param profile the GI profile
		 * @param free    whether each memory slice of the GPU is free
		 * @return the permissible start slices, in increasing order
		 */
		public List<Integer> getStartSlices(Profile profile, boolean[] free) {
			List<Integer> startSlices = new ArrayList<>();
			for (int z = 0; z <= profile.getLastStartSlice() && z + profile.getSlices() <= memorySlices; z += profile
					.getSlices()) {
				boolean fits = true;
				for (int u = z; u < z + profile.getSlices(); u++) {
					if (!free[u]) {
						fits = false;
						break;
					}
				}
				if (fits) {
					startSlices.add(z);
				}
			}
			return startSlices;
		}

		/**
		 * CF(G): the configuration flexibility of a GPU, i.e. the total number of
		 * placements of all profiles in the free memory slices left by its GIs.
		 *
		 * @param free whether each memory slice of the GPU is free
		 */
		public int getConfigurationFlexibility(boolean[] free) {
			int cf = 0;
			for (Profile profile : profiles) {
				cf += getStartSlices(profile, free).size();
			}
			return cf;
		}
	}

	/** NVIDIA A100 40GB: 7 compute slices and 8 memory slices of 5 GB */
	public final static String NVIDIA_A100_40GB_CARD = "NVIDIA A100-40GB";

	/** NVIDIA A30 24GB: 4 compute slices and 4 memory slices of 6 GB */
	public final static String NVIDIA_A30_24GB_CARD = "NVIDIA A30-24GB";

	private final static Map<String, Profiles> PROFILES = new HashMap<>();

	static {
		// MIG User Guide, Table 2 (A100 40GB); see also Table tab:a100-gis of the paper
		PROFILES.put(NVIDIA_A100_40GB_CARD, new Profiles(8, 5 * 1024, 7)
				.add("1g.5gb", 1, 7)
				.add("1g.10gb", 2, 4)
				.add("2g.10gb", 2, 3)
				.add("3g.20gb", 4, 2)
				.add("4g.20gb", 4, 1)
				.add("7g.40gb", 8, 1));
		// MIG User Guide, A30 24GB: the +me variants share the placements of their profiles
		PROFILES.put(NVIDIA_A30_24GB_CARD, new Profiles(4, 6 * 1024, 4)
				.add("1g.6gb", 1, 4)
				.add("2g.12gb", 2, 2)
				.add("4g.24gb", 4, 1));
	}

	/**
	 * @return the GI profiles of the video card type, or null if the video card is
	 *         not MIG-enabled
	 */
	public static Profiles getProfiles(String videoCardType) {
		return PROFILES.get(videoCardType);
	}

	/**
	 * Singleton class (i.e. cannot be initialized)
	 */
	private MigGpuTags() {
	}

}
