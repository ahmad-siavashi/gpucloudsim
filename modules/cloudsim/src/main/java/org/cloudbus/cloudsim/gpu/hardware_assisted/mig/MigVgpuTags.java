package org.cloudbus.cloudsim.gpu.hardware_assisted.mig;

import org.cloudbus.cloudsim.gpu.BusTags;
import org.cloudbus.cloudsim.gpu.GpuTaskScheduler;
import org.cloudbus.cloudsim.gpu.Vgpu;

/**
 * Methods and constants related to MIG-backed {@link Vgpu virtual GPUs}. A VM
 * receives a GPU instance (GI) as a MIG-backed vGPU, with one vGPU per GI.
 * Types are named after the video card and the GI profile, e.g. "NVIDIA
 * A100-40GB-2g.10gb".
 *
 * @author Ahmad Siavashi
 */
public class MigVgpuTags {

	/** 1312 MHz base clock; 64 FP32 cores / SM on A100 */
	public final static double NVIDIA_A100_40GB_PE_MIPS = 1312 * 64 * 2;
	/** 108 SMs */
	public final static int NVIDIA_A100_40GB_GPU_PES = 108;
	/** 1555 GB/s */
	public final static long NVIDIA_A100_40GB_BW = 1555L * 1024;
	/** 930 MHz base clock; 64 FP32 cores / SM on A30 */
	public final static double NVIDIA_A30_24GB_PE_MIPS = 930 * 64 * 2;
	/** 56 SMs */
	public final static int NVIDIA_A30_24GB_GPU_PES = 56;
	/** 933 GB/s */
	public final static long NVIDIA_A30_24GB_BW = 933L * 1024;

	/**
	 * A MIG-backed vGPU on an NVIDIA A100 40GB GPU.
	 *
	 * @param profileName the GI profile, e.g. "2g.10gb"
	 */
	public static Vgpu getA100(int vgpuId, String profileName, GpuTaskScheduler scheduler) {
		return getVgpu(vgpuId, MigGpuTags.NVIDIA_A100_40GB_CARD, profileName, NVIDIA_A100_40GB_PE_MIPS,
				NVIDIA_A100_40GB_GPU_PES, NVIDIA_A100_40GB_BW, scheduler);
	}

	/**
	 * A MIG-backed vGPU on an NVIDIA A30 24GB GPU.
	 *
	 * @param profileName the GI profile, e.g. "2g.12gb"
	 */
	public static Vgpu getA30(int vgpuId, String profileName, GpuTaskScheduler scheduler) {
		return getVgpu(vgpuId, MigGpuTags.NVIDIA_A30_24GB_CARD, profileName, NVIDIA_A30_24GB_PE_MIPS,
				NVIDIA_A30_24GB_GPU_PES, NVIDIA_A30_24GB_BW, scheduler);
	}

	/**
	 * A GI is a partition of its GPU, so it receives the share of the compute
	 * slices, the memory and the memory bandwidth of its profile.
	 */
	private static Vgpu getVgpu(int vgpuId, String videoCardType, String profileName, double mips, int numberOfPes,
			long bw, GpuTaskScheduler scheduler) {
		MigGpuTags.Profiles profiles = MigGpuTags.getProfiles(videoCardType);
		if (profiles == null) {
			throw new IllegalArgumentException("Not a MIG-enabled video card type: " + videoCardType);
		}
		MigGpuTags.Profile profile = profiles.getProfile(profileName);
		if (profile == null) {
			throw new IllegalArgumentException(videoCardType + " has no GI profile named " + profileName);
		}
		final int gddram = profile.getSlices() * profiles.getSliceGddram();
		// the compute slices of the profile, e.g. 2 of 7 for a 2g.10gb GI
		final int computeSlices = getComputeSlices(profile);
		final int vgpuPes = Math.max(1, numberOfPes * computeSlices / profiles.getComputeSlices());
		final long vgpuBw = bw * profile.getSlices() / profiles.getMemorySlices();
		final String type = getType(videoCardType, profileName);
		return new Vgpu(vgpuId, mips, vgpuPes, gddram, vgpuBw, type, scheduler, BusTags.PCI_E_4_X16_BW);
	}

	/**
	 * @return the compute slices of the profile, i.e. the leading digit of its
	 *         name, e.g. 2 for "2g.10gb"
	 */
	public static int getComputeSlices(MigGpuTags.Profile profile) {
		String name = profile.getName();
		return Integer.parseInt(name.substring(0, name.indexOf('g')));
	}

	/**
	 * @return the vGPU type of a GI of the given profile on the video card, e.g.
	 *         "NVIDIA A100-40GB-2g.10gb"
	 */
	public static String getType(String videoCardType, String profileName) {
		return videoCardType + "-" + profileName;
	}

	/**
	 * @return the video card type of the vGPU type, e.g. "NVIDIA A100-40GB"
	 */
	public static String getVideoCardType(Vgpu vgpu) {
		int end = vgpu.getType().lastIndexOf('-');
		return end < 0 ? vgpu.getType() : vgpu.getType().substring(0, end);
	}

	/**
	 * @return the GI profile of the vGPU type, e.g. "2g.10gb"
	 */
	public static String getProfileName(Vgpu vgpu) {
		int start = vgpu.getType().lastIndexOf('-');
		return start < 0 ? vgpu.getType() : vgpu.getType().substring(start + 1);
	}

	/**
	 * Singleton class (i.e. cannot be initialized)
	 */
	private MigVgpuTags() {
	}

}
