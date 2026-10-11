package org.cloudbus.cloudsim.gpu.hardware_assisted.mig;

import java.util.List;

import org.cloudbus.cloudsim.gpu.Pgpu;
import org.cloudbus.cloudsim.gpu.Vgpu;

/**
 * The built-in GI placement policy of NVIDIA, as observed with driver R530. It
 * places a GI at the start slice that maximizes the configuration flexibility
 * (CF) of the resulting configuration, and at the lowest such slice when
 * several attain the maximum.
 *
 * @author Ahmad Siavashi
 */
public class MigVgpuPlacementPolicyCf implements MigVgpuPlacementPolicy {

	@Override
	public int selectStartSlice(MigVgpuPlacement placement, Pgpu pgpu, Vgpu vgpu, List<Integer> candidates) {
		int selected = -1;
		int maxCf = Integer.MIN_VALUE;
		// candidates are in increasing order, so the lowest slice of the maximum wins
		for (int z : candidates) {
			int cf = placement.getConfigurationFlexibility(pgpu, vgpu, z);
			if (cf > maxCf) {
				maxCf = cf;
				selected = z;
			}
		}
		return selected;
	}

}
