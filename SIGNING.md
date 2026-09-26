# Loop Dots: assinatura e instalação

A partir da versão 2.1, somente APKs de release assinados com a mesma chave persistente são publicados no GitHub Releases. A ação **falha** se os segredos da assinatura não estiverem presentes. O arquivo de distribuição é `LoopDots-v1.0.8.apk`, diretamente baixável sem ZIP.

## Configuração inicial (uma vez)

No repositório, vá a **Settings → Secrets and variables → Actions → New repository secret** e configure estes quatro valores (obtidos do arquivo privado `credentials.txt` entregue ao proprietário, não adicione o arquivo ao repositório):

- `LOOP_DOTS_KEYSTORE_BASE64`: conteúdo completo em uma linha.
- `LOOP_DOTS_STORE_PASSWORD`: senha da chave.
- `LOOP_DOTS_KEY_ALIAS`: `loopdots`.
- `LOOP_DOTS_KEY_PASSWORD`: senha da chave (igual à senha do store para PKCS12).

Guarde `loopdots-upload.jks` e `credentials.txt` fora do GitHub, em backup seguro. **Não commite a chave ou as senhas**. Perder a chave significa perder a capacidade de atualizar instalações assinadas por ela. Após registrar os quatro segredos, execute **Actions → Build and release Loop Dots APK → Run workflow**. Aguarde a compilação e baixe em **Releases → v1.0.8 → Assets**. Atualizações subsequentes devem conservar o mesmo `applicationId`, aumentar `versionCode` e usar esta mesma chave.

## Migração a partir de versões 1.x/2.0 debug

As compilações anteriores foram feitas em runners efêmeros e não compartilham uma assinatura de confiança com a nova release. Neste caso, desinstale a versão debug **uma única vez**, instale `LoopDots-v1.0.8.apk` assinado e, daí em diante, instale novas releases sem desinstalar. A desinstalação apaga os dados locais; faça backup antes se os dados importarem. Um ZIP de Actions não deve ser aberto diretamente pelo instalador Android.

Se o app não instalar mesmo após a migração, inspecione o erro com `adb install -r LoopDots-v2.1.apk`; não assuma que a compilação bem-sucedida prova a instalação ou o funcionamento do widget.
