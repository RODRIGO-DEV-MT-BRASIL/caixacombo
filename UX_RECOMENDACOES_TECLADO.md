# Recomendações de UX - Tela de Bloqueio Mobile

## Problema Atual
Quando usuário clica no campo de senha na tela de bloqueio, o teclado cobre o botão "Desbloquear", dificultando a experiência.

## Solução Recomendada

### 1. Comportamento ao Focar Campo de Senha
```java
// Quando campo de senha receber foco
passwordEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus) {
            // Fazer o conteúdo subir
            animateContentUp();
            // Posicionar teclado abaixo do botão
            adjustWindowSoftInputMode();
        } else {
            // Voltar ao normal quando perder foco
            animateContentDown();
        }
    }
});
```

### 2. Ajuste do Layout
```xml
<!-- Usar ScrollView para permitir movimento -->
<ScrollView
    android:id="@+id/scrollContainer"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:fillViewport="true">
    
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="16dp">
        
        <!-- Conteúdo que vai subir -->
        <ImageView
            android:id="@+id/lockIcon"
            android:layout_width="80dp"
            android:layout_height="80dp"
            android:layout_gravity="center"
            android:layout_marginBottom="24dp"/>
            
        <TextView
            android:id="@+id/lockMessage"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="Dispositivo Bloqueado"
            android:textSize="18sp"
            android:gravity="center"
            android:layout_marginBottom="32dp"/>
            
        <!-- Campo de senha -->
        <EditText
            android:id="@+id/passwordInput"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:hint="Digite a senha"
            android:inputType="numberPassword"
            android:maxLength="6"
            android:gravity="center"
            android:layout_marginBottom="16dp"/>
            
        <!-- Botão Desbloquear -->
        <Button
            android:id="@+id/unlockButton"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="Desbloquear"
            android:layout_marginBottom="32dp"/>
            
    </LinearLayout>
</ScrollView>
```

### 3. Animação Suave
```java
private void animateContentUp() {
    ScrollView scrollView = findViewById(R.id.scrollContainer);
    scrollView.post(() -> {
        // Rolar suavemente para cima para mostrar botão acima do teclado
        scrollView.smoothScrollTo(0, 200, 300);
    });
}

private void animateContentDown() {
    ScrollView scrollView = findViewById(R.id.scrollContainer);
    scrollView.post(() -> {
        // Voltar ao topo
        scrollView.smoothScrollTo(0, 0, 300);
    });
}
```

### 4. Ajuste da Janela
```java
// No onCreate da Activity
getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

// Ou usar ADJUST_PAN se preferir
getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
```

### 5. Detecção de Teclado
```java
private ViewTreeObserver.OnGlobalLayoutListener keyboardLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
    @Override
    public void onGlobalLayout() {
        Rect r = new Rect();
        getWindow().getDecorView().getWindowVisibleDisplayFrame(r);
        int screenHeight = getWindow().getDecorView().getRootView().getHeight();
        int keypadHeight = screenHeight - r.bottom;

        if (keypadHeight > screenHeight * 0.15) { 
            // Teclado visível
            animateContentUp();
        } else {
            // Teclado escondido
            animateContentDown();
        }
    }
};

// Adicionar listener
findViewById(android.R.id.content).getViewTreeObserver()
    .addOnGlobalLayoutListener(keyboardLayoutListener);
```

## Benefícios

1. **Experiência Mobile Otimizada** - Conteúdo se ajusta automaticamente
2. **Botão Sempre Visível** - Nunca fica escondido pelo teclado
3. **Animação Suave** - Transição profissional e agradável
4. **Compatibilidade** - Funciona em diferentes tamanhos de tela

## Implementação Prioritária

1. ✅ Usar `SOFT_INPUT_ADJUST_RESIZE` no AndroidManifest
2. ✅ Adicionar ScrollView no layout
3. ✅ Implementar animação ao focar campo
4. ✅ Detectar abertura/fechamento do teclado
5. ✅ Testar em diferentes dispositivos

Essa melhoria vai proporcionar uma experiência muito mais profissional e intuitiva para os usuários ao desbloquearem os terminais.
