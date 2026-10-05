# Movies Service Console — frontend

Моковый React-клиент для `moviesApi.yaml` и `oscarApi.yaml`. Все изменения живут в памяти браузера и сбрасываются после перезагрузки страницы.

## Локальный запуск

```bash
npm install
npm run dev
```

Приложение откроется на `http://localhost:5173`.

## Docker

```bash
docker build -t filmoteca-frontend .
docker run --rm -p 8088:80 filmoteca-frontend
```

После запуска откройте `http://localhost:8088`.

## Сборка

```bash
npm run build
```
