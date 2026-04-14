package cc.shaoyi.sl651.common.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.net.NetUtil;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 终极网络工具类：属性探测版
 * 解决 Docker 桥接网卡干扰，同时兼容政务网非标准命名物理网卡
 */
public class BetterNetUtil {

	public static String getRealIp() {
		List<IpCandidate> candidates = new ArrayList<>();

		try {
			List<NetworkInterface> interfaces = CollUtil.newArrayList(NetworkInterface.getNetworkInterfaces().asIterator());

			for (NetworkInterface ni : interfaces) {
				// 1. 基础过滤：必须启动、非回环
				if (!ni.isUp() || ni.isLoopback()) continue;

				// 2. 关键：获取硬件地址（MAC地址）
				byte[] mac = ni.getHardwareAddress();
				// 虚拟设备（如某些隧道）可能没有 MAC 地址
				if (mac == null || mac.length == 0) continue;

				ni.getInetAddresses().asIterator().forEachRemaining(addr -> {
					if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
						candidates.add(new IpCandidate(ni, addr));
					}
				});
			}
		} catch (SocketException e) {
			return NetUtil.getLocalhostStr();
		}

		if (CollUtil.isNotEmpty(candidates)) {
			return candidates.stream()
				.sorted(Comparator.comparingInt(BetterNetUtil::judgePriority))
				.map(c -> c.address.getHostAddress())
				.findFirst()
				.orElse(NetUtil.getLocalhostStr());
		}

		return NetUtil.getLocalhostStr();
	}

	/**
	 * 智能判定优先级
	 */
	private static int judgePriority(IpCandidate c) {
		String name = c.ni.getName().toLowerCase();
		String ip = c.address.getHostAddress();

		// 优先级 1：物理机特征最明显的网卡
		// 1.1 名字像物理网卡 (en, eth, em)
		// 1.2 或者 IP 是 192.168 段（通常是办公/机房物理网）
		if (name.startsWith("en") || name.startsWith("eth") || name.startsWith("em")) {
			return 1;
		}
		if (ip.startsWith("192.168.")) {
			return 2;
		}

		// 优先级 2：政务网/专网特征
		// 这种环境 IP 可能是 10.x.x.x，且网卡名不规范，但只要它有物理 MAC 且不是虚拟的
		if (ip.startsWith("10.")) {
			return 3;
		}

		// 优先级 3：疑似虚拟桥接但带 MAC 地址的
		// Docker 的桥接网卡通常以 172.16-31 开头
		if (name.startsWith("br-") || name.startsWith("docker") || ip.startsWith("172.")) {
			return 100; // 尽量往后排
		}

		return 50; // 中庸地址
	}

	private static class IpCandidate {
		NetworkInterface ni;
		InetAddress address;
		IpCandidate(NetworkInterface ni, InetAddress addr) {
			this.ni = ni;
			this.address = addr;
		}
	}
}
