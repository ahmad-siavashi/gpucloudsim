package org.cloudbus.cloudsim.gpu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.NotImplementedException;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudActionTags;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.core.predicates.PredicateType;
import org.cloudbus.cloudsim.gpu.core.GpuCloudSimTags;
import org.cloudbus.cloudsim.lists.VmList;

/**
 * {@link GpuDatacenterBroker} extends {@link DatacenterBroker} to add support
 * for GpuCloudlets. Each {@link GpuCloudlet} must have a {@link GpuVm}
 * associated with it. If a {@link GpuVm} fails to find a {@link GpuHost} in all
 * {@link GpuDatacenter}s, it will be rejected.
 * 
 * @author Ahmad Siavashi
 * 
 */
public class GpuDatacenterBroker extends DatacenterBroker {

	/** A structure to maintain VM-GpuCloudlet mapping */
	private HashMap<String, List<GpuCloudlet>> vmGpuCloudletMap;

	/** The number of submitted gpuCloudlets in each vm. */
	private HashMap<String, Integer> vmGpuCloudletsSubmitted;

	/** The number of VM destroy requests whose acknowledgement has not arrived yet. */
	private int vmDestroyAcksPending;

	/**
	 * @see DatacenterBroker
	 */
	public GpuDatacenterBroker(String name) throws Exception {
		super(name);
		setGpuVmCloudletMap(new HashMap<String, List<GpuCloudlet>>());
		setVmGpuCloudletsSubmitted(new HashMap<String, Integer>());
	}

	@Override
	protected void finishExecution() {
		for (Integer datacenterId : getDatacenterIdsList()) {
			CloudSim.cancelAll(datacenterId.intValue(), new PredicateType(GpuCloudSimTags.VGPU_DATACENTER_EVENT));
		}
		super.finishExecution();
	}

	@Override
	protected void createVmsInDatacenter(int datacenterId) {
		// send as much vms as possible for this datacenter before trying the
		// next one
		int requestedVms = 0;
		for (GpuVm vm : this.<GpuVm>getGuestList()) {
			if (!getVmsToDatacentersMap().containsKey(vm.getId()) && !getGuestsCreatedList().contains(vm)) {
				getVmsToDatacentersMap().put(vm.getId(), datacenterId);
				send(datacenterId, vm.getArrivalTime(), CloudActionTags.VM_CREATE_ACK, vm);
				requestedVms++;
			}
		}
		getDatacenterRequestedIdsList().add(datacenterId);
		send(datacenterId, CloudSim.getMinTimeBetweenEvents(), GpuCloudSimTags.VGPU_DATACENTER_EVENT);
		setVmsRequested(requestedVms);
		setVmsAcks(0);
	}

	@Override
	protected void processResourceCharacteristics(SimEvent ev) {
		DatacenterCharacteristics characteristics = (DatacenterCharacteristics) ev.getData();
		getDatacenterCharacteristicsList().put(characteristics.getId(), characteristics);

		if (getDatacenterCharacteristicsList().size() == getDatacenterIdsList().size()) {
			setDatacenterRequestedIdsList(new ArrayList<Integer>());
			createVmsInDatacenter(getDatacenterIdsList().get(0));
		}

	}

