# Origem da fonte

- Projeto: [Gathin23/cleanyourtv](https://github.com/Gathin23/cleanyourtv).
- Snapshot: arquivo ZIP do branch `main`, obtido em 24/10/2026.
- SHA-256 do ZIP: `B12598C368C494BE8B0A48634FBE9A5AF03DE6646B0C2C950BC4DA2AA5B9A778`.
- Diretório preservado: `cleanyourtv-main/`.
- Launcher: versão 2.7, versionCode 8, `minSdk` 26 e `targetSdk` 34.
- Personalização inicial: ordem e seleção do array `dock_apps` em
  `launcher/app/src/main/res/values/config.xml`, com Netflix, YouTube, Prime Video e Disney+.
- Alteração local posterior: opção `wallpaper_change_on_home`, desativada por padrão,
  lida em `Config.java` e acionada por `MainActivity`/`WallpaperPlayer`, com a política
  testável `WallpaperHomeRefresh` e oito testes JVM. Reutiliza a seleção e o decodificador
  de imagens locais existentes, sem novas permissões ou dependências.
- SHA-256 do APK da configuração inicial:
  `90DBBE01F5F81FC27D0D787519DE671AF53FFC9AE72FA77FF8938817B7F778E9`.
- SHA-256 do APK com wallpapers por eventos:
  `1D14AEBE2137118803928195FF1685192E82441851FE643F0B31CC16B5CE48AE`.

O hash identifica o snapshot utilizado. Este repositório inicia seu próprio histórico;
não reproduz o histórico de commits do projeto original nem se apresenta como projeto oficial.

## Licenças e atribuição

A licença MIT e o copyright de Gathin foram mantidos em `cleanyourtv-main/LICENSE`.
A fonte Outfit mantém o aviso SIL Open Font License 1.1 em
`cleanyourtv-main/launcher/FONT-LICENSE-OFL.txt`.

Os scripts na raiz reutilizam as ferramentas do snapshot. O perfil AIWA e a documentação
na raiz registram o trabalho realizado para a placa `stanford`.

Backups, chaves ADB, registros de recuperação, capturas privadas, SDKs, ambientes Python,
caches do Gradle, APKs e logs da sessão não fazem parte da publicação.
