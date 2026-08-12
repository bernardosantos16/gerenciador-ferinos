# ==========================================
# Estágio 1: Build
# ==========================================
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app

# Copia apenas os arquivos de configuração do Maven primeiro (cache layer)
# Assim, o Docker só reexecuta o download de dependências quando o pom.xml mudar
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x ./mvnw
RUN ./mvnw dependency:go-offline -B

# Copia o código-fonte e compila
# Testes ficam fora do build da imagem (rodam em job separado no GitHub Actions);
# aqui usamos -DskipTests para manter o build da imagem rápido e determinístico
COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# Download do OpenTelemetry Java Agent (instrumentação automática de traces)
RUN curl -L -o opentelemetry-javaagent.jar \
    https://repo1.maven.org/maven2/io/opentelemetry/javaagent/opentelemetry-javaagent/2.16.0/opentelemetry-javaagent-2.16.0.jar

# ==========================================
# Estágio 2: Produção (imagem final com glibc nativo)
# ==========================================
FROM eclipse-temurin:21-jre
WORKDIR /app

# Usuário não-root por segurança (Sintaxe Debian/Ubuntu)
RUN groupadd -r spring && useradd -r -g spring spring

# Copia o jar pelo nome fixo, independente de versão/artifactId
COPY --from=builder /app/target/*.jar app.jar
# Copia o agente de observabilidade
COPY --from=builder /app/opentelemetry-javaagent.jar opentelemetry-javaagent.jar

# Ajusta o dono dos arquivos para o usuário não-root antes de trocar de usuário
RUN chown spring:spring app.jar opentelemetry-javaagent.jar
USER spring:spring

EXPOSE 8080

# Permite tunar memória/JVM via env var sem rebuild da imagem
ENV JAVA_OPTS=""

# Profile e demais configs (DB_URL, JWT secrets etc.) entram via
# variáveis de ambiente no docker-compose/docker run, nunca hardcoded na imagem
ENTRYPOINT ["sh", "-c", "java -javaagent:/app/opentelemetry-javaagent.jar $JAVA_OPTS -jar /app/app.jar"]