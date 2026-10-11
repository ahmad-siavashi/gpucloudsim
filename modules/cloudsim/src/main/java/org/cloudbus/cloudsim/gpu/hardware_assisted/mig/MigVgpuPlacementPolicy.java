package org.cloudbus.cloudsim.gpu.hardware_assisted.mig;

import java.util.List;

import org.cloudbus.cloudsim.gpu.Pgpu;
import org.cloudbus.cloudsim.gpu.Vgpu;

/**
 * Selects the start slice of a GPU instance (GI) among the start slices the
 * MIG placement rules permit on a GPU.
 *
 * @author Ahmad Siavashi
 */
public interface MigVgpuPlacementPolicy {

	/**
	 * @param placement  the placement of the video card
	 * @param pgpu       the GPU the GI is placed on
	 * @param vgpu       the vGPU the GI backs
	 * @param candidates the permissible start slices, in increasing order; never
	 *                   empty
	 * @return the selected start slice
	 */
	int selectStartSlice(MigVgpuPlacement placement, Pgpu pgpu, Vgpu vgpu, List<Integer> candidates);

}
