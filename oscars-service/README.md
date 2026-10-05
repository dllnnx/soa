# Oscars service

Второй сервис из `oscarApi.yaml`: Spring MVC REST, WAR для WildFly. Джобы и их статусы хранятся только в `ConcurrentHashMap` и пропадают при перезапуске контейнера.

## Сборка

Из корня репозитория:

```bash
mvn -pl oscars-service -am clean package
```

WAR появится в `oscars-service/target/oscar.war`. При развёртывании под именем `oscar.war` внешний base path будет `/oscar`.

## Docker

```bash
docker build -f oscars-service/Dockerfile -t oscars-service .
docker run --rm \
  --name oscars \
  --network soa \
  -e MOVIES_SERVICE_URL=http://movies:8080 \
  -p 8081:8080 \
  oscars-service
```

`MOVIES_SERVICE_URL` — адрес первого сервиса без завершающего `/`. Между контейнерами Oscars обращается к Movies по HTTP. Таймауты можно менять через `MOVIES_CONNECT_TIMEOUT` и `MOVIES_READ_TIMEOUT`, например `5s` и `1m`.

Проверка:

```bash
curl -i -X POST http://localhost:8081/oscar/movies/reward-r
curl -i http://localhost:8081/oscar/jobs/<job-id>/status
```

## Подключение HTTPS позже

Текущий Dockerfile намеренно поднимает HTTP. Чтобы выполнить требование по входящему HTTPS:

1. Создать PKCS#12 с самоподписанным сертификатом, SAN которого содержит DNS-имя и IP сервера.
2. Передать `.p12` и пароль в контейнер через Docker secret или read-only volume, не добавляя их в образ и Git.
3. В WildFly Elytron создать `key-store`, `key-manager` и `server-ssl-context`.
4. В Undertow добавить `https-listener` на `8443`, связанный с созданным `server-ssl-context`.
5. Удалить или отключить `http-listener`, не публиковать порт `8080`, публиковать только `8443`.
6. Добавить сертификат в trust store браузера/клиента либо явно настроить доверие к нему.
7. Проверить, что `http://.../oscar/...` недоступен, а `https://.../oscar/...` работает.

Исходящий адрес Movies останется HTTP в `MOVIES_SERVICE_URL`, как требуется для текущего развёртывания.
