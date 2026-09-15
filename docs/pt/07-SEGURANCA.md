# FlowFinance — Notas de Segurança

Este documento descreve a postura de segurança atual do app. Há um relatório de auditoria dedicado (título *Relatório de Auditoria de Segurança*) em `docs/security-audit/relatorio-auditoria-seguranca.pdf` (regenerável via `gerar_relatorio.py`).

## 1. Modelo de ameaças em resumo

- **Sem conta, sem servidor, sem rede.** Os dados nunca saem do dispositivo, exceto pelo compartilhamento iniciado pelo usuário (backup `.flowbackup`, planilha `.xls`, CSV, charts PNG).
- **Dados sensíveis**: transações financeiras, categorias, nome do usuário, moeda/preferências — tudo em SQLite e um arquivo DataStore não criptografados no dispositivo.
- **A exposição vem de:** acesso físico ao aparelho, Android Auto Backup / backups em nuvem, e quem receber um arquivo de backup.

## 2. Criptografia (`util/CryptoUtils.kt`)

Dois caminhos de chave independentes:

### a) Criptografia de backup por senha (exportação/importação para o usuário)
```
PBKDF2WithHmacSHA256(senha, salt 16B, 65536 iterações) → chave AES-256
AES/GCM/NoPadding (tag de 128 bits, IV 12B)
saída = Base64( salt ‖ iv ‖ texto_cifrado )
```
- `encrypt(data, password)` / `decrypt(encryptedDataString, password)`.
- Usado por `SettingsViewModel.exportBackup()` / `importBackup()`.
- GCM fornece **criptografia autenticada** — backups corrompidos/adulterados falham na descriptografia (verificação da tag GCM).

### b) Criptografia vinculada ao dispositivo (backups automáticos — sem senha)
- Chave AES-256/GCM gerada no **AndroidKeyStore** sob o alias `flowfinance_backup_key`.
- `KeyGenParameterSpec`: PURPOSE_ENCRYPT|DECRYPT, GCM, sem padding, 256 bits. A chave é **não exportável** (com suporte de hardware onde o dispositivo permitir).
- `encryptWithKeystoreKey(data): ByteArray` retorna `iv ‖ ciphertext`; `decryptWithKeystoreKey(data)` separa o IV.
- Usado por `BackupWorker` (backup CSV semanal automático).

### Parâmetros de segurança
| Parâmetro | Valor |
|-----------|-------|
| KDF | PBKDF2WithHmacSHA256 |
| Iterações | 65 536 |
| Salt | 16 bytes (SecureRandom) |
| Cipher | AES/GCM/NoPadding |
| Tamanho da chave | 256 bits |
| IV | 12 bytes (SecureRandom) |
| Tag GCM | 128 bits |

## 3. Backups

- **Exportação/importação manual** (`SettingsViewModel`): `BackupPayload` serializado com Gson (transações + categorias + prefs do usuário) criptografado com a senha do usuário; embrulhado em `BackupFile { metadata, encryptedData }`; gravado em arquivo `.flowbackup` e compartilhado via FileProvider.
- **Fluxo de importação**: abrir documento → `getBackupMetadata` lê apenas os metadados não criptografados (versão / data, mostrados no diálogo) → o usuário informa a senha → `importBackup` valida a forma, descriptografa e **substitui** todos os dados atuais (com aviso na UI).
- **Backup semanal automático** (`BackupWorker`): CSV + criptografia AndroidKeyStore, gravado em `filesDir` interno do app (não visível ao usuário). O escape de campos do CSV neutraliza injeção de fórmula em planilhas (`=`, `+`, `-`, `@` → prefixados com `'`).

## 4. Android Auto Backup

`res/xml/backup_rules.xml` + `res/xml/data_extraction_rules.xml` regem os backups em nuvem:
- O **banco Room (`flowfinance_db`)** e o **arquivo DataStore** estão **excluídos** do auto-backup do Android (commit `b36615d`), impedindo que dados financeiros em texto claro sejam copiados para backups em nuvem.

## 5. Compartilhamento e permissões de arquivo

- `FileProvider` configurado com autoridade `${applicationId}.provider` e mapa de caminhos `res/xml/file_paths.xml`.
- O commit recente `4375570` estreitou o escopo externo do FileProvider (apenas arquivos internos / external-files).
- Compartilhamentos são fire-and-forget via `ACTION_SEND`; os receptores recebem content URIs com grant de leitura.

## 6. Notificações

- Permissão `POST_NOTIFICATIONS` solicitada em runtime no Android 13+ (`MainActivity.requestPermissionLauncher`); `NotificationWorker`/`NotificationHelper` checam `checkSelfPermission` antes de exibir.
- Canal `flow_finance_channel` ("Lembretes e Alertas", IMPORTANCE_DEFAULT) criado no API 26+.
- PendingIntents são **FLAG_IMMUTABLE**.

## 7. Endurecimento / ofuscação (build release)

- Build release: `isMinifyEnabled = true`, `isShrinkResources = true`, ProGuard `app/proguard-rules.pro`.
- Regras de keep:
  - Campos das entidades Room (`com.flowfinance.app.data.local.entity.**`) — para mapeamento dos DAOs + Gson.
  - Campos dos modelos de backup/Gson (`data.local.model.**`, `data.preferences.UserData`).
  - `keepattributes Signature, *Annotation*` e keep de `TypeToken` para o Gson.
- Observação: o Gson pode ser tratado com modo full do R8 no futuro; as regras atuais mantêm os membros necessários.

## 8. Limpeza de dados

- `SettingsScreen` "Limpar Dados": ação destrutiva protegida por um `AlertDialog` que exige digitar a frase de confirmação `i am sure` (sem diferenciar maiúsculas) antes de habilitar o botão vermelho de confirmar.
- `clearAllData()` em `SettingsViewModel` exclui todas as categorias customizadas + todas as transações (as padrão são preservadas).

## 9. Fraquezas conhecidas / recomendações (resumo)

- **Banco em repouso não criptografado.** Todos os dados financeiros estão em SQLite em texto claro. Considere SQLCipher ou criptografia de disco com chave no Keystore se o app mirar usuários de maior risco.
- **Sem trava de app / biometria.** Qualquer pessoa com o dispositivo desbloqueado lê tudo.
- **Iterações PBKDF2 (65536)** são moderadas; para postura mais forte, prefira ≥ 200k em novos backups por senha, ou migre para Argon2id (indisponível no Android nativo sem bibliotecas).
- **Inconsistência de `dbVersion`**: `BackupMetadata.dbVersion` grava `3` enquanto `AppDatabase` está na versão 2 (o WIP das recorrentes precisará reconciliar).
- **Captura de tela** permitida por padrão — aparelhos sensíveis podem querer `FLAG_SECURE`.
- **Premissa de usuário único**: exportar/importar com substituição total pode perder dados se usado com descuido.
- A feature de recorrentes (WIP) trará mais mudanças de versão do banco — continue aplicando migrações explícitas em vez de fallback destrutivo.