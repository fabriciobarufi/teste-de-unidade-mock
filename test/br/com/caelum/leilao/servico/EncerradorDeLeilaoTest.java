package br.com.caelum.leilao.servico;

import static org.junit.Assert.assertEquals;



import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Matchers.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

import org.junit.Test;
import org.mockito.InOrder;

import br.com.caelum.leilao.builder.CriadorDeLeilao;
import br.com.caelum.leilao.dominio.Leilao;
import br.com.caelum.leilao.infra.dao.RepositorioDeLeiloes;

public class EncerradorDeLeilaoTest {
	
	@Test
	public void deveEncerrarLeiloesQueComecaramUmaSemanaAtras() {
		
		Calendar antiga = Calendar.getInstance();
		antiga.set(1999, 1, 20);
		
		Leilao leilaoTvPlasma = new CriadorDeLeilao().para("TV de Plasma")
				.naData(antiga)
				.constroi();
		
		Leilao leilaoGeladeira = new CriadorDeLeilao().para("Geladeira")
				.naData(antiga)
				.constroi();
		
		//Criamos o mock
		RepositorioDeLeiloes daoFalso = mock(RepositorioDeLeiloes.class);
		
		//ensinamos ele a retornar a lista de leiloes antigos
	    when(daoFalso.correntes()).thenReturn(Arrays.asList(leilaoTvPlasma,leilaoGeladeira));
	    
	    EnviadorDeEmail carteiroFalso = mock(EnviadorDeEmail.class);
		
		EncerradorDeLeilao encerrador = new EncerradorDeLeilao(daoFalso, carteiroFalso);
		encerrador.encerra();
		
		assertEquals(2, encerrador.getTotalEncerrados());
		assertTrue(leilaoTvPlasma.isEncerrado());
		assertTrue(leilaoGeladeira.isEncerrado());
	}
	
	@Test
	public void naoDeveEncerrarLeiloesQueComecaramOntem() {
		
		Calendar ontem = Calendar.getInstance();
		ontem.add(Calendar.DAY_OF_MONTH, -1);
		
		Leilao leilaoTvPlasma = new CriadorDeLeilao().para("TV de Plasma")
				.naData(ontem)
				.constroi();
		
		Leilao leilaoGeladeira = new CriadorDeLeilao().para("Geladeira")
				.naData(ontem)
				.constroi();
		
		RepositorioDeLeiloes daoFalso = mock(RepositorioDeLeiloes.class);
		when(daoFalso.correntes()).thenReturn(Arrays.asList(leilaoTvPlasma,leilaoGeladeira));
		
		EnviadorDeEmail carteiroFalso = mock(EnviadorDeEmail.class);
		EncerradorDeLeilao encerrador = new EncerradorDeLeilao(daoFalso, carteiroFalso);
		
		assertEquals(0, encerrador.getTotalEncerrados());
		assertFalse(leilaoTvPlasma.isEncerrado());
		assertFalse(leilaoGeladeira.isEncerrado());
		
		verify(daoFalso, never()).atualiza(leilaoGeladeira);
		verify(daoFalso, never()).atualiza(leilaoTvPlasma);
		
	}
	
	@Test
	public void naoDeveEncerrarLeiloesSeListaDeLeiloesEstiverVazia() {
		RepositorioDeLeiloes daoFalso = mock(RepositorioDeLeiloes.class);
		when(daoFalso.correntes()).thenReturn(new ArrayList<Leilao>());
		
		EnviadorDeEmail carteiroFalso = mock(EnviadorDeEmail.class);
		EncerradorDeLeilao encerrador = new EncerradorDeLeilao(daoFalso, carteiroFalso);
		encerrador.encerra();
		
		assertEquals(0, encerrador.getTotalEncerrados());
	}
	
	@Test
	public void deveAtualizarLeiloesEncerrados() {
		
		Calendar antiga = Calendar.getInstance();
		antiga.set(1999, 1, 20);
		
		Leilao leilao = new CriadorDeLeilao().naData(antiga).para("TV de Plasma").constroi();
		
		RepositorioDeLeiloes daoFalso = mock(RepositorioDeLeiloes.class);
		when(daoFalso.correntes()).thenReturn(Arrays.asList(leilao));
		
		EnviadorDeEmail carteiroFalso = mock(EnviadorDeEmail.class);
		EncerradorDeLeilao encerrador = new EncerradorDeLeilao(daoFalso, carteiroFalso);
		encerrador.encerra();
		
		// verificando que o metodo atualiza foi realmente invocado!
		verify(daoFalso, times(1)).atualiza(leilao);
	}
	
