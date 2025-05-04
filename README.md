# 🧠 Trabalho de Inteligência Artificial

## 🛠️ Compilação

Para compilar todos os arquivos `.java` com avisos de possíveis problemas no código (como usos inseguros ou métodos obsoletos), utilize:

```bash
javac -Xlint *.java
```

## ▶️ Execução

Para executar o programa principal:

```bash
java TrabalhoIA
```

## ⚠️ Observação sobre uso de memória

Se a execução envolver **muitos casos de teste** ou se os dados ultrapassarem o tamanho padrão da estrutura `HASH`, pode ocorrer erro de **falta de memória**.

Para evitar esse problema, recomenda-se aumentar o limite de memória da JVM com o parâmetro `-Xmx`. Por exemplo:

```bash
java -Xmx4g TrabalhoIA
```

> 💡 Isso permite que a JVM utilize até **4 GB de memória RAM**, ajudando a evitar falhas por **estouro de heap (heap space)**.

## 📸 Prints de Execução

*Adicione aqui imagens mostrando a execução do programa, exemplos de entrada e saída, etc.*
![image](https://github.com/user-attachments/assets/cc025566-421c-4684-a215-760bb389ea2c)


---



