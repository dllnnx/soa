# Деплой (docker compose, HTTPS)

## Состав

| Контейнер | Образ | Порт | Назначение |
|---|---|---|---|
| `postgres` | postgres:16-alpine | — | БД `movies` (schema — `postgres/init.sql`) |
| `payara` | payara/server-full:6.2024.12 | 8181 (HTTPS) | movies-service (JAX-RS), context root `/` |
| `wildfly` | quay.io/wildfly/wildfly:32.0.1.Final | 8443 (HTTPS) | oscars-service (Spring Boot WAR), context root `/oscar` |
| `frontend` | nginx:1.27-alpine | 3000 (HTTP) | SPA + reverse-proxy `/movies` → payara, `/oscar` → wildfly |

Оба сервиса — **только HTTPS** с сертификатами собственного мини-CA
(`deploy/certs/out/`): у каждого сервиса свой сертификат, подписанный общим корнем
`ca.crt`. HTTP-порты выключены (Payara 8080, WildFly 8080). Пароли хранилищ — `changeit`,
в Payara сертификат лежит под alias `s1as`.

nginx проксирует API с честной проверкой цепочки (`proxy_ssl_verify on`) через
`ca.crt`, смонтированный в `/etc/nginx/certs/`.

## Запуск

```bash
# 1. Сертификаты (если ещё нет): см. шаги генерации — ca.crt, keystore.jks, wildfly.p12
#    в deploy/certs/out/
# 2. Стек:
docker compose -f deploy/docker-compose.yml up --build -d
```

Payara поднимается 1–2 минуты (деплой WAR идёт последним asadmin-шагом, после создания JDBC-пула).

Проверка:

```bash
curl --cacert deploy/certs/out/ca.crt https://localhost:8181/movies/average-budget
curl --cacert deploy/certs/out/ca.crt https://localhost:8443/oscar/jobs/<uuid>/status
open http://localhost:3000    # SPA; API ходит через nginx с проверкой сертификатов
```

Без `--cacert` curl (как и браузер) ругнётся на неизвестный корень — это ожидаемо.
Чтобы браузер не показывал предупреждение: импортировать `ca.crt` в связку ключей
(macOS: двойной клик → «Система» → «Всегда доверять»).

## Перенос на сервер

Собираем на сервере (контекст сборки — весь репозиторий):

```bash
scp -r deploy/certs/out user@31.59.126.210:~/soa/deploy/certs/   # секреты не в git
git clone ... && docker compose -f deploy/docker-compose.yml up --build -d
```

Сертификаты валидны для `payara`, `wildfly`, `localhost` и `31.59.126.210` (SAN).

## Примечания

- `payara/server-full` публикуется только под `linux/amd64` — на Apple Silicon нужен
  Colima с Rosetta (`colima start --vz-rosetta`) или qemu (медленнее).
- Учётные данные локальные: БД `movies/movies`, хранилища `changeit`. Для продакшена вынести в env.
- oscars-service ходит на `https://payara:8181` (env `MOVIES_SERVICE_URL` в compose);
  доверие настроено импортом `ca.crt` в truststore JVM образа WildFly.
