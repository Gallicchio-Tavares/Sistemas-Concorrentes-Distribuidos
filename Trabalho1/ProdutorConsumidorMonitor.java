package Trabalho1;
public class ProdutorConsumidorMonitor {
    public static final String RESET = "\u001B[0m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String CYAN = "\u001B[36m";
    public static final String PURPLE = "\u001B[35m";

    // Buffer compartilhado
    static class Monitor {
        private final int[] buffer;
        private int in = 0, out = 0, count = 0;

        public Monitor(int tamanho) {
            buffer = new int[tamanho];
            for (int i = 0; i < buffer.length; i++) buffer[i] = -1;
        }

        public synchronized void produzir(int item, int id) throws InterruptedException {
            while (count == buffer.length) {
                System.out.println(RED + "[PRODUTOR " + id + "] BUFFER CHEIO - aguardando..." + RESET);
                wait();
            }

            buffer[in] = item;
            in = (in + 1) % buffer.length;
            count++;
            System.out.println(CYAN + "[PRODUTOR " + id + "] produziu " + item + RESET);
            exibirBuffer();

            notifyAll(); // acorda consumidores
        }

        public synchronized int consumir(int id) throws InterruptedException {
            while (count == 0) {
                System.out.println(RED + "[CONSUMIDOR " + id + "] BUFFER VAZIO - aguardando..." + RESET);
                wait();
            }

            int item = buffer[out];
            buffer[out] = -1;
            out = (out + 1) % buffer.length;
            count--;
            System.out.println(YELLOW + "[CONSUMIDOR " + id + "] consumiu " + item + RESET);
            exibirBuffer();

            notifyAll(); // acorda produtores
            return item;
        }

        private void exibirBuffer() {
            StringBuilder sb = new StringBuilder("BUFFER: [");
            for (int i = 0; i < buffer.length; i++) {
                if (buffer[i] == -1) sb.append("__");
                else sb.append(String.format("%02d", buffer[i]));
                if (i < buffer.length - 1) sb.append(" ");
            }
            sb.append("]");
            System.out.println(sb.toString());
        }
    }

    public static void main(String[] args) {
        final int TAM_BUFFER = 5;
        final int TOTAL_OPERACOES = 10;

        Monitor buffer = new Monitor(TAM_BUFFER);

        // Produtores
        for (int i = 1; i <= 2; i++) {
            final int id = i;
            new Thread(() -> {
                try {
                    for (int j = 0; j < TOTAL_OPERACOES; j++) {
                        int item = (int) (Math.random() * 100);
                        Thread.sleep((int) (Math.random() * 500));
                        buffer.produzir(item, id);
                    }
                    System.out.println(PURPLE + "[PRODUTOR " + id + "] finalizou." + RESET);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }

        // Consumidores
        for (int i = 1; i <= 2; i++) {
            final int id = i;
            new Thread(() -> {
                try {
                    for (int j = 0; j < TOTAL_OPERACOES; j++) {
                        Thread.sleep((int) (Math.random() * 800));
                        buffer.consumir(id);
                    }
                    System.out.println(PURPLE + "[CONSUMIDOR " + id + "] finalizou." + RESET);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }
}
