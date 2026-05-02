package Mensageria_frete.apiMensagem.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FretePayloadRequest(
		@NotNull Long id,
		@NotBlank String numero,
		@NotBlank String status,
		Long idRemetente,
		Long idDestinatario,
		Long idMotorista,
		Long idVeiculo,
		String origem,
		String destino,
		@Positive BigDecimal pesoKg,
		@Positive BigDecimal valorTotal
) {
}
