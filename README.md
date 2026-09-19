# GPUCloudSim: An Extension Of CloudSim For Modeling And Simulation Of GPUs In Cloud Data Centers #

In order to satisfy graphical and computational requirements of end-users, today cloud providers offer GPU-enabled services. Evaluating new GPU provisioning, scheduling and placement policies on real GPU-equipped testbeds is costly, limits experiments to the scale of the testbed and makes the reproduction of results difficult. Hence, simulation may be used.

GPUCloudSim extends [CloudSim](http://cloudbus.org/cloudsim/) with models and policies for GPUs in cloud data centers, from video cards and physical GPUs (pGPUs) to virtual GPUs (vGPUs) and GPU applications, allowing its users to focus on GPU-specific design issues that they want to investigate.


# Main features #

  * Support for modeling and simulation of GPU-equipped hosts with multiple video cards and pGPUs
  * Support for modeling and simulation of GPU-enabled VMs with one or more vGPUs and GPU applications
  * Support for modeling and simulation of CPU-GPU memory transfers over PCIe
  * Support for modeling and simulation of NVIDIA A16 and L40S boards, vGPU types and pass-through
  * Support for NVIDIA vGPU placement rules, including equal-size and mixed-size modes and multi-vGPU VMs
  * Support for user-defined GPU memory, GPU memory bandwidth and PCIe bandwidth provisioning policies
  * Support for user-defined vGPU scheduling policies, including space-shared, time-shared and fair-share
  * Support for user-defined GPU task scheduling policies
  * Support for user-defined video card allocation and pGPU selection policies
  * Support for user-defined GPU-aware VM placement policies and placement windows
  * Support for modeling and simulation of GPU remoting ([Siavashi et al. 2023](https://doi.org/10.1016/j.jpdc.2022.10.008))
  * Support for modeling and simulation of energy-aware GPU-equipped data centers
  * Support for modeling and simulation of vGPU virtualization overhead
  * Support for modeling and simulation of interference among co-running GPU tasks


# Package structure #

The extension has three layers. Generic GPU sharing is in [`org.cloudbus.cloudsim.gpu`](modules/cloudsim/src/main/java/org/cloudbus/cloudsim/gpu), the rules of NVIDIA vGPU are in [`gpu.hardware_assisted.grid`](modules/cloudsim/src/main/java/org/cloudbus/cloudsim/gpu/hardware_assisted/grid), and GPU remoting is in [`gpu.remote`](modules/cloudsim/src/main/java/org/cloudbus/cloudsim/gpu/remote). Each layer uses the layers below it, never above.

**Generic.** This layer models any shared GPU. No vendor rules are here. A host has video cards. A card has one or more physical GPUs (pGPUs). A pGPU has streaming multiprocessors (PEs), memory and memory bandwidth. When a VM arrives, three choices are made in order: which host runs the VM, which card holds each of its vGPUs, and which pGPU on that card. Each choice has several policies: take the first that fits, fill the busiest one first, use the emptiest one first, or count vGPUs instead of free memory. A VM is placed completely or not at all; if one vGPU does not fit, the VM releases everything and is rejected. Next, a vGPU scheduler decides speed. A vGPU can own its multiprocessors alone, share them, or share them in proportion to what each vGPU requested. Inside one vGPU, a task scheduler runs the GPU tasks: each task takes the multiprocessors it requests, and the free ones go to the next task, so tasks run together until the pGPU is full. Two optional models add the cost of sharing: a fixed percentage lost to virtualization, and a slowdown when tasks together need more memory bandwidth than the vGPU has.

**Grid.** This layer contains only the rules of NVIDIA vGPU. Real hardware does not allow arbitrary partitions. A vGPU type of a given memory size can only be placed at certain positions inside the pGPU memory. A pGPU works in one of two modes: all vGPUs have the same size, or sizes may differ but only at allowed positions. One VM can have at most 16 vGPUs, and they must come from boards of the same type. This layer stores these tables and rejects placements that break them. For this reason, a pGPU with enough free memory can still reject a vGPU: the memory is free, but not at a legal position. This layer also provides the two NVIDIA schedulers. The default one divides the pGPU only among the vGPUs that have work, so an idle vGPU costs nothing. The equal-share one divides the pGPU among all vGPUs, busy or not.

**Remote.** This layer models GPU sharing over the network. The GPU calls of an application are intercepted and sent to a GPU in another host, so a host without a GPU can still run GPU work. Two things follow. First, placement changes: a local vGPU must stay on the host of its VM, but a remote vGPU can be on any GPU host, and the VM itself can run on a host without a GPU. Second, each vGPU has a tenancy, which says local or remote, and also whether the vGPU shares its pGPU or uses it alone. The scheduler enforces this in both directions: an exclusive vGPU does not join an occupied pGPU, and no vGPU joins a pGPU held by an exclusive one. Network work is not free: a remote GPU task pays a communication overhead proportional to its execution time.


# Download #

This package contains CloudSim 7.0.1 with the latest version of the GPU extension. The extension is located in [`org.cloudbus.cloudsim.gpu`](modules/cloudsim/src/main/java/org/cloudbus/cloudsim/gpu) and its examples in [`org.cloudbus.cloudsim.examples.gpu`](modules/cloudsim-examples/src/main/java/org/cloudbus/cloudsim/examples/gpu).


# Publications #

If you used the extension, please consider citing the following paper,

  * **Siavashi, A., Momtazpour, M. GPUCloudSim: an extension of CloudSim for modeling and simulation of GPUs in cloud data centers. J Supercomput 75, 2535–2561 (2019).**


# Disclaimer #

This code is provided as is, and no guarantee is given that this code will perform in the desired way.
