package Mensageria_frete.apiMensagem.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OcorrenciaPayloadRequest(
		@NotBlank String tipo,
		@NotBlank String descricaoTipo,
		@NotNull LocalDateTime dataHora,
		@NotBlank String municipio,
		@NotBlank String uf,
		String descricao,
		String nomeRecebedor,
		String documentoRecebedor
) {
}
