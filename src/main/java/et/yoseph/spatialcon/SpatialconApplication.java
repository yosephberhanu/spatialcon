package et.yoseph.spatialcon;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import et.yoseph.spatialcon.common.StorageService;
import et.yoseph.spatialcon.conversion.ConvertProperties;

@SpringBootApplication
@EnableConfigurationProperties(ConvertProperties.class)
public class SpatialconApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpatialconApplication.class, args);
	}

	CommandLineRunner init(StorageService storageService) {
		return (args) -> {
			storageService.deleteAll();
			storageService.init();
		};
	}

}
