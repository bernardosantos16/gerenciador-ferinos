# ==========================================
# Estágio 1: Build (Compilação)
# ==========================================
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copia apenas os arquivos de configuração do Maven primeiro (cache layer)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
# Baixa as dependências offline para acelerar builds futuros
RUN ./mvnw dependency:go-offline

# Copia o código-fonte e compila (executando os testes)
COPY src ./src
RUN ./mvnw clean package

# ==========================================
# Estágio 2: Produção (Imagem final leve)
# ==========================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Cria um usuário não-root por segurança (Boa prática para Nuvem/VPS)
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copia o .jar gerado no estágio anterior
COPY --from=builder /app/target/*.jar app.jar

# Expõe a porta padrão da aplicação
EXPOSE 8080

# Executa a aplicação com o perfil de produção
ENTRYPOINT ["java", "-jar", "/app/app.jar"]