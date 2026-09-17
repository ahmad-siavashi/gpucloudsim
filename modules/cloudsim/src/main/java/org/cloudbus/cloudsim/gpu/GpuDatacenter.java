package org.cloudbus.cloudsim.gpu;

import java.util.List;

import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicy;
import org.cloudbus.cloudsim.core.CloudActionTags;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.gpu.core.GpuCloudSimTags;

/**
 * {@link GpuDatacenter} extends {@link Datacenter} to support
 * {@link GpuCloudlet}s as well as the memory transfer between CPU and GPU.
 * 
 * @author Ahmad Siavashi
 * 
 */
public class GpuDatacenter extends Datacenter {

	private double gpuTaskLastProcessTime;

	/**
	 * See {@link Datacenter#Datacenter}
	 */
	public GpuDatacenter(String name, DatacenterCharacteristics characteristics, VmAllocationPolicy vmAllocationPolicy,
			List<Storage> storageList, double schedulingInterval) throws Exception {
		super(name, characteristics, vmAllocationPolicy, storageList, schedulingInterval);
		setGpuTaskLastProcessTime(0.0);
	}

	@Override
	public void processEvent(SimEvent ev) {
		super.processEvent(ev);
	}

	@Override
	protected void processOtherEvent(SimEvent ev) {
		CloudSimTags tag = ev.getTag();
		if (tag == GpuCloudSimTags.GPU_MEMORY_TRANSFER) {
			processGpuMemoryTransfer(ev);
		} else if (tag == GpuCloudSimTags.GPU_TASK_SUBMIT) {
			processGpuTaskSubmit(ev);
		} else if (tag == GpuCloudSimTags.GPU_CLOUDLET_RETURN) {
			processGpuCloudletReturn(ev);
		} else if (tag == GpuCloudSimTags.VGPU_DATACENTER_EVENT) {
			updateGpuTaskProcessing();
			checkGpuTaskCompletion();
		} else {
			super.processOtherEvent(ev);
		}
	}

	protected GpuVm getGpuTaskVm(GpuTask gt) {
		int userId = gt.getCloudlet().getUserId();
		int vmId = gt.getCloudlet().getVmId();

		GpuHost host = (GpuHost) getVmAllocationPolicy().getHost(vmId, userId);
		GpuVm vm = (GpuVm) host.getVm(vmId, userId);

		return vm;
	}

	protected void notifyGpuTaskCompletion(GpuTask gt) {
		GpuVm vm = getGpuTaskVm(gt);
		GpuCloudletScheduler scheduler = (GpuCloudletScheduler) vm.getCloudletScheduler();
		scheduler.notifyGpuTaskCompletion(gt);
	}

	protected void processGpuCloudletReturn(SimEvent ev) {
		GpuCloudlet cloudlet = (GpuCloudlet) ev.getData();
		sendNow(cloudlet.getUserId(), CloudActionTags.CLOUDLET_RETURN, cloudlet);
		notifyGpuTaskCompletion(cloudlet.getGpuTask());
	}

	protected void processGpuMemoryTransfer(SimEvent ev) {
		GpuTask gt = (GpuTask) ev.getData();

		double bandwidth = Double.valueOf(BusTags.PCI_E_3_X16_BW);

		if (gt.getStatus() == GpuTask.CREATED) {
			double delay = gt.getTaskInputSize() / bandwidth;
			send(getId(), delay, GpuCloudSimTags.GPU_TASK_SUBMIT, gt);
		} else if (gt.getStatus() == GpuTask.SUCCESS) {
			double delay = gt.getTaskOutputSize() / bandwidth;
			send(getId(), delay, GpuCloudSimTags.GPU_CLOUDLET_RETURN, gt.getCloudlet());
		}
	}

	protected void updateGpuTaskProcessing() {
		// if some time passed since last processing
		// R: for term is to allow loop at simulation start. Otherwise, one initial
		// simulation step is skipped and schedulers are not properly initialized
		if (CloudSim.clock() < 0.111
				|| CloudSim.clock() > getGpuTaskLastProcessTime() + CloudSim.getMinTimeBetweenEvents()) {
			List<? extends HostEntity> list = getVmAllocationPolicy().getHostList();
			double smallerTime = Double.MAX_VALUE;
			// for each host...
			for (int i = 0; i < list.size(); i++) {
				GpuHost host = (GpuHost) list.get(i);
				// inform VMs to update processing
				double time = host.updateVgpusProcessing(CloudSim.clock());
				// what time do we expect that the next task will finish?
				if (time < smallerTime) {
					smallerTime = time;
				}
			}
			// guarantees a minimal interval before scheduling the event
			if (smallerTime < CloudSim.clock() + CloudSim.getMinTimeBetweenEvents() + 0.01) {
				smallerTime = CloudSim.clock() + CloudSim.getMinTimeBetweenEvents() + 0.01;
			}
			if (smallerTime != Double.MAX_VALUE) {
				schedule(getId(), (smallerTime - CloudSim.clock()), GpuCloudSimTags.VGPU_DATACENTER_EVENT);
			}
			setGpuTaskLastProcessTime(CloudSim.clock());
		}
	}

