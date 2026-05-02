package Mensageria_frete.apiMensagem.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

	@Bean
	public DirectExchange freteExchange(@Value("${app.mensageria.rabbit.exchange}") String exchange) {
		return new DirectExchange(exchange, true, false);
	}

	@Bean
	public Queue freteQueue(@Value("${app.mensageria.rabbit.queue}") String queue) {
		return new Queue(queue, true);
	}

	@Bean
	public Binding freteBinding(
			Queue freteQueue,
			DirectExchange freteExchange,
			@Value("${app.mensageria.rabbit.routing-key}") String routingKey
	) {
		return BindingBuilder.bind(freteQueue).to(freteExchange).with(routingKey);
	}

	@Bean
	public MessageConverter messageConverter() {
		return new JacksonJsonMessageConverter();
	}

	@Bean
	public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
		RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
		rabbitTemplate.setMessageConverter(messageConverter);
		return rabbitTemplate;
	}
}
