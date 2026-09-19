package org.cloudbus.cloudsim.gpu.hardware_assisted.grid;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

import org.cloudbus.cloudsim.gpu.VideoCardTags;

/**
 * 
 * NVIDIA vGPU rules of {@link VideoCardTags video cards}. Values follow NVIDIA
 * Virtual GPU Software User Guide, Release 20 (Chapter 9, Virtual GPU Types
 * Reference).
 * 
 * @author Ahmad Siavashi
 * 
 */
public class GridVideoCardTags {

	/** Maximum number of vGPUs assigned to a single VM */
	public final static int MAX_VGPUS_PER_VM = 16;

	/**
	 * vGPU placements of a video card. A vGPU with a frame buffer of s GB occupies
	 * the placements [id, id + s) of its GPU, where id is one of its placement IDs.
	 */
	public static class Placements {
		/** Maximum vGPUs on a GPU, regardless of their types */
		public final int maxVgpusPerGpu;
		/** All vGPUs of a VM must be of the same type (Ampere boards) */
		public final boolean sameTypePerVm;
		private final Map<Integer, int[]> equalSize = new HashMap<>();
		private final Map<Integer, int[]> mixedSize = new HashMap<>();

		Placements(int maxVgpusPerGpu, boolean sameTypePerVm) {
			this.maxVgpusPerGpu = maxVgpusPerGpu;
			this.sameTypePerVm = sameTypePerVm;
		}

		/** Equal-size IDs are 0, s, 2s, ... for the maximum vGPUs per GPU. */
		Placements add(int sizeGb, int maxEqualSize, int... mixedSizeIds) {
			equalSize.put(sizeGb, IntStream.range(0, maxEqualSize).map(k -> k * sizeGb).toArray());
			mixedSize.put(sizeGb, mixedSizeIds);
			return this;
		}

		/**
		 * @return the placement IDs of a vGPU of the given size, or null if the size
		 *         is not supported
		 */
		public int[] getIds(int sizeGb, boolean mixedSizeMode) {
			return (mixedSizeMode ? mixedSize : equalSize).get(sizeGb);
		}

		/**
		 * @return the frame buffer (MB) of a GPU of the video card
		 */
		public int getGpuGddram() {
			return Arrays.stream(getSizes()).max().getAsInt() * 1024;
		}

		public int[] getSizes() {
			return equalSize.keySet().stream().mapToInt(Integer::intValue).sorted().toArray();
		}
	}

	private final static Map<String, Placements> PLACEMENTS = new HashMap<>();

	static {
		// User Guide Section 9.3.10 (16 GB); mixed-size IDs equal equal-size IDs
		PLACEMENTS.put(VideoCardTags.NVIDIA_A16_CARD, new Placements(16, true)
				.add(16, 1, 0)
				.add(8, 2, 0, 8)
				.add(4, 4, 0, 4, 8, 12)
				.add(2, 8, 0, 2, 4, 6, 8, 10, 12, 14)
				.add(1, 16, IntStream.range(0, 16).toArray()));
		// User Guide Section 9.3.5 (48 GB); at most 32 vGPUs on an L40S
		PLACEMENTS.put(VideoCardTags.NVIDIA_L40S_CARD, new Placements(32, false)
				.add(48, 1, 0)
				.add(24, 2, 0, 24)
				.add(16, 3, 0, 32)
				.add(12, 4, 0, 12, 24, 36)
				.add(8, 6, 0, 16, 24, 40)
				.add(6, 8, 0, 6, 12, 18, 24, 30, 36, 42)
				.add(4, 12, 0, 8, 12, 20, 24, 32, 36, 44)
				.add(3, 16, 0, 3, 6, 9, 12, 15, 18, 21, 24, 27, 30, 33, 36, 39, 42, 45)
				.add(2, 24, 0, 4, 6, 10, 12, 16, 18, 22, 24, 28, 30, 34, 36, 40, 42, 46)
				.add(1, 32, 0, 5, 6, 11, 12, 17, 18, 23, 24, 29, 30, 35, 36, 41, 42, 47));
	}

	/**
	 * @return the vGPU placements of the video card type, or null if unknown
	 */
	public static Placements getPlacements(String videoCardType) {
		return PLACEMENTS.get(videoCardType);
	}

	/**
	 * @return the frame buffer sizes (MB) of the vGPU types of the video card
	 */
	public static int[] getVgpuGddrams(String videoCardType) {
		return Arrays.stream(getPlacements(videoCardType).getSizes()).map(s -> s * 1024).toArray();
	}

	/**
	 * Singleton class (i.e. cannot be initialized)
	 */
	private GridVideoCardTags() {
	}

}
