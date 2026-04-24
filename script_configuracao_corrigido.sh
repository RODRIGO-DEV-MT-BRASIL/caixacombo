#!/bin/bash

# Script Unificado de Configuração Completa
# Windows e Linux - Tudo em um único lugar
# VERSÃO CORRIGIDA - Mantém acesso à internet

echo "=== Script Unificado de Configuração Completa ==="
echo ""

# Verificar se está rodando como root
if [ "$EUID" -ne 0 ]; then
    echo "Este script precisa ser executado como root (sudo)"
    exit 1
fi

# Detectar usuário real (quando rodando com sudo)
if [ -n "$SUDO_USER" ] && [ "$SUDO_USER" != "root" ]; then
    REAL_USER="$SUDO_USER"
    REAL_HOME="$(getent passwd "$SUDO_USER" | cut -d: -f6)"
else
    REAL_USER="$USER"
    REAL_HOME="$HOME"
fi

# Função 1: Configurar TV Panasonic
configurar_tv_panasonic() {
    echo ""
    echo "=== Configurando TV Panasonic ==="
    apt update
    apt install -y intel-media-va-driver i965-va-driver mesa-utils xcvt read-edid edid-decode
    
    tee /etc/X11/xorg.conf << 'EOF'
Section "Monitor"
    Identifier "Panasonic-TV"
    ModelName "Panasonic-TV"
    VendorName "MEI"
    DisplaySize 1600 900
    HorizSync 15-68
    VertRefresh 23-61
    Option "DPMS" "false"
    ModeLine "1920x1080_60" 148.500 1920 2448 2492 2640 1080 1084 1089 1125 +hsync +vsync
    ModeLine "1280x720_60" 74.250 1280 1720 1760 1980 720 725 730 750 +hsync +vsync
    Option "PreferredMode" "1920x1080_60"
EndSection

Section "Device"
    Identifier "Intel Graphics"
    Driver "intel"
    Option "AccelMethod" "sna"
    Option "TearFree" "true"
EndSection

Section "Screen"
    Identifier "Screen0"
    Device "Intel Graphics"
    Monitor "Panasonic-TV"
    DefaultDepth 24
    SubSection "Display"
        Depth 24
        Modes "1920x1080_60" "1280x720_60" "1280x1024"
    EndSubSection
EndSection
EOF
    
    # xrandr deve rodar no display do usuário, não como root diretamente
    if [ -n "$REAL_USER" ]; then
        DISPLAY=:0 XAUTHORITY="$REAL_HOME/.Xauthority" su - "$REAL_USER" -c \
            'xrandr --newmode "1920x1080_60" 148.500 1920 2448 2492 2640 1080 1084 1089 1125 +hsync +vsync' 2>/dev/null || true
        DISPLAY=:0 XAUTHORITY="$REAL_HOME/.Xauthority" su - "$REAL_USER" -c \
            'xrandr --addmode HDMI-1 1920x1080_60' 2>/dev/null || true
        DISPLAY=:0 XAUTHORITY="$REAL_HOME/.Xauthority" su - "$REAL_USER" -c \
            'xrandr --newmode "1280x720_60" 74.250 1280 1720 1760 1980 720 725 730 750 +hsync +vsync' 2>/dev/null || true
        DISPLAY=:0 XAUTHORITY="$REAL_HOME/.Xauthority" su - "$REAL_USER" -c \
            'xrandr --addmode HDMI-1 1280x720_60' 2>/dev/null || true
    fi
    
    echo "Configuração da TV concluída. Reinicie o sistema para aplicar as mudanças."
}