	protected void checkGpuTaskCompletion() {
		List<? extends HostEntity> list = getVmAllocationPolicy().getHostList();
		for (int i = 0; i < list.size(); i++) {
			GpuHost host = (GpuHost) list.get(i);
			for (Vm vm : host.getVmList()) {
				GpuVm gpuVm = (GpuVm) vm;
				Vgpu vgpu = gpuVm.getVgpu();
				if (vgpu != null) {
					while (vgpu.getGpuTaskScheduler().hasFinishedTasks()) {
						ResGpuTask rgt = vgpu.getGpuTaskScheduler().getNextFinishedTask();
						try {
							sendNow(getId(), GpuCloudSimTags.GPU_MEMORY_TRANSFER, rgt.getGpuTask());
						} catch (Exception e) {
							e.printStackTrace();
							CloudSim.abruptallyTerminate();
						}
					}
				}
			}
		}
	}

	@Override
	protected void checkCloudletCompletion() {
		super.checkCloudletCompletion();
		List<? extends HostEntity> list = getVmAllocationPolicy().getHostList();
		for (int i = 0; i < list.size(); i++) {
			HostEntity host = list.get(i);
			for (GuestEntity vm : host.getGuestList()) {
				GpuCloudletScheduler scheduler = (GpuCloudletScheduler) vm.getCloudletScheduler();
				while (scheduler.hasGpuTask()) {
					GpuTask gt = scheduler.getNextGpuTask();
					sendNow(getId(), GpuCloudSimTags.GPU_MEMORY_TRANSFER, gt);
				}
			}
		}
	}

	@Override
	protected void processVmCreate(SimEvent ev, boolean ack) {
		Vm vm = (Vm) ev.getData();
		Log.printLine(CloudSim.clock() + ": Trying to Create VM #" + vm.getId() + " in " + getName());

		boolean result = getVmAllocationPolicy().allocateHostForVm(vm);

		if (ack) {
			int[] data = new int[3];
			data[0] = getId();
			data[1] = vm.getId();

			if (result) {
				data[2] = CloudSimTags.TRUE;
			} else {
				data[2] = CloudSimTags.FALSE;
			}
			send(vm.getUserId(), CloudSim.getMinTimeBetweenEvents(), CloudActionTags.VM_CREATE_ACK, data);
		}

		if (result) {
			getVmList().add(vm);
			GpuVm gpuVm = (GpuVm) vm;
			Vgpu vgpu = gpuVm.getVgpu();

			if (vm.isBeingInstantiated()) {
				vm.setBeingInstantiated(false);
			}

			vm.updateVmProcessing(CloudSim.clock(),
					getVmAllocationPolicy().getHost(vm).getVmScheduler().getAllocatedMipsForVm(vm));

			if (vgpu != null) {
				if (vgpu.isBeingInstantiated()) {
					vgpu.setBeingInstantiated(false);
				}

				VideoCard videoCard = vgpu.getVideoCard();
				vgpu.updateGpuTaskProcessing(CloudSim.clock(),
						videoCard.getVgpuScheduler().getAllocatedMipsForVgpu(vgpu));
			}
		}

	}

	@Override
	protected void processVmDestroy(SimEvent ev, boolean ack) {
		GpuVm vm = (GpuVm) ev.getData();
		if (vm.hasVgpu()) {
			((GpuVmAllocationPolicy) getVmAllocationPolicy()).deallocateGpuForVgpu(vm.getVgpu());
		}
		super.processVmDestroy(ev, ack);
	}

	protected void processGpuTaskSubmit(SimEvent ev) {
		updateGpuTaskProcessing();

		try {
			// gets the task object
			GpuTask gt = (GpuTask) ev.getData();

			// TODO: checks whether this task has finished or not

			// process this task to this CloudResource
			gt.setResourceParameter(getId(), getCharacteristics().getCostPerSecond(),
					getCharacteristics().getCostPerBw());

			GpuVm vm = getGpuTaskVm(gt);
			Vgpu vgpu = vm.getVgpu();

			GpuTaskScheduler scheduler = vgpu.getGpuTaskScheduler();

			double estimatedFinishTime = scheduler.taskSubmit(gt);

			// if this task is in the exec queue
			if (estimatedFinishTime > 0.0 && !Double.isInfinite(estimatedFinishTime)) {
				send(getId(), estimatedFinishTime, GpuCloudSimTags.VGPU_DATACENTER_EVENT);
			}

		} catch (ClassCastException c) {
			Log.printLine(getName() + ".processGpuTaskSubmit(): " + "ClassCastException error.");
			c.printStackTrace();
		} catch (Exception e) {
			Log.printLine(getName() + ".processGpuTaskSubmit(): " + "Exception error.");
			e.printStackTrace();
			System.exit(-1);
		}

		checkGpuTaskCompletion();
	}

	protected double getGpuTaskLastProcessTime() {
		return gpuTaskLastProcessTime;
	}

	protected void setGpuTaskLastProcessTime(double lastGpuTaskProcessTime) {
		this.gpuTaskLastProcessTime = lastGpuTaskProcessTime;
	}

}
