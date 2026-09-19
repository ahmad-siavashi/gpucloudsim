package org.cloudbus.cloudsim.gpu.power;

import org.cloudbus.cloudsim.gpu.VideoCardTags;
import org.cloudbus.cloudsim.gpu.power.models.VideoCardPowerModel;
import org.cloudbus.cloudsim.gpu.power.models.VideoCardPowerModelLinear;

/**
 *
 * Methods that are related to the power models of {@link VideoCardTags
 * VideoCardTags} types.
 *
 * @author Ahmad Siavashi
 *
 */
public class PowerVideoCardTags {

	/**
	 * A linear power model of an A16 GPU between its idle and maximum power.
	 */
	public static VideoCardPowerModel getA16PowerModel(boolean powerGate) {
		return getLinearPowerModel(VideoCardTags.NVIDIA_A16_GPU_IDLE_POWER, VideoCardTags.NVIDIA_A16_GPU_MAX_POWER,
				powerGate);
	}

	/**
	 * A linear power model of an L40S GPU between its idle and maximum power.
	 */
	public static VideoCardPowerModel getL40SPowerModel(boolean powerGate) {
		return getLinearPowerModel(VideoCardTags.NVIDIA_L40S_GPU_IDLE_POWER, VideoCardTags.NVIDIA_L40S_GPU_MAX_POWER,
				powerGate);
	}

	private static VideoCardPowerModel getLinearPowerModel(double idle, double max, boolean powerGate) {
		return new VideoCardPowerModelLinear(0, 0, 0, (max - idle) / 100, idle, powerGate);
	}

}
