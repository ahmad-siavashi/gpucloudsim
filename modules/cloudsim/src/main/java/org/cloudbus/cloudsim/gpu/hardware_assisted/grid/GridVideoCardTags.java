package org.cloudbus.cloudsim.gpu.hardware_assisted.grid;

import org.cloudbus.cloudsim.gpu.VideoCard;

/**
 * 
 * Methods & constants that are related to {@link VideoCard VideoCards} types
 * and configurations.
 * 
 * @author Ahmad Siavashi
 * 
 */
public class GridVideoCardTags {

	// Constants

	public final static String NVIDIA_K1_CARD = "NVIDIA K1";
	public final static String NVIDIA_K2_CARD = "NVIDIA K2";
	public final static String NVIDIA_K80_CARD = "NVIDIA K80";
	public final static String NVIDIA_M60_CARD = "NVIDIA M60";

	public final static int NVIDIA_KEPLER_SMX_CUDA_CORES = 192;
	public final static int NVIDIA_MAXWELL_SMM_CUDA_CORES = 128;

	// NVIDIA GRID K1 Spec

	/** 130 Watts */
	public final static int NVIDIA_K1_CARD_POWER = 130;
	/** 4 GPUs */
	public final static int NVIDIA_K1_CARD_GPUS = 4;
	/** GPU type */
	public final static String NVIDIA_K1_GPU_TYPE = "GK107";
	/** 4 GBs/GPU */
	public final static int NVIDIA_K1_CARD_GPU_MEM = 4096;
	/** 1 SMX / GPU */
	public final static int NVIDIA_K1_CARD_GPU_PES = 1;
	/** 850 MHz */
	public final static double NVIDIA_K1_CARD_PE_MIPS = getGpuPeMipsFromFrequency(NVIDIA_K1_CARD, 850);
	/** 4 */
	public final static int NVIDIA_K1_CARD_NUM_BUS = 4;
	/** 4 x 28.5/s */
	public final static long NVIDIA_K1_CARD_BW_PER_BUS = (long) 28.5 * 1024;

	public final static String[] K1_VGPUS = { GridVgpuTags.K1_K120Q, GridVgpuTags.K1_K140Q, GridVgpuTags.K1_K160Q,
			GridVgpuTags.K1_K180Q };

	// NVIDIA GRID K2 Spec

	/** 225 Watts */
	public final static int NVIDIA_K2_CARD_POWER = 225;
	/** 2 GPUs */
	public final static int NVIDIA_K2_CARD_GPUS = 2;
	/** GPU Type */
	public final static String NVIDIA_K2_GPU_TYPE = "GK104";
	/** 4 GBs/GPU */
	public final static int NVIDIA_K2_CARD_GPU_MEM = 4096;
	/** 1 SMX / GPU */
	public final static int NVIDIA_K2_CARD_GPU_PES = 8;
	/** 750 MHz */
	public final static double NVIDIA_K2_CARD_PE_MIPS = getGpuPeMipsFromFrequency(NVIDIA_K2_CARD, 745);
	/** 2 */
	public final static int NVIDIA_K2_CARD_NUM_BUS = 2;
	/** 2 x 160.0 GB/s */
	public final static long NVIDIA_K2_CARD_BW_PER_BUS = 160 * 1024;

	public final static String[] K2_VGPUS = { GridVgpuTags.K2_K220Q, GridVgpuTags.K2_K240Q, GridVgpuTags.K2_K260Q,
			GridVgpuTags.K2_K280Q };

	public static double getGpuPeFrequencyFromMips(String type, double mips) {
		double frequency = mips;
		switch (type) {
		case NVIDIA_K1_CARD:
		case NVIDIA_K2_CARD:
		case NVIDIA_K80_CARD:
			frequency /= NVIDIA_KEPLER_SMX_CUDA_CORES * 2;
			break;
		case NVIDIA_M60_CARD:
			frequency /= NVIDIA_MAXWELL_SMM_CUDA_CORES * 2;
			break;
		default:
			break;
		}
		return frequency;
	}

	public static double getGpuPeMipsFromFrequency(String type, double frequency) {
		double mips = frequency;
		switch (type) {
		case NVIDIA_K1_CARD:
		case NVIDIA_K2_CARD:
		case NVIDIA_K80_CARD:
			mips *= NVIDIA_KEPLER_SMX_CUDA_CORES * 2;
			break;
		case NVIDIA_M60_CARD:
			mips *= NVIDIA_MAXWELL_SMM_CUDA_CORES * 2;
			break;
		default:
			break;
		}
		return mips;
	}

	/**
	 * Singleton class (i.e. cannot be initialized)
	 */
	private GridVideoCardTags() {
	}

}
