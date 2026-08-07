# HTTPS no backend

## Arquitetura

```
Internet (HTTPS) → VM pública (147.15.72.190)
                    ├── NGINX (:443 TLS, :80 redirect→443)
                    │     └── proxy_pass → app:8080 (HTTP, rede interna Docker)
                    └── Spring Boot (:8080, HTTP, apenas rede interna)
```

- **Domínio**: `geniofc.duckdns.org` (DuckDNS gratuito)
- **Certificado**: Let's Encrypt (renovação automática via cron)
- **Frontend Angular**: projeto separado, será hospedado na Vercel (HTTPS próprio)

## Arquivos relevantes

| Arquivo | Função |
|---------|--------|
| `deploy/nginx.conf` | Config do NGINX: TLS, proxy reverso, HSTS, redirect HTTP→HTTPS |
| `deploy/docker-compose.yml` | Compose de produção (app + nginx, sem postgres) |
| `.github/workflows/deploy.yml` | CI/CD: SCP dos arquivos de deploy → docker compose up |

## Deploy

O CI (`push` na `main`/`master`) faz:
1. Build + push da imagem Docker para o Docker Hub
2. SCP de `deploy/nginx.conf` e `deploy/docker-compose.yml` para `/home/ubuntu/` na VM
3. `docker compose up -d` com `DOCKER_USERNAME=<user>` como env var

## Configuração na VM

### Arquivos gerenciados

| Caminho | Propósito |
|---------|-----------|
| `/home/ubuntu/nginx.conf` | Copiado pelo CI |
| `/home/ubuntu/docker-compose.yml` | Copiado pelo CI |
| `/home/ubuntu/gerador-times.env` | Variáveis da aplicação (editado manualmente) |
| `/home/ubuntu/certbot/conf/` | Certificados Let's Encrypt |
| `/home/ubuntu/certbot/www/` | Desafio HTTP webroot |

### Crontab do host

```cron
# Renovação do certificado (diário às 3h — só age se <30 dias da expiração)
0 3 * * * docker run --rm -v /home/ubuntu/certbot/conf:/etc/letsencrypt -v /home/ubuntu/certbot/www:/var/www/certbot certbot/certbot renew --webroot -w /var/www/certbot --quiet --deploy-hook "docker exec nginx nginx -s reload"

# Renovação do domínio DuckDNS (dia 1 de cada mês)
0 4 1 * * curl -s "https://www.duckdns.org/update?domains=geniofc&token=<TOKEN>&ip="
```

## Headers de segurança

Todos os headers de segurança são definidos **apenas pelo NGINX** (`deploy/nginx.conf`), exceto `Content-Security-Policy` que vem do Spring (`SecurityConfig.java`). Headers duplicados foram removidos do Spring para evitar conflitos.

## Variáveis de ambiente relevantes (`.env` da VM)

```properties
APP_CORS_ALLOWED_ORIGINS=https://geniofc.duckdns.org
AUTH_COOKIE_SECURE=true
AUTH_COOKIE_SAMESITE=Strict
SPRING_PROFILES_ACTIVE=prod
```

## Troubleshooting

```bash
# Ver status dos containers
docker ps

# Logs
docker logs gerador-times-app
docker logs nginx

# Testar HTTPS
curl -I https://geniofc.duckdns.org

# Testar renovação (dry-run)
docker run --rm \
  -v /home/ubuntu/certbot/conf:/etc/letsencrypt \
  -v /home/ubuntu/certbot/www:/var/www/certbot \
  certbot/certbot renew --dry-run

# Recarregar NGINX após renovação manual
docker exec nginx nginx -s reload

# Reimplantar manualmente
DOCKER_USERNAME=<user> docker compose -f /home/ubuntu/docker-compose.yml up -d
```

## Observações

- O PostgreSQL roda em **outra instância** (não está no compose de produção)
- O NGINX **não sobe se os certificados não existirem**. Em caso de VM limpa, gere o certificado primeiro com certbot standalone
- Docker Compose V2 (`docker compose`, plugin) é necessário na VM
- `AUTH_COOKIE_SECURE=true` é obrigatório para cookies funcionarem em produção com HTTPS
