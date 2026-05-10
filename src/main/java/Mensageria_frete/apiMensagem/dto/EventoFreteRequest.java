package Mensageria_frete.apiMensagem.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EventoFreteRequest(
		@NotBlank String versao,
		@NotBlank String evento,
		@NotBlank String origem,
		String endpointMensageria,
		Boolean mensageriaHabilitada,
		@NotNull LocalDateTime dataEvento,
		@NotNull @Valid FretePayloadRequest frete,
		@Valid OcorrenciaPayloadRequest ocorrencia
) {
	public String chaveIdempotencia() {
		return evento + ":" + frete.id() + ":" + dataEvento;
	}
}
