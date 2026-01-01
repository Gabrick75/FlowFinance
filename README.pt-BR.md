# FlowFinance

FlowFinance é um aplicativo moderno de gestão financeira pessoal para Android, desenvolvido para oferecer uma experiência de controle orçamentário intuitiva e poderosa. Ele utiliza as bibliotecas Jetpack mais recentes e práticas modernas de desenvolvimento Android, incluindo uma base de código 100% em Kotlin e Jetpack Compose.

### Captura de Tela Principal
<p align="center">
  <img src="art/main_screenshot.png" alt="Captura de Tela do App" width="300"/>
</p>

### Mais Capturas de Tela
<table>
  <tr>
    <td><img src="art/screenshot1.png" alt="Screenshot 1"></td>
    <td><img src="art/screenshot2.png" alt="Screenshot 2"></td>
  </tr>
  <tr>
    <td><img src="art/screenshot3.png" alt="Screenshot 3"></td>
    <td><img src="art/screenshot4.png" alt="Screenshot 4"></td>
  </tr>
</table>



## Sobre o Projeto

Este aplicativo foi projetado para oferecer um monitoramento detalhado de receitas e despesas, permitindo que o usuário registre cada movimentação financeira com atribuição de valores e categorias específicas. Através de um sistema de persistência de dados robusto, o app organiza todas as informações cronologicamente e gera resumos estatísticos automáticos, ajudando o usuário a compreender seu comportamento de consumo ao longo do mês.

A operação do sistema baseia-se na reatividade entre o banco de dados e a interface do usuário. Isso significa que qualquer alteração nos registros financeiros é refletida instantaneamente nos gráficos de distribuição e nos totais acumulados, sem a necessidade de intervenção manual para atualização da tela.

## Primeiros Passos: Um Rápido Tutorial

1.  **Adicione sua Primeira Transação**: Toque no botão `+` na tela Inicial ou no Histórico.
2.  **Preencha os Detalhes**: Insira o valor, uma descrição, escolha uma categoria e selecione a data da transação.
3.  **Explore Suas Finanças**: Veja seu saldo ser atualizado instantaneamente no Dashboard e consulte seu histórico completo de gastos na aba "Histórico".
4.  **Personalize sua Experiência**: Vá para a aba "Configurações" para alterar seu nome de usuário, moeda preferida e ativar o modo escuro.

## Principais Funcionalidades

-   **Dashboard Reativo**: Visualize instantaneamente seu saldo total, receitas e despesas do mês.
-   **Gráfico de Despesas**: Um `PieChart` customizado no dashboard exibe a distribuição de gastos por categoria.
-   **UI em Tempo Real**: A interface é totalmente reativa e se atualiza automaticamente com qualquer mudança no banco de dados, graças ao Kotlin Flow.
-   **Histórico de Transações**: Navegue por seu histórico completo de transações, agrupado por data com cabeçalhos fixos (`sticky headers`) para facilitar a navegação.
-   **Filtros Mensais**: Alterne facilmente entre diferentes meses para visualizar seu histórico financeiro.
-   **Deslizar para Excluir**: Apague transações de forma fluida ao deslizá-las para o lado na lista de histórico.
-   **Planejamento de Orçamentos**: Defina e acompanhe metas de gastos por categoria com barras de progresso que mudam de cor conforme o limite se aproxima. *(Observação: Esta funcionalidade está em desenvolvimento e usa dados de exemplo.)*
-   **Gerenciamento Seguro de Dados**: Uma opção de "Limpar Dados" com um diálogo de confirmação para evitar perdas acidentais.
-   **Exportação de Dados**: Exporte todos os seus dados de transações para um arquivo CSV para compartilhar ou analisar em outro lugar.
-   **Personalização do Usuário**:
    -   Defina seu nome de usuário.
    -   Escolha sua moeda preferida (BRL, USD, EUR), que atualiza a formatação em todo o aplicativo.
    -   Alterne entre um tema Escuro e Claro, com a preferência salva.
-   **Backups Automáticos**: Um worker em segundo plano (WorkManager) realiza automaticamente um backup semanal do banco de dados.

## Stack Técnica e Arquitetura

Este projeto demonstra domínio das técnicas modernas de desenvolvimento Android e segue uma arquitetura limpa e escalável.

-   **Core**
    -   **Stack**: 100% [Kotlin](https://kotlinlang.org/)
    -   **UI**: [Jetpack Compose](https://developer.android.com/jetpack/compose) para uma UI totalmente declarativa.
    -   **Programação Assíncrona**: [Kotlin Coroutines & Flow](https://kotlinlang.org/docs/coroutines-guide.html) para gerenciar threads em segundo plano e lidar com fluxos de dados.

-   **Arquitetura**
    -   **Padrão**: MVVM (Model-View-ViewModel) para separar a UI da lógica de negócios.
    -   **Injeção de Dependência**: [Hilt](https://developer.android.com/training/dependency-injection/hilt-android) para gerenciar dependências e criar uma estrutura escalável.
    -   **Navegação**: [Jetpack Navigation para Compose](https://developer.android.com/jetpack/compose/navigation) para gerenciar toda a navegação no app.

-   **Dados**
    -   **Persistência**: [Room Database](https://developer.android.com/training/data-storage/room) para armazenamento local e estruturado de dados com queries reativas.
    -   **Preferências do Usuário**: [Jetpack DataStore](https://developer.android.com/topic/libraries/architecture/datastore) para salvar as configurações do usuário, como tema e moeda.
    -   **Tarefas em Segundo Plano**: [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) para agendar tarefas confiáveis, como backups semanais.

## Download

Você pode baixar a versão instalável mais recente do aplicativo na página de [**Releases**](https://github.com/Gabrick75/FlowFinance/releases).

## Como Compilar

Para compilar e executar este projeto, você precisará do Android Studio Giraffe (2023.3.1) ou mais recente.

1.  Clone o repositório:
    ```sh
    git clone https://github.com/Gabrick75/FlowFinance.git
    ```
2.  Abra o projeto no Android Studio.
3.  Aguarde o Gradle sincronizar todas as dependências.
4.  Execute o módulo `app` em um emulador ou dispositivo físico.

## Créditos

Desenvolvido por **Gabrick75.**
