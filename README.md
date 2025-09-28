# Sistemas-Concorrentes-Distribuidos

## 1. Problema dos Produtores-Consumidores

O problema dos produtores-consumidores é um problema de **sincronização**. Ele demonstra como vários processos ou threads podem compartilhar recursos de forma segura sem conflitos.

Nesse problema temos os **Produtores**, que geram dados e os colocam no buffer compartilhado, e os **Consumidores**, que pegam o dado do buffer e o processam.

O desafio é garantir:

1. Um `Produtor` não pode adicionar um item a um buffer cheio
2. Um `Consumidor` não pode consumir um item de um buffer vazio
3. Múltiplos `Produtores` e `Consumidores` não podem acessar o buffer simultaneamente (para evitar condição de corrida)

### 1.1 Usando Semáforos

Um `semáforo` é um mecanismo de sinalização baseado em um número inteiro para coordenar o acesso a recursos compartilhados. Ele contempla duas operações atômicas:

- `wait(S)`: Decrementa o valor do `semáforo` em 1. Se o valor for menor ou igual a 0, o processo fica **EM ESPERA**. Semáforo *fechado*.
- `signal(S)`: Incrementa o valor do `semáforo` em 1, permitindo a entrada de um processo
em espera. É como se o semáforo abrisse.

O código em `ProdutorConsumidorSemaforos.java` resolve esse problema. Ele foi feito com base [neste exemplo Java](https://www.geeksforgeeks.org/operating-systems/producer-consumer-problem-using-semaphores-set-1/), adaptado para múltiplas threads. A documentação desse problema é o pdf na pasta `Trabalho1`.

### 1.2 Usando Monitores

Um `monitor` é uma estrutura de alto nível que encapsula dados e procedimentos, garantindo que apenas uma thread execute dentro do monitor por vez. Ele combina:

- **Lock**: controle de exclusão mútua
- **Variáveis de condição**: filas de espera para condições específicas
- **Operações**:
  - `await()`: libera o lock e coloca a thread em espera
  - `signal()`: acorda uma thread em espera
  - `signalAll()`: acorda todas as threads em espera

O código em `ProdutorConsumidorMonitores.java` implementa a solução usando:

- `lock`: controle de acesso ao monitor
- `espacosDisponiveis`: condição para produtores esperarem por espaço
- `itensDisponiveis`: condição para consumidores esperarem por itens

Um padrão fundamental em monitores é sempre usar `while` para verificar condições, nunca `if`, para garantir re-verificação após ser acordado.

O código foi feito com base no código dos semáforos, mas adaptando para a lógica dos monitores.
