package br.com.poc.batch;

import java.nio.file.Files;
import java.nio.file.Path;
import br.com.poc.batch.domain.Produto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.core.io.FileSystemResource;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.batch.job.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:integration;DB_CLOSE_DELAY=-1"
})
@ExtendWith(OutputCaptureExtension.class)
// Apenas beans reais: dispensa listeners de mocks e anexação de agentes à JVM.
@TestExecutionListeners(DependencyInjectionTestExecutionListener.class)
class ProdutoJobIntegrationTest {
    @Autowired JobLauncher jobLauncher;
    @Autowired Job reajusteProdutoJob;
    @Autowired FlatFileItemWriter<Produto> produtoWriter;
    @TempDir Path diretorio;

    @Test
    void executaFluxoCompletoDoCsvAoArquivoFinal(CapturedOutput logs) throws Exception {
        // Usa o Writer real, isolando apenas o destino do arquivo no teste.
        Path saida = diretorio.resolve("produtos-reajustados.csv");
        produtoWriter.setResource(new FileSystemResource(saida));

        var execution = jobLauncher.run(reajusteProdutoJob,
                new JobParametersBuilder().addString("caso", "integracao").toJobParameters());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(execution.getStepExecutions()).singleElement().satisfies(step -> {
            assertThat(step.getReadCount()).isEqualTo(3);
            assertThat(step.getWriteCount()).isEqualTo(3);
            assertThat(step.getCommitCount()).isEqualTo(2);
            assertThat(step.getRollbackCount()).isZero();
        });
        assertThat(Files.readAllLines(saida)).containsExactly(
                "1,Teclado,110.00", "2,Mouse,55.00", "3,Monitor,1100.00");
        assertThat(logs.getOut())
                .contains("[JOB][INICIO][nome=reajusteProdutoJob]")
                .contains("[CHUNK][INICIO][numero=1]")
                .contains("[READER][ITEM_LIDO][id=1][nome=Teclado][preco=100.00]")
                .contains("[PROCESSOR][CALCULO][id=1][nome=Teclado][precoOriginal=100.00]")
                .contains("[WRITER][ITEM_ESCRITO][id=1][nome=Teclado][preco=110.00]")
                .contains("[STEP][RESUMO][nome=reajusteProdutoStep]")
                .contains("[JOB][RESUMO][nome=reajusteProdutoJob]");
    }
}
