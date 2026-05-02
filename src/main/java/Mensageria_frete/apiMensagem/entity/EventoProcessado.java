package Mensageria_frete.apiMensagem.entity;

import Mensageria_frete.apiMensagem.dto.EventoFreteRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
		name = "evento_processado",
		uniqueConstraints = @UniqueConstraint(name = "uk_evento_processado_chave", columnNames = "chave_idempotencia")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventoProcessado {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "chave_idempotencia", nullable = false, length = 255)
	private String chaveIdempotencia;

	@Column(nullable = false, length = 80)
	private String evento;

	@Column(name = "frete_id", nullable = false)
	private Long freteId;

	@Column(name = "data_evento", nullable = false)
	private LocalDateTime dataEvento;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private EventoProcessadoStatus status;

	@Column(name = "data_recebimento", nullable = false)
	private LocalDateTime dataRecebimento;

	@Column(name = "data_processamento")
	private LocalDateTime dataProcessamento;

	@Column(name = "mensagem_erro", length = 500)
	private String mensagemErro;

	public static EventoProcessado iniciar(EventoFreteRequest evento, String chaveIdempotencia) {
		EventoProcessado eventoProcessado = new EventoProcessado();
		eventoProcessado.chaveIdempotencia = chaveIdempotencia;
		eventoProcessado.evento = evento.evento();
		eventoProcessado.freteId = evento.frete().id();
		eventoProcessado.dataEvento = evento.dataEvento();
		eventoProcessado.status = EventoProcessadoStatus.PROCESSANDO;
		eventoProcessado.dataRecebimento = LocalDateTime.now();
		return eventoProcessado;
	}

	public void reprocessar() {
		this.status = EventoProcessadoStatus.PROCESSANDO;
		this.dataProcessamento = null;
		this.mensagemErro = null;
	}

	public void concluir() {
		this.status = EventoProcessadoStatus.PROCESSADO;
		this.dataProcessamento = LocalDateTime.now();
		this.mensagemErro = null;
	}

	public void registrarErro(String mensagemErro) {
		this.status = EventoProcessadoStatus.ERRO;
		this.dataProcessamento = LocalDateTime.now();
		this.mensagemErro = limitarMensagemErro(mensagemErro);
	}

	private String limitarMensagemErro(String mensagemErro) {
		if (mensagemErro == null || mensagemErro.length() <= 500) {
			return mensagemErro;
		}

		return mensagemErro.substring(0, 500);
	}
}