	@Override
	protected void processVmCreateAck(SimEvent ev) {
		int[] data = (int[]) ev.getData();
		int datacenterId = data[0];
		int vmId = data[1];
		int result = data[2];

		Vm vm = VmList.getById(getGuestList(), vmId);
		String vmUid = vm.getUid();

		if (result == CloudSimTags.TRUE) {
			getVmsToDatacentersMap().put(vmId, datacenterId);
			getGuestsCreatedList().add(vm);
			setVmsAcks(getVmsAcks() + 1);

			Log.printConcatLine(CloudSim.clock(), ": ", getName(), ": VM #", vmId, " has been created in Datacenter #",
					datacenterId, ", Host #", vm.getHost().getId());
			System.out.println("{'clock': " + CloudSim.clock() + ", 'type': 'vm allocation',  'vm': " + vm.getId()
					+ ", 'host': " + vm.getHost().getId() + "}");
			Vgpu vgpu = ((GpuVm) vm).getVgpu();
			if (vgpu != null) {
				Pgpu pgpu = vgpu.getVideoCard().getVgpuScheduler().getPgpuForVgpu(vgpu);
				System.out.println("{'clock': " + CloudSim.clock() + ", 'type': 'vgpu allocation', 'vgpu': "
						+ vgpu.getId() + ", 'pgpu': " + pgpu.getId() + ", 'vm': " + vm.getId() + "}");
			}
			// VM has been created successfully, submit its cloudlets now.
			List<GpuCloudlet> vmCloudlets = getVmGpuCloudletMap().get(vmUid);
			for (GpuCloudlet cloudlet : vmCloudlets) {
				submitGpuCloudlet(cloudlet);
			}
			getVmGpuCloudletsSubmitted().put(vmUid, vmCloudlets.size());
			// Remove submitted cloudlets from queue
			getCloudletList().removeAll(vmCloudlets);
			getVmGpuCloudletMap().get(vmUid).removeAll(vmCloudlets);
			getVmGpuCloudletMap().remove(vmUid);
		} else {
			Log.printConcatLine(CloudSim.clock(), ": ", getName(), ": Creation of VM #", vmId,
					" failed in Datacenter #", datacenterId);
			// Create the VM in another datacenter.
			int nextDatacenterId = getDatacenterIdsList()
					.get((getDatacenterIdsList().indexOf(datacenterId) + 1) % getDatacenterIdsList().size());
			if (!getDatacenterRequestedIdsList().contains(nextDatacenterId)) {
				getDatacenterRequestedIdsList().add(nextDatacenterId);
				send(nextDatacenterId, CloudSim.getMinTimeBetweenEvents(), GpuCloudSimTags.VGPU_DATACENTER_EVENT);
			}
			// Check for looping datacenters
			if (getDatacenterIdsList().indexOf(nextDatacenterId) != 0) {
				getVmsToDatacentersMap().replace(vmId, nextDatacenterId);
				send(nextDatacenterId, CloudSim.getMinTimeBetweenEvents(), CloudActionTags.VM_CREATE_ACK, vm);
			} else {
				// Reject the VM
				System.out.println(
						"{'clock': " + CloudSim.clock() + ", 'type': 'vm rejection',  'vm': " + vm.getId() + "}");
				List<GpuCloudlet> vmCloudlets = getVmGpuCloudletMap().get(vmUid);
				getCloudletList().removeAll(vmCloudlets);
				getVmGpuCloudletMap().get(vmUid).removeAll(vmCloudlets);
				getVmGpuCloudletMap().remove(vmUid);
			}
		}
	}

	protected void processVmDestroy(SimEvent ev) {
		int[] data = (int[]) ev.getData();
		int datacenterId = data[0];
		int vmId = data[1];
		int result = data[2];

		if (result == CloudSimTags.TRUE) {
			Log.printLine(CloudSim.clock() + ": VM #" + vmId + " destroyed in Datacenter #" + datacenterId);
			System.out.println("{'clock': " + CloudSim.clock() + ", 'type': 'vm deallocation',  'vm': " + vmId + "}");
			setVmsDestroyed(getVmsDestroyed() + 1);
			getVmGpuCloudletsSubmitted().remove(Vm.getUid(getId(), vmId));
		} else {
			Log.printLine(CloudSim.clock() + ": Failed to destroy VM #" + vmId + " in Datacenter #" + datacenterId);
		}
		vmDestroyAcksPending--;
		finishExecutionIfDone();
	}

	/**
	 * Finishes the execution once all cloudlets have returned and all VM destroy
	 * acknowledgements have been received. Since CloudSim 7, a finished entity
	 * can no longer receive events, so it must not shut down while acks are
	 * still on their way.
	 */
	protected void finishExecutionIfDone() {
		if (getCloudletList().isEmpty() && cloudletsSubmitted == 0 && vmDestroyAcksPending == 0) {
			Log.printConcatLine(CloudSim.clock(), ": ", getName(), ": All Jobs executed. Finishing...");
			clearDatacenters();
			finishExecution();
		}
	}

