package Mensageria_frete.apiMensagem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ApiMensagemApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiMensagemApplication.class, args);
	}

}
