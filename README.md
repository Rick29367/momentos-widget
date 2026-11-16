# Momentos ❤️

Aplicativo Android nativo para registrar datas importantes e acompanhar contagens crescentes e regressivas.

## Primeira versão
- Cadastro de datas com título, emoji, dia e horário.
- Contagem crescente ou regressiva.
- Widget Android redimensionável na tela inicial.
- Dados salvos localmente no aparelho.
- Build automático do APK pelo GitHub Actions.

## Como gerar o APK
Abra **Actions > Build Android APK > Run workflow**. Ao terminar, baixe o artefato **Momentos-debug-apk**.

## Nota sobre atualização
O Android limita atualizações periódicas de widgets para preservar bateria. Nesta versão o widget atualiza periodicamente (intervalo de 30 minutos) e quando o aplicativo salva ou exclui uma data. O aplicativo mostra a contagem detalhada com segundos ao ser aberto. Próximas versões podem adicionar seleção de qual data cada instância do widget exibe, estilos e atualização visual mais frequente respeitando os limites do Android.

## Requisitos
Android 8.0 ou superior.