	@Override
	protected void processCloudletReturn(SimEvent ev) {
		Cloudlet cloudlet = (Cloudlet) ev.getData();
		getCloudletReceivedList().add(cloudlet);
		Log.printConcatLine(CloudSim.clock(), ": ", getName(), ": Cloudlet ", cloudlet.getCloudletId(), " received");
		GpuVm cloudletVm = VmList.getByIdAndUserId(getGuestList(), cloudlet.getVmId(), getId());
		getVmGpuCloudletsSubmitted().replace(cloudletVm.getUid(),
				getVmGpuCloudletsSubmitted().get(cloudletVm.getUid()) - 1);
		cloudletsSubmitted--;
		if (getVmGpuCloudletsSubmitted().get(cloudletVm.getUid()) == 0) {
			sendNow(getVmsToDatacentersMap().get(cloudlet.getVmId()), CloudActionTags.VM_DESTROY_ACK, cloudletVm);
			vmDestroyAcksPending++;
			getGuestsCreatedList().remove(cloudletVm);
		}
		finishExecutionIfDone();
	}

	@Override
	protected void processOtherEvent(SimEvent ev) {
		// VM Destroy answer
		if (ev.getTag() == CloudActionTags.VM_DESTROY_ACK) {
			processVmDestroy(ev);
		} else {
			super.processOtherEvent(ev);
		}
	}

	protected void submitGpuCloudlet(GpuCloudlet gpuCloudlet) {
		int datacenterId = getVmsToDatacentersMap().get(gpuCloudlet.getVmId());
		sendNow(datacenterId, CloudActionTags.CLOUDLET_SUBMIT, gpuCloudlet);
		getCloudletSubmittedList().add(gpuCloudlet);
		cloudletsSubmitted++;
	}

	@Override
	public void bindCloudletToVm(int cloudletId, int vmId) {
		throw new NotImplementedException("not implemented");
	}

	@Override
	public void submitCloudletList(List<? extends Cloudlet> list) {
		getCloudletList().addAll(list);
		if (getGuestList().isEmpty()) {
			throw new IllegalArgumentException("no vm submitted.");
		}
		for (Cloudlet cloudlet : getCloudletList()) {
			if (cloudlet.getVmId() < 0) {
				throw new IllegalArgumentException("cloudlet (#" + cloudlet.getCloudletId() + ") has no VM.");
			}
			GpuVm vm = VmList.getById(getGuestList(), cloudlet.getVmId());
			if (vm == null) {
				throw new IllegalArgumentException("no such vm (Id #" + cloudlet.getVmId() + ") exists for cloudlet (#"
						+ cloudlet.getCloudletId() + ")");
			}
			getVmGpuCloudletMap().get(vm.getUid()).add((GpuCloudlet) cloudlet);
		}
	}

	@Override
	public void submitGuestList(List<? extends GuestEntity> list) {
		super.submitGuestList(list);
		for (GuestEntity vm : getGuestList()) {
			if (!getVmGpuCloudletMap().containsKey(vm.getUid())) {
				getVmGpuCloudletMap().put(vm.getUid(), new ArrayList<>());
			}
		}
	}

	/**
	 * @return the vmGpuCloudletsSubmitted
	 */
	protected HashMap<String, Integer> getVmGpuCloudletsSubmitted() {
		return vmGpuCloudletsSubmitted;
	}

	/**
	 * @param vmGpuCloudletsSubmitted the vmGpuCloudletsSubmitted to set
	 */
	protected void setVmGpuCloudletsSubmitted(HashMap<String, Integer> vmGpuCloudletsSubmitted) {
		this.vmGpuCloudletsSubmitted = vmGpuCloudletsSubmitted;
	}

	/**
	 * @return the vmGpuCloudletMap
	 */
	protected HashMap<String, List<GpuCloudlet>> getVmGpuCloudletMap() {
		return vmGpuCloudletMap;
	}

	/**
	 * @param vmGpuCloudletMap the vmGpuCloudletMap to set
	 */
	protected void setGpuVmCloudletMap(HashMap<String, List<GpuCloudlet>> vmGpuCloudletMap) {
		this.vmGpuCloudletMap = vmGpuCloudletMap;
	}
}
