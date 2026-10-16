package br.com.poc.batch;

import java.nio.file.Files;
import java.nio.file.Path;
import br.com.poc.batch.domain.Produto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.batch.job.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:integration;DB_CLOSE_DELAY=-1"
})
// Apenas beans reais: dispensa listeners de mocks e anexação de agentes à JVM.
@TestExecutionListeners(DependencyInjectionTestExecutionListener.class)
class ProdutoJobIntegrationTest {
    @Autowired JobLauncher jobLauncher;
    @Autowired Job reajusteProdutoJob;
    @Autowired FlatFileItemWriter<Produto> produtoWriter;
    @TempDir Path diretorio;

    @Test
    void executaFluxoCompletoDoCsvAoArquivoFinal() throws Exception {
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
    }
}
