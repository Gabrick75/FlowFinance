# FlowFinance

FlowFinance é um aplicativo moderno e poderoso de gestão financeira pessoal para Android.  
Mais do que um simples controle de despesas, ele oferece **análises financeiras avançadas**, ajudando o usuário a compreender profundamente seus ganhos, gastos e a evolução da sua vida financeira ao longo do tempo.

O aplicativo é desenvolvido seguindo as **práticas modernas de desenvolvimento Android**, com uma base de código **100% em Kotlin** e uma interface totalmente declarativa construída com **Jetpack Compose**.

---

### Captura de Tela Principal
<p align="center">
  <img src="docs/images/main_screenshot.jpg" alt="App Screenshot" width="300"/>
</p>

---

### Mais Capturas de Tela
<table>
  <tr>
    <td><img src="docs/images/screenshot1.png" alt="Screenshot 1"></td>
    <td><img src="docs/images/screenshot2.png" alt="Screenshot 2"></td>
  </tr>
  <tr>
    <td><img src="docs/images/screenshot3.png" alt="Screenshot 3"></td>
    <td><img src="docs/images/screenshot4.png" alt="Screenshot 4"></td>
  </tr>
</table>

---

## Sobre o Projeto

O FlowFinance foi criado para oferecer **controle total e visibilidade completa** sobre as finanças pessoais.  
Ele permite o registro detalhado de receitas e despesas, categorização inteligente de transações e geração automática de insights financeiros relevantes.

Com a chegada do **FlowFinance v2.0.0**, o aplicativo evolui para uma **plataforma completa de análise financeira**, incorporando painéis avançados, métricas de gastos, análises por categoria e resumos históricos detalhados.

Toda a operação do sistema é baseada em uma **arquitetura totalmente reativa**, onde qualquer alteração nos dados financeiros é refletida instantaneamente na interface, gráficos e totais — sem necessidade de atualizações manuais.

---

## Primeiros Passos: Um Rápido Tutorial

1. **Adicione sua Primeira Transação**  
   Toque no botão `+` na tela Inicial ou no Histórico.

2. **Preencha os Detalhes**  
   Informe o valor, a descrição, a categoria e a data da transação.

3. **Explore Suas Finanças**  
   Veja seu saldo, gráficos e análises serem atualizados instantaneamente no Dashboard.

4. **Personalize sua Experiência**  
   Acesse a aba **Configurações** para definir idioma, moeda, tema e opções de backup.

---

## Principais Funcionalidades

- **Painel Financeiro Avançado**
    - Evolução do patrimônio, receitas e despesas.
    - Comparativo entre salário e rendimentos.
    - Resumos financeiros mensais e históricos.

- **Análise de Gastos**
    - Médias de gasto diário, semanal e mensal.
    - Identificação de picos de gastos por dia da semana e do mês.
    - Heatmap semanal para visualização de intensidade.
    - Separação entre gastos recorrentes e ocasionais.

- **Insights por Categoria**
    - Gráfico de pizza com distribuição de gastos.
    - Ranking de categorias com maior consumo.
    - Evolução mensal por categoria.
    - Gráfico de área empilhada para composição de despesas.

- **Histórico de Transações**
    - Lista cronológica completa.
    - Cabeçalhos fixos (`sticky headers`) por data.
    - Exclusão rápida com gesto de deslizar.

- **Exportação e Backup**
    - Exportação de todos os dados para arquivos `.XLS`.
    - Importação e exportação de backups criptografados.
    - Backups automáticos semanais via WorkManager.

- **Internacionalização (i18n)**
    - Suporte a **Português (Brasil)**, **Inglês** e **Espanhol**.
    - Seleção manual ou automática baseada no idioma do sistema.

- **Personalização do Usuário**
    - Definição de nome de usuário.
    - Escolha de moeda (BRL, USD, EUR) com atualização imediata.
    - Suporte a temas Claro e Escuro.

- **Gerenciamento Seguro de Dados**
    - Opção de limpar dados com confirmação para evitar exclusões acidentais.

---

## Stack Técnica e Arquitetura

Este projeto demonstra o uso de **boas práticas modernas de desenvolvimento Android**, com foco em escalabilidade, manutenção e performance.

### Core
- **Linguagem**: 100% [Kotlin](https://kotlinlang.org/)
- **UI**: [Jetpack Compose](https://developer.android.com/jetpack/compose)
- **Programação Assíncrona**: [Kotlin Coroutines & Flow](https://kotlinlang.org/docs/coroutines-guide.html)

### Arquitetura
- **Padrão**: MVVM (Model-View-ViewModel)
- **Injeção de Dependência**: [Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
- **Navegação**: [Jetpack Navigation para Compose](https://developer.android.com/jetpack/compose/navigation)

### Dados
- **Persistência**: [Room Database](https://developer.android.com/training/data-storage/room)
- **Preferências do Usuário**: [Jetpack DataStore](https://developer.android.com/topic/libraries/architecture/datastore)
- **Tarefas em Segundo Plano**: [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)

---

## Download

Você pode baixar a versão instalável mais recente do aplicativo na página de  
[**Releases**](https://github.com/Gabrick75/FlowFinance/releases).

---

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
