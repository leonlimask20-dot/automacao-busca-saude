package com.leonlima.automacao.servico;

import com.leonlima.automacao.dto.BuscaDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Testes unitários do ServicoAutomacao.
 *
 * Em testes unitários o browser nunca é aberto de verdade.
 * O WebDriver é substituído por um mock do Mockito, tornando
 * os testes rápidos e independentes de ambiente.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ServicoAutomacao — testes unitários")
class ServicoAutomacaoTest {

    @Mock
    private ApplicationContext contexto;

    @Mock
    private WebDriver navegadorMock;

    @InjectMocks
    private ServicoAutomacao servicoAutomacao;

    @BeforeEach
    void configurar() {
        ReflectionTestUtils.setField(servicoAutomacao, "tempoEsperaSegundos", 10);
    }

    @Test
    @DisplayName("Deve retornar status ERRO quando o navegador falha ao abrir a página")
    void buscar_quandoNavegadorFalha_retornaRespostaDeErro() {
        when(contexto.getBean(WebDriver.class)).thenReturn(navegadorMock);
        doThrow(new WebDriverException("Falha de conexão")).when(navegadorMock).get(anyString());

        BuscaDTO.Requisicao requisicao = new BuscaDTO.Requisicao();
        requisicao.setTermo("paracetamol");

        BuscaDTO.Resposta resposta = servicoAutomacao.buscar(requisicao);

        assertThat(resposta.getStatus()).isEqualTo("ERRO");
        assertThat(resposta.getTotalResultados()).isEqualTo(0);
        assertThat(resposta.getMensagemErro()).contains("navegador");

        // Garante que o navegador foi fechado mesmo com erro (bloco finally)
        verify(navegadorMock).quit();
    }

    @Test
    @DisplayName("Deve fechar o navegador mesmo quando ocorre exceção")
    void buscar_sempreEncerraNavegador() {
        when(contexto.getBean(WebDriver.class)).thenReturn(navegadorMock);
        doThrow(new WebDriverException("Erro qualquer")).when(navegadorMock).get(anyString());

        BuscaDTO.Requisicao requisicao = new BuscaDTO.Requisicao();
        requisicao.setTermo("dipirona");

        servicoAutomacao.buscar(requisicao);

        // O quit() deve ser chamado independente do resultado
        verify(navegadorMock, times(1)).quit();
    }

    @Test
    @DisplayName("Deve aplicar o sitioAlvo padrão quando não informado")
    void requisicao_sitioAltoPadrao() {
        BuscaDTO.Requisicao requisicao = new BuscaDTO.Requisicao();
        requisicao.setTermo("dipirona");

        assertThat(requisicao.getSitioAlvo()).isEqualTo("bula.fiocruz.br");
    }
}