	@Test
	public void execucaoMetodosEmOrdem() {
		
		Calendar antiga = Calendar.getInstance();
		antiga.set(1999, 1, 20);
		
		Leilao leilao = new CriadorDeLeilao().naData(antiga).para("TV de Plasma").constroi();
		
		RepositorioDeLeiloes daoFalso = mock(RepositorioDeLeiloes.class);
		when(daoFalso.correntes()).thenReturn(Arrays.asList(leilao));
		
		EnviadorDeEmail carteiroFalso = mock(EnviadorDeEmail.class);
		
		EncerradorDeLeilao encerrador = new EncerradorDeLeilao(daoFalso, carteiroFalso);
		encerrador.encerra();
		
		// Testar se os métodos do mock foram executados na ordem esperada
		// daoFalso e depois carteiroFalso
		
		// passamos os mocks que serao verificados
		InOrder inOrder = inOrder(daoFalso,carteiroFalso);
		// primeira invocação
		inOrder.verify(daoFalso, times(1)).atualiza(leilao);
		// segunda invocação
		inOrder.verify(carteiroFalso, times(1)).envia(leilao);
		
	}
	
	@Test
	public void deveContinuarAExecucaoMesmoQuandoDaoFalha() {
		
		Calendar antiga  = Calendar.getInstance();
		antiga.set(1999, 1, 20);
		
		Leilao leilao1 = new CriadorDeLeilao().naData(antiga).para("TV de Plasma").constroi();
		Leilao leilao2 = new CriadorDeLeilao().naData(antiga).para("Geladeira").constroi();
		
		RepositorioDeLeiloes daoFalso = mock(RepositorioDeLeiloes.class);
		when(daoFalso.correntes()).thenReturn(Arrays.asList(leilao1, leilao2));
		
		doThrow(new RuntimeException()).when(daoFalso).atualiza(leilao1);
		
		EnviadorDeEmail carteiroFalso = mock(EnviadorDeEmail.class);
		
		EncerradorDeLeilao encerrador = new EncerradorDeLeilao(daoFalso, carteiroFalso);
		encerrador.encerra();
		
		verify(daoFalso).atualiza(leilao2);
		verify(carteiroFalso).envia(leilao2);
		
	}
	
	@Test
	public void deveContinuarAExecucaoMesmoQuandoEnviadorDeEmailFalha() {
		
		Calendar antiga = Calendar.getInstance();
		antiga.set(199, 1, 20);
		
		Leilao leilao1 = new CriadorDeLeilao().naData(antiga).para("TV de Plasma").constroi();
		Leilao leilao2 = new CriadorDeLeilao().naData(antiga).para("Geladeira").constroi();
		
		RepositorioDeLeiloes daoFalso = mock(RepositorioDeLeiloes.class);
		when(daoFalso.correntes()).thenReturn(Arrays.asList(leilao1, leilao2));
		
		EnviadorDeEmail carteiroFalso = mock(EnviadorDeEmail.class);
		doThrow(new RuntimeException()).when(carteiroFalso).envia(leilao1);;
		
		EncerradorDeLeilao encerrador = new EncerradorDeLeilao(daoFalso, carteiroFalso);
		encerrador.encerra();
		
		verify(daoFalso).atualiza(leilao2);
		verify(carteiroFalso).envia(leilao2);		
		
	}
	
	@Test
	public void naoDeveExecutarEnviadorDeEmailSeTodosDaosLancamExcecoes() {
		
		Calendar antiga = Calendar.getInstance();
		antiga.set(199, 1, 20);
		
		Leilao leilao1 = new CriadorDeLeilao().naData(antiga).para("TV de Plasma").constroi();
		Leilao leilao2 = new CriadorDeLeilao().naData(antiga).para("Geladeira").constroi();
		
		RepositorioDeLeiloes daoFalso = mock(RepositorioDeLeiloes.class);
		when(daoFalso.correntes()).thenReturn(Arrays.asList(leilao1, leilao2));
		
		doThrow(new RuntimeException()).when(daoFalso).atualiza(any(Leilao.class));

		EnviadorDeEmail carteiroFalso = mock(EnviadorDeEmail.class);
		
		EncerradorDeLeilao encerrador = new EncerradorDeLeilao(daoFalso, carteiroFalso);
		encerrador.encerra();
		
		verify(carteiroFalso, never()).envia(any(Leilao.class));
		
	}
	
}
