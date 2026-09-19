package org.cloudbus.cloudsim.gpu;

/**
 * 
 * Methods & constants that are related to {@link VideoCard VideoCards} types
 * and their hardware specifications.
 * 
 * @author Ahmad Siavashi
 * 
 */
public class VideoCardTags {

	// Constants

	public final static String NVIDIA_A16_CARD = "NVIDIA A16";
	public final static String NVIDIA_L40S_CARD = "NVIDIA L40S";

	public final static int NVIDIA_AMPERE_ADA_SM_CUDA_CORES = 128;

	// NVIDIA A16 Spec (Ampere)

	/** 250 Watts (board) */
	public final static int NVIDIA_A16_CARD_POWER = 250;
	/** 4 GPUs */
	public final static int NVIDIA_A16_CARD_GPUS = 4;
	/** GPU type */
	public final static String NVIDIA_A16_GPU_TYPE = "GA107";
	/** 16 GBs/GPU */
	public final static int NVIDIA_A16_CARD_GPU_MEM = 16 * 1024;
	/** 10 SMs / GPU */
	public final static int NVIDIA_A16_CARD_GPU_PES = 10;
	/** 1312 MHz base clock; 128 CUDA cores / SM */
	public final static double NVIDIA_A16_CARD_PE_MIPS = getGpuPeMipsFromFrequency(NVIDIA_A16_CARD, 1312);
	/** 4 */
	public final static int NVIDIA_A16_CARD_NUM_BUS = 4;
	/** 4 x 200.0 GB/s */
	public final static long NVIDIA_A16_CARD_BW_PER_BUS = 200 * 1024;
	/**
	 * Idle power per GPU (W). Not measured; assumed 20% of the maximum, the idle
	 * share reported for NVIDIA A100 and H100 in "The xPU-athalon: Quantifying the
	 * Competition of AI Acceleration", arXiv:2604.10852.
	 */
	public final static double NVIDIA_A16_GPU_IDLE_POWER = 0.2 * NVIDIA_A16_CARD_POWER / NVIDIA_A16_CARD_GPUS;
	/** Maximum power per GPU (W): board power divided by the GPUs */
	public final static double NVIDIA_A16_GPU_MAX_POWER = (double) NVIDIA_A16_CARD_POWER / NVIDIA_A16_CARD_GPUS;

	// NVIDIA L40S Spec (Ada Lovelace)

	/** 350 Watts (board) */
	public final static int NVIDIA_L40S_CARD_POWER = 350;
	/** 1 GPU */
	public final static int NVIDIA_L40S_CARD_GPUS = 1;
	/** GPU type */
	public final static String NVIDIA_L40S_GPU_TYPE = "AD102";
	/** 48 GBs/GPU */
	public final static int NVIDIA_L40S_CARD_GPU_MEM = 48 * 1024;
	/** 142 SMs / GPU */
	public final static int NVIDIA_L40S_CARD_GPU_PES = 142;
	/** 1110 MHz base clock; 128 CUDA cores / SM */
	public final static double NVIDIA_L40S_CARD_PE_MIPS = getGpuPeMipsFromFrequency(NVIDIA_L40S_CARD, 1110);
	/** 1 */
	public final static int NVIDIA_L40S_CARD_NUM_BUS = 1;
	/** 1 x 864.0 GB/s */
	public final static long NVIDIA_L40S_CARD_BW_PER_BUS = 864 * 1024;
	/**
	 * Idle power per GPU (W), measured with no CUDA context in S. S. Vadari, "The
	 * Model Parking Tax: Quantifying the Hidden Energy Cost of Always-On GPU Model
	 * Deployment", arXiv:2605.23918, Table 2.
	 */
	public final static double NVIDIA_L40S_GPU_IDLE_POWER = 35.6;
	/** Maximum power per GPU (W) */
	public final static double NVIDIA_L40S_GPU_MAX_POWER = NVIDIA_L40S_CARD_POWER;

	public static double getGpuPeMipsFromFrequency(String type, double frequency) {
		double mips = frequency;
		switch (type) {
		case NVIDIA_A16_CARD:
		case NVIDIA_L40S_CARD:
			mips *= NVIDIA_AMPERE_ADA_SM_CUDA_CORES * 2;
			break;
		default:
			break;
		}
		return mips;
	}

	/**
	 * Singleton class (i.e. cannot be initialized)
	 */
	private VideoCardTags() {
	}

}
