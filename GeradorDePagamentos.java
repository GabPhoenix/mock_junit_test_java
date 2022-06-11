package servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import infra.EnviadorDeEmail;
import infra.Relogio;
import infra.dao.RepositorioDeLeiloes;
import infra.dao.RepositorioDePagamentos;
import modelo.Avaliador;
import modelo.Lance;
import modelo.Leilao;
import modelo.Pagamento;
import modelo.Usuario;

/*
 * @author Gabriel Carvalho
 */
class GeradorDePagamentos {
	private RepositorioDeLeiloes daoFalso;
	private RepositorioDePagamentos repositorioDePagamentos;
	private Avaliador avaliador;
	private GeradorDePagamento gerador;
	private EncerradorDeLeilao encerrador;
	private EnviadorDeEmail carteiroFalso;

	
	@BeforeEach
	public void beforeEach() {
		daoFalso = mock(RepositorioDeLeiloes.class);
		carteiroFalso = mock(EnviadorDeEmail.class);
		repositorioDePagamentos = mock(RepositorioDePagamentos.class);
		avaliador = new Avaliador("Tiago");
		gerador = new GeradorDePagamento(daoFalso, repositorioDePagamentos, avaliador);
		encerrador = new EncerradorDeLeilao(daoFalso, carteiroFalso);
	}
	
	@DisplayName("Teste Gera Pagamento Sem Leião")
	@Test
	public void geraPagamentoSemLeilão() {
		
		// criar lista de leilões encerrados e atribuir o leilão 
		List<Leilao> encerrados = new ArrayList<>();
		
		when(daoFalso.encerrados()).thenReturn(encerrados);
		gerador.gera();
		
		ArgumentCaptor<Pagamento> argumentoPagamento = ArgumentCaptor.forClass(Pagamento.class);
		
		// verificar se o número de leilões encerrados foi igual a 0 
		assertEquals(0, encerrados.size());
		
		// verifica se o método 'salva' de repositório de pagamentos não foi executado
		verify(repositorioDePagamentos, times(0)).salva(argumentoPagamento.capture());;

	}
	
	@DisplayName("Teste Gera Pagamento de um leilão")
	@Test
	public void geraPagamentoDeUmLeilao() {
		// mockar o relógio
		Relogio relogioFalso = mock(Relogio.class);
		
		// criar um novo leilão
		Leilao leilao = new Leilao("Quadro");
		
		// pegar e atribuir a data inicial
		Calendar dataIni = Calendar.getInstance();
		dataIni.set(2020, 5, 1);
		
		leilao.setData(dataIni);
		
		// criar usuários
		
		Usuario user1 = new Usuario("Gabriel");
		Usuario user2 = new Usuario("Maria");
		
		// criar lances
		Lance lance1 = new Lance(user1, 70000);
		Lance lance2 = new Lance(user2, 75000);
		
		// adicionar lances
		List<Lance> lances = new ArrayList<>();
		lances.add(lance1);
		lances.add(lance2);
		
		// atribuir a lista de lances ao leilão
		leilao.setLances(lances);
		
		// criar lista de leilões encerrados e atribuir o leilão 
		List<Leilao> encerrados = new ArrayList<>();
		encerrados.add(leilao);
		
		when(daoFalso.encerrados()).thenReturn(encerrados);

		gerador.gera();
		
		ArgumentCaptor<Pagamento> argumentoPagamento = ArgumentCaptor.forClass(Pagamento.class);
		
		// verificar se o número de leilões encerrados foi igual a 1 
		assertEquals(1, encerrados.size());
		
		/*
		 * Por serem dois lances de valores diferentes, o lance a ser 
		 * considerado deve ser o maior, no caso -> 75000
		 */
		
		// verificar se salvou o pagamento
		verify(repositorioDePagamentos).salva(argumentoPagamento.capture());
		
		// verificar se foi o maior valor
		Pagamento valorDoPagamento = argumentoPagamento.getValue();
		assertEquals(75000, valorDoPagamento.getValor());
		
		// verificar se os lances não foram iguais 
		assertNotEquals(avaliador.getMenorLance(), valorDoPagamento.getValor());
		
		// verificar se o método salva() de repositório de pagamentos foi executado uma vez
		verify(repositorioDePagamentos, times(1)).salva(valorDoPagamento);
		
	}
	
