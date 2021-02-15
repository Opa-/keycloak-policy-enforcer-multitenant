# Keycloak Policy-Enforcer Multi-tenant issue

This is an example project to demonstrate some issue when using Keycloak's Policy-Enforcers in a multi-tenant configuration.

## Problematic

TODO

## Build & Run

### Keycloak

This will create a Keycloak with 2 realms: `rayman` & `globox`. It is exposed on port `8081`.

Each realm look exactly the same, they both have a `bar` client configured with policy-enforcers on the route `/api/v1/foo/*`.

```sh
docker-compose up
```

### Spring-Boot

The Spring-Boot application is exposed under port `8080`.

It exposes two APIs:
- `GET /api/v1/foo`. This one is protected using Keycloak policy-enforcer configuration.
- `GET /api/v1/bar`. This one is not protected at all in order to compare.

```sh
./mvnw spring-boot:run -Dspring-boot.run.arguments=--logging.level.org.keycloak=TRACE
```

### Running the python script

In order to test, you need to create a user in both realms and assign him the realm role `ROLE_TEST`.

After doing that, you need to run the script once for each realm.
Open two terminal windows and execute the following commands.
You should notice that with 1 script running you always get a 200 response code and then when you run the script against the second realms, you'll notice 403 errors happening randomly on each realm.

```sh
python3 ./keycloak.py -keycloak_endpoint 'http://localhost:8081' -realm rayman -client_id bar -client_secret '03daa338-740e-42e8-8166-3db2b4848d4d' -username test -password test -service_endpoint 'http://localhost:8080/api/v1/foo'
python3 ./keycloak.py -keycloak_endpoint 'http://localhost:8081' -realm globox -client_id bar -client_secret '0cfa413d-63d1-4787-a9f5-0b5ac76540ec' -username test -password test -service_endpoint 'http://localhost:8080/api/v1/foo'
```
