package org.cloudbus.cloudsim.gpu.hardware_assisted.grid;

import org.cloudbus.cloudsim.gpu.BusTags;
import org.cloudbus.cloudsim.gpu.GpuTaskScheduler;
import org.cloudbus.cloudsim.gpu.Vgpu;

/**
 * 
 * Methods & constants that are related to {@link Vgpu virtual gpus} types and
 * configurations.
 * 
 * @author Ahmad Siavashi
 * 
 */

public class GridVgpuTags {

	/** NVIDIA GRID K1 Profiles */
	public final static String K1_K120Q = "NVIDIA K120Q";
	public final static String K1_K140Q = "NVIDIA K140Q";
	public final static String K1_K160Q = "NVIDIA K160Q";
	public final static String K1_K180Q = "NVIDIA K180Q";

	/** NVIDIA GRID K2 Profiles */
	public final static String K2_K220Q = "NVIDIA K220Q";
	public final static String K2_K240Q = "NVIDIA K240Q";
	public final static String K2_K260Q = "NVIDIA K260Q";
	public final static String K2_K280Q = "NVIDIA K280Q";

	/**
	 * K1 Board Pass through type 1/pGPU, 4/board
	 * 
	 * @return a GK107 physical GPU (pass through)
	 */
	public static Vgpu getK180Q(int vgpuId, GpuTaskScheduler scheduler) {
		final String type = K1_K180Q;
		// GPU Clock: 850 MHz
		final double mips = GridVideoCardTags.getGpuPeMipsFromFrequency(GridVideoCardTags.NVIDIA_K1_CARD, 850);
		// SMX count: 1
		final int numberOfPes = 1;
		// GDDRAM: 256 MB
		final int gddram = 4096;
		// Bandwidth: 28.5 GB/s
		final long bw = (long) 28.5 * 1024;
		Vgpu vgpu = new Vgpu(vgpuId, mips, numberOfPes, gddram, bw, type, null, scheduler, BusTags.PCI_E_3_X16_BW);
		return vgpu;
	}

	/**
	 * K1 Board K160Q vGPU type 2/pGPU, 8/board
	 * 
	 * @return a K140Q virtual GPU
	 */
	public static Vgpu getK160Q(int vgpuId, GpuTaskScheduler scheduler) {
		final String type = K1_K160Q;
		// GPU Clock: 850 MHz
		final double mips = GridVideoCardTags.getGpuPeMipsFromFrequency(GridVideoCardTags.NVIDIA_K1_CARD, 850);
		// SMX count: 1
		final int numberOfPes = 1;
		// GDDRAM: 2 GB
		final int gddram = 2048;
		// Bandwidth: 28.5 GB/s
		final long bw = (long) 28.5 * 1024;
		Vgpu vgpu = new Vgpu(vgpuId, mips, numberOfPes, gddram, bw, type, null, scheduler, BusTags.PCI_E_3_X16_BW);
		return vgpu;
	}

	/**
	 * K1 Board K140Q vGPU type 4/pGPU, 16/board
	 * 
	 * @return a K140Q virtual GPU
	 */
	public static Vgpu getK140Q(int vgpuId, GpuTaskScheduler scheduler) {
		final String type = K1_K140Q;
		// GPU Clock: 850 MHz
		final double mips = GridVideoCardTags.getGpuPeMipsFromFrequency(GridVideoCardTags.NVIDIA_K1_CARD, 850);
		// SMX count: 1
		final int numberOfPes = 1;
		// GDDRAM: 256 MB
		final int gddram = 1024;
		// Bandwidth: 28.5 GB/s
		final long bw = (long) 28.5 * 1024;
		Vgpu vgpu = new Vgpu(vgpuId, mips, numberOfPes, gddram, bw, type, null, scheduler, BusTags.PCI_E_3_X16_BW);
		return vgpu;
	}

