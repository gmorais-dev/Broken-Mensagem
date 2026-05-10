package Mensageria_frete.apiMensagem.repository;

import Mensageria_frete.apiMensagem.entity.EventoProcessado;
import Mensageria_frete.apiMensagem.entity.EventoProcessadoStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EventoProcessadoRepository extends JpaRepository<EventoProcessado, Long> {

	Optional<EventoProcessado> findByChaveIdempotencia(String chaveIdempotencia);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			update EventoProcessado evento
			   set evento.status = :statusDestino,
			       evento.dataProcessamento = null,
			       evento.mensagemErro = null
			 where evento.chaveIdempotencia = :chaveIdempotencia
			   and evento.status = :statusAtual
			""")
	int atualizarStatusSeCorresponder(
			@Param("chaveIdempotencia") String chaveIdempotencia,
			@Param("statusAtual") EventoProcessadoStatus statusAtual,
			@Param("statusDestino") EventoProcessadoStatus statusDestino
	);
}