# Função 2: Instalar desenvolvimento e segurança
instalar_desenvolvimento_seguranca() {
    echo ""
    echo "=== Instalando desenvolvimento e segurança ==="
    
    apt update
    
    # Instalar dependências básicas primeiro
    apt install -y curl openjdk-21-jdk wget unzip
    
    # Instalar SDKMAN para Java
    if [ ! -d "$REAL_HOME/.sdkman" ]; then
        su - "$REAL_USER" -c 'curl -s "https://get.sdkman.io" | bash'
    fi

    # Instalar Java 21 LTS via SDKMAN (fallback para OpenJDK do sistema)
    su - "$REAL_USER" -c 'source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk install java 21.0.1-tem || true && sdk default java 21.0.1-tem || true'
    
    # Configurar JAVA_HOME se SDKMAN falhou
    if [ ! -f "$REAL_HOME/.sdkman/candidates/java/current/bin/java" ]; then
        JAVA_HOME_PATH="/usr/lib/jvm/java-21-openjdk-amd64"
        add_to_bashrc "export JAVA_HOME=$JAVA_HOME_PATH"
        add_to_bashrc "export PATH=\$PATH:\$JAVA_HOME/bin"
    fi

    # Instalar NVM para Node.js
    if [ ! -d "$REAL_HOME/.nvm" ]; then
        su - "$REAL_USER" -c 'curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.7/install.sh | bash'
    fi

    su - "$REAL_USER" -c 'export NVM_DIR="$HOME/.nvm" && [ -s "$NVM_DIR/nvm.sh" ] && \. "$NVM_DIR/nvm.sh" && nvm install --lts && nvm use --lts && nvm alias default lts/*'

    su - "$REAL_USER" -c 'export NVM_DIR="$HOME/.nvm" && [ -s "$NVM_DIR/nvm.sh" ] && \. "$NVM_DIR/nvm.sh" && npm install -g npm@latest typescript @angular/cli create-react-app vite'

    # Instalar Kotlin
    su - "$REAL_USER" -c 'source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk install kotlin || true'

    # Instalar Android SDK
    apt install -y wget unzip
    ANDROID_SDK_DIR="$REAL_HOME/Android/sdk"
    mkdir -p "$ANDROID_SDK_DIR"

    # Tentar baixar a versão mais recente dos commandlinetools
    CMDLINE_URL="https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
    wget "$CMDLINE_URL" -O /tmp/commandlinetools.zip || \
        wget "https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip" -O /tmp/commandlinetools.zip
    unzip -q /tmp/commandlinetools.zip -d /tmp/cmdline-tools-tmp
    mkdir -p "$ANDROID_SDK_DIR/cmdline-tools/latest"
    mv /tmp/cmdline-tools-tmp/cmdline-tools/* "$ANDROID_SDK_DIR/cmdline-tools/latest/" 2>/dev/null || \
        mv /tmp/cmdline-tools-tmp/* "$ANDROID_SDK_DIR/cmdline-tools/latest/" 2>/dev/null || true
    rm -f /tmp/commandlinetools.zip
    rm -rf /tmp/cmdline-tools-tmp

    export ANDROID_HOME="$ANDROID_SDK_DIR"
    export ANDROID_SDK_ROOT="$ANDROID_HOME"
    export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin
    export PATH=$PATH:$ANDROID_HOME/platform-tools

    yes | sdkmanager --licenses
    sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"

    # Configurar variáveis no .bashrc do usuário real (sem duplicatas)
    BASHRC="$REAL_HOME/.bashrc"
    add_to_bashrc() {
        local line="$1"
        grep -Fxq "$line" "$BASHRC" 2>/dev/null || echo "$line" >> "$BASHRC"
    }

    add_to_bashrc 'export ANDROID_HOME="$HOME/Android/sdk"'
    add_to_bashrc 'export ANDROID_SDK_ROOT="$ANDROID_HOME"'
    add_to_bashrc 'export PATH="$PATH:$ANDROID_HOME/cmdline-tools/latest/bin"'
    add_to_bashrc 'export PATH="$PATH:$ANDROID_HOME/platform-tools"'
    add_to_bashrc 'export NVM_DIR="$HOME/.nvm"'
    add_to_bashrc '[ -s "$NVM_DIR/nvm.sh" ] && \. "$NVM_DIR/nvm.sh"'
    add_to_bashrc 'export SDKMAN_DIR="$HOME/.sdkman"'
    add_to_bashrc '[[ -s "$HOME/.sdkman/bin/sdkman-init.sh" ]] && source "$HOME/.sdkman/bin/sdkman-init.sh"'
    
    # Configurar segurança básica (sem bloquear internet)
    apt install -y ufw fail2ban clamav rkhunter chkrootkit
    
    # Firewall básico que permite internet
    ufw default deny incoming
    ufw default allow outgoing
    ufw allow ssh
    ufw allow 80/tcp
    ufw allow 443/tcp
    ufw --force enable
    
    systemctl enable fail2ban
    systemctl start fail2ban
    
    # Corrigir freshclam - parar serviço se estiver rodando e atualizar
    systemctl stop clamav-freshclam 2>/dev/null || true
    freshclam || (systemctl start clamav-freshclam && sleep 2 && freshclam) || true
    
    echo "Instalação de desenvolvimento e segurança concluída."
}

# Função 3: Configurar firewall avançado (CORRIGIDO - mantém internet)
configurar_firewall_avancado() {
    echo ""
    echo "=== Configurando firewall avançado (Mantendo acesso à internet) ==="
    
    apt update
    apt install -y ufw fail2ban rkhunter chkrootkit
    
    ufw --force reset
    
    # Política: Bloquear entrada, permitir saída essencial
    ufw default deny incoming
    ufw default allow outgoing
    ufw default deny routed
    
    # BLOQUEAR PORTAS DE ENTRADA PERIGOSAS
    # Bloquear RPC
    ufw deny in 111/tcp
    ufw deny in 111/udp
    ufw deny in 2049/tcp
    ufw deny in 2049/udp
    
    # Bloquear portas de ataques de entrada
    ufw deny in 23/tcp    # Telnet
    ufw deny in 21/tcp    # FTP
    ufw deny in 25/tcp    # SMTP
    ufw deny in 137/tcp   # NetBIOS
    ufw deny in 137/udp   # NetBIOS
    ufw deny in 138/tcp   # NetBIOS
    ufw deny in 138/udp   # NetBIOS
    ufw deny in 139/tcp   # NetBIOS
    ufw deny in 445/tcp   # SMB
    ufw deny in 445/udp   # SMB
    ufw deny in 135/tcp   # RPC
    ufw deny in 135/udp   # RPC
    ufw deny in 5900/tcp  # VNC
    ufw deny in 3389/tcp  # RDP
    
    # PERMITIR SERVIÇOS ESSENCIAIS DE ENTRADA
    ufw allow in ssh      # SSH
    ufw allow in 80/tcp   # HTTP (se for servidor web)
    ufw allow in 443/tcp  # HTTPS (se for servidor web)
    
    # PERMITIR TRÁFEGO DE SAÍDA ESSENCIAL PARA INTERNET
    ufw allow out 53/udp    # DNS (essencial para internet)
    ufw allow out 53/tcp    # DNS (essencial para internet)
    ufw allow out 80/tcp    # HTTP (navegação web)
    ufw allow out 443/tcp   # HTTPS (navegação segura)
    ufw allow out 22/tcp    # SSH (para conexões externas)

    # UFW já gerencia conexões estabelecidas automaticamente via iptables rules internas
    
    # Configurar logging
    ufw logging on
    ufw logging medium
    ufw --force enable
    
    # Desabilitar serviços perigosos
    systemctl stop rpcbind 2>/dev/null || true
    systemctl disable rpcbind 2>/dev/null || true
    
    # Append condicional para evitar duplicatas
    append_if_missing() {
        local line="$1"
        local file="$2"
        grep -Fxq "$line" "$file" 2>/dev/null || echo "$line" >> "$file"
    }

    append_if_missing "rpcbind:ALL" /etc/hosts.deny
    append_if_missing "portmap:ALL" /etc/hosts.deny

    # Hardening do kernel (sem bloquear internet)
    append_if_missing "net.ipv6.conf.all.disable_ipv6 = 1" /etc/sysctl.conf
    append_if_missing "net.ipv6.conf.default.disable_ipv6 = 1" /etc/sysctl.conf
    append_if_missing "net.ipv6.conf.lo.disable_ipv6 = 1" /etc/sysctl.conf
    append_if_missing "net.ipv4.conf.all.accept_source_route = 0" /etc/sysctl.conf
    append_if_missing "net.ipv4.conf.all.accept_redirects = 0" /etc/sysctl.conf
    append_if_missing "net.ipv4.conf.all.send_redirects = 0" /etc/sysctl.conf
    append_if_missing "net.ipv4.conf.all.rp_filter = 1" /etc/sysctl.conf
    append_if_missing "net.ipv4.tcp_syncookies = 1" /etc/sysctl.conf

    sysctl -p
    
    # Atualizar ferramentas de segurança
    rkhunter --update
    rkhunter --propupd
    chkrootkit
    
    echo "Configuração de firewall avançado concluída."
    echo "Acesso à internet MANTIDO com segurança."
    echo ""
    echo "Resumo das regras:"
    echo "- ENTRADA: Bloqueado (exceto SSH, HTTP, HTTPS)"
    echo "- SAÍDA: Permitido (DNS, HTTP, HTTPS, SSH)"
    echo "- Serviços perigosos desabilitados"
    echo "- Hardening do kernel aplicado"
}

# Menu principal
while true; do
    echo ""
    echo "Selecione o que deseja fazer:"
    echo "1) Configurar TV Panasonic (driver de vídeo + resolução 1920x1080)"
    echo "2) Instalar desenvolvimento e segurança (Java, Node.js, Kotlin, Android SDK, firewall)"
    echo "3) Configurar firewall avançado (SEGURANÇA + INTERNET)"
    echo "4) Executar todas as configurações (1-3)"
    echo "0) Sair"
    echo ""
    read -p "Opção: " opcao

    case $opcao in
        1)
            configurar_tv_panasonic
            ;;
        2)
            instalar_desenvolvimento_seguranca
            ;;
        3)
            configurar_firewall_avancado
            ;;
        4)
            echo ""
            echo "=== Executando todas as configurações ==="
            echo "Isso pode levar muito tempo..."
            read -p "Continuar? (s/n): " confirm
            if [ "$confirm" = "s" ]; then
                configurar_tv_panasonic
                instalar_desenvolvimento_seguranca
                configurar_firewall_avancado
                echo "=== Todas as configurações concluídas! ==="
                echo "Acesso à internet mantido com segurança."
            fi
            ;;
        0)
            echo "Saindo..."
            exit 0
            ;;
        *)
            echo "Opção inválida!"
            ;;
    esac
done