	/**
	 * K1 Board K120Q vGPU type 8/pGPU, 32/board
	 * 
	 * @return a K120Q virtual GPU
	 */
	public static Vgpu getK120Q(int vgpuId, GpuTaskScheduler scheduler) {
		final String type = K1_K120Q;
		// GPU Clock: 850 MHz
		final double mips = GridVideoCardTags.getGpuPeMipsFromFrequency(GridVideoCardTags.NVIDIA_K1_CARD, 850);
		// SMX count: 1
		final int numberOfPes = 1;
		// GDDRAM: 512 MB
		final int gddram = 512;
		// Bandwidth: 28.5 GB/s
		final long bw = (long) 28.5 * 1024;
		Vgpu vgpu = new Vgpu(vgpuId, mips, numberOfPes, gddram, bw, type, null, scheduler, BusTags.PCI_E_3_X16_BW);
		return vgpu;
	}

	/**
	 * K2 Board K220Q vGPU type 8/pGPU, 16/board
	 * 
	 * @return a K220Q virtual GPU
	 */
	public static Vgpu getK220Q(int vgpuId, GpuTaskScheduler scheduler) {
		final String type = K2_K220Q;
		// GPU Clock: 745 MHz
		final double mips = GridVideoCardTags.getGpuPeMipsFromFrequency(GridVideoCardTags.NVIDIA_K2_CARD, 745);
		// SMX count: 8
		final int numberOfPes = 8;
		// GDDRAM: 512 MB
		final int gddram = 512;
		// Bandwidth: 160 GB/s
		final long bw = 160 * 1024;
		Vgpu vgpu = new Vgpu(vgpuId, mips, numberOfPes, gddram, bw, type, null, scheduler, BusTags.PCI_E_3_X16_BW);
		return vgpu;
	}

	/**
	 * K2 Board K240Q vGPU type 4/pGPU, 8/board
	 * 
	 * @return a K240Q virtual GPU
	 */
	public static Vgpu getK240Q(int vgpuId, GpuTaskScheduler scheduler) {
		final String type = K2_K240Q;
		// GPU Clock: 745 MHz
		final double mips = GridVideoCardTags.getGpuPeMipsFromFrequency(GridVideoCardTags.NVIDIA_K2_CARD, 745);
		// SMX count: 8
		final int numberOfPes = 8;
		// GDDRAM: 1 GB
		final int gddram = 1024;
		// Bandwidth: 160 GB/s
		final long bw = 160 * 1024;
		Vgpu vgpu = new Vgpu(vgpuId, mips, numberOfPes, gddram, bw, type, null, scheduler, BusTags.PCI_E_3_X16_BW);
		return vgpu;
	}

	/**
	 * K2 Board K260Q vGPU type 2/pGPU, 4/board
	 * 
	 * @return a K260Q virtual GPU
	 */
	public static Vgpu getK260Q(int vgpuId, GpuTaskScheduler scheduler) {
		final String type = K2_K260Q;
		// GPU Clock: 745 MHz
		final double mips = GridVideoCardTags.getGpuPeMipsFromFrequency(GridVideoCardTags.NVIDIA_K2_CARD, 745);
		// SMX count: 8
		final int numberOfPes = 8;
		// GDDRAM: 2 GB
		final int gddram = 2048;
		// Bandwidth: 160 GB/s
		final long bw = 160 * 1024;
		Vgpu vgpu = new Vgpu(vgpuId, mips, numberOfPes, gddram, bw, type, null, scheduler, BusTags.PCI_E_3_X16_BW);
		return vgpu;
	}

	/**
	 * K2 Board K280Q vGPU type 1/pGPU, 2/board
	 * 
	 * @return a K280Q virtual GPU
	 */
	public static Vgpu getK280Q(int vgpuId, GpuTaskScheduler scheduler) {
		final String type = K2_K280Q;
		// GPU Clock: 745 MHz
		final double mips = GridVideoCardTags.getGpuPeMipsFromFrequency(GridVideoCardTags.NVIDIA_K2_CARD, 745);
		// SMX count: 8
		final int numberOfPes = 8;
		// GDDRAM: 4 GB
		final int gddram = 4096;
		// Bandwidth: 160 GB/s
		final long bw = 160 * 1024;
		Vgpu vgpu = new Vgpu(vgpuId, mips, numberOfPes, gddram, bw, type, null, scheduler, BusTags.PCI_E_3_X16_BW);
		return vgpu;
	}

	/**
	 * Singleton class (cannot be instantiated)
	 */
	private GridVgpuTags() {
	}

}
