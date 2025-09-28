package Trabalho1;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class ProdutorConsumidorSemaforos {
    public static final String RESET = "\u001B[0m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String CYAN = "\u001B[36m";
    public static final String PURPLE = "\u001B[35m";

    private static final int TAM_BUFFER = 5;
    private static final int[] buffer = new int[TAM_BUFFER];
    private static int in = 0, out = 0;
    
    private static final AtomicInteger contadorProduzidos = new AtomicInteger(0);
    private static final AtomicInteger contadorConsumidos = new AtomicInteger(0);
    private static final AtomicBoolean executando = new AtomicBoolean(true);
    private static final int TOTAL_OPERACOES = 20;

    // Semáforos - ORDEM CORRETA: mutex primeiro
    private static final Semaphore mutex = new Semaphore(1);
    private static final Semaphore espacos = new Semaphore(TAM_BUFFER);
    private static final Semaphore itens = new Semaphore(0);

    // Lock para sincronizar impressões
    private static final ReentrantLock lockImpressao = new ReentrantLock();

    public static void main(String[] args) throws InterruptedException {
        imprimir("=== PRODUTOR-CONSUMIDOR COM SEMAFOROS CORRIGIDO ===");
        imprimir("Tamanho do buffer: " + TAM_BUFFER);
        imprimir("Total de operacoes: " + TOTAL_OPERACOES);
        imprimir("=================================================");

        for (int i = 0; i < TAM_BUFFER; i++) buffer[i] = -1;

        Thread[] produtores = new Thread[4];
        Thread[] consumidores = new Thread[4];

        // Produtores
        for (int i = 0; i < produtores.length; i++) {
            final int id = i + 1;
            int delay = (int) (Math.random() * 1000);
            produtores[i] = new Thread(() -> {
                try {
                    Thread.sleep(delay);
                    imprimir(PURPLE + "[SISTEMA] Produtor " + id + " iniciando apos " + delay + "ms" + RESET);
                    produtor(id);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            produtores[i].start();
        }

        // Consumidores
        for (int i = 0; i < consumidores.length; i++) {
            final int id = i + 1;
            int delay = (int) (Math.random() * 1500);
            consumidores[i] = new Thread(() -> {
                try {
                    Thread.sleep(delay);
                    imprimir(PURPLE + "[SISTEMA] Consumidor " + id + " iniciando apos " + delay + "ms" + RESET);
                    consumidor(id);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            consumidores[i].start();
        }

        for (Thread t : produtores) t.join();
        for (Thread t : consumidores) t.join();

        imprimir("==================================================");
        imprimir("FINALIZADO!");
        imprimir("Total produzido: " + contadorProduzidos.get());
        imprimir("Total consumido: " + contadorConsumidos.get());
    }

    private static void produtor(int id) {
        while (executando.get() && contadorProduzidos.get() < TOTAL_OPERACOES) {
            try {
                int item = (int) (Math.random() * 100);
                
                // Simula tempo de producao ANTES de acessar regiao critica
                Thread.sleep((int) (Math.random() * 500));

                imprimir(CYAN + "[PRODUTOR " + id + "] quer requisitar espaco para produzir: " + item + RESET);
                logEstadoSemaforos("ANTES de requisitar espacos - Produtor " + id);
                
                if (!espacos.tryAcquire(500, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                    imprimir(RED + "[PRODUTOR " + id + "] aguardando espaco" + RESET);
                    continue; // vai aparecer msg de que ele está bloquado se demorar muito pra ele conseguir entrar na região crítica
                }
                
                logEstadoSemaforos("APOS adquirir espacos - Produtor " + id);
                mutex.acquire();
                logEstadoSemaforos("DENTRO da regiao critica - Produtor " + id);
                
                buffer[in] = item;
                int produzidos = contadorProduzidos.incrementAndGet();
                imprimir(GREEN + "[PRODUTOR " + id + "] produziu: " + item + " na posicao " + in +
                        " | Total produzidos: " + produzidos + RESET);
                in = (in + 1) % TAM_BUFFER;
                
                mutex.release();
                itens.release();
                logEstadoSemaforos("APOS producao - Produtor " + id);
                
                exibirBuffer();
                
                Thread.sleep((int) (Math.random() * 800) + 200);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        imprimir("[PRODUTOR " + id + "] Finalizou");
    }

    private static void consumidor(int id) {
        while (executando.get() && contadorConsumidos.get() < TOTAL_OPERACOES) {
            try {
                imprimir(YELLOW + "[CONSUMIDOR " + id + "] quer requisitar item..." + RESET);
                logEstadoSemaforos("ANTES de requisitar itens - Consumidor " + id);
                
                if (!itens.tryAcquire(500, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                    imprimir(RED + "[CONSUMIDOR " + id + "] aguardando item" + RESET);
                    continue;
                }
                
                logEstadoSemaforos("APOS adquirir itens - Consumidor " + id);
                mutex.acquire();
                logEstadoSemaforos("DENTRO da regiao critica - Consumidor " + id);
                
                int item = buffer[out];
                buffer[out] = -1;
                int consumidos = contadorConsumidos.incrementAndGet();
                imprimir(GREEN + "[CONSUMIDOR " + id + "] consumiu: " + item + " da posicao " + out +
                        " | Total consumidos: " + consumidos + RESET);
                out = (out + 1) % TAM_BUFFER;
                
                mutex.release();
                espacos.release();
                logEstadoSemaforos("APOS consumo - Consumidor " + id);
                
                exibirBuffer();
                
                Thread.sleep((int) (Math.random() * 800) + 200);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        imprimir("[CONSUMIDOR " + id + "] Finalizou");
    }


    // ✅ MÉTODO PARA LOGAR OS SEMÁFOROS (como no seu código original)
    private static void logEstadoSemaforos(String contexto) {
        imprimir("[SEMAFOROS] " + contexto +
                " | espacos: " + espacos.availablePermits() +
                " | itens: " + itens.availablePermits() +
                " | mutex: " + mutex.availablePermits());
    }

    private static void exibirBuffer() {
        try {
            mutex.acquire();
            StringBuilder sb = new StringBuilder("BUFFER: [");
            for (int i = 0; i < TAM_BUFFER; i++) {
                if (buffer[i] == -1) {
                    sb.append("__");
                } else {
                    sb.append(String.format("%02d", buffer[i]));
                }
                
                if (i < TAM_BUFFER - 1) sb.append(" ");
            }
            sb.append("]");
            imprimir(sb.toString());
            mutex.release();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void imprimir(String mensagem) {
        lockImpressao.lock();
        try {
            System.out.println(mensagem);
        } finally {
            lockImpressao.unlock();
        }
    }
}