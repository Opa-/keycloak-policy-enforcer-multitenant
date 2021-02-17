# The issue is now _fixed_, you can browse the pull request of the branch `fix` to check how.

# Keycloak Policy-Enforcer Multi-tenant issue

This is an example project to demonstrate some issue when using Keycloak's Policy-Enforcers in a multi-tenant configuration.

## Problematic

It seems that Keycloak Spring-Boot adapter's policy-enforcer, used in a multi-tenant architecture, causes some requests to randomly respond with a 403 error code.

It is unclear at the moment what is the root cause of this but from what I understand so far, this could be due to some "shared" object having the policy-enforcer URL in the Spring context.

If two requests, for distinct realms, happen at the same time, there's a slight chance for one request to impact the other and having one of the request to call Keycloak policy-enforcer check on the wrong realm which result in a DENIED response from Keycloak and a 403 response from Spring.

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
# Create users
./create-test-users.sh

# Execute the test (should be executed at the same time using different terminal windows)
python3 ./keycloak-test.py -keycloak_endpoint 'http://localhost:8081' -realm rayman -client_id bar -client_secret '03daa338-740e-42e8-8166-3db2b4848d4d' -username test -password test -service_endpoint 'http://localhost:8080/api/v1/foo'
python3 ./keycloak-test.py -keycloak_endpoint 'http://localhost:8081' -realm globox -client_id bar -client_secret '0cfa413d-63d1-4787-a9f5-0b5ac76540ec' -username test -password test -service_endpoint 'http://localhost:8080/api/v1/foo'
```
