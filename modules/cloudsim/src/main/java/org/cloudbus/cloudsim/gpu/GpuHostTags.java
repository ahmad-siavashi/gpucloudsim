package org.cloudbus.cloudsim.gpu;

/**
 * 
 * Methods & constants that are related to {@link GpuHost GpuHosts} types and configurations.
 * 
 * @author Ahmad Siavashi
 *
 */
public class GpuHostTags {
	// Host Types
	public final static String HOST_CUSTOM = "Custom";
	public final static String DUAL_INTEL_XEON_PLATINUM_8380 = "Dual Intel Xeon Platinum 8380 (80 Cores, 2.30 GHz)";
	public final static String DUAL_INTEL_XEON_PLATINUM_8380_A16 = "Dual Intel Xeon Platinum 8380 (80 Cores, 2.30 GHz, 1 x NVIDIA A16)";
	public final static String DUAL_INTEL_XEON_PLATINUM_8380_L40S = "Dual Intel Xeon Platinum 8380 (80 Cores, 2.30 GHz, 1 x NVIDIA L40S)";

	// Instruction per Cycle (IPC)
	private final static int INTEL_XEON_IPC = 16;
	
	// 25 Gbit/s in MB/s
	public final static int NETWORK_BANDWIDTH_25_GBIT_PER_SEC = 3125;

	// Dual Intel Xeon Platinum 8380 (Ice Lake, PCIe 4.0)
	/** 80 Cores */
	public final static int DUAL_INTEL_XEON_PLATINUM_8380_NUM_PES = 80;
	/** Dual Intel Xeon Platinum 8380 (2.3 GHz) */
	public final static double DUAL_INTEL_XEON_PLATINUM_8380_PE_MIPS = 2300 * INTEL_XEON_IPC;
	/** 512GB RAM */
	public final static int DUAL_INTEL_XEON_PLATINUM_8380_RAM = 512 * 1024;
	/** 2 x 2TB NVMe Local Storage */
	public final static int DUAL_INTEL_XEON_PLATINUM_8380_STORAGE = 2 * 2048 * 1024;
	/** 25 GB/s */
	public final static int DUAL_INTEL_XEON_PLATINUM_8380_BW = NETWORK_BANDWIDTH_25_GBIT_PER_SEC;
	/**
	 * 633 W at full load without video cards; SPECpower_ssj2008 result of
	 * Supermicro SuperServer SYS-740GP-TNRT (2 x Platinum 8380, 512 GB), May 2021.
	 */
	public final static double DUAL_INTEL_XEON_PLATINUM_8380_MAX_POWER = 633;
	/** 118 W at active idle in the same result */
	public final static double DUAL_INTEL_XEON_PLATINUM_8380_STATIC_POWER_PERCENT = 118.0 / 633;

	// Dual Intel Xeon Platinum 8380 with NVIDIA A16
	/** 1 Video Card Per GPU Host */
	public final static int DUAL_INTEL_XEON_PLATINUM_8380_A16_NUM_VIDEO_CARDS = 1;
	/** 1 NVIDIA A16 Per Host */
	public final static String DUAL_INTEL_XEON_PLATINUM_8380_A16_VIDEO_CARD = VideoCardTags.NVIDIA_A16_CARD;

	// Dual Intel Xeon Platinum 8380 with NVIDIA L40S
	/** 1 Video Card Per GPU Host */
	public final static int DUAL_INTEL_XEON_PLATINUM_8380_L40S_NUM_VIDEO_CARDS = 1;
	/** 1 NVIDIA L40S Per Host */
	public final static String DUAL_INTEL_XEON_PLATINUM_8380_L40S_VIDEO_CARD = VideoCardTags.NVIDIA_L40S_CARD;

}
