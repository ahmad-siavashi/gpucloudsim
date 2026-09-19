package org.cloudbus.cloudsim.gpu;

import java.util.ArrayList;
import java.util.List;

import org.cloudbus.cloudsim.CloudletScheduler;
import org.cloudbus.cloudsim.Vm;

/**
 * 
 * {@link GpuVm} extends {@link Vm} to represent a VM with GPU requirements.
 * 
 * @author Ahmad Siavashi
 * 
 */
public class GpuVm extends Vm {

	/**
	 * Describes vm's type. A type can be associated with a configuration, therefore
	 * it helps identifying the vm
	 */
	private String type;

	/**
	 * Denotes the time in which the VM enters the system.
	 */
	private double arrivalTime;

	/** The Vgpus associated with the Vm */
	private final List<Vgpu> vgpuList = new ArrayList<Vgpu>();

	/**
	 * @see Vm
	 * @param vgpu
	 *            the vgpu associated with this VM. Pass null in case of no vgpu.
	 * @param type
	 *            specifies the type of the vm
	 */
	public GpuVm(int id, int userId, double mips, int numberOfPes, int ram, long bw, long size, String vmm, String type,
			CloudletScheduler cloudletScheduler) {
		super(id, userId, mips, numberOfPes, ram, bw, size, vmm, cloudletScheduler);
		setType(type);
		setArrivalTime(0.0);
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param type
	 *            the type to set
	 */
	protected void setType(String type) {
		this.type = type;
	}

	/**
	 * @return the first vgpu, or null if the VM has none
	 */
	public Vgpu getVgpu() {
		return hasVgpu() ? vgpuList.get(0) : null;
	}

	/**
	 * @param vgpuId
	 *            the id of a vgpu, or -1 for the first vgpu
	 * @return the vgpu, or null if the VM has no such vgpu
	 */
	public Vgpu getVgpu(int vgpuId) {
		if (vgpuId == -1) {
			return getVgpu();
		}
		for (Vgpu vgpu : vgpuList) {
			if (vgpu.getId() == vgpuId) {
				return vgpu;
			}
		}
		return null;
	}

	/**
	 * @param gpuTask
	 *            the task to find the vgpu of (see {@link GpuTask#setVgpuId(int)})
	 * @return the vgpu that executes the task
	 */
	public Vgpu getVgpu(GpuTask gpuTask) {
		Vgpu vgpu = getVgpu(gpuTask.getVgpuId());
		if (vgpu == null) {
			throw new IllegalArgumentException("GpuTask #" + gpuTask.getTaskId() + " has no Vgpu #"
					+ gpuTask.getVgpuId() + " in VM #" + getId());
		}
		return vgpu;
	}

	/**
	 * Replaces the vgpus of the VM with the given one.
	 * 
	 * @param vgpu
	 *            the vgpu to set
	 */
	public void setVgpu(Vgpu vgpu) {
		vgpuList.clear();
		addVgpu(vgpu);
	}

	/**
	 * @param vgpu
	 *            the vgpu to add
	 */
	public void addVgpu(Vgpu vgpu) {
		vgpuList.add(vgpu);
		if (vgpu.getVm() != this) {
			vgpu.setGpuVm(this);
		}
	}

	/**
	 * @return the vgpus of the VM
	 */
	public List<Vgpu> getVgpuList() {
		return vgpuList;
	}
	
	public boolean hasVgpu() {
		return !vgpuList.isEmpty();
	}

	public double getArrivalTime() {
		return arrivalTime;
	}

	public void setArrivalTime(double arrivalTime) {
		this.arrivalTime = arrivalTime;
	}

}
