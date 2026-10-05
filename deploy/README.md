# Деплой (docker compose)

## Состав

| Контейнер | Образ | Порт | Назначение |
|---|---|---|---|
| `postgres` | postgres:16-alpine | — | БД `movies` (schema — `postgres/init.sql`) |
| `payara` | payara/server-full:6.2024.12 | 8080 | movies-service (JAX-RS), context root `/` |
| `wildfly` | quay.io/wildfly/wildfly:32.0.1.Final | 8480 → 8080 | oscars-service (Spring Boot WAR), context root `/oscar` |
| `frontend` | nginx:1.27-alpine | 3000 | SPA + reverse-proxy `/movies` → payara, `/oscar` → wildfly |

## Запуск

```bash
docker compose -f deploy/docker-compose.yml up --build -d
```

Payara поднимается 1–2 минуты (деплой WAR идёт последним asadmin-шагом, после создания JDBC-пула).

Проверка:

```bash
curl http://localhost:8080/movies/average-budget        # 200 {"averageBudget":0.0}
curl http://localhost:8480/oscar/jobs/<uuid>/status     # oscars-service
open http://localhost:3000                              # SPA
```

## Примечания

- `payara/server-full` публикуется только под `linux/amd64` — на Apple Silicon нужен
  Colima с Rosetta (`colima start --vz-rosetta`) или qemu (медленнее).
- Учётные данные локальные: БД `movies/movies`. Для продакшена вынести в env.
- oscars-service ходит на `http://payara:8080` (env `MOVIES_SERVICE_URL` в compose).
- HTTPS (самоподписанный сертификат) сейчас не настроен — будет добавлен позже:
  Payara https-listener 8181, WildFly elytron + https-listener 8443, общий сертификат
  с SAN `localhost, payara, wildfly`.