	@DisplayName("Teste Gera Pagamento de um leilão Sem Lances")
	@Test
	public void geraPagamentoDeUmLeilaoSemLances() {
		// mockar o relógio
		Relogio relogioFalso = mock(Relogio.class);
		
		// criar um novo leilão
		Leilao leilao = new Leilao("Quadro");
		
		// pegar e atribuir a data inicial
		Calendar dataIni = Calendar.getInstance();
		dataIni.set(2020, 5, 1);
		
		leilao.setData(dataIni);
		
		// adicionar lances
		List<Lance> lances = new ArrayList<>();
		
		// atribuir a lista de lances ao leilão
		leilao.setLances(lances);
		
		when(daoFalso.encerrados()).thenReturn(Arrays.asList(leilao));

		gerador.gera();
		
		ArgumentCaptor<Pagamento> argumentoPagamento = ArgumentCaptor.forClass(Pagamento.class);
		verify(repositorioDePagamentos).salva(argumentoPagamento.capture());
		Pagamento valorDoPagamento = argumentoPagamento.getValue();
		assertEquals(Double.NEGATIVE_INFINITY, avaliador.getMaiorLance());
		assertEquals(Double.POSITIVE_INFINITY, avaliador.getMenorLance());
		
		assertEquals(Double.NEGATIVE_INFINITY, valorDoPagamento.getValor());
		
	}
	
	@DisplayName("Teste Gera Pagamento de dois leilões")
	@Test
	public void geraPagamentoComDoisLeilões() {
		// mockar o relógio
		Relogio relogioFalso = mock(Relogio.class);
		
		// criar um novo leilão
		Leilao leilao1 = new Leilao("Quadro");
		Leilao leilao2 = new Leilao("Carro");
		
		// pegar e atribuir a data inicial
		Calendar dataIni = Calendar.getInstance();
		dataIni.set(2020, 5, 1);
		
		leilao1.setData(dataIni);
		leilao2.setData(dataIni);
		
		
		// criar usuários
		
		Usuario user1 = new Usuario("Gabriel");
		Usuario user2 = new Usuario("Maria");
		
		// criar lances
		Lance lance1 = new Lance(user1, 70000);
		Lance lance2 = new Lance(user2, 75000);
		
		Lance lance3 = new Lance(user1, 90000);
		Lance lance4 = new Lance(user2, 80000);
		
		// adicionar lances
		List<Lance> lances1 = new ArrayList<>();
		List<Lance> lances2 = new ArrayList<>();
		lances1.add(lance1);
		lances1.add(lance2);
		lances2.add(lance3);
		lances2.add(lance4);
		
		// atribuir a lista de lances ao leilão
		leilao1.setLances(lances1);
		leilao2.setLances(lances2);
		
		// criar lista de leilões encerrados e atribuir o leilão 
		List<Leilao> encerrados = new ArrayList<>();
		encerrados.add(leilao1);
		encerrados.add(leilao2);
		
		when(daoFalso.encerrados()).thenReturn(encerrados);

		gerador.gera();
		
		ArgumentCaptor<Pagamento> argumentoPagamento = ArgumentCaptor.forClass(Pagamento.class);
		
		// verificar se o número de leilões encerrados foi igual a 1 
		assertEquals(2, encerrados.size());
		
		/*
		 * Por serem dois lances de valores diferentes, o lance a ser 
		 * considerado deve ser o maior, no caso -> 75000
		 */
		
		// verificar se salvou o pagamento
		verify(repositorioDePagamentos, times(2)).salva(argumentoPagamento.capture());
		
		// verificar se foi o maior valor
		Pagamento valorDoPagamento = argumentoPagamento.getValue();
		assertEquals(90000, valorDoPagamento.getValor());
		
		// verificar se os lances não foram iguais 
		assertNotEquals(avaliador.getMenorLance(), valorDoPagamento.getValor());
		
		// verificar se o método salva() de repositório de pagamentos foi executado uma vez
		verify(repositorioDePagamentos, times(1)).salva(valorDoPagamento);
		
	} 
	
