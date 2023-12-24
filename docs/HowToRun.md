# How To Run

this project

There are three different ways to run the project that I'll be discussing here.

- docker compose
- regular docker
- gradle

I'll list details about each below.

## Docker Compose

You'll need to have Docker installed on your system. Docker Compose is typically bundled with Docker, but you should
verify its availability.

Run

   ```bash
   docker-compose --version
   ```

If you have a version, great! It should just work. If not, you'll need to install it. Docker Desktop is the easiest way,
if you don't want to do it that way you'll have to refer to their [docs](https://docs.docker.com/compose/install/).

## Regular Docker

You need to build and then run the file.

Same as the previous section, make sure you have Docker installed.

```bash
docker --version
```

If not, go to their website, so you can download the engine.

Once you have it installed, you need to run two commands.

```bash
docker build -t app .
```

and

```bash
docker run -p 8080:8080 app
```

That should spin the server.

## gradlew

Finally, the default way during development to spin up the server is to just run

```bash
./gradlew bootRun
```