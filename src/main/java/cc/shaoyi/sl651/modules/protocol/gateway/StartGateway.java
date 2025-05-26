package cc.shaoyi.sl651.modules.protocol.gateway;


import cc.shaoyi.sl651.modules.protocol.gateway.customize.SimpleCustomizeGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * @author Shao Yi
 * @description 网关启动
 * @date 2022年08月03日 20:05
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StartGateway implements CommandLineRunner {

	private final SimpleCustomizeGateway simpleCustomizeGateway;


	@Override
	public void run(String... args) throws Exception {
		new Thread(() -> {
			try {
				simpleCustomizeGateway.startup();
			} catch (Exception e) {
				log.error("网关启动失败", e);
			}
		}).start();
//		simpleCustomizeGateway.startup();
	}
}
