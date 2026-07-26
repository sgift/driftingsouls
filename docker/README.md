# Local instance

Serves the game on http://localhost:8080 against a MySQL container. The WAR must be built first —
**with `clean`**, or `maven-war-plugin` packages a stale exploded webapp and deleted assets reappear:

```bash
./mvnw -pl game -am clean package -DskipTests
docker compose -f docker/docker-compose.yml build app
docker compose -f docker/docker-compose.yml up -d
```

`db-init/` is auto-loaded into a *fresh* data volume: drop a `.sql`/`.sql.gz` dump in there before the
first `up`. Flyway then migrates whatever is in the database up to what the current code expects, and
runs once per `up`.

## A second stack in parallel

Nothing in the compose file is named or bound absolutely, so a second checkout — typically a git
worktree for a hotfix running alongside longer-lived work — can bring up its own stack. In the second
checkout only:

```bash
git worktree add ../ds2-hotfix master        # from the main checkout
cd ../ds2-hotfix
cp docker/.env.sample docker/.env            # sets project name + ports; edit if 8081/3307 are taken
cp ../driftingsouls/docker/db-init/dump.sql.gz docker/db-init/   # optional, for real data
./mvnw -pl game -am clean package -DskipTests
docker compose -f docker/docker-compose.yml up -d --build
```

That stack gets its own containers, its own `db-data` volume and its own ports; the two share only the
Docker daemon and the images. Loading the dump again takes a while and costs a few GB of disk, so skip
it if an empty migrated schema is enough for what you are fixing.

**Do not create a `docker/.env` in the main checkout.** Without one the project name defaults to the
directory name (`docker`), which is what the existing `docker_db-data` volume — the one holding your
loaded dump — belongs to. Setting `COMPOSE_PROJECT_NAME` there would silently start from an empty
database.

Container names follow the project name, so commands that used to name containers directly become:

```bash
docker compose -f docker/docker-compose.yml exec db mysql -uroot -pds ds
docker compose -f docker/docker-compose.yml logs -f app
```

Run them from the checkout you mean, or pass `-p <project>` from anywhere.

## Tearing down

```bash
docker compose -f docker/docker-compose.yml down          # stop, keep the database
docker compose -f docker/docker-compose.yml down -v       # ...and delete its data volume
git worktree remove ../ds2-hotfix                         # once the branch is merged
```

`down -v` deletes the data volume of *that* project. Run from the wrong checkout it will happily drop
the main instance's loaded dump, so check `docker compose ls` first if in doubt.
