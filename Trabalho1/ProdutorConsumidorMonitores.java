package Trabalho1;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

public class ProdutorConsumidorMonitores {
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

    private static final Lock lock = new ReentrantLock();
    private static final Condition espacosDisponiveis = lock.newCondition();
    private static final Condition itensDisponiveis = lock.newCondition();

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== PRODUTOR-CONSUMIDOR COM MONITORES ===");
        System.out.println("Tamanho do buffer: " + TAM_BUFFER);
        System.out.println("Total de operacoes: " + TOTAL_OPERACOES);
        System.out.println("==========================================");

        for (int i = 0; i < TAM_BUFFER; i++) buffer[i] = -1;

        Thread[] produtores = new Thread[4];
        Thread[] consumidores = new Thread[4];

        for (int i = 0; i < produtores.length; i++) {
            final int id = i + 1;
            int delay = (int) (Math.random() * 1000);
            produtores[i] = new Thread(() -> {
                try {
                    Thread.sleep(delay);
                    System.out.println(PURPLE + "[SISTEMA] Produtor " + id + " iniciando apos " + delay + "ms" + RESET);
                    produtor(id);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            produtores[i].start();
        }

        for (int i = 0; i < consumidores.length; i++) {
            final int id = i + 1;
            int delay = (int) (Math.random() * 1500);
            consumidores[i] = new Thread(() -> {
                try {
                    Thread.sleep(delay);
                    System.out.println(PURPLE + "[SISTEMA] Consumidor " + id + " iniciando apos " + delay + "ms" + RESET);
                    consumidor(id);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            consumidores[i].start();
        }

        for (Thread t : produtores) t.join();

        // ### ALTERAÇÃO ###: sinaliza consumidores no final para que não fiquem presos
        executando.set(false);
        lock.lock();
        try {
            itensDisponiveis.signalAll();
        } finally {
            lock.unlock();
        }

        for (Thread t : consumidores) t.join();

        System.out.println("==========================================");
        System.out.println("FINALIZADO!");
        System.out.println("Total produzido: " + contadorProduzidos.get());
        System.out.println("Total consumido: " + contadorConsumidos.get());
    }

    private static void produtor(int id) {
        while (executando.get() && contadorProduzidos.get() < TOTAL_OPERACOES) {
            try {
                int item = (int) (Math.random() * 100);

                Thread.sleep((int) (Math.random() * 500));

                System.out.println(CYAN + "[PRODUTOR " + id + "] quer produzir: " + item + RESET);

                lock.lock();
                try {
                    logEstadoMonitor("ENTROU no monitor - Produtor " + id);

                    while (bufferCheio()) {
                        System.out.println(RED + "[PRODUTOR " + id + "] BUFFER CHEIO - aguardando espaco..." + RESET);
                        logEstadoMonitor("ANTES de await() - Produtor " + id);
                        espacosDisponiveis.await();
                        logEstadoMonitor("APOS await() - Produtor " + id);
                    }

                    buffer[in] = item;
                    int produzidos = contadorProduzidos.incrementAndGet();
                    System.out.println(GREEN + "[PRODUTOR " + id + "] produziu: " + item + " na posicao " + in +
                            " | Total: " + produzidos + RESET);
                    in = (in + 1) % TAM_BUFFER;

                    // ### ALTERAÇÃO ###: usar signalAll() para acordar todos consumidores
                    itensDisponiveis.signalAll();
                    logEstadoMonitor("APOS producao e signalAll() - Produtor " + id);

                } finally {
                    lock.unlock();
                }

                exibirBuffer();
                Thread.sleep((int) (Math.random() * 800) + 200);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        System.out.println(PURPLE + "[PRODUTOR " + id + "] Finalizou" + RESET);
    }

    private static void consumidor(int id) {
        while (executando.get() || contadorConsumidos.get() < TOTAL_OPERACOES) { // ### ALTERAÇÃO ###
            try {
                System.out.println(YELLOW + "[CONSUMIDOR " + id + "] quer consumir..." + RESET);

                lock.lock();
                try {
                    logEstadoMonitor("ENTROU no monitor - Consumidor " + id);

                    while (bufferVazio()) {
                        System.out.println(RED + "[CONSUMIDOR " + id + "] BUFFER VAZIO - aguardando itens..." + RESET);
                        logEstadoMonitor("ANTES de await() - Consumidor " + id);
                        itensDisponiveis.await();
                        logEstadoMonitor("APOS await() - Consumidor " + id);
                        if (!executando.get() && bufferVazio()) break; // ### ALTERAÇÃO ###
                    }

                    if (bufferVazio()) break; // ### ALTERAÇÃO ###

                    int item = buffer[out];
                    buffer[out] = -1;
                    int consumidos = contadorConsumidos.incrementAndGet();
                    System.out.println(GREEN + "[CONSUMIDOR " + id + "] consumiu: " + item + " da posicao " + out +
                            " | Total: " + consumidos + RESET);
                    out = (out + 1) % TAM_BUFFER;

                    // ### ALTERAÇÃO ###: usar signalAll() para acordar todos produtores
                    espacosDisponiveis.signalAll();
                    logEstadoMonitor("APOS consumo e signalAll() - Consumidor " + id);

                } finally {
                    lock.unlock();
                }

                exibirBuffer();
                Thread.sleep((int) (Math.random() * 800) + 200);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        System.out.println(PURPLE + "[CONSUMIDOR " + id + "] Finalizou" + RESET);
    }

    private static boolean bufferCheio() {
        return contadorProduzidos.get() - contadorConsumidos.get() >= TAM_BUFFER;
    }

    private static boolean bufferVazio() {
        return contadorProduzidos.get() - contadorConsumidos.get() <= 0;
    }

    private static void logEstadoMonitor(String contexto) {
        int itensNoBuffer = contadorProduzidos.get() - contadorConsumidos.get();
        // ### ALTERAÇÃO ###: removido tryLock() para evitar deadlock
        System.out.println("[MONITOR] " + contexto + " | Itens no buffer: " + itensNoBuffer);
    }

    private static void exibirBuffer() {
        lock.lock();
        try {
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
            System.out.println(sb.toString());
        } finally {
            lock.unlock();
        }
    }
}