	// teste com leilão sendo encerrado em uma segunda 
	@DisplayName("Teste Gera Pagamento de Leilão encerrado na segunda")
	@Test
	public void geraPagamentoDeUmLeilaoEncerradoNaSegunda() {
		Relogio relogioFalso = mock(Relogio.class);
		
		Calendar dataHoje = Calendar.getInstance();
		dataHoje.set(2022, 5, 13);
		
		when(relogioFalso.hoje()).thenReturn(dataHoje);
		
		Leilao leilao1 = new Leilao("Playstation 5");
		
		Usuario user1 = new Usuario("Gabriel");
		Usuario user2 = new Usuario("Maria");
		
		// criar lances
		Lance lance1 = new Lance(user1, 70000);
		Lance lance2 = new Lance(user2, 75000);
		
		// adicionar lances
		List<Lance> lances1 = new ArrayList<>();
		lances1.add(lance1);
		lances1.add(lance2);
		
		// atribuir a lista de lances ao leilão
		leilao1.setLances(lances1);
		
		Calendar dataIni = Calendar.getInstance();
		dataIni.set(2022, 5, 6);
		leilao1.setData(dataIni);
		
		when(daoFalso.encerrados()).thenReturn(Arrays.asList(leilao1));
		
		gerador.gera();
		
		ArgumentCaptor<Pagamento> argumentoPagamento = ArgumentCaptor.forClass(Pagamento.class);
		verify(repositorioDePagamentos).salva(argumentoPagamento.capture());
		Pagamento pg = argumentoPagamento.getValue();

		assertEquals(Calendar.MONDAY, pg.getData().get(Calendar.DAY_OF_WEEK));
		assertEquals(13, pg.getData().get(Calendar.DAY_OF_MONTH));
	
	}
	
	
	// teste com leilão sendo encerrado em um sábado 
	@DisplayName("Teste Gera Pagamento de Leilão encerrado no sábado")
	@Test
	public void geraPagamentoDeUmLeilaoEncerradoNoSábado() {
		Relogio relogioFalso = mock(Relogio.class);
		
		Calendar dataHoje = Calendar.getInstance();
		dataHoje.set(2022, 5, 11);
		
		when(relogioFalso.hoje()).thenReturn(dataHoje);
		
		Leilao leilao1 = new Leilao("Playstation 5");
		
		Usuario user1 = new Usuario("Gabriel");
		Usuario user2 = new Usuario("Maria");
		
		// criar lances
		Lance lance1 = new Lance(user1, 70000);
		Lance lance2 = new Lance(user2, 75000);
		
		// adicionar lances
		List<Lance> lances1 = new ArrayList<>();
		lances1.add(lance1);
		lances1.add(lance2);
		
		// atribuir a lista de lances ao leilão
		leilao1.setLances(lances1);
		
		Calendar dataIni = Calendar.getInstance();
		dataIni.set(2022, 5, 4);
		leilao1.setData(dataIni);
		
		when(daoFalso.encerrados()).thenReturn(Arrays.asList(leilao1));
		
		gerador.gera();
		
		ArgumentCaptor<Pagamento> argumentoPagamento = ArgumentCaptor.forClass(Pagamento.class);
		verify(repositorioDePagamentos).salva(argumentoPagamento.capture());
		Pagamento pg = argumentoPagamento.getValue();

		assertEquals(Calendar.MONDAY, pg.getData().get(Calendar.DAY_OF_WEEK));
		assertEquals(13, pg.getData().get(Calendar.DAY_OF_MONTH));
	
	}
	
	// teste com leilão sendo encerrado em um domingo 
	@DisplayName("Teste Gera Pagamento de Leilão encerrado no domingo")
	@Test
	public void geraPagamentoDeUmLeilaoEncerradoNoDomingo() {
		Relogio relogioFalso = mock(Relogio.class);
		
		Calendar dataHoje = Calendar.getInstance();
		dataHoje.set(2022, 5, 12);
		
		when(relogioFalso.hoje()).thenReturn(dataHoje);
		
		Leilao leilao1 = new Leilao("Playstation 5");
		
		Usuario user1 = new Usuario("Gabriel");
		Usuario user2 = new Usuario("Maria");
		
		// criar lances
		Lance lance1 = new Lance(user1, 70000);
		Lance lance2 = new Lance(user2, 75000);
		
		// adicionar lances
		List<Lance> lances1 = new ArrayList<>();
		lances1.add(lance1);
		lances1.add(lance2);
		
		// atribuir a lista de lances ao leilão
		leilao1.setLances(lances1);
		
		Calendar dataIni = Calendar.getInstance();
		dataIni.set(2022, 5, 5);
		leilao1.setData(dataIni);
		
		when(daoFalso.encerrados()).thenReturn(Arrays.asList(leilao1));
		
		gerador.gera();
		
		ArgumentCaptor<Pagamento> argumentoPagamento = ArgumentCaptor.forClass(Pagamento.class);
		verify(repositorioDePagamentos).salva(argumentoPagamento.capture());
		Pagamento pg = argumentoPagamento.getValue();

		assertEquals(Calendar.MONDAY, pg.getData().get(Calendar.DAY_OF_WEEK));
		assertEquals(13, pg.getData().get(Calendar.DAY_OF_MONTH));
	
	}
	

}
