package org.cloudbus.cloudsim.gpu.hardware_assisted.grid;

import org.apache.commons.lang3.ArrayUtils;
import org.cloudbus.cloudsim.gpu.BusTags;
import org.cloudbus.cloudsim.gpu.GpuTaskScheduler;
import org.cloudbus.cloudsim.gpu.Vgpu;
import org.cloudbus.cloudsim.gpu.VideoCardTags;

/**
 * 
 * Methods & constants that are related to {@link Vgpu virtual gpus} types and
 * configurations. Types are named as in NVIDIA vGPU software, e.g. "NVIDIA
 * A16-4Q" for a Q-series vGPU with 4 GB of frame buffer on an NVIDIA A16.
 * 
 * @author Ahmad Siavashi
 * 
 */

public class GridVgpuTags {

	/**
	 * A Q-series vGPU on an NVIDIA A16 GPU (1, 2, 4, 8 or 16 GB).
	 * 
	 * @param gddram frame buffer in MB
	 */
	public static Vgpu getA16Q(int vgpuId, int gddram, GpuTaskScheduler scheduler) {
		return getVgpu(vgpuId, VideoCardTags.NVIDIA_A16_CARD, gddram, VideoCardTags.NVIDIA_A16_CARD_PE_MIPS,
				VideoCardTags.NVIDIA_A16_CARD_GPU_PES, VideoCardTags.NVIDIA_A16_CARD_BW_PER_BUS, scheduler);
	}

	/**
	 * A Q-series vGPU on an NVIDIA L40S GPU (1, 2, 3, 4, 6, 8, 12, 16, 24 or 48
	 * GB).
	 * 
	 * @param gddram frame buffer in MB
	 */
	public static Vgpu getL40SQ(int vgpuId, int gddram, GpuTaskScheduler scheduler) {
		return getVgpu(vgpuId, VideoCardTags.NVIDIA_L40S_CARD, gddram, VideoCardTags.NVIDIA_L40S_CARD_PE_MIPS,
				VideoCardTags.NVIDIA_L40S_CARD_GPU_PES, VideoCardTags.NVIDIA_L40S_CARD_BW_PER_BUS, scheduler);
	}

	/**
	 * vGPUs are time-sliced, so every type requests the PEs and the memory
	 * bandwidth of its GPU.
	 */
	private static Vgpu getVgpu(int vgpuId, String videoCardType, int gddram, double mips, int numberOfPes, long bw,
			GpuTaskScheduler scheduler) {
		if (!ArrayUtils.contains(GridVideoCardTags.getVgpuGddrams(videoCardType), gddram)) {
			throw new IllegalArgumentException(videoCardType + " has no vGPU type with " + gddram + " MB");
		}
		final String type = getQType(videoCardType, gddram);
		Vgpu vgpu = new Vgpu(vgpuId, mips, numberOfPes, gddram, bw, type, scheduler, BusTags.PCI_E_4_X16_BW);
		return vgpu;
	}

	/**
	 * @param gddram frame buffer in MB
	 * @return the Q-series vGPU type of the video card with the given frame buffer,
	 *         e.g. "NVIDIA A16-4Q"
	 */
	public static String getQType(String videoCardType, int gddram) {
		return videoCardType + "-" + gddram / 1024 + "Q";
	}

	/**
	 * @return the video card type of the vGPU type, e.g. "NVIDIA A16"
	 */
	public static String getVideoCardType(Vgpu vgpu) {
		int end = vgpu.getType().lastIndexOf('-');
		return end < 0 ? vgpu.getType() : vgpu.getType().substring(0, end);
	}

	/**
	 * @return the series of the vGPU type, e.g. 'Q'
	 */
	public static char getSeries(Vgpu vgpu) {
		return vgpu.getType().charAt(vgpu.getType().length() - 1);
	}

	/**
	 * Singleton class (cannot be instantiated)
	 */
	private GridVgpuTags() {
	}

}
