# Guia de Configuração de Assinatura do APK

## Passo 1: Gerar o Keystore (Arquivo de Assinatura)

### Via Android Studio:
1. **Build** → **Generate Signed Bundle / APK**
2. Selecione **APK** e clique em **Next**
3. Clique em **Create new...**
4. Preencha os campos:
   - **Key store path**: Escolha onde salvar (ex: `/home/rodrigodevmt/Documentos/teste/caixa combo/app/release.keystore`)
   - **Password**: Crie uma senha forte (anote!)
   - **Key alias**: `caixacombo` (ou o nome que preferir)
   - **Key password**: Crie uma senha forte (anote!)
   - **Certificate**: Preencha com seus dados
5. Clique em **OK** e depois em **Next**
6. Selecione **release** e clique em **Finish**

### Via Terminal (keytool):
```bash
keytool -genkey -v -keystore app/release.keystore -alias caixacombo -keyalg RSA -keysize 2048 -validity 10000
```

**⚠️ IMPORTANTE:** Anote todas as senhas em lugar seguro! Se perder, não poderá atualizar o app na Play Store.

---

## Passo 2: Configurar local.properties

Edite o arquivo `local.properties` e adicione:

```properties
sdk.dir=/home/rodrigodevmt/Android/Sdk

# Configuração de assinatura do APK
RELEASE_KEYSTORE_FILE=app/release.keystore
RELEASE_KEYSTORE_PASSWORD=sua_senha_keystore
RELEASE_KEY_ALIAS=caixacombo
RELEASE_KEY_PASSWORD=sua_senha_alias
```

**Substitua:**
- `sua_senha_keystore` pela senha do keystore
- `caixacombo` pelo alias que você criou
- `sua_senha_alias` pela senha do alias

---

## Passo 3: Gerar APK Assinado

### Via Android Studio:
1. **Build** → **Generate Signed Bundle / APK**
2. Selecione **APK** → **Next**
3. Selecione o keystore que você criou
4. Preencha as senhas
5. Selecione **release** → **Finish**

### Via Terminal:
```bash
cd "/home/rodrigodevmt/Documentos/teste/caixa combo"
./gradlew assembleRelease
```

O APK assinado será gerado em:
```
app/build/outputs/apk/release/app-release.apk
```

---

## Passo 4: Verificar Assinatura (Opcional)

Para verificar se o APK está assinado:
```bash
jarsigner -verify -verbose -certs app/build/outputs/apk/release/app-release.apk
```

---

## Exemplo Completo de local.properties

```properties
# SDK Android
sdk.dir=/home/rodrigodevmt/Android/Sdk

# Assinatura do APK - MANTENHA ESTE ARQUIVO PRIVADO!
# NÃO COMMITAR NO GITHUB
RELEASE_KEYSTORE_FILE=app/release.keystore
RELEASE_KEYSTORE_PASSWORD=MinhaSenhaForte123!
RELEASE_KEY_ALIAS=caixacombo
RELEASE_KEY_PASSWORD=OutraSenhaForte456!
```

---

## ⚠️ SEGURANÇA IMPORTANTE

1. **NUNCA** commitar o arquivo `local.properties` no GitHub (já está no .gitignore)
2. **NUNCA** commitar o arquivo `.keystore` (já está no .gitignore)
3. Faça **backup** do keystore em local seguro
4. Se perder o keystore ou as senhas, **NÃO** poderá atualizar o app na Play Store

---

## Solução de Problemas

### Erro: "Keystore file not found"
- Verifique se o caminho em `RELEASE_KEYSTORE_FILE` está correto
- Use caminho relativo: `app/release.keystore`

### Erro: "Incorrect keystore password"
- Verifique se a senha em `RELEASE_KEYSTORE_PASSWORD` está correta

### Erro: "Key alias not found"
- Verifique se o alias em `RELEASE_KEY_ALIAS` está correto
- Use `keytool -list -v -keystore app/release.keystore` para listar os aliases

---

## Dicas Adicionais

### Para desenvolvimento (debug):
O Android Studio gera automaticamente um keystore de debug em:
```
~/.android/debug.keystore
```
Não precisa configurar nada para builds de debug.

### Para múltiplos desenvolvedores:
Cada desenvolvedor deve ter seu próprio `local.properties` com suas configurações.
O keystore deve ser compartilhado apenas entre desenvolvedores confiáveis.
