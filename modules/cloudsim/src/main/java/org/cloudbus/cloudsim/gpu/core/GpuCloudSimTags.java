package org.cloudbus.cloudsim.gpu.core;

import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.gpu.GpuVm;

/**
 * Contains Gpu-related events in the simulator.
 *
 * @author Ahmad Siavashi
 *
 */
public enum GpuCloudSimTags implements CloudSimTags {

	/**
	 * Denotes an event to submit a GpuTask for execution.
	 */
	GPU_TASK_SUBMIT,

	/**
	 * Denotes an internal event in the GpuDatacenter. Updates the progress of
	 * executions.
	 */
	VGPU_DATACENTER_EVENT,

	/**
	 * Denotes an event to evaluate the power consumption of a
	 * {@link org.cloudbus.cloudsim.gpu.power.PowerGpuDatacenter
	 * PowerGpuDatacenter}.
	 */
	GPU_VM_DATACENTER_POWER_EVENT,

	/**
	 * Denotes an event to perform a {@link GpuVm} placement in a
	 * {@link org.cloudbus.cloudsim.gpu.remote.RemoteGpuDatacenterEx
	 * RemoteGpuDatacenterEx}.
	 */
	GPU_VM_DATACENTER_PLACEMENT,

	/**
	 * Denotes an event to update GPU memory transfers.
	 */
	GPU_MEMORY_TRANSFER,

	/**
	 * Denotes the return of a GpuCloudlet to the sender.
	 */
	GPU_CLOUDLET_RETURN

}
