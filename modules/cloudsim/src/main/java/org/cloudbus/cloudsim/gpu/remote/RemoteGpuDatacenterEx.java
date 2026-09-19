package org.cloudbus.cloudsim.gpu.remote;

import java.util.List;

import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.VmAllocationPolicy;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.gpu.BusTags;
import org.cloudbus.cloudsim.gpu.GpuCloudlet;
import org.cloudbus.cloudsim.gpu.GpuTask;
import org.cloudbus.cloudsim.gpu.core.GpuCloudSimTags;
import org.cloudbus.cloudsim.gpu.placement.GpuDatacenterBrokerEx;
import org.cloudbus.cloudsim.gpu.placement.GpuDatacenterEx;

/**
 * 
 * This class extends {@link GpuDatacenterEx} to support remote vGPUs. It must
 * be used along with {@link GpuDatacenterBrokerEx} or its subclasses.
 * 
 * @author Ahmad Siavashi
 *
 */
public class RemoteGpuDatacenterEx extends GpuDatacenterEx {

	public RemoteGpuDatacenterEx(String name, DatacenterCharacteristics characteristics,
			VmAllocationPolicy vmAllocationPolicy, List<Storage> storageList, double schedulingInterval,
			double placementWindow) throws Exception {
		super(name, characteristics, vmAllocationPolicy, storageList, schedulingInterval, placementWindow);
	}

	protected boolean hasRemoteGpuOverhead(GpuTask gt) {
		if (getVmAllocationPolicy() instanceof RemoteGpuVmAllocationPolicy) {
			RemoteGpuVmAllocationPolicy vmAllocationPolicy = (RemoteGpuVmAllocationPolicy) getVmAllocationPolicy();
			if (vmAllocationPolicy.isRemoteVgpu(getGpuTaskVm(gt).getVgpu(gt))) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected void processGpuMemoryTransfer(SimEvent ev) {
		RemoteGpuTask gt = (RemoteGpuTask) ev.getData();

		double bandwidth = Double.valueOf(BusTags.PCI_E_3_X16_BW);

		if (gt.getStatus() == GpuTask.CREATED) {
			double delay = gt.getTaskInputSize() / bandwidth;
			send(getId(), delay, GpuCloudSimTags.GPU_TASK_SUBMIT, gt);
		} else if (gt.getStatus() == GpuTask.SUCCESS) {
			double delay = gt.getTaskOutputSize() / bandwidth;
			GpuCloudlet cl = gt.getCloudlet();
			if (hasRemoteGpuOverhead(gt)) {
				delay = gt.getCommunicationOverhead() / 100.0 * (CloudSim.clock() - cl.getExecStartTime() + delay);
			}
			send(getId(), delay, GpuCloudSimTags.GPU_CLOUDLET_RETURN, cl);
		}
	}

}
