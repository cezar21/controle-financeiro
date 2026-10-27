# Meu Financeiro — projeto inicial Android

Protótipo Kotlin + Jetpack Compose com alternância visual PF/PJ, dashboard, resumos ilustrativos e formulário simples de nova despesa.

## Abrir e compilar
1. Instale o Android Studio estável e um JDK compatível (JDK 17).
2. Abra a pasta `ControleFinanceiro` no Android Studio.
3. Aguarde o Gradle Sync e instale os componentes Android SDK solicitados.
4. Para gerar o APK de depuração: **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
5. O APK normalmente ficará em `app/build/outputs/apk/debug/app-debug.apk`.

## Importante
Este é um protótipo inicial para validar a interface. Os valores de Pix/cartão e categorias são demonstrativos, a alternância PF/PJ é apenas visual e os lançamentos ficam somente na memória durante a execução. As dependências Room e Hilt estão incluídas, mas ainda não há DAOs, repositórios, isolamento persistente de perfis, faturas ou parcelamento implementados. Não use para registrar finanças reais ainda.
