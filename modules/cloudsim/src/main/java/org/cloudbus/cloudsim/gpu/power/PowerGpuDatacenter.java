package org.cloudbus.cloudsim.gpu.power;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.VmAllocationPolicy;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.gpu.GpuDatacenter;
import org.cloudbus.cloudsim.gpu.VideoCard;
import org.cloudbus.cloudsim.gpu.allocation.VideoCardAllocationPolicy;
import org.cloudbus.cloudsim.gpu.core.GpuCloudSimTags;

/**
 * {@link PowerGpuDatacenter} extends {@link GpuDatacenter} to enable simulation
 * of power-aware data centers.
 * 
 * @author Ahmad Siavashi
 * 
 */
public class PowerGpuDatacenter extends GpuDatacenter {

	/** host-energy mapping. */
	private Map<PowerGpuHost, Double> hostEnergyMap;
	/** host-cpu energy mapping. */
	private Map<PowerGpuHost, Double> hostCpuEnergyMap;
	/** host-videoCard energy mapping. */
	private Map<PowerGpuHost, Map<PowerVideoCard, Double>> hostVideoCardEnergyMap;
	
	/** to set aside idle hosts from power calculations. **/
	private boolean powerSavingMode;

	/**
	 * @see org.cloudbus.cloudsim.gpu.GpuDatacenter#GpuDatacenter(String,
	 *      DatacenterCharacteristics, VmAllocationPolicy, List, double)
	 *      GpuDatacenter(String, DatacenterCharacteristics, VmAllocationPolicy,
	 *      List, double)
	 */
	@SuppressWarnings("unchecked")
	public PowerGpuDatacenter(String name, DatacenterCharacteristics characteristics,
			VmAllocationPolicy vmAllocationPolicy, List<Storage> storageList, double schedulingInterval)
			throws Exception {
		super(name, characteristics, vmAllocationPolicy, storageList, schedulingInterval);
		setHostEnergyMap(new LinkedHashMap<PowerGpuHost, Double>());
		setHostCpuEnergyMap(new HashMap<PowerGpuHost, Double>());
		setHostVideoCardEnergyMap(new HashMap<PowerGpuHost, Map<PowerVideoCard, Double>>());
		for (Host host : getCharacteristics().<Host>getHostList()) {
			PowerGpuHost powerGpuHost = (PowerGpuHost) host;
			getHostEnergyMap().put(powerGpuHost, 0.0);
			getHostCpuEnergyMap().put(powerGpuHost, 0.0);
			getHostVideoCardEnergyMap().put(powerGpuHost, new HashMap<PowerVideoCard, Double>());
			VideoCardAllocationPolicy videoCardAllocationPolicy = powerGpuHost.getVideoCardAllocationPolicy();
			if (powerGpuHost.isGpuEquipped()) {
				for (PowerVideoCard videoCard : (List<PowerVideoCard>) videoCardAllocationPolicy.getVideoCards()) {
					getHostVideoCardEnergyMap().get(powerGpuHost).put(videoCard, 0.0);
				}
			}
		}
		setPowerSavingMode(false);
	}

	@SuppressWarnings("unchecked")
	protected void updatePower(double deltaTime) {
		for (Host host : this.<Host>getHostList()) {
			PowerGpuHost powerGpuHost = (PowerGpuHost) host;
			if (isPowerSavingMode() && powerGpuHost.isIdle()) {
				// Assume unused machines are powered off
				continue;
			}
			double hostCpuPower = powerGpuHost.getCurrentHostCpuPower();
			double hostCpuEnergy = getHostCpuEnergyMap().get(powerGpuHost);
			double hostCpuDeltaEnergy = hostCpuPower * deltaTime;
			hostCpuEnergy += hostCpuDeltaEnergy;
			getHostCpuEnergyMap().put(powerGpuHost, hostCpuEnergy);
			double videoCardsEnergy = 0.0;
			for (Entry<VideoCard, Double> videoCardPowerEntry : powerGpuHost.getCurrentVideoCardsPower().entrySet()) {
				PowerVideoCard videoCard = (PowerVideoCard) videoCardPowerEntry.getKey();
				double videoCardPower = videoCardPowerEntry.getValue();
				double videoCardDeltaEnergy = videoCardPower * deltaTime;
				double videoCardEnergy = getHostVideoCardEnergyMap().get(powerGpuHost).get(videoCard);
				videoCardEnergy += videoCardDeltaEnergy;
				getHostVideoCardEnergyMap().get(powerGpuHost).put(videoCard, videoCardEnergy);
				videoCardsEnergy += videoCardEnergy;
			}
			double hostTotalEnergy = hostCpuEnergy + videoCardsEnergy;
			getHostEnergyMap().put(powerGpuHost, hostTotalEnergy);
		}
	}
	
	@Override
	public void startEntity() {
		schedule(getId(), getSchedulingInterval(), GpuCloudSimTags.GPU_VM_DATACENTER_POWER_EVENT);
		super.startEntity();
	}

	@Override
	protected void processOtherEvent(SimEvent ev) {
		super.processOtherEvent(ev);
		if (ev.getTag() == GpuCloudSimTags.GPU_VM_DATACENTER_POWER_EVENT) {
			updatePower(getSchedulingInterval());
			schedule(getId(), getSchedulingInterval(), GpuCloudSimTags.GPU_VM_DATACENTER_POWER_EVENT);
		}
	}

	/**
	 * @return consumed energy so far
	 */
	public double getConsumedEnergy() {
		Double totalEnergy = 0.0;
		for (Double hostEnergy : getHostEnergyMap().values()) {
			totalEnergy += hostEnergy;
		}
		return totalEnergy.doubleValue();
	}

	/**
	 * @return the hostEnergyMap
	 */
	public Map<PowerGpuHost, Double> getHostEnergyMap() {
		return hostEnergyMap;
	}

	/**
	 * @param hostEnergyMap the hostEnergyMap to set
	 */
	protected void setHostEnergyMap(Map<PowerGpuHost, Double> hostEnergyMap) {
		this.hostEnergyMap = hostEnergyMap;
	}

	/**
	 * @return the hostVideoCardEnergyMap
	 */
	public Map<PowerGpuHost, Map<PowerVideoCard, Double>> getHostVideoCardEnergyMap() {
		return hostVideoCardEnergyMap;
	}

	/**
	 * @param hostVideoCardEnergyMap the hostVideoCardEnergyMap to set
	 */
	protected void setHostVideoCardEnergyMap(Map<PowerGpuHost, Map<PowerVideoCard, Double>> hostVideoCardEnergyMap) {
		this.hostVideoCardEnergyMap = hostVideoCardEnergyMap;
	}

	/**
	 * @return the hostCpuEnergyMap
	 */
	public Map<PowerGpuHost, Double> getHostCpuEnergyMap() {
		return hostCpuEnergyMap;
	}

	/**
	 * @param hostCpuEnergyMap the hostCpuEnergyMap to set
	 */
	protected void setHostCpuEnergyMap(Map<PowerGpuHost, Double> hostCpuEnergyMap) {
		this.hostCpuEnergyMap = hostCpuEnergyMap;
	}

	public boolean isPowerSavingMode() {
		return powerSavingMode;
	}

	public void setPowerSavingMode(boolean consolidate) {
		this.powerSavingMode = consolidate;
	}

}
